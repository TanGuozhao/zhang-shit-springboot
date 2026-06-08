# 访问日志服务设计修正稿

对应原稿：`可复用访问日志服务设计-2024091602016-谭国照.md`

## 1. 文档目的

本文档用于替代原始访问日志服务设计稿中范围失控、接口路径不统一、查询能力写得过满、ClickHouse 角色描述失真等问题，作为当前仓库 `log-service` 的正式设计说明。

本文档以当前实现为准，冻结以下内容：

- 服务边界
- 对外接口与内部运行时接口
- 当前已实现的日志接入、检索、指标、告警、导出能力
- 关键请求/响应 DTO
- 可切换存储与运行时约束
- 占位实现和未来扩展的边界

## 2. 服务定位

`log-service` 是平台的访问日志领域服务，负责访问日志接入、检索、Trace 查询、指标统计、告警规则、告警事件、导出任务和运行时任务控制。

本服务当前聚焦“访问日志域能力”，不是完整可观测平台。它不提供 APM 全套能力，不提供统一网关，也不持有业务主数据。

本服务不依赖 `user-service`、`message-service` 或 `topbiz-service` 补全日志本身的数据，它处理的是调用方明确写入的日志请求和服务内的控制面配置。

## 3. 核心领域对象

当前实现已经稳定形成以下核心对象：

- `AccessLogRecord`：访问日志记录，包含服务名、traceId、级别、消息、路径、状态码、耗时、请求标识、客户端 IP、标签、时间戳等。
- `LogSearchCriteria`：内部检索条件对象，用于组合查询。
- `AlertRule`：告警规则，描述规则名称、类型、服务范围、阈值、时间窗口、通知通道和启停状态。
- `AlertEvent`：告警事件，描述实际命中的规则和事件状态。
- `ExportTask`：导出任务，描述导出格式、状态、下载路径、过期时间、失败原因等。
- `RuntimeState`：运行时摘要，描述队列深度、最近刷新结果、告警评估结果、导出执行结果和错误摘要。
- `ClickHouseLogStore`：可选 ClickHouse 存储实现。

## 4. 调用方模型

### 4.1 业务系统调用方

业务系统可通过公开接口：

- 写入日志
- 查询日志
- 查询 Trace
- 查询指标
- 查询告警事件
- 创建导出任务
- 更新告警事件状态

### 4.2 内部运维调用方

内部运维与平台调试可通过内部接口：

- 做组合检索
- 查看运行态
- 维护告警规则
- 查询导出任务列表
- 手工触发刷新、告警评估、导出运行和导出清理任务

### 4.3 存储后端

当前服务支持两类后端：

- 本地内存后备存储
- 可选 ClickHouse 存储

这两类后端共用同一服务契约。

## 5. 已实现能力范围

当前仓库内 `log-service` 已实现以下能力：

- 访问日志接入
- 关键词公开检索
- Trace 查询
- 指标查询
- 告警事件查询
- 告警事件状态更新
- 导出任务创建
- 内部组合检索
- 运行态查询
- 告警规则查询
- 告警规则新增/更新
- 告警规则启停
- 告警规则删除
- 手工刷新队列
- 手工执行告警评估
- 手工执行导出任务
- 手工清理过期导出
- 可选 ClickHouse 存储实现
- 本地后备存储实现
- 队列缓冲
- 批量刷新
- 基础脱敏开关
- 控制台通知与可选 webhook 通知

## 6. 接口设计

### 6.1 架构概览接口

| 方法 | 路径 | 用途 | 备注 |
| --- | --- | --- | --- |
| `GET` | `/internal/architecture/overview` | 返回服务边界、分层和核心模块概览 | 供内部排障与架构说明使用 |

### 6.2 公开命令接口

基础路径：`/api/logs`

| 方法 | 路径 | 用途 | 关键说明 |
| --- | --- | --- | --- |
| `POST` | `/ingest` | 接入访问日志 | 写入缓冲并进入后台刷新链路 |
| `POST` | `/alerts/{alertId}/status` | 更新告警事件状态 | 修改事件流转状态 |
| `POST` | `/exports` | 创建导出任务 | 异步导出，不是同步下载 |

