# 基于当前实现的原始设计文档工程化修订说明

## 1. 文档目的与输入说明

本文档不是新的业务需求文档，而是对以下三份原始设计稿进行工程化反向修订的依据：

- `可复用用户管理服务设计-2024091602016-谭国照.md`
- `可复用消息通道服务设计-2024091602016-谭国照.md`
- `可复用访问日志服务设计-2024091602016-谭国照.md`

本次修订遵循“以当前实现为准”的原则，评审依据包括：

- 当前服务实现代码
- `docs/contracts/*.md` 冻结契约
- `docs/service-boundaries.md` 服务边界文档
- `docs/specs/topbiz-service-full-spec.md` 与现有 `topbiz-service` 实现

说明：

- 当前工作区未发现这三份文档的 `.doc` 或 `.docx` 原件，仓库中可读取的源稿为 UTF-8 编码的 `.md` 文件。
- 因此本次输出是“基于当前 UTF-8 文稿与实现的修订版说明”，不是对 Word 原件的逐字转写。
- 后续若补充 `.docx` 原件，可以再做一次版式层面的归档转换，但内容基线仍应以本文档为准。

## 2. 总体工程修订原则

### 2.1 以实现反向校正文档，而不是让实现迁就草稿

- 原始设计稿中存在大量理想化、扩展性设想、前端叙事和未冻结接口，这些内容不能继续写成“当前设计已确认事实”。
- 文档必须区分三类状态：
  - `已实现`
  - `占位实现 / 可替换实现`
  - `未来扩展`
- 所有对外接口、路径、请求头、配置约束、数据边界，必须与当前实现一致。

### 2.2 统一四服务关系

- `topbiz-service` 是唯一统一对外入口，负责登录入口、会话、鉴权、授权、OpenFeign 转发和跨服务编排。
- `user-service`、`message-service`、`log-service` 是底层能力服务，不承担统一外部网关职责。
- 底层服务之间禁止直接发生业务 API 相互调用。
- 原始设计稿中如果把底层服务写成“外部直接统一访问入口”，需要全部修正。

### 2.3 文档不能再混写“领域设计”与“产品愿景”

- 需求分析应描述本服务负责什么，不负责什么。
- 接口设计应写冻结契约，不应混入“页面点击后如何展示”的前端说明。
- 技术方案应写当前仓库落地方式，不应把未来接入厂商、MQ、SSO、可观测平台写成现状。

### 2.4 必须显式写明运行模式

- 本仓库定位是“可复用微服务能力底座”，不是“已接入真实生产外围系统的完整业务系统”。
- 每个服务文档都必须补充：
  - 本地模式
  - 生产替换点
  - 占位实现说明
  - 非功能约束

## 3. 三份原始设计稿的统一格式修订要求

三份文档都应按统一结构重写，不建议继续保留当前草稿式结构。

建议结构如下：

1. 文档目的
2. 服务定位与边界
3. 领域对象与主数据归属
4. 角色与调用方
5. 已实现能力范围
6. 对外接口契约
7. 内部接口契约
8. 数据存储与可替换实现
9. 配置项与运行约束
10. 安全与权限模型
11. 占位实现与未来扩展
12. 明确不做的事情

统一写法要求：

- 统一使用二级、三级标题，不要重复出现多个“需求分析”或“接口设计”大段散文。
- 接口一律写成表格或列表，至少包含：路径、方法、用途、调用身份、备注。
- 所有“真实外部接入”都必须打标为“未来扩展”或“可替换实现”。
- 所有默认参数和限制值应写进文档，不能只留在配置文件里。
- 删除草稿中的重复段落、问题清单式自言自语、前端交互叙事、部署畅想式段落。

## 4. 用户服务设计修订

### 4.1 应以当前实现重写的核心定位

`user-service` 的正确定位是：

- 身份与访问控制主数据服务
- 负责用户、角色、权限、部门、账号状态和用户自助资料能力
- 负责注册、登录、验证码、找回密码、解冻等认证支撑能力
- 不负责统一外部门户网关
- 不负责跨服务业务编排

### 4.2 原稿中必须替换的内容

#### 4.2.1 接口路径全部过时

原稿中出现的以下风格需要整体删除：

- `/user/**`
- `/admin/user/**`
- `/org/**`

应统一改为当前实现路径：

- 认证：`/api/users/auth/**`
- 用户自助：`/api/users/**`
- 管理端：`/api/users/admin/**`
- 部门：`/api/users/departments/**` 与 `/api/users/admin/departments/**`

