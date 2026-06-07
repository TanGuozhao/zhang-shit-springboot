# Spec: Log Service P2 Productionized Delivery

**Author:** Codex
**Date:** 2026-06-07
**Status:** Approved
**Reviewers:** User
**Related specs:** docs/contracts/log-service-contract.md, 可复用访问日志服务设计-2024091602016-谭国照.md

## Context

当前仓库中的 `log-service` 仍是演示骨架：日志只保存在内存列表中，检索条件单一，指标是固定值，告警和导出没有真实后台流程。与此同时，原始设计文档已经把目标定义成可复用访问日志平台，要求覆盖日志接入、缓冲、存储、检索、指标、告警和导出能力。

本次 P2 交付的目标不是一次性完成完整生产集群，而是在当前 Spring Boot 多模块仓库中落下一套可运行、可测试、可继续扩展的“生产化骨架”。它必须保持 `docs/contracts/log-service-contract.md` 中已冻结的公开接口不漂移，同时在服务内部补齐 ClickHouse 适配层、异步缓冲写入、定时调度、告警规则引擎、通知通道和导出任务模型。

由于当前仓库本地开发环境默认没有外部基础设施，本方案要求服务在未连接 ClickHouse 时仍可启动和通过测试。因此实现需要同时提供可切换的数据面：启用时走 ClickHouse JDBC 适配，未启用时走内存后备实现，但二者都遵守同一服务契约和后台处理流程。

## Functional Requirements

- FR-1: 服务 MUST 保持以下公开接口路径与统一响应结构不变：`POST /api/logs/ingest`、`GET /api/logs/search`、`GET /api/logs/trace/{traceId}`、`GET /api/logs/metrics`、`GET /api/logs/alerts`、`POST /api/logs/alerts/{alertId}/status`、`POST /api/logs/exports`。
- FR-2: `POST /api/logs/ingest` MUST 接收访问日志写入请求，并在通过基础校验后先写入内存缓冲队列，再返回已受理结果，而不是同步直写存储。
- FR-3: 日志写入请求 MUST 至少包含服务名、链路标识、日志级别、消息体，并 SHOULD 支持接口路径、状态码、耗时、时间戳、请求标识、客户端 IP、扩展标签等访问日志字段。
- FR-4: 后台刷新任务 MUST 在达到批量阈值或时间窗口时批量消费缓冲队列，并调用统一的数据存储接口写入日志。
- FR-5: 数据存储层 MUST 支持两种实现：启用配置时写入 ClickHouse；未启用时写入内存后备仓储。
- FR-6: 服务 MUST 提供组合检索能力，至少支持按关键词、服务名、级别、状态码、时间范围和 traceId 过滤，并对外兼容 `GET /api/logs/search?keyword=...` 的简化查询方式。
- FR-7: `GET /api/logs/trace/{traceId}` MUST 返回同一 traceId 下按时间排序的完整访问日志列表。
- FR-8: 指标统计模块 MUST 基于已落库日志实时聚合 QPS、错误率、平均耗时、P95 耗时和总请求数，并支持按服务名过滤；对外指标键至少包含 `p95`，并 SHOULD 兼容返回 `p95Latency` 别名。
- FR-9: 服务 MUST 支持告警规则的创建、查询、启停和删除，并通过内部控制面接口暴露这些能力。
- FR-10: 后台告警评估任务 MUST 按配置周期扫描最新日志与聚合指标，在命中阈值规则时生成告警事件。
- FR-11: 告警通知模块 MUST 支持至少控制台通知和 Webhook 通知两种通道，并允许按配置启用；通知失败 MUST 记录到运行态摘要中，但 MUST NOT 阻断告警事件创建。
- FR-12: `GET /api/logs/alerts` MUST 返回告警事件列表；`POST /api/logs/alerts/{alertId}/status` MUST 支持更新事件状态。
- FR-13: `POST /api/logs/exports` MUST 创建异步导出任务，后台任务 MUST 负责生成导出结果，并提供状态流转；返回的 `downloadPath` MUST 在任务完成后可被服务本身访问。
- FR-14: 服务 MUST 通过内部控制面接口暴露缓冲队列、刷新任务、告警任务和导出任务的运行概览。
- FR-15: 所有后台处理失败 MUST 记录到服务内运行状态中，并 MUST NOT 因单次批处理失败导致整个服务不可用。
- FR-16: 实现 MUST NOT 依赖其他业务微服务补全日志数据，`log-service` 仅处理自身接收到的日志与控制面配置。

## Non-Functional Requirements