### 6.3 公开查询接口

基础路径：`/api/logs`

| 方法 | 路径 | 用途 | 关键说明 |
| --- | --- | --- | --- |
| `GET` | `/search` | 公开关键词检索 | 当前对外只暴露简化检索 |
| `GET` | `/trace/{traceId}` | 查询整条 Trace | 返回按时间排序的记录列表 |
| `GET` | `/metrics` | 查询指标 | 支持按服务名过滤 |
| `GET` | `/alerts` | 查询告警事件 | 返回事件列表 |

### 6.4 内部运行时接口

基础路径：`/api/logs/internal`

| 方法 | 路径 | 用途 | 关键说明 |
| --- | --- | --- | --- |
| `POST` | `/search` | 组合检索 | 支持服务名、级别、状态码、时间范围等条件 |
| `GET` | `/runtime` | 运行态摘要 | 返回缓冲、任务、导出和错误摘要 |
| `GET` | `/alert-rules` | 查询告警规则 | 返回规则定义列表 |
| `GET` | `/exports` | 查询导出任务 | 返回导出任务列表 |
| `POST` | `/alert-rules` | 新增或更新告警规则 | 当前使用 upsert 语义 |
| `POST` | `/alert-rules/{ruleId}/enabled` | 更新规则启停状态 | 控制规则是否参与评估 |
| `DELETE` | `/alert-rules/{ruleId}` | 删除告警规则 | 删除规则定义 |
| `POST` | `/tasks/flush` | 立即刷新缓冲队列 | 手工执行批量写入 |
| `POST` | `/tasks/alerts/evaluate` | 立即评估告警规则 | 手工执行告警扫描 |
| `POST` | `/tasks/exports/run` | 立即执行导出任务 | 手工生成导出文件 |
| `POST` | `/tasks/exports/cleanup` | 清理过期导出 | 手工执行导出清理 |

## 7. 关键请求与响应 DTO

### 7.1 日志接入请求

`LogIngestRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `serviceName` | 是 | 服务名 |
| `traceId` | 是 | Trace 标识 |
| `level` | 是 | 日志级别 |
| `message` | 是 | 日志消息 |
| `path` | 否 | 接口路径 |
| `statusCode` | 否 | 状态码，要求非负 |
| `latencyMs` | 否 | 耗时，要求非负 |
| `requestId` | 否 | 请求 ID |
| `clientIp` | 否 | 客户端 IP |
| `timestamp` | 否 | 日志时间，未传时可由服务补齐 |
| `tags` | 否 | 扩展标签 |

### 7.2 日志基础响应

`LogEntryResponse`

| 字段 | 说明 |
| --- | --- |
| `logId` | 日志主键 |
| `serviceName` | 服务名 |
| `traceId` | Trace 标识 |
| `level` | 日志级别 |
| `message` | 日志消息 |
| `timestamp` | 日志时间 |

`LogSearchResponse`

| 字段 | 说明 |
| --- | --- |
| `records` | 日志记录列表 |
| `total` | 总记录数 |

### 7.3 公开导出 DTO

`ExportRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `format` | 是 | 导出格式，当前支持服务内允许的格式 |
| `query` | 否 | 查询关键词或表达式 |

`ExportResponse`

| 字段 | 说明 |
| --- | --- |
| `exportId` | 导出任务 ID |
| `status` | 当前任务状态 |
| `downloadPath` | 下载路径或结果定位 |

### 7.4 指标与告警 DTO

`MetricsResponse`

| 字段 | 说明 |
| --- | --- |
| `serviceName` | 服务名 |
| `metrics` | 指标键值对，当前实现返回 `Map<String, Number>` |

当前指标语义至少覆盖：

- `qps`
- `errorRate`
- `avgLatency`
- `p95`
- `p95Latency`
- `totalRequests`

`AlertResponse`