#### 4.2.2 认证模型写错

原稿中多处把 `token` 当作请求参数传递，这与当前实现不一致。

当前实现中应写为：

- 会话上下文优先通过 `X-Session-Key` 解析
- 必要时结合 `X-User-Id`
- 登录返回 `sessionKey` 与过期时间
- 登出通过请求头携带会话，不再使用表单 `token` 字段

#### 4.2.3 密码方案必须修正

原稿中的“前端先 MD5 再传输”不能保留，应明确改为：

- 密码由服务端使用加盐 `PBKDF2WithHmacSHA256` 进行哈希存储
- 文档可以说明传输层依赖 HTTPS
- 不再要求前端承担密码哈希职责

#### 4.2.4 登录能力描述超出实现

原稿中写了：

- QQ 账号密码登录
- QQ 扫码登录
- 微信扫码登录
- 邮箱验证码登录等真实第三方身份接入

当前实现并未落真实第三方接入，因此文档必须改为：

- 当前仅冻结 `loginType=password|thirdParty` 契约
- `thirdParty` 仅表示预留扩展入口
- 真实 QQ、微信、OAuth2、企业统一身份源均属于未来扩展，不得写成已实现

#### 4.2.5 “删除用户”语义与实现冲突

当前实现不对外提供删除用户能力，因此文档必须改为：

- 管理员可新增用户、修改用户、变更状态、重置密码、调整角色与权限
- 用户生命周期控制以启用、禁用、冻结、注销申请等状态管理为主
- 不提供物理删除用户接口

#### 4.2.6 术语错误必须修正

- 原稿中的 `RABC` 应统一修正为 `RBAC`

### 4.3 文档必须补充但原稿缺失的内容

#### 4.3.1 当前冻结接口分层

文档中至少应分成四类接口：

- 架构概览接口：`GET /internal/architecture/overview`
- 认证接口：
  - `POST /api/users/auth/verify-codes`
  - `POST /api/users/auth/register`
  - `POST /api/users/auth/login`
  - `POST /api/users/auth/logout`
- 用户自助接口：
  - `GET /api/users/me`
  - `PUT /api/users/me`
  - `GET /api/users/me/modify-records`
  - `PUT /api/users/me/password`
  - `POST /api/users/password/forgot/send-code`
  - `POST /api/users/password/forgot/reset`
  - `GET /api/users/me/status`
  - `POST /api/users/me/status/unfreeze`
  - `POST /api/users/me/status/cancel`
- 管理端接口：
  - `GET /api/users/admin`
  - `POST /api/users/admin`
  - `PUT /api/users/admin/{userId}`
  - `PATCH /api/users/admin/{userId}/status`
  - `PATCH /api/users/admin/{userId}/authorization`
  - `POST /api/users/admin/{userId}/authorization/permissions:refresh`
  - `POST /api/users/admin/{userId}/password:reset`
- 角色权限接口：
  - `GET /api/users/admin/roles`
  - `GET /api/users/admin/permissions`
  - `POST /api/users/admin/roles`
  - `PUT /api/users/admin/roles/{roleId}`
  - `PUT /api/users/admin/roles/{roleId}/permissions`
  - `DELETE /api/users/admin/roles/{roleId}`
- 部门管理接口：
  - `GET /api/users/admin/departments`
  - `GET /api/users/admin/departments/tree`
  - `POST /api/users/admin/departments`
  - `PUT /api/users/admin/departments/{departmentId}`
  - `DELETE /api/users/admin/departments/{departmentId}`
  - `GET /api/users/admin/departments/{departmentId}/users`
  - `GET /api/users/admin/departments/{departmentId}/attributes/definitions`
  - `POST /api/users/admin/departments/{departmentId}/attributes/definitions`
  - `DELETE /api/users/admin/departments/{departmentId}/attributes/definitions/{attributeKey}`
  - `PUT /api/users/admin/departments/{departmentId}/attributes`
  - `POST /api/users/admin/departments/{departmentId}/members`
  - `DELETE /api/users/admin/departments/{departmentId}/members/{userId}`
  - `POST /api/users/admin/departments/transfer`
  - `GET /api/users/admin/departments/users/{userId}/membership`
  - `PUT /api/users/admin/departments/{departmentId}/members/{userId}/attributes`
  - `POST /api/users/admin/departments/{departmentId}/members/attributes:batch`

