# Spec: Message Service Full Delivery

**Author:** Codex
**Date:** 2026-06-07
**Status:** Approved
**Reviewers:** User (delegated autonomous delivery)
**Related specs:** `docs/contracts/message-service-contract.md`, `docs/specs/message-service-mvp-spec.md`, `可复用消息通道服务设计-2024091602016-谭国照.md`

## Context

仓库中的 `message-service` 已经具备模板查询、变量填充、消息发送和基础记录查询能力，但仍然只覆盖了原始设计文档中的一部分用户链路，尚未形成完整的“消息通道平台”。当前缺失的能力主要集中在管理员配置、调度任务管理、重试追踪、收件箱闭环和统计分析。

原始设计文档本身也存在多处不一致，直接按原文编码会造成接口漂移和边界混乱。最明显的问题包括：同一服务同时出现 `/api/v1/message`、`/api/v1/messages`、`/api/messages` 三套路径；模板预览、草稿保存、消息状态查询等接口的命名风格不统一；“接收对象解析”混入了用户主数据职责；管理员配置接口、用户业务接口和内部调度接口没有清晰分层。

本规格在保留现有冻结 contract 的前提下，对消息服务进行一次完整纠偏。目标不是一次性接入真实第三方短信、邮件或数据库，而是在当前多模块 Spring Boot 仓库内交付一套可运行、可测试、职责边界清晰、支持后续持久化和外部通道接入的完整内存版消息平台。

## Functional Requirements

- FR-1: `message-service` MUST 继续使用 `/api/messages` 作为统一公开业务前缀，并保持 `docs/contracts/message-service-contract.md` 中冻结接口可用。
- FR-2: 系统 MUST 支持模板列表、模板详情、模板变量说明和模板预览能力。
- FR-3: 系统 MUST 支持变量填充、变量完整性校验、默认值回填和自动填充。
- FR-4: 系统 MUST 支持消息发送请求的模板合法性、变量合法性、通道可用性和接收对象有效性校验。
- FR-5: 系统 MUST 支持即时发送和定时发送两种发送策略，并在定时发送时生成调度任务。
- FR-6: 即时发送 MUST 在服务内生成调度执行单元并立即尝试投递；定时发送 MUST 进入待执行队列，等待后台或人工触发。
- FR-7: 系统 MUST 为发送失败的调度任务生成重试记录，并支持后台或人工重试。
- FR-8: 系统 MUST 记录消息、调度任务、重试记录和收件箱投递结果的全生命周期状态。
- FR-9: 系统 MUST 提供消息详情、消息状态、失败原因、记录列表和关键词搜索能力。
- FR-10: 系统 MUST 提供收件箱列表、收件箱详情和已读标记能力。
- FR-11: 系统 MUST 支持管理员创建模板、启用/禁用模板，并在模板禁用时阻止新发送流程继续引用。
- FR-12: 系统 MUST 支持管理员新增变量、调整变量数据类型和必填属性。
- FR-13: 系统 MUST 支持管理员维护运营商账户和发送通道配置，并支持设置默认发送者信息。
- FR-14: 系统 MUST 支持管理员创建调度策略、创建调度任务、查询待执行任务、修改执行时间、取消任务和强制立即触发任务。
- FR-15: 系统 MUST 支持按时间范围统计消息总量、成功量、失败量、成功率、通道占比和失败原因分布。
- FR-16: 系统 MUST 提供内部运行态查看与后台任务手动触发能力，便于排障和测试。
- FR-17: 系统 SHOULD 支持接收对象解析能力，包括显式接收人和预定义接收组。
- FR-18: 系统 SHOULD 将收件箱作为消息接收侧统一抽象，而不是仅服务于站内信单一通道。
- FR-19: 系统 MUST NOT 依赖 `user-service` 才能完成消息投递主流程；若调用方已解析好接收对象，服务应可独立工作。
- FR-20: 原始设计文档中的 `/api/v1/message/*` 与 `/api/v1/messages/*` 混用 MUST 统一修正为 `/api/messages/*`、`/api/messages/admin/*` 与 `/api/messages/internal/*` 三层结构。

