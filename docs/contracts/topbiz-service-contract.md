# Topbiz 服务契约

## 目的

`topbiz-service` 是整个平台的对外网关与编排层。
它不持有用户、消息、日志主数据。
它负责用户认证、权限校验、能力代理以及跨服务流程编排。

## 已修正的设计决策

- `topbiz-service` 是跨服务编排的唯一公开入口。
- 底层服务之间不允许直接通过业务 API 相互调用。
- `topbiz-service` 当前使用 `Shiro + Servlet Session 桥接` 维护运行时登录态。
  生产环境通过 `prod` profile 启用 `Redis Session`，本地开发仍保留 Servlet Session 桥接方式。
- 服务间调用统一使用 `OpenFeign`。
- `topbiz-service` 对外持有平台外层 Session，底层认证仍然以 `user-service` 返回的 `sessionKey` 为准。
- 第三方登录当前采用 `OAuth authorize/callback + user-service 绑定登录` 模型。
- 当前 provider 入口支持 `QQ`、`WECHAT`，但真实开放平台换码仍是可替换实现。
- 编排执行记录通过仓储抽象持久化。
  本地开发默认使用内存存储，生产模式使用可持久化的 JDBC 仓储。

## 通用规则

- 所有响应统一使用 `ApiResponse<T>`。
- 对外公开基础路径为 `/api/topbiz`。
- `topbiz-service` 通过 `user-service` 的下列认证接口完成底层认证：
  - `POST /api/users/auth/login`
  - `POST /api/users/auth/email/login`
  - `POST /api/users/auth/third-party/login`
- 登录成功后，`topbiz-service` 会把 `TopbizPrincipal` 写入 Session，并向下游透传：
  - `X-User-Id`
  - `X-Session-Key`
- `topbiz-service` 对客户端返回：
  - `sessionId`：平台外层 Session 标识
  - `sessionKey`：底层 `user-service` 会话键
- 如请求中存在链路追踪头，也会继续透传：
  - `X-Trace-Id`
  - `X-Request-Id`
- 匿名可访问路径：
  - `POST /api/topbiz/auth/login`
  - `POST /api/topbiz/auth/email/send-code`
  - `POST /api/topbiz/auth/email/login`
  - `GET /api/topbiz/auth/oauth/*/authorize`
  - `GET /api/topbiz/auth/oauth/*/callback`
  - `POST /api/topbiz/users/auth/verify-codes`
  - `POST /api/topbiz/users/auth/register`
  - `POST /api/topbiz/users/password/forgot/send-code`
  - `POST /api/topbiz/users/password/forgot/reset`
  - `POST /api/topbiz/users/me/status/unfreeze`

## 认证接口

- `POST /api/topbiz/auth/login`
- `POST /api/topbiz/auth/email/send-code`
- `POST /api/topbiz/auth/email/login`
- `GET /api/topbiz/auth/oauth/{provider}/authorize`
- `GET /api/topbiz/auth/oauth/{provider}/callback`
- `POST /api/topbiz/auth/logout`
- `GET /api/topbiz/auth/session`

### 认证说明

- 密码登录由 `topbiz-service` 调用 `user-service /api/users/auth/login` 完成。
- 邮箱验证码登录由 `topbiz-service` 调用 `user-service /api/users/auth/email/send-code` 与 `/api/users/auth/email/login` 完成。
- 第三方登录对外走 `authorize/callback`，回调后由 `topbiz-service` 调用 `user-service /api/users/auth/third-party/login` 完成绑定或自动注册。
- OAuth state 由 `topbiz-service` 本地维护并校验。
- 当前 OAuth 用户资料交换使用 `MockTopbizOAuthProviderClient`，属于可替换实现。

## 平台接口

- `GET /api/topbiz/platform/overview`
- `GET /api/topbiz/platform/architecture`
- `GET /api/topbiz/platform/runtime`

## 编排接口

- `POST /api/topbiz/orchestrations/user-provisioning`
- `POST /api/topbiz/orchestrations/department-transfer`
- `POST /api/topbiz/orchestrations/message-audit`
- `GET /api/topbiz/orchestrations`
- `GET /api/topbiz/orchestrations/{orchestrationId}`

## 用户网关接口

### 用户自助与查询

- `POST /api/topbiz/users/auth/verify-codes`
- `POST /api/topbiz/users/auth/register`
- `GET /api/topbiz/users/me`
- `PUT /api/topbiz/users/me`
- `GET /api/topbiz/users/me/modify-records`
- `PUT /api/topbiz/users/me/password`
- `POST /api/topbiz/users/password/forgot/send-code`
- `POST /api/topbiz/users/password/forgot/reset`
- `GET /api/topbiz/users/me/status`
- `POST /api/topbiz/users/me/status/unfreeze`
- `POST /api/topbiz/users/me/status/cancel`
- `GET /api/topbiz/users/{userId}`
- `GET /api/topbiz/users/{userId}/permissions`
- `GET /api/topbiz/users/{userId}/roles`
- `GET /api/topbiz/users/departments/{deptId}`

### 用户管理

- `GET /api/topbiz/users/admin`
- `POST /api/topbiz/users/admin`
- `PUT /api/topbiz/users/admin/{userId}`
- `PATCH /api/topbiz/users/admin/{userId}/status`
- `PATCH /api/topbiz/users/admin/{userId}/authorization`
- `POST /api/topbiz/users/admin/{userId}/authorization/permissions:refresh`
- `POST /api/topbiz/users/admin/{userId}/password:reset`

### 角色管理