#### 4.3.2 工程约束

文档中必须补上当前默认配置：

- 会话时长：`session-ttl-hours: 8`
- 最小密码长度：`password-min-length: 8`
- 密码历史保留数：`password-history-limit: 3`
- 登录失败冻结阈值：`login-failure-freeze-threshold: 5`
- 验证码有效期：`verify-code-ttl-minutes: 5`
- 验证码发送冷却：`verify-code-cooldown-seconds: 60`

#### 4.3.3 权限与角色边界

文档中应明确：

- 预置角色包括 `ADMIN`、`OPERATOR`、`USER`、`AUDITOR`、`OPS_ADMIN`、`DEPARTMENT_ADMIN`
- `DEPARTMENT_ADMIN` 的权限范围限制在部门子树
- 管理端接口不是匿名接口，必须带身份上下文

#### 4.3.4 占位实现说明

文档中必须标注：

- 当前仓储实现以本地内存为主，可替换为数据库持久化
- 验证码、短信、邮件通知属于占位实现
- 第三方身份接入为预留扩展点，不是当前交付内容

### 4.4 用户服务原稿中建议删除或降级的内容

- 删除把多种第三方登录写成既成事实的段落
- 删除“前端 MD5”方案
- 删除物理删除用户设计
- 删除把单点登录、单点登出、统一身份源接入写成当前完成项的表述
- 将“未来统一 SSO / OAuth2 / CAS”统一移到“未来扩展”

## 5. 消息服务设计修订

### 5.1 应以当前实现重写的核心定位

`message-service` 的正确定位是：

- 消息模板、变量、渠道、投递、草稿、调度、重试、收件箱的领域服务
- 对外提供消息发送与查询契约
- 对内提供调度任务和运行时接口
- 当前是可复用消息域骨架，不是已接通真实第三方消息供应商的平台

### 5.2 原稿中必须替换的内容

#### 5.2.1 基础路径错误

原稿中大量使用 `/api/v1/messages/**`、`/api/v1/message/**` 等路径，应统一修正为：

- 对外基础路径：`/api/messages`
- 管理路径：`/api/messages/admin`
- 内部运行时路径：`/api/messages/internal`

#### 5.2.2 资源命名需要统一

原稿中存在：

- `message/templates`
- `messages/draft`
- `users/resolve`

等风格不一致的问题。修订后应统一按当前实现命名：

- 模板：`/api/messages/templates`
- 草稿：`/api/messages/drafts`
- 接收人解析：`/api/messages/receivers/resolve`
- 收件箱：`/api/messages/inbox`

#### 5.2.3 真实供应商接入不能写成已完成

原稿中对短信、邮件、微信、飞书等真实投递能力描述过实，应改为：

- 当前只提供消息领域契约和调度骨架
- 渠道账号、投递器、供应商 SDK 属于可替换实现
- 真正的外部供应商接入为后续扩展项

#### 5.2.4 调度表达方式要工程化

原稿中把 Cron 和定时发送叙述得过于简单，应修正为：

- 调度是消息服务内部能力
- 文档应区分消息发送请求、调度策略、调度任务、重试执行
- 定时任务不是“操作系统 Cron 已接入”的承诺，而是服务内任务模型与运行时触发接口骨架

### 5.3 文档必须补充但原稿缺失的内容

#### 5.3.1 接口分层

文档至少应按三层接口重写：

- 公开业务接口：
  - `POST /api/messages/send`
  - `POST /api/messages/drafts`
  - `POST /api/messages/templates/{templateCode}/preview`
  - `POST /api/messages/variables/fill`
  - `POST /api/messages/variables/validate`
  - `POST /api/messages/schedule/validate`
  - `POST /api/messages/receivers/resolve`
  - `PUT /api/messages/inbox/{inboxId}/read`
  - `GET /api/messages/{messageId}`
  - `GET /api/messages/{messageId}/status`
  - `GET /api/messages/{messageId}/error`
  - `GET /api/messages/templates`
  - `GET /api/messages/templates/{templateCode}`
  - `GET /api/messages/templates/{templateCode}/variables`
  - `GET /api/messages/channels`
  - `GET /api/messages/records`
  - `GET /api/messages/search`
  - `GET /api/messages/inbox`
  - `GET /api/messages/inbox/{inboxId}`
- 管理接口：
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
- 内部运行时接口：
  - `GET /api/messages/internal/runtime`
  - `POST /api/messages/internal/tasks/dispatch/run`
  - `POST /api/messages/internal/tasks/retry/run`