| 字段 | 说明 |
| --- | --- |
| `alertId` | 告警事件 ID |
| `alertCode` | 告警编码 |
| `level` | 告警等级 |
| `status` | 当前状态 |
| `summary` | 摘要 |

`AlertStatusUpdateRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `status` | 是 | 告警事件目标状态 |

### 7.5 告警规则 DTO

`AlertRuleUpsertRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `ruleName` | 是 | 规则名称 |
| `ruleType` | 是 | 规则类型 |
| `serviceName` | 否 | 限定服务名 |
| `threshold` | 是 | 阈值，要求大于 0 |
| `windowMinutes` | 是 | 统计窗口分钟数，要求大于 0 |
| `enabled` | 是 | 是否启用 |
| `notificationChannels` | 是 | 通知通道列表 |

`AlertRuleResponse`

| 字段 | 说明 |
| --- | --- |
| `ruleId` | 规则 ID |
| `ruleName` | 规则名称 |
| `ruleType` | 规则类型 |
| `serviceName` | 服务名范围 |
| `threshold` | 阈值 |
| `windowMinutes` | 时间窗口 |
| `enabled` | 是否启用 |
| `notificationChannels` | 通知通道列表 |

`AlertRuleStatusUpdateRequest`

| 字段 | 说明 |
| --- | --- |
| `enabled` | 目标启停状态 |

### 7.6 内部检索 DTO

`InternalLogSearchRequest`

| 字段 | 说明 |
| --- | --- |
| `keyword` | 关键词 |
| `serviceName` | 服务名 |
| `level` | 日志级别 |
| `traceId` | Trace 标识 |
| `statusCode` | 状态码 |
| `startTime` | 起始时间 |
| `endTime` | 结束时间 |
| `page` | 页码，要求大于等于 0 |
| `size` | 分页大小，要求 1 到 500 之间 |

### 7.7 导出任务与运行态 DTO

`ExportTaskResponse`

| 字段 | 说明 |
| --- | --- |
| `exportId` | 导出任务 ID |
| `format` | 导出格式 |
| `status` | 当前状态 |
| `downloadPath` | 文件位置 |
| `createdAt` | 创建时间 |
| `completedAt` | 完成时间 |
| `expiresAt` | 过期时间 |
| `recordCount` | 记录数 |
| `failureReason` | 失败原因 |

`RuntimeOverviewResponse`

| 字段 | 说明 |
| --- | --- |
| `queueDepth` | 当前队列深度 |
| `queueCapacity` | 队列容量 |
| `schedulerEnabled` | 调度器是否启用 |
| `lastFlushAt` | 最近刷新时间 |
| `lastFlushResult` | 最近刷新结果 |
| `lastAlertEvaluationAt` | 最近告警评估时间 |
| `lastAlertEvaluationResult` | 最近告警评估结果 |
| `lastExportRunAt` | 最近导出执行时间 |
| `lastExportRunResult` | 最近导出执行结果 |
| `lastCleanupAt` | 最近清理时间 |
| `lastCleanupResult` | 最近清理结果 |
| `pendingExports` | 待执行导出数量 |
| `completedExports` | 已完成导出数量 |
| `failedExports` | 失败导出数量 |
| `expiredExports` | 过期导出数量 |
| `archivedFailedBatches` | 已归档失败批次数量 |
| `lastError` | 最近错误摘要 |

## 8. 典型请求示例

### 8.1 接入日志

```json
POST /api/logs/ingest
{
  "serviceName": "topbiz-service",
  "traceId": "trace-demo-001",
  "level": "INFO",
  "message": "request completed",
  "path": "/api/topbiz/platform/overview",
  "statusCode": 200,
  "latencyMs": 32,
  "requestId": "req-001",
  "clientIp": "127.0.0.1",
  "tags": {
    "module": "platform",
    "env": "local"
  }
}
```

### 8.2 创建导出任务

```json
POST /api/logs/exports
{
  "format": "CSV",
  "query": "topbiz-service"
}
```

### 8.3 内部组合检索

