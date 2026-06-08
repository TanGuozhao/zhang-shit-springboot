# 消息服务设计修正稿

对应原稿：`可复用消息通道服务设计-2024091602016-谭国照.md`

## 1. 文档目的

本文档用于替代原始消息服务设计稿中路径不统一、能力边界混乱、真实供应商接入表述过实等问题，作为当前仓库 `message-service` 的正式设计说明。

本文档以当前实现为准，冻结以下内容：

- 服务边界
- 公开业务接口、管理接口、内部运行时接口
- 核心请求/响应 DTO
- 当前已实现的消息领域能力
- 占位实现与可替换实现
- 运行参数与工程约束

## 2. 服务定位

`message-service` 是平台的消息通道领域服务，负责模板、变量、消息、调度任务、重试记录、渠道配置、运营商账户和收件箱能力。

本服务的核心职责是提供可复用的消息发送与查询契约，而不是直接充当真实短信、邮件、微信、飞书供应商的生产接入平台。真实外部供应商接入属于后续替换实现。

本服务不负责用户主数据，不负责统一登录和鉴权入口，也不承担统一外部网关职责。统一对外暴露由 `topbiz-service` 完成。

## 3. 核心领域对象

当前实现已稳定形成以下消息领域对象：

- `Message`：消息主记录，描述一条消息从创建到发送完成的业务状态。
- `Template`：消息模板，包含模板编码、名称、渠道、主题模板、正文模板、启用状态等。
- `VariableDefinition`：变量定义，描述变量名、类型、默认值、是否必填、是否自动填充等。
- `DispatchTask`：调度执行单元，用于表示一条消息面向具体接收人的执行计划。
- `RetryRecord`：重试记录，用于追踪失败后的自动或人工重试过程。
- `CarrierAccount`：运营商账户配置，描述外部通道账户的接入信息。
- `ChannelConfig`：渠道配置，描述渠道编码、类型、发送者、健康状态等。
- `SchedulePolicy`：调度策略定义，描述定时策略与表达式。
- `InboxMessage`：收件箱记录，用于站内查询和已读管理。

## 4. 角色模型与调用方

### 4.1 普通业务调用方

普通业务调用方使用消息发送、模板预览、变量填充、状态查询、收件箱查询等公开业务接口。

### 4.2 管理端调用方

管理端负责：

- 维护模板
- 维护变量
- 配置运营商账户
- 配置渠道
- 管理调度策略和调度任务
- 运行重试任务
- 查询统计结果

### 4.3 内部运行时调用方

内部运行时接口用于：

- 查询运行概览
- 手工触发调度任务执行
- 手工触发重试任务执行

这类接口不应暴露给普通业务调用方。

## 5. 已实现能力范围

当前仓库内 `message-service` 已实现以下能力：

- 模板列表查询
- 模板详情查询
- 模板变量说明查询
- 模板预览
- 变量填充
- 变量校验
- 接收对象解析
- 调度规则校验
- 发送消息
- 保存草稿
- 查询消息详情
- 查询消息状态
- 查询消息失败原因
- 消息记录查询
- 关键词搜索
- 渠道列表查询
- 收件箱列表查询
- 收件箱详情查询
- 收件箱已读标记
- 模板新增/更新
- 模板启用/禁用
- 变量新增/更新
- 变量类型调整
- 变量必填属性调整
- 运营商账户新增
- 渠道配置新增
- 渠道发送者修改
- 调度策略新增
- 调度任务新增
- 调度任务列表查询
- 调度任务改期
- 调度任务取消
- 调度任务立即触发
- 批量运行重试
- 查询消息统计
- 查询运行概览
- 手工执行待处理调度任务
- 手工执行待处理重试任务

## 6. 接口设计

### 6.1 架构概览接口

| 方法 | 路径 | 用途 | 备注 |
| --- | --- | --- | --- |
| `GET` | `/internal/architecture/overview` | 返回服务边界、层次和核心模块概览 | 供内部排障与架构说明使用 |

### 6.2 公开业务接口

基础路径：`/api/messages`