- `GET /api/topbiz/users/admin/roles`
- `GET /api/topbiz/users/admin/permissions`
- `POST /api/topbiz/users/admin/roles`
- `PUT /api/topbiz/users/admin/roles/{roleId}`
- `PUT /api/topbiz/users/admin/roles/{roleId}/permissions`
- `DELETE /api/topbiz/users/admin/roles/{roleId}`

### 部门管理

- `GET /api/topbiz/users/admin/departments`
- `GET /api/topbiz/users/admin/departments/tree`
- `POST /api/topbiz/users/admin/departments`
- `PUT /api/topbiz/users/admin/departments/{departmentId}`
- `DELETE /api/topbiz/users/admin/departments/{departmentId}`
- `GET /api/topbiz/users/admin/departments/{departmentId}/users`
- `GET /api/topbiz/users/admin/departments/{departmentId}/attributes/definitions`
- `POST /api/topbiz/users/admin/departments/{departmentId}/attributes/definitions`
- `DELETE /api/topbiz/users/admin/departments/{departmentId}/attributes/definitions/{attributeKey}`
- `PUT /api/topbiz/users/admin/departments/{departmentId}/attributes`
- `POST /api/topbiz/users/admin/departments/{departmentId}/members`
- `DELETE /api/topbiz/users/admin/departments/{departmentId}/members/{userId}`
- `POST /api/topbiz/users/admin/departments/transfer`
- `GET /api/topbiz/users/admin/departments/users/{userId}/membership`
- `PUT /api/topbiz/users/admin/departments/{departmentId}/members/{userId}/attributes`
- `POST /api/topbiz/users/admin/departments/{departmentId}/members/attributes:batch`

## 消息网关接口

### 消息使用接口

- `POST /api/topbiz/messages/send`
- `POST /api/topbiz/messages/drafts`
- `POST /api/topbiz/messages/templates/{templateCode}/preview`
- `POST /api/topbiz/messages/variables/fill`
- `POST /api/topbiz/messages/variables/validate`
- `POST /api/topbiz/messages/schedule/validate`
- `POST /api/topbiz/messages/receivers/resolve`
- `PUT /api/topbiz/messages/inbox/{inboxId}/read`
- `GET /api/topbiz/messages/{messageId}`
- `GET /api/topbiz/messages/{messageId}/status`
- `GET /api/topbiz/messages/{messageId}/error`
- `GET /api/topbiz/messages/templates`
- `GET /api/topbiz/messages/templates/{templateCode}`
- `GET /api/topbiz/messages/templates/{templateCode}/variables`
- `GET /api/topbiz/messages/channels`
- `GET /api/topbiz/messages/records`
- `GET /api/topbiz/messages/search`
- `GET /api/topbiz/messages/inbox`
- `GET /api/topbiz/messages/inbox/{inboxId}`

### 消息管理与运行时接口

- `POST /api/topbiz/messages/admin/templates`
- `PUT /api/topbiz/messages/admin/templates/{templateCode}/status`
- `POST /api/topbiz/messages/admin/variables`
- `PUT /api/topbiz/messages/admin/variables/{variableCode}/type`
- `PUT /api/topbiz/messages/admin/variables/{variableCode}/required`
- `POST /api/topbiz/messages/admin/carrier/accounts`
- `POST /api/topbiz/messages/admin/channels/config`
- `PUT /api/topbiz/messages/admin/channels/{channelCode}/sender`
- `POST /api/topbiz/messages/admin/schedule/policies`
- `POST /api/topbiz/messages/admin/dispatch/tasks`
- `GET /api/topbiz/messages/admin/dispatch/tasks`
- `PUT /api/topbiz/messages/admin/dispatch/tasks/{taskId}/time`
- `PUT /api/topbiz/messages/admin/dispatch/tasks/{taskId}/cancel`
- `POST /api/topbiz/messages/admin/dispatch/tasks/{taskId}/trigger`
- `POST /api/topbiz/messages/admin/retries/run`
- `GET /api/topbiz/messages/admin/statistics`
- `GET /api/topbiz/messages/internal/runtime`
- `POST /api/topbiz/messages/internal/tasks/dispatch/run`
- `POST /api/topbiz/messages/internal/tasks/retry/run`

## 日志网关接口

### 日志查询与操作接口

- `POST /api/topbiz/logs/ingest`
- `GET /api/topbiz/logs/search`
- `GET /api/topbiz/logs/trace/{traceId}`
- `GET /api/topbiz/logs/metrics`
- `GET /api/topbiz/logs/alerts`
- `POST /api/topbiz/logs/alerts/{alertId}/status`
- `POST /api/topbiz/logs/exports`
- `POST /api/topbiz/logs/internal/search`
- `GET /api/topbiz/logs/internal/runtime`
- `GET /api/topbiz/logs/internal/alert-rules`
- `GET /api/topbiz/logs/internal/exports`
- `POST /api/topbiz/logs/internal/alert-rules`
- `POST /api/topbiz/logs/internal/alert-rules/{ruleId}/enabled`
- `DELETE /api/topbiz/logs/internal/alert-rules/{ruleId}`
- `POST /api/topbiz/logs/internal/tasks/flush`
- `POST /api/topbiz/logs/internal/tasks/alerts/evaluate`
- `POST /api/topbiz/logs/internal/tasks/exports/run`
- `POST /api/topbiz/logs/internal/tasks/exports/cleanup`

## Topbiz 权限集合

- `topbiz:admin`
- `topbiz:platform:read`
- `topbiz:architecture:read`
- `topbiz:orchestration:write`
- `topbiz:message:admin`
- `topbiz:log:admin`
- `topbiz:runtime:operate`