```json
POST /api/logs/internal/search
{
  "keyword": "error",
  "serviceName": "topbiz-service",
  "level": "ERROR",
  "startTime": "2026-06-07T00:00:00Z",
  "endTime": "2026-06-07T23:59:59Z",
  "page": 0,
  "size": 100
}
```

### 8.4 新增告警规则

```json
POST /api/logs/internal/alert-rules
{
  "ruleName": "topbiz-error-rate",
  "ruleType": "ERROR_RATE",
  "serviceName": "topbiz-service",
  "threshold": 0.2,
  "windowMinutes": 5,
  "enabled": true,
  "notificationChannels": ["CONSOLE"]
}
```

## 9. 工程设计

### 9.1 服务边界

- 本服务负责什么：
  - 访问日志接入
  - 日志检索
  - Trace 查询
  - 指标统计
  - 告警规则和告警事件
  - 导出任务
  - 运行态任务控制
  - 日志缓冲与批量刷新
- 本服务不负责什么：
  - 用户认证
  - 消息通知总线
  - 主业务数据持久化
  - 统一外部网关
  - 完整 APM/Tracing/监控平台
  - 前端观测大盘
- 主数据是否由本服务持有：
  - 是。访问日志、告警规则、告警事件和导出任务由本服务持有。
- 是否允许其他底层服务直接调用：
  - 平台架构上不建议底层服务通过业务 API 相互直调。
  - 跨服务业务编排统一通过 `topbiz-service`。

### 9.2 运行模式

#### 本地默认模式

- 启动命令：

```powershell
.\mvnw.cmd -pl log-service -am spring-boot:run
```

- 默认端口：`8083`
- 默认存储：本地内存后备实现
- 默认允许：
  - 日志写入
  - 本地检索
  - 告警规则维护
  - 导出任务
  - 调度任务手工执行
- 无需 ClickHouse、对象存储、Webhook 平台也可启动和联调

#### 可替换生产模式

- 存储层可替换为 ClickHouse
- 导出结果可替换为对象存储
- 告警通知可替换为企业微信、钉钉、短信、邮件或 Webhook 平台
- 检索层后续可替换为专门的检索后端

#### 外部依赖是否可选

- 当前阶段全部可选
- 即使不启用 ClickHouse，也必须保证服务可启动和可联调

### 9.3 契约冻结说明

#### 已冻结接口

- `/api/logs/ingest`
- `/api/logs/search`
- `/api/logs/trace/{traceId}`
- `/api/logs/metrics`
- `/api/logs/alerts`
- `/api/logs/alerts/{alertId}/status`
- `/api/logs/exports`
- `/api/logs/internal/search`
- `/api/logs/internal/runtime`
- `/api/logs/internal/alert-rules`
- `/api/logs/internal/exports`
- `/api/logs/internal/alert-rules`
- `/api/logs/internal/alert-rules/{ruleId}/enabled`
- `/api/logs/internal/tasks/flush`
- `/api/logs/internal/tasks/alerts/evaluate`
- `/api/logs/internal/tasks/exports/run`
- `/api/logs/internal/tasks/exports/cleanup`
- `/internal/architecture/overview`

#### 已冻结 DTO

- `LogIngestRequest`
- `LogEntryResponse`
- `LogSearchResponse`
- `MetricsResponse`
- `AlertResponse`
- `AlertStatusUpdateRequest`
- `ExportRequest`
- `ExportResponse`
- `InternalLogSearchRequest`
- `AlertRuleUpsertRequest`
- `AlertRuleResponse`
- `AlertRuleStatusUpdateRequest`
- `ExportTaskResponse`
- `RuntimeOverviewResponse`

#### 仅为预留扩展点

- 完整正则/DSL/聚合分析能力
- 专业日志检索平台
- 复杂告警编排
- 多租户平台化观测中心
- 外部采集 Agent、Sidecar、SDK 平台能力

### 9.4 占位实现与可替换实现