#### 5.3.2 关键请求 DTO

文档必须补齐当前已冻结的发送请求语义，而不是只写“模板 ID、变量、接收人”。

`MessageSendRequest` 当前包括：

- `templateCode`
- `channel`
- `receivers`
- `receiverGroups`
- `variables`
- `dispatchType`
- `scheduledAt`
- `cronExpression`
- `schedulePolicyCode`
- `channelAccountCode`
- `attachmentIds`
- `saveToInbox`

`DispatchTaskRequest` 当前包括：

- `taskCode`
- `messageId`
- `channelCode`
- `channelAccountCode`
- `schedulePolicyCode`
- `plannedAt`
- `sortOrder`

#### 5.3.3 工程约束

文档必须补上：

- `scheduler-enabled: true`
- `retry-enabled: true`
- `max-retry-attempts: 3`

#### 5.3.4 收件箱边界

原稿中应补明确：

- 收件箱是消息服务内部提供的消息域语义
- 它不是外部 IM、邮件服务器或 MQ 的替代物
- `saveToInbox` 只是是否把消息落入站内收件箱的业务开关

#### 5.3.5 占位实现说明

文档必须写明：

- 当前模板、调度、重试、投递、渠道账户是可复用骨架
- 当前没有真实短信、邮件、企业微信、飞书供应商对接
- 当前仓储主要是本地实现，后续可替换为数据库、MQ、任务中心

### 5.4 消息服务原稿中建议删除或降级的内容

- 删除把真实消息供应商能力写成已落地的描述
- 删除混乱的 `/api/v1` 与 `/message`、`/messages` 双风格路径
- 删除“用户在页面中如何一步步点击”的大段前端叙事
- 将“真实调度平台、MQ、渠道 SDK 对接”统一挪到“可替换实现 / 未来扩展”

## 6. 日志服务设计修订

### 6.1 应以当前实现重写的核心定位

`log-service` 的正确定位是：

- 访问日志接入、检索、Trace 查询、指标统计、告警、导出与运行时管理服务
- 当前聚焦“访问日志域能力”，不是完整可观测平台
- 当前实现支持本地存储抽象与可选 ClickHouse 存储，不应默认写成强依赖 ClickHouse 平台

### 6.2 原稿中必须替换的内容

#### 6.2.1 接口路径错误

原稿中出现的 `/api/log/ingest`、`/log/search`、`/log/capture` 等路径，应统一修正为：

- 对外基础路径：`/api/logs`
- 内部路径：`/api/logs/internal`

#### 6.2.2 范围失控

原稿把日志服务写成了一个接近“完整可观测平台”的大而全方案，夹杂：

- 前端页面叙事
- 海量平台化构想
- 检索方式枚举过多
- 与当前实现无关的架构畅想

修订后必须收敛为当前已实现边界：

- 访问日志接入
- 查询检索
- Trace 关联查看
- 指标统计
- 告警规则与告警事件处理
- 导出任务
- 运行时任务触发

#### 6.2.3 查询能力写得过满

原稿中的以下能力不能继续写成当前冻结能力：

- 正则检索
- 复杂布尔组合检索
- 标签检索
- 通配符检索
- 反向检索
- 全套平台化聚合分析

修订后应改为：

- 当前提供基础查询、筛选、Trace 查询和指标查询
- 更复杂的 DSL、正则、聚合能力属于未来扩展

#### 6.2.4 ClickHouse 角色写错

文档必须改为：

- ClickHouse 是可切换后端实现
- 默认配置 `clickhouse-enabled: false`
- 当前实现允许使用本地仓储抽象运行
- 不应把 ClickHouse 写成系统不可缺少的硬依赖

### 6.3 文档必须补充但原稿缺失的内容

#### 6.3.1 接口分层

文档至少应分三类接口：

- 公开查询接口：
  - `GET /api/logs/search`
  - `GET /api/logs/trace/{traceId}`
  - `GET /api/logs/metrics`
  - `GET /api/logs/alerts`
- 公开命令接口：
  - `POST /api/logs/ingest`
  - `POST /api/logs/alerts/{alertId}/status`
  - `POST /api/logs/exports`