- NFR-1: `POST /api/logs/ingest` 在内存后备模式下 MUST 在 200ms 内返回（p95，单实例，批量 100 条以内）。
- NFR-2: 缓冲队列 MUST 支持至少 10,000 条待处理日志；队列满时 MUST 返回业务错误而不是无限阻塞。
- NFR-3: 后台刷新任务 MUST 默认每 5 秒执行一次，并在单次最多处理 500 条日志。
- NFR-4: 查询接口在内存后备模式下，对 5,000 条日志数据集的查询 MUST 在 500ms 内返回（p95）。
- NFR-5: 服务 MUST 支持通过配置完全关闭 ClickHouse、Webhook 通知和定时任务，以保证本地开发可运行。
- NFR-6: 所有时间字段 MUST 使用 `Instant` 语义，并统一以 UTC 存储。
- NFR-7: 缓冲刷新、告警评估和导出生成 MUST 具备幂等保护，避免同一批次因重试被重复标记为完成。
- NFR-8: 任何异常响应 MUST 继续复用 `ApiResponse` 和全局异常处理，不引入新的外层响应格式。
- NFR-9: 新增配置项 MUST 有合理默认值，使 `log-service` 在无外部依赖下可直接启动并通过测试。

## Acceptance Criteria

### AC-1: 日志异步入队成功 (FR-2, FR-3, NFR-1)
Given `log-service` 已启动且缓冲队列未满
When 客户端调用 `POST /api/logs/ingest` 提交合法日志
Then 接口返回 `200`
And 返回体中的日志主键非空
And 日志立即出现在缓冲概览中
And 日志在后台刷新任务执行后可被查询接口检索到

### AC-2: 队列满时拒绝写入 (FR-2, NFR-2)
Given 缓冲队列已达到最大容量
When 客户端再次调用 `POST /api/logs/ingest`
Then 接口返回业务错误
And 错误码为可识别的队列已满错误
And 已在队列中的日志不被覆盖

### AC-3: 定时批量刷新到后备存储 (FR-4, FR-5, NFR-3)
Given ClickHouse 模式未启用且缓冲队列中存在多条日志
When 后台刷新任务被触发
Then 日志被批量写入后备存储
And 缓冲队列中的对应日志被移除
And 运行概览中的最近一次刷新结果为成功

### AC-4: 公开检索接口兼容旧契约 (FR-1, FR-6)
Given 服务中已存在包含关键字 `order` 的日志
When 客户端调用 `GET /api/logs/search?keyword=order`
Then 接口返回 `200`
And 返回体仍为 `records` 与 `total` 结构
And 至少有一条结果命中关键字

### AC-5: 内部组合检索生效 (FR-6)
Given 服务中存在多服务、多级别、多状态码日志
When 客户端调用内部组合检索接口并同时传入 `serviceName`、`level` 与时间范围
Then 返回结果仅包含匹配条件的日志
And 结果按时间倒序排列

### AC-6: trace 查询按时间排序 (FR-7)
Given 同一 `traceId` 下存在多条不同时间的日志
When 客户端调用 `GET /api/logs/trace/{traceId}`
Then 返回结果仅包含该 `traceId` 的日志
And 日志按时间升序排列

### AC-7: 指标从实际日志聚合 (FR-8)
Given 已落库日志中同时存在成功和失败请求
When 客户端调用 `GET /api/logs/metrics?serviceName=topbiz`
Then 返回的 `qps`、`errorRate`、`avgLatency`、`p95`、`p95Latency` 和 `totalRequests` 来源于当前数据集
And 不再返回纯写死的固定值

### AC-8: 规则触发告警事件 (FR-9, FR-10, FR-11)
Given 已创建并启用一条错误率阈值规则
When 后台评估任务扫描到命中阈值的数据窗口
Then 生成新的告警事件
And 告警事件状态初始为 `OPEN`
And 已通过启用的通知通道发送通知或记录通知失败原因

### AC-9: 告警状态可更新 (FR-12)
Given 已存在一条 `OPEN` 状态告警事件
When 客户端调用 `POST /api/logs/alerts/{alertId}/status`
Then 事件状态被更新为请求中的目标状态
And 后续查询接口返回更新后的状态

### AC-10: 导出任务异步完成 (FR-13)
Given 服务中存在可导出的日志
When 客户端调用 `POST /api/logs/exports`
Then 接口立即返回导出任务编号和初始状态
And 后台导出任务运行后该任务状态变为 `COMPLETED`
And 任务结果中包含可下载路径或文件定位信息