## Non-Functional Requirements

- NFR-1: 所有公开与管理接口 MUST 继续复用 `ApiResponse` 统一响应结构。
- NFR-2: 服务在不依赖外部数据库和第三方通道的情况下 MUST 可直接启动并通过测试。
- NFR-3: 所有时间字段 MUST 使用 `Instant` 表达并以 UTC 语义存储。
- NFR-4: 内存版后台调度与重试执行 MUST 具备幂等保护，不得重复投递同一已完成调度任务。
- NFR-5: 管理接口和内部接口 SHOULD 与公开接口分层，避免业务调用方误用内部调度控制能力。
- NFR-6: 新增能力 MUST 与现有 `topbiz-service` 的 Feign 依赖保持可兼容演进，不得破坏已有冻结调用。
- NFR-7: 消息列表、收件箱列表和调度任务列表在 1,000 条以内内存数据集上 SHOULD 在 500ms 内完成查询。
- NFR-8: 运行态和统计结果 MUST 来源于当前服务真实内存状态，而不是硬编码常量。

## Acceptance Criteria

### AC-1: 冻结公开接口保持可用 (FR-1, NFR-1, NFR-6)
Given `message-service` 已启动
When 客户端调用 `POST /api/messages/send`、`POST /api/messages/drafts`、`GET /api/messages/{messageId}`、`GET /api/messages/{messageId}/status`、`GET /api/messages/templates`、`GET /api/messages/templates/{templateCode}`、`POST /api/messages/templates/{templateCode}/preview`
Then 接口返回 `ApiResponse`
And 现有调用路径不发生破坏性变化

### AC-2: 定时消息进入调度队列 (FR-5, FR-6)
Given 一个合法的模板、变量、通道和接收对象
When 客户端提交 `dispatchType=SCHEDULED` 且指定未来 `scheduledAt` 的发送请求
Then 系统创建消息记录
And 系统创建至少一个状态为 `PENDING` 的调度任务
And 消息状态为 `SCHEDULED`

### AC-3: 即时消息生成收件箱记录 (FR-6, FR-8, FR-10, FR-18)
Given 一个合法的即时发送请求
When 系统完成投递
Then 消息状态更新为 `SENT`
And 每个接收对象都生成收件箱记录
And 收件箱记录初始状态为 `UNREAD`

### AC-4: 失败投递生成重试记录 (FR-7, FR-8)
Given 一个会触发临时失败的发送请求
When 系统首次执行投递
Then 消息状态为 `FAILED`
And 系统创建 `PENDING` 状态的重试记录
And 失败原因可通过 `GET /api/messages/{messageId}/error` 查询

### AC-5: 后台或人工重试可修复临时失败 (FR-7, FR-16)
Given 某消息存在 `PENDING` 状态的重试记录
When 管理员触发重试执行
Then 调度任务再次尝试投递
And 成功时消息状态变更为 `SENT`
And 重试记录状态变更为 `SUCCESS`

### AC-6: 管理员可维护模板与变量 (FR-11, FR-12)
Given 管理员调用模板和变量管理接口
When 新建模板、禁用模板、新增变量、修改变量类型或必填属性
Then 仓储状态被更新
And 后续发送校验即时反映最新配置

### AC-7: 管理员可维护通道与调度任务 (FR-13, FR-14)
Given 管理员已配置运营商账户与通道
When 管理员创建调度策略和调度任务，随后执行改期、取消或立即触发
Then 系统返回对应任务状态变化
And 待执行任务列表可查询到最新结果

### AC-8: 收件箱支持已读管理 (FR-10)
Given 某接收对象已有未读收件箱记录
When 客户端调用已读标记接口
Then 收件箱记录状态变为 `READ`
And `readAt` 被记录

### AC-9: 统计结果来源于真实消息数据 (FR-15, NFR-8)
Given 服务中同时存在成功、失败、定时与不同通道的消息数据
When 管理员查询统计接口
Then 返回消息总量、成功量、失败量、成功率、通道分布和失败原因分布
And 统计值与当前内存数据一致