- 当前告警通知的控制台输出属于基础占位实现。
- Webhook 通知是可选实现，不代表已接入完整通知平台。
- 当前导出结果主要走本地文件目录和任务模型，可替换为对象存储和异步报表系统。
- 当前后备存储是本地内存实现。
- ClickHouse 是可切换实现，而不是系统强依赖。

### 9.5 默认配置与约束

当前 `application.yml` 与 `LogServiceProperties` 已冻结以下约束：

#### 存储与刷新

- `storage.clickhouse-enabled: false`
- `storage.retention-days: 30`
- `storage.max-flush-retries: 3`
- `buffer.capacity: 10000`
- `buffer.batch-size: 500`
- `buffer.flush-interval-ms: 5000`

#### 调度器

- `scheduler.enabled: true`
- `scheduler.alert-evaluation-interval-ms: 10000`
- `scheduler.export-interval-ms: 15000`
- `scheduler.cleanup-interval-ms: 3600000`

#### 通知

- `notification.console-enabled: true`
- `notification.webhook-enabled: false`

#### 导出

- `export.directory: build/log-exports`
- `export.default-window-minutes: 60`
- `export.max-rows: 10000`
- `export.file-ttl-hours: 24`

#### 检索

- `search.default-window-hours: 24`
- `search.max-window-days: 7`
- `search.max-keyword-length: 100`
- `search.max-page-size: 500`

#### 接入校验

- `ingest.max-future-skew-seconds: 300`
- `ingest.max-past-days: 30`
- `ingest.max-message-length: 4000`
- `ingest.allowed-levels: INFO, DEBUG, WARN, ERROR, FATAL`

#### 脱敏

- `masking.enabled: true`

工程含义如下：

- ClickHouse 默认关闭，本地优先使用后备模式
- 缓冲队列默认支持 10000 条待处理日志
- 单次刷新最多处理 500 条日志
- 默认每 5 秒执行一次刷新
- 默认每 10 秒做一次告警评估
- 导出单次最多 10000 行
- 公开关键词长度上限为 100
- 内部分页大小上限为 500
- 日志消息长度上限为 4000
- 支持的日志级别已显式列举

## 10. ClickHouse 实现说明

当前仓库已经提供 `ClickHouseLogStore`，其表结构策略已写实到代码中：

- 引擎：`MergeTree`
- 分区：`PARTITION BY toYYYYMMDD(timestamp)`
- 排序键：`ORDER BY (service_name, timestamp, trace_id)`
- TTL：`TTL timestamp + INTERVAL retentionDays DAY`

因此文档中必须把 ClickHouse 描述为：

- “可选存储后端”
- “在启用时承担日志持久化”
- “不启用时服务仍能以本地模式工作”

不能再写成“没有 ClickHouse 服务就无法运行”。

## 11. 对原稿的具体修正

本次修正明确替换了原稿中的以下问题：

- 原稿中的 `/api/log/ingest`、`/log/search` 等路径全部统一为 `/api/logs/**`
- 原稿把完整可观测平台、前端查询页面、复杂聚合平台式构想混在一起，当前已收敛为访问日志领域服务
- 原稿中正则检索、标签检索、反向检索、复杂组合 DSL 等能力写得过满，当前统一降级为未来扩展
- 原稿中导出与下载流程写成了前端交互叙事，当前修正为导出任务模型
- 原稿中没有清楚区分公开接口与内部控制面接口，当前已正式分层
- 原稿中把 ClickHouse 写成隐性强依赖，当前修正为可切换存储实现
- 原稿中缺少明确的缓冲、批量刷新、时间窗口和最大分页等工程约束，当前已按代码补齐

## 12. 后续扩展建议

在不破坏当前契约的前提下，后续可继续扩展：

- 真正的 ClickHouse 生产接入和运维脚本
- 告警通知多通道接入
- 对象存储导出
- 更强的组合检索、聚合检索和报表分析
- 日志采集 SDK 或 Agent
- 更完整的可观测平台联动

但这些能力进入实现前，不应再改动本文档中已冻结的公开路径和核心 DTO 结构。