### AC-11: 运行概览可观测 (FR-14, FR-15)
Given 已发生过缓冲刷新、告警扫描或导出任务
When 客户端调用内部运行概览接口
Then 返回当前队列深度、最近刷新时间、最近告警扫描结果和导出任务统计
And 如存在失败会暴露最近失败原因摘要

### AC-12: ClickHouse 关闭时服务仍可启动 (FR-5, NFR-5, NFR-9)
Given 本地环境未配置 ClickHouse 地址
When 服务启动并执行测试
Then 服务成功启动
And 写入、检索、告警和导出主流程仍可在后备模式下运行

### AC-13: 服务边界保持独立 (FR-16)
Given `log-service` 正在处理日志写入、查询、告警和导出请求
When 检查服务实现的依赖与执行路径
Then 不存在对 `user-service`、`message-service` 或 `topbiz-service` 的运行时业务调用
And 日志处理所需数据全部来自请求体、服务内配置或服务内存储

## Edge Cases

- EC-1: 当请求时间戳为空时，服务 MUST 使用接收时间补齐，而不是拒绝整个请求。
- EC-2: 当请求中的状态码或耗时为负数时，服务 MUST 返回参数错误。
- EC-3: 当 ClickHouse 写入失败时，刷新任务 MUST 记录失败并将未成功写入的数据回退到后备失败列表，供后续重试或排查。
- EC-4: 当告警评估周期内无日志数据时，评估任务 MUST 返回空结果且不得创建告警事件。
- EC-5: 当同一规则在同一时间窗口重复命中时，系统 MUST 避免生成完全重复的开放告警事件。
- EC-6: 当 Webhook 通知失败时，告警事件 MUST 仍然创建成功，但通知记录为失败。
- EC-7: 当导出任务查询条件为空时，系统 MUST 允许创建任务，并按默认时间窗口导出最近数据。
- EC-8: 当导出任务格式不受支持时，系统 MUST 返回参数错误。
- EC-9: 当内部调度被配置为关闭时，公开接口 MUST 仍可使用，但运行概览中需明确对应任务已禁用。

## API Contracts

```ts
type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T | null;
};

type LogIngestRequest = {
  serviceName: string;
  traceId: string;
  level: string;
  message: string;
  path?: string;
  statusCode?: number;
  latencyMs?: number;
  requestId?: string;
  clientIp?: string;
  timestamp?: string;
  tags?: Record<string, string>;
};

type LogEntryResponse = {
  logId: string;
  serviceName: string;
  traceId: string;
  level: string;
  message: string;
  timestamp: string;
};

type LogSearchResponse = {
  records: LogEntryResponse[];
  total: number;
};

type MetricsResponse = {
  serviceName: string;
  metrics: Record<string, number>;
};

type AlertResponse = {
  alertId: string;
  alertCode: string;
  level: string;
  status: string;
  summary: string;
};

type ExportRequest = {
  format: "CSV" | "JSON";
  query: string;
};

type ExportResponse = {
  exportId: string;
  status: string;
  downloadPath: string;
};

type InternalLogSearchRequest = {
  keyword?: string;
  serviceName?: string;
  level?: string;
  traceId?: string;
  statusCode?: number;
  startTime?: string;
  endTime?: string;
  page?: number;
  size?: number;
};

type AlertRuleUpsertRequest = {
  ruleName: string;
  ruleType: "ERROR_RATE" | "LATENCY_P95" | "ERROR_COUNT";
  serviceName?: string;
  threshold: number;
  windowMinutes: number;
  enabled: boolean;
  notificationChannels: string[];
};

type RuntimeOverviewResponse = {
  queueDepth: number;
  queueCapacity: number;
  schedulerEnabled: boolean;
  lastFlushAt?: string;
  lastFlushResult: string;
  lastAlertEvaluationAt?: string;
  lastAlertEvaluationResult: string;
  pendingExports: number;
  completedExports: number;
  failedExports: number;
  lastError?: string;
};
```

公开接口：

- `POST /api/logs/ingest`
- `GET /api/logs/search?keyword=...`
- `GET /api/logs/trace/{traceId}`
- `GET /api/logs/metrics?serviceName=...`
- `GET /api/logs/alerts`
- `POST /api/logs/alerts/{alertId}/status`
- `POST /api/logs/exports`

内部控制面接口：

- `POST /api/logs/internal/search`
- `GET /api/logs/internal/runtime`
- `GET /api/logs/internal/alert-rules`
- `POST /api/logs/internal/alert-rules`
- `POST /api/logs/internal/alert-rules/{ruleId}/enabled`
- `DELETE /api/logs/internal/alert-rules/{ruleId}`
- `POST /api/logs/internal/tasks/flush`
- `POST /api/logs/internal/tasks/alerts/evaluate`
- `POST /api/logs/internal/tasks/exports/run`