### AC-10: 运行态接口可观测后台处理状态 (FR-16, NFR-4)
Given 服务已发生调度执行或重试执行
When 管理员查询运行态接口
Then 可看到待执行调度数、待处理重试数、收件箱总数、最近一次调度执行时间和最近一次重试执行时间

## Edge Cases

- EC-1: 当模板存在但已禁用时，系统 MUST 拒绝新的发送请求。
- EC-2: 当变量缺失必填项或数据类型不匹配时，系统 MUST 返回明确的校验错误信息。
- EC-3: 当定时发送请求的 `scheduledAt` 早于当前时间时，系统 MUST 拒绝该请求。
- EC-4: 当调度任务已取消或已完成时，系统 MUST NOT 再次重复投递。
- EC-5: 当重试次数达到系统上限后，系统 MUST 将重试记录标记为 `FAILED`，并停止继续重试。
- EC-6: 当接收对象同时来自显式列表和接收组时，系统 MUST 去重。
- EC-7: 当管理员禁用模板时，历史消息和历史调度任务 MUST 保留并可查询。
- EC-8: 当调用方只使用冻结 contract 的旧请求结构时，系统 MUST 继续兼容。
- EC-9: 当 `topbiz-service` 读取模板详情时，远程 DTO MUST 能正确反序列化模板变量详情结构。

## API Contracts

```ts
type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T | null;
};

type MessageSendRequest = {
  templateCode: string;
  channel: string;
  receivers?: string[];
  receiverGroups?: string[];
  variables?: Record<string, string>;
  dispatchType?: "IMMEDIATE" | "SCHEDULED";
  scheduledAt?: string;
  cronExpression?: string;
  schedulePolicyCode?: string;
  channelAccountCode?: string;
  attachmentIds?: string[];
  saveToInbox?: boolean;
};

type MessageResponse = {
  messageId: string;
  templateCode: string;
  templateName: string;
  channel: string;
  status: string;
  subject: string;
  content: string;
  receivers: string[];
  variables: Record<string, string>;
  dispatchType: string;
  scheduledAt?: string;
  cronExpression?: string;
  batchCode?: string;
  createdAt: string;
  updatedAt: string;
  sentAt?: string;
  attachmentIds?: string[];
};

type ReceiverResolutionRequest = {
  receivers?: string[];
  receiverGroups?: string[];
};

type ReceiverResolutionResponse = {
  receivers: string[];
  groups: Array<{ groupCode: string; receivers: string[] }>;
};

type InboxMessageResponse = {
  inboxId: string;
  messageId: string;
  receiver: string;
  channel: string;
  subject: string;
  content: string;
  readStatus: "UNREAD" | "READ";
  deliveredAt: string;
  readAt?: string;
};

type TemplateUpsertRequest = {
  templateCode: string;
  templateName: string;
  channel: string;
  subjectTemplate: string;
  contentTemplate: string;
  description?: string;
  variableCodes: string[];
  enabled?: boolean;
};

type VariableUpsertRequest = {
  variableCode: string;
  variableName: string;
  description?: string;
  dataType: "TEXT" | "NUMBER" | "DATE" | "DATETIME" | "BOOLEAN";
  defaultValue?: string;
  required: boolean;
  enabled?: boolean;
  autoFill?: boolean;
};

type CarrierAccountRequest = {
  carrierName: string;
  channelType: string;
  accountCode: string;
  apiKey?: string;
  endpoint?: string;
  signature?: string;
  enabled?: boolean;
};

type ChannelConfigRequest = {
  channelCode: string;
  channelType: string;
  carrierName: string;
  accountCode: string;
  sender: string;
  enabled?: boolean;
  healthy?: boolean;
  description?: string;
};

type SchedulePolicyRequest = {
  policyCode: string;
  cronExpression?: string;
  policyType: "CRON" | "ONCE";
  enabled?: boolean;
  description?: string;
};

type DispatchTaskRequest = {
  taskCode: string;
  messageId: string;
  channelCode: string;
  channelAccountCode?: string;
  schedulePolicyCode?: string;
  plannedAt?: string;
  sortOrder?: number;
};

type DispatchTaskResponse = {
  taskId: string;
  taskCode: string;
  messageId: string;
  channelCode: string;
  plannedAt?: string;
  actualAt?: string;
  status: string;
  sortOrder: number;
  lastError?: string;
};

type RuntimeOverviewResponse = {
  pendingDispatchTasks: number;
  processingDispatchTasks: number;
  pendingRetryRecords: number;
  inboxMessages: number;
  schedulerEnabled: boolean;
  retryEnabled: boolean;
  lastDispatchRunAt?: string;
  lastRetryRunAt?: string;
};

type MessageStatisticsResponse = {
  totalMessages: number;
  successfulMessages: number;
  failedMessages: number;
  scheduledMessages: number;
  successRate: number;
  channelBreakdown: Record<string, number>;
  failureReasons: Record<string, number>;
};
```