| 方法 | 路径 | 用途 | 关键说明 |
| --- | --- | --- | --- |
| `POST` | `/send` | 发送消息 | 支持即时和定时 |
| `POST` | `/drafts` | 保存草稿 | 不立即进入发送 |
| `POST` | `/templates/{templateCode}/preview` | 预览模板渲染结果 | 根据变量渲染主题与内容 |
| `POST` | `/variables/fill` | 变量回填 | 用默认值或自动填充补齐 |
| `POST` | `/variables/validate` | 变量校验 | 校验必填、类型、模板约束 |
| `POST` | `/schedule/validate` | 调度校验 | 校验调度时间或表达式 |
| `POST` | `/receivers/resolve` | 解析接收对象 | 合并显式接收人与接收组 |
| `PUT` | `/inbox/{inboxId}/read` | 标记已读 | 修改收件箱阅读状态 |
| `GET` | `/{messageId}` | 查询消息详情 | 返回完整消息记录 |
| `GET` | `/{messageId}/status` | 查询消息状态 | 返回状态和关键时间 |
| `GET` | `/{messageId}/error` | 查询失败原因 | 返回最近一次失败信息 |
| `GET` | `/templates` | 查询模板列表 | 支持按渠道过滤 |
| `GET` | `/templates/{templateCode}` | 查询模板详情 | 返回模板和变量 |
| `GET` | `/templates/{templateCode}/variables` | 查询模板变量定义 | 返回变量说明 |
| `GET` | `/channels` | 查询渠道列表 | 返回已配置渠道 |
| `GET` | `/records` | 查询消息记录 | 支持状态、渠道、关键词、接收人和时间范围过滤 |
| `GET` | `/search` | 简化搜索 | 当前只暴露 `keyword` 检索 |
| `GET` | `/inbox` | 查询收件箱列表 | 支持按接收人过滤 |
| `GET` | `/inbox/{inboxId}` | 查询收件箱详情 | 返回单条收件箱记录 |

### 6.3 管理接口

基础路径：`/api/messages/admin`

| 方法 | 路径 | 用途 | 关键说明 |
| --- | --- | --- | --- |
| `POST` | `/templates` | 新增或更新模板 | 当前使用 upsert 语义 |
| `PUT` | `/templates/{templateCode}/status` | 启用/禁用模板 | 禁用后新发送不可再引用 |
| `POST` | `/variables` | 新增或更新变量 | 当前使用 upsert 语义 |
| `PUT` | `/variables/{variableCode}/type` | 修改变量类型 | 调整变量数据类型 |
| `PUT` | `/variables/{variableCode}/required` | 修改变量必填属性 | 控制是否必填 |
| `POST` | `/carrier/accounts` | 新增运营商账户 | 管理供应商账户配置 |
| `POST` | `/channels/config` | 新增渠道配置 | 建立业务渠道与账户映射 |
| `PUT` | `/channels/{channelCode}/sender` | 修改默认发送者 | 更新 sender |
| `POST` | `/schedule/policies` | 新增调度策略 | 新增 cron 或其他策略 |
| `POST` | `/dispatch/tasks` | 新增调度任务 | 直接写入调度执行单元 |
| `GET` | `/dispatch/tasks` | 查询调度任务 | 支持状态和时间范围过滤 |
| `PUT` | `/dispatch/tasks/{taskId}/time` | 调整执行时间 | 用于改期 |
| `PUT` | `/dispatch/tasks/{taskId}/cancel` | 取消任务 | 状态流转为取消 |
| `POST` | `/dispatch/tasks/{taskId}/trigger` | 立即触发任务 | 人工强制执行 |
| `POST` | `/retries/run` | 运行重试 | 处理待重试记录 |
| `GET` | `/statistics` | 查询统计 | 返回总量、成功率、渠道分布、失败原因分布 |

### 6.4 内部运行时接口

基础路径：`/api/messages/internal`

| 方法 | 路径 | 用途 | 关键说明 |
| --- | --- | --- | --- |
| `GET` | `/runtime` | 查询运行概览 | 返回调度、重试、收件箱等运行状态 |
| `POST` | `/tasks/dispatch/run` | 运行待处理调度任务 | 用于本地联调与排障 |
| `POST` | `/tasks/retry/run` | 运行待处理重试 | 用于本地联调与排障 |

## 7. 关键请求与响应 DTO

### 7.1 发送消息请求

`MessageSendRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `templateCode` | 是 | 模板编码 |
| `channel` | 是 | 渠道编码或渠道类型 |
| `receivers` | 否 | 显式接收人列表 |
| `receiverGroups` | 否 | 接收组列表 |
| `variables` | 否 | 模板变量键值对 |
| `dispatchType` | 否 | 调度类型，通常为即时或定时 |
| `scheduledAt` | 否 | 计划发送时间 |
| `cronExpression` | 否 | cron 表达式 |
| `schedulePolicyCode` | 否 | 调度策略编码 |
| `channelAccountCode` | 否 | 运营商账户编码 |
| `attachmentIds` | 否 | 附件 ID 列表 |
| `saveToInbox` | 否 | 是否写入站内收件箱 |

### 7.2 消息响应

`MessageResponse`

| 字段 | 说明 |
| --- | --- |
| `messageId` | 消息主键 |
| `templateCode` | 模板编码 |
| `templateName` | 模板名称 |
| `channel` | 渠道 |
| `status` | 当前状态 |
| `subject` | 渲染后的主题 |
| `content` | 渲染后的内容 |
| `receivers` | 接收人列表 |
| `variables` | 使用的变量 |
| `dispatchType` | 调度类型 |
| `scheduledAt` | 计划执行时间 |
| `cronExpression` | cron 表达式 |
| `batchCode` | 批次码 |
| `createdAt` | 创建时间 |
| `updatedAt` | 更新时间 |
| `sentAt` | 发送时间 |
| `attachmentIds` | 附件 ID 列表 |

### 7.3 草稿请求

`MessageDraftRequest`

| 字段 | 说明 |
| --- | --- |
| `templateCode` | 模板编码 |
| `channel` | 渠道 |
| `receivers` | 接收人列表 |
| `receiverGroups` | 接收组列表 |
| `title` | 草稿标题 |
| `content` | 草稿正文 |
| `variables` | 草稿变量 |
| `scheduledAt` | 草稿计划时间 |
| `cronExpression` | 草稿 cron |
| `attachmentIds` | 附件列表 |

### 7.4 模板 DTO

`TemplateUpsertRequest`

| 字段 | 说明 |
| --- | --- |
| `templateCode` | 模板编码 |
| `templateName` | 模板名称 |
| `channel` | 渠道 |
| `subjectTemplate` | 主题模板 |
| `contentTemplate` | 内容模板 |
| `description` | 描述 |
| `variableCodes` | 关联变量编码列表 |
| `enabled` | 是否启用 |

`TemplateDetailResponse`

| 字段 | 说明 |
| --- | --- |
| `templateCode` | 模板编码 |
| `templateName` | 模板名称 |
| `channel` | 渠道 |
| `subjectTemplate` | 主题模板 |
| `contentTemplate` | 内容模板 |
| `description` | 描述 |
| `enabled` | 是否启用 |
| `variables` | 变量定义详情列表 |

### 7.5 变量 DTO

`VariableDefinitionResponse`

| 字段 | 说明 |
| --- | --- |
| `variableCode` | 变量编码 |
| `variableName` | 变量名称 |
| `description` | 描述 |
| `dataType` | 数据类型 |
| `defaultValue` | 默认值 |
| `required` | 是否必填 |
| `enabled` | 是否启用 |
| `autoFill` | 是否自动填充 |

### 7.6 运营商账户与渠道 DTO

`CarrierAccountRequest`

| 字段 | 说明 |
| --- | --- |
| `carrierName` | 运营商名称 |
| `channelType` | 通道类型 |
| `accountCode` | 账户编码 |
| `apiKey` | 接口密钥 |
| `endpoint` | 接口地址 |
| `signature` | 签名 |
| `enabled` | 是否启用 |

`ChannelConfigRequest`

| 字段 | 说明 |
| --- | --- |
| `channelCode` | 渠道编码 |
| `channelType` | 渠道类型 |
| `carrierName` | 运营商名称 |
| `accountCode` | 账户编码 |
| `sender` | 默认发送者 |
| `enabled` | 是否启用 |
| `healthy` | 是否健康 |
| `description` | 描述 |

### 7.7 调度任务 DTO

`DispatchTaskRequest`

| 字段 | 说明 |
| --- | --- |
| `taskCode` | 任务编码 |
| `messageId` | 消息 ID |
| `channelCode` | 渠道编码 |
| `channelAccountCode` | 账户编码 |
| `schedulePolicyCode` | 调度策略编码 |
| `plannedAt` | 计划执行时间 |
| `sortOrder` | 顺序号 |

`DispatchTaskResponse`

| 字段 | 说明 |
| --- | --- |
| `taskId` | 任务 ID |
| `taskCode` | 任务编码 |
| `messageId` | 消息 ID |
| `receiver` | 接收人 |
| `channelCode` | 渠道编码 |
| `plannedAt` | 计划时间 |
| `actualAt` | 实际执行时间 |
| `status` | 状态 |
| `sortOrder` | 排序 |
| `lastError` | 最近错误 |

### 7.8 统计与运行时 DTO

`MessageStatisticsResponse`

| 字段 | 说明 |
| --- | --- |
| `totalMessages` | 总消息数 |
| `successfulMessages` | 成功数 |
| `failedMessages` | 失败数 |
| `scheduledMessages` | 定时消息数 |
| `successRate` | 成功率 |
| `channelBreakdown` | 渠道分布 |
| `failureReasons` | 失败原因分布 |

`RuntimeOverviewResponse`

| 字段 | 说明 |
| --- | --- |
| `pendingDispatchTasks` | 待执行调度任务数 |
| `processingDispatchTasks` | 执行中任务数 |
| `pendingRetryRecords` | 待重试记录数 |
| `inboxMessages` | 收件箱消息数 |
| `schedulerEnabled` | 调度是否启用 |
| `retryEnabled` | 重试是否启用 |
| `lastDispatchRunAt` | 最近调度运行时间 |
| `lastRetryRunAt` | 最近重试运行时间 |

## 8. 典型请求示例

### 8.1 发送消息

```json
POST /api/messages/send
{
  "templateCode": "NOTICE",
  "channel": "EMAIL",
  "receivers": ["u1001"],
  "variables": {
    "name": "张三",
    "title": "审批通知"
  },
  "dispatchType": "IMMEDIATE",
  "saveToInbox": true
}
```

### 8.2 定时发送

```json
POST /api/messages/send
{
  "templateCode": "NOTICE",
  "channel": "EMAIL",
  "receiverGroups": ["OPS_GROUP"],
  "variables": {
    "name": "运维组"
  },
  "dispatchType": "SCHEDULED",
  "scheduledAt": "2026-06-08T09:00:00Z",
  "schedulePolicyCode": "WORKDAY_9AM"
}
```

### 8.3 新增模板

```json
POST /api/messages/admin/templates
{
  "templateCode": "NOTICE",
  "templateName": "通用通知模板",
  "channel": "EMAIL",
  "subjectTemplate": "通知：{{title}}",
  "contentTemplate": "您好，{{name}}，您有一条新的通知。",
  "description": "用于站内公告和邮件通知",
  "variableCodes": ["title", "name"],
  "enabled": true
}
```

### 8.4 新增调度任务

```json
POST /api/messages/admin/dispatch/tasks
{
  "taskCode": "TASK-001",
  "messageId": "MSG-1001",
  "channelCode": "EMAIL_MAIN",
  "channelAccountCode": "MAIL_ACCT_01",
  "schedulePolicyCode": "WORKDAY_9AM",
  "plannedAt": "2026-06-08T09:00:00Z",
  "sortOrder": 1
}
```

## 9. 工程设计

### 9.1 服务边界

- 本服务负责什么：
  - 消息模板和变量
  - 消息发送主流程
  - 草稿
  - 调度任务
  - 重试记录
  - 渠道配置
  - 运营商账户配置
  - 收件箱
  - 统计与运行态
- 本服务不负责什么：
  - 用户主数据
  - 平台统一登录与会话
  - 统一对外网关
  - 分布式事务总控
  - 真正的短信、邮件、飞书、微信供应商生产接入
- 主数据是否由本服务持有：
  - 是。消息、模板、变量、调度任务、收件箱均由本服务持有。
- 是否允许其他底层服务直接调用：
  - 平台规则上不建议底层服务相互直接走业务 API。
  - 跨服务业务编排统一通过 `topbiz-service`。

### 9.2 运行模式

#### 本地默认模式

- 启动命令：

```powershell
.\mvnw.cmd -pl message-service -am spring-boot:run
```

- 默认端口：`8082`
- 默认仓储：本地内存实现
- 默认可运行调度与重试骨架
- 无需数据库、MQ、真实供应商 SDK 也可启动和联调

#### 可替换生产模式

- 模板、消息、调度任务、收件箱可替换为数据库持久化
- 调度器可替换为专业任务平台
- 投递器可替换为短信、邮件、企业微信、飞书等供应商实现
- 重试可替换为 MQ 或异步任务系统

#### 外部依赖是否可选

- 当前阶段全部可选
- 没有外部短信/邮件服务时，本服务仍能完成平台骨架联调

### 9.3 契约冻结说明

#### 已冻结接口

- `/api/messages/send`
- `/api/messages/drafts`
- `/api/messages/templates/{templateCode}/preview`
- `/api/messages/variables/fill`
- `/api/messages/variables/validate`
- `/api/messages/schedule/validate`
- `/api/messages/receivers/resolve`
- `/api/messages/inbox/{inboxId}/read`
- `/api/messages/{messageId}`
- `/api/messages/{messageId}/status`
- `/api/messages/{messageId}/error`
- `/api/messages/templates`
- `/api/messages/templates/{templateCode}`
- `/api/messages/templates/{templateCode}/variables`
- `/api/messages/channels`
- `/api/messages/records`
- `/api/messages/search`
- `/api/messages/inbox`
- `/api/messages/inbox/{inboxId}`
- `/api/messages/admin/**`
- `/api/messages/internal/**`
- `/internal/architecture/overview`

#### 已冻结 DTO

- `MessageSendRequest`
- `MessageDraftRequest`
- `MessageResponse`
- `TemplateUpsertRequest`
- `TemplateDetailResponse`
- `VariableDefinitionResponse`
- `CarrierAccountRequest`
- `ChannelConfigRequest`
- `DispatchTaskRequest`
- `DispatchTaskResponse`
- `MessageStatisticsResponse`
- `RuntimeOverviewResponse`

#### 仅为预留扩展点

- 真实第三方消息供应商接入
- MQ 异步投递
- 外部任务调度中心
- 复杂策略中心
- 大规模附件服务

### 9.4 占位实现与可替换实现

- 当前发送链路是消息域骨架，不是生产级供应商接入实现。
- 当前渠道账户、发送者、通道健康状态都是可替换实现。
- 当前收件箱是消息域内部抽象，不是外部 IM 或邮件服务器。
- 当前调度和重试是服务内部骨架，可替换为任务调度平台或消息队列。
- 当前记录与查询主要基于本地仓储抽象，可替换为数据库或检索引擎。

### 9.5 默认配置与约束

当前 `application.yml` 已冻结以下默认配置：

- `scheduler-enabled: true`
- `retry-enabled: true`
- `max-retry-attempts: 3`

工程含义如下：

- 服务默认开启调度能力
- 服务默认开启失败重试能力
- 单条消息或调度任务最多重试 3 次

## 10. 对原稿的具体修正

本次修正明确替换了原稿中的以下问题：

- 原稿中的 `/api/v1/message/**`、`/api/v1/messages/**`、`/api/messages/**` 三套路径全部收敛为 `/api/messages/**`
- 原稿中管理接口、业务接口、内部接口混写，当前已正式分为公开业务层、管理层、内部运行时层
- 原稿中把真实短信、邮件、微信、飞书通道写成既成事实，现统一改为可替换实现
- 原稿中对定时发送只描述为“Cron”，当前已落地为调度策略、调度任务和重试执行模型
- 原稿中接收对象解析与用户主数据边界不清，现修正为消息服务只处理接收对象解析，不持有用户主数据
- 原稿中没有明确收件箱边界，现补充为消息服务内部的消息接收语义，不替代外部 IM 或邮件系统

## 11. 后续扩展建议

在不破坏当前契约的前提下，后续可继续扩展：

- 真实邮件、短信、微信、飞书投递器
- 数据库持久化
- MQ 异步投递
- 定时任务中心
- 失败重试死信处理
- 附件实际上传与对象存储
- 与 `topbiz-service` 的统一权限接入

但这些扩展不应再改动本文档中已冻结的公开路径和核心 DTO 结构。