- 内部运行时接口：
  - `POST /api/logs/internal/search`
  - `GET /api/logs/internal/runtime`
  - `GET /api/logs/internal/alert-rules`
  - `GET /api/logs/internal/exports`
  - `POST /api/logs/internal/alert-rules`
  - `POST /api/logs/internal/alert-rules/{ruleId}/enabled`
  - `DELETE /api/logs/internal/alert-rules/{ruleId}`
  - `POST /api/logs/internal/tasks/flush`
  - `POST /api/logs/internal/tasks/alerts/evaluate`
  - `POST /api/logs/internal/tasks/exports/run`
  - `POST /api/logs/internal/tasks/exports/cleanup`

#### 6.3.2 工程约束

日志服务文档必须把当前限制值补出来：

- `search.default-window-hours: 24`
- `search.max-window-days: 7`
- `search.max-keyword-length: 100`
- `search.max-page-size: 500`
- `export.default-window-minutes: 60`
- `export.max-rows: 10000`
- `export.file-ttl-hours: 24`
- `buffer.capacity: 10000`
- `buffer.batch-size: 500`
- `storage.max-flush-retries: 3`
- `ingest.max-future-skew-seconds: 300`
- `ingest.max-past-days: 30`
- `ingest.max-message-length: 4000`
- `masking.enabled: true`

#### 6.3.3 已支持的运行模型

文档应明确：

- 当前 ingest 是 HTTP 接入模型
- 后端具备缓冲、批量刷新、导出任务、告警评估任务
- 调度器由服务自身运行时驱动
- 通知默认是控制台输出，可选 webhook

#### 6.3.4 可替换存储说明

文档中必须说明：

- 默认不是强制 ClickHouse
- 已预留 ClickHouse 实现，表结构策略包含：
  - `MergeTree`
  - `PARTITION BY toYYYYMMDD(timestamp)`
  - `ORDER BY (service_name, timestamp, trace_id)`
  - `TTL timestamp + INTERVAL retentionDays DAY`

#### 6.3.5 占位实现说明

文档必须标注：

- 告警通知当前是平台内基础实现
- 对象存储、报表系统、复杂检索引擎、完整 APM/Tracing 平台均未真实接入
- 日志采集 Agent、SDK、Sidecar 等不是当前实现前提

### 6.4 日志服务原稿中建议删除或降级的内容

- 删除完整可观测平台式大而全描述
- 删除未冻结的复杂检索方式说明
- 删除“用户点击下载日志”式前端叙事，改成导出任务模型
- 删除把 ClickHouse 写成不可替代前提的内容
- 将“链路追踪平台化能力”“全栈观测体系”移入未来扩展

## 7. 三份文档都必须补充的公共章节

原稿普遍缺以下工程章节，重写时必须全部补齐：

### 7.1 服务边界

- 本服务负责什么
- 本服务不负责什么
- 主数据是否由本服务持有
- 是否允许其他底层服务直接调用

### 7.2 运行模式

- 本地默认模式
- 可替换生产模式
- 外部依赖是否可选

### 7.3 契约冻结说明

- 哪些接口已冻结
- 哪些 DTO 已冻结
- 哪些仅为预留扩展点

### 7.4 占位实现说明

- 当前哪些能力只是模板实现
- 后续替换点在哪里
- 替换后需要保持哪些契约不变

### 7.5 非功能约束

- 参数长度
- 时间窗口
- 分页上限
- 重试次数
- 脱敏策略
- 会话时长

## 8. 建议的重写成果物

不建议继续直接在原始 3 份草稿上追加零散修补，而应根据本文档重写为以下正式文档：

- `docs/specs/user-service-design-rewritten.md`
- `docs/specs/message-service-design-rewritten.md`
- `docs/specs/log-service-design-rewritten.md`

每份重写文档都应：

- 用当前实现替换原稿中的过时路径和错误模型
- 用“已实现 / 占位 / 扩展”标记各项能力
- 删除产品化空想和未落地的厂商接入叙述
- 保留未来扩展，但必须降级为规划项

## 9. 结论

当前实现已经形成了比原始设计稿更可靠的工程基线，因此后续文档工作不应再以原始草稿为中心，而应以实现、契约和服务边界文档为中心进行重写。

三份原稿中最需要立刻修正的核心问题是：

- 接口路径与当前实现严重不一致
- 认证与密码模型存在错误
- 把未来扩展写成了当前已交付
- 缺失工程约束、占位实现说明和服务边界
- 文档结构混乱，难以作为开发和协作基线

后续如果继续完善文档，建议先按本文档重写三份正式设计稿，再视需要把原始草稿归档到 `docs/archive/`，避免团队继续误用过时设计。