公开业务接口：

- `POST /api/messages/send`
- `POST /api/messages/drafts`
- `POST /api/messages/templates/{templateCode}/preview`
- `POST /api/messages/variables/fill`
- `POST /api/messages/variables/validate`
- `POST /api/messages/schedule/validate`
- `POST /api/messages/receivers/resolve`
- `GET /api/messages/templates`
- `GET /api/messages/templates/{templateCode}`
- `GET /api/messages/templates/{templateCode}/variables`
- `GET /api/messages/channels`
- `GET /api/messages/{messageId}`
- `GET /api/messages/{messageId}/status`
- `GET /api/messages/{messageId}/error`
- `GET /api/messages/records`
- `GET /api/messages/search`
- `GET /api/messages/inbox`
- `GET /api/messages/inbox/{inboxId}`
- `PUT /api/messages/inbox/{inboxId}/read`

管理接口：

- `POST /api/messages/admin/templates`
- `PUT /api/messages/admin/templates/{templateCode}/status`
- `POST /api/messages/admin/variables`
- `PUT /api/messages/admin/variables/{variableCode}/type`
- `PUT /api/messages/admin/variables/{variableCode}/required`
- `POST /api/messages/admin/carrier/accounts`
- `POST /api/messages/admin/channels/config`
- `PUT /api/messages/admin/channels/{channelCode}/sender`
- `POST /api/messages/admin/schedule/policies`
- `POST /api/messages/admin/dispatch/tasks`
- `GET /api/messages/admin/dispatch/tasks`
- `PUT /api/messages/admin/dispatch/tasks/{taskId}/time`
- `PUT /api/messages/admin/dispatch/tasks/{taskId}/cancel`
- `POST /api/messages/admin/dispatch/tasks/{taskId}/trigger`
- `POST /api/messages/admin/retries/run`
- `GET /api/messages/admin/statistics`

内部接口：

- `GET /internal/architecture/overview`
- `GET /api/messages/internal/runtime`
- `POST /api/messages/internal/tasks/dispatch/run`
- `POST /api/messages/internal/tasks/retry/run`

## Data Models