错误响应：

- `QUEUE_FULL`
- `UNSUPPORTED_EXPORT_FORMAT`
- `INVALID_TIME_RANGE`
- `ALERT_NOT_FOUND`
- `ALERT_RULE_NOT_FOUND`
- `CLICKHOUSE_WRITE_FAILED`
- `VALIDATION_ERROR`

## Data Models

| Entity | Field | Type | Constraints |
|---|---|---|---|
| AccessLogRecord | logId | string | 必填，唯一 |
| AccessLogRecord | serviceName | string | 必填，非空 |
| AccessLogRecord | traceId | string | 必填，非空 |
| AccessLogRecord | level | string | 必填，枚举型文本 |
| AccessLogRecord | message | string | 必填，非空 |
| AccessLogRecord | path | string | 可空 |
| AccessLogRecord | statusCode | integer | 可空，若存在则必须 >= 0 |
| AccessLogRecord | latencyMs | long | 可空，若存在则必须 >= 0 |
| AccessLogRecord | requestId | string | 可空 |
| AccessLogRecord | clientIp | string | 可空 |
| AccessLogRecord | timestamp | instant | 必填，UTC |
| AccessLogRecord | tags | map<string,string> | 可空 |
| BufferedLogBatch | batchId | string | 必填，唯一 |
| BufferedLogBatch | records | list<AccessLogRecord> | 必填，1..500 |
| BufferedLogBatch | createdAt | instant | 必填 |
| AlertRule | ruleId | string | 必填，唯一 |
| AlertRule | ruleName | string | 必填，非空 |
| AlertRule | ruleType | string | 必填，枚举 |
| AlertRule | serviceName | string | 可空 |
| AlertRule | threshold | double | 必填，> 0 |
| AlertRule | windowMinutes | integer | 必填，> 0 |
| AlertRule | enabled | boolean | 必填 |
| AlertRule | notificationChannels | list<string> | 必填，可为空列表，空列表表示仅创建事件不发送通知 |
| AlertEvent | alertId | string | 必填，唯一 |
| AlertEvent | alertCode | string | 必填 |
| AlertEvent | level | string | 必填 |
| AlertEvent | status | string | 必填，`OPEN/ACKED/CLOSED` |
| AlertEvent | summary | string | 必填 |
| AlertEvent | ruleId | string | 可空，系统内规则关联 |
| AlertEvent | createdAt | instant | 必填 |
| ExportTask | exportId | string | 必填，唯一 |
| ExportTask | format | string | 必填，`CSV/JSON` |
| ExportTask | query | string | 必填，可为空字符串 |
| ExportTask | status | string | 必填，`QUEUED/RUNNING/COMPLETED/FAILED` |
| ExportTask | downloadPath | string | 可空，完成后必有值 |
| ExportTask | createdAt | instant | 必填 |
| ExportTask | completedAt | instant | 可空 |
| RuntimeState | queueDepth | integer | 必填，>= 0 |
| RuntimeState | lastFlushResult | string | 必填 |
| RuntimeState | lastAlertEvaluationResult | string | 必填 |
| RuntimeState | lastError | string | 可空 |

## Out of Scope

- OS-1: 本次实现 MUST NOT 引入 Kafka、RabbitMQ 或其他独立消息中间件；缓冲仍基于进程内队列。
- OS-2: 本次实现 MUST NOT 提供前端页面或可视化大盘，只交付后端接口与后台任务。
- OS-3: 本次实现 MUST NOT 落完整权限体系；内部控制面接口默认仅作为服务内或测试用途。
- OS-4: 本次实现 MUST NOT 实现短信、邮件、钉钉等多供应商通知集成，通知通道仅交付控制台和通用 Webhook。
- OS-5: 本次实现 MUST NOT 保证真实生产级高可用、分布式一致性和跨实例去重；本交付是单实例生产化骨架。

## Corrections Applied

- 原始文档对 ClickHouse 存储仅停留在概念层；本落地实现已明确按天分区、按 `service + timestamp + traceId` 排序，并设置 30 天 TTL。
- 原始文档要求返回下载链接，但未定义如何真正访问文件；本落地实现已将 `/downloads/**` 映射到导出目录，使返回路径可直接访问。
- 原始文档提到告警通知，但未定义通知失败策略；本落地实现采用“事件照常创建，失败摘要写入运行态”的策略。
- 原始文档与早期规格对 P95 指标命名不一致；本落地实现同时返回 `p95` 与 `p95Latency`，降低调用方兼容成本。