| Entity | Field | Type | Constraints |
|---|---|---|---|
| MessageTemplate | templateCode | string | required, unique |
| MessageTemplate | templateName | string | required |
| MessageTemplate | channel | string | required |
| MessageTemplate | subjectTemplate | string | required |
| MessageTemplate | contentTemplate | string | required |
| MessageTemplate | description | string | optional |
| MessageTemplate | enabled | boolean | required |
| MessageTemplate | variableCodes | string[] | required |
| MessageVariable | variableCode | string | required, unique |
| MessageVariable | variableName | string | required |
| MessageVariable | description | string | optional |
| MessageVariable | dataType | string | required |
| MessageVariable | defaultValue | string | optional |
| MessageVariable | required | boolean | required |
| MessageVariable | enabled | boolean | required |
| MessageVariable | autoFill | boolean | required |
| CarrierAccount | accountCode | string | required, unique |
| CarrierAccount | carrierName | string | required |
| CarrierAccount | channelType | string | required |
| CarrierAccount | apiKey | string | optional |
| CarrierAccount | endpoint | string | optional |
| CarrierAccount | signature | string | optional |
| CarrierAccount | enabled | boolean | required |
| MessageChannel | channelCode | string | required, unique |
| MessageChannel | channelType | string | required |
| MessageChannel | carrierName | string | required |
| MessageChannel | accountCode | string | required |
| MessageChannel | sender | string | required |
| MessageChannel | enabled | boolean | required |
| MessageChannel | healthy | boolean | required |
| MessageChannel | description | string | optional |
| SchedulePolicy | policyCode | string | required, unique |
| SchedulePolicy | cronExpression | string | optional |
| SchedulePolicy | policyType | string | required |
| SchedulePolicy | enabled | boolean | required |
| SchedulePolicy | description | string | optional |
| TaskBatch | batchCode | string | required, unique |
| TaskBatch | totalTaskCount | number | required |
| TaskBatch | processedTaskCount | number | required |
| TaskBatch | status | string | required |
| DispatchTask | taskId | string | required, unique |
| DispatchTask | taskCode | string | required, unique |
| DispatchTask | messageId | string | required |
| DispatchTask | channelCode | string | required |
| DispatchTask | channelAccountCode | string | optional |
| DispatchTask | schedulePolicyCode | string | optional |
| DispatchTask | batchCode | string | optional |
| DispatchTask | plannedAt | string | optional |
| DispatchTask | actualAt | string | optional |
| DispatchTask | status | string | required |
| DispatchTask | sortOrder | number | required |
| DispatchTask | lastError | string | optional |
| MessageTask | messageId | string | required, unique |
| MessageTask | templateCode | string | required |
| MessageTask | templateName | string | required |
| MessageTask | channel | string | required |
| MessageTask | channelAccountCode | string | optional |
| MessageTask | status | string | required |
| MessageTask | subject | string | required |
| MessageTask | content | string | required |
| MessageTask | receivers | string[] | required |
| MessageTask | variables | Record<string,string> | required |
| MessageTask | dispatchType | string | required |
| MessageTask | scheduledAt | string | optional |
| MessageTask | cronExpression | string | optional |
| MessageTask | batchCode | string | optional |
| MessageTask | createdAt | string | required |
| MessageTask | updatedAt | string | required |
| MessageTask | sentAt | string | optional |
| MessageTask | errorCode | string | optional |
| MessageTask | errorReason | string | optional |
| MessageTask | retryCount | number | required |
| MessageTask | attachmentIds | string[] | optional |
| RetryRecord | retryId | string | required, unique |
| RetryRecord | messageId | string | required |
| RetryRecord | dispatchTaskId | string | required |
| RetryRecord | retryCount | number | required |
| RetryRecord | retryStatus | string | required |
| RetryRecord | lastRetryAt | string | optional |
| RetryRecord | failureReason | string | optional |
| InboxMessage | inboxId | string | required, unique |
| InboxMessage | messageId | string | required |
| InboxMessage | receiver | string | required |
| InboxMessage | channel | string | required |
| InboxMessage | subject | string | required |
| InboxMessage | content | string | required |
| InboxMessage | readStatus | string | required |
| InboxMessage | deliveredAt | string | required |
| InboxMessage | readAt | string | optional |

## Out of Scope

- OS-1: 本次实现不接入真实短信、邮件、飞书等第三方通道凭证与网络调用，改为内存版投递模拟。这是为了保证本地可运行和测试稳定。
- OS-2: 本次实现不引入数据库持久化，所有实体使用内存仓储承载，但仓储接口和领域模型需为后续持久化保留演进空间。
- OS-3: 原始文档中仅在数据模型出现、但未形成完整业务流程的附件元数据管理，不提供单独管理接口；仅保留消息层的附件关联字段。
- OS-4: 本次不实现复杂权限系统与管理员鉴权，中后台接口默认视为可信调用。
