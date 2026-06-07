# Topbiz Service Contract

## Purpose

`topbiz-service` is the external gateway and orchestration layer for the platform.
It does not own user, message, or log master data.
It authenticates users, enforces permissions, proxies allowed capabilities, and coordinates cross-service workflows.

## Corrected Design Decisions

- `topbiz-service` is the only public entry for cross-service orchestration.
- Bottom services are not allowed to call each other directly through business APIs.
- `topbiz-service` currently uses `Shiro + servlet session bridge` for runtime login state.
  Production deployment uses `Redis Session` through the `prod` profile, while local development keeps the servlet-session bridge.
- Service-to-service calls use `OpenFeign`.
- Orchestration execution records are persisted through a repository abstraction.
  Local development defaults to in-memory storage, and production uses a durable JDBC repository.

## Shared Rules

- All responses use `ApiResponse<T>`.
- Public base path is `/api/topbiz`.
- `topbiz-service` authenticates through `user-service /api/users/auth/login`.
- After login, `topbiz-service` stores `TopbizPrincipal` in session and forwards:
  - `X-User-Id`
  - `X-Session-Key`
- Trace headers are forwarded when present:
  - `X-Trace-Id`
  - `X-Request-Id`
- Anonymous paths:
  - `POST /api/topbiz/auth/login`
  - `POST /api/topbiz/users/auth/verify-codes`
  - `POST /api/topbiz/users/auth/register`
  - `POST /api/topbiz/users/password/forgot/send-code`
  - `POST /api/topbiz/users/password/forgot/reset`
  - `POST /api/topbiz/users/me/status/unfreeze`

## Auth APIs

- `POST /api/topbiz/auth/login`
- `POST /api/topbiz/auth/logout`
- `GET /api/topbiz/auth/session`

## Platform APIs

- `GET /api/topbiz/platform/overview`
- `GET /api/topbiz/platform/architecture`
- `GET /api/topbiz/platform/runtime`

## Orchestration APIs

- `POST /api/topbiz/orchestrations/user-provisioning`
- `POST /api/topbiz/orchestrations/department-transfer`
- `POST /api/topbiz/orchestrations/message-audit`
- `GET /api/topbiz/orchestrations`
- `GET /api/topbiz/orchestrations/{orchestrationId}`

## User Gateway APIs

### User Self and Query

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

### User Admin

- `GET /api/topbiz/users/admin`
- `POST /api/topbiz/users/admin`
- `PUT /api/topbiz/users/admin/{userId}`
- `PATCH /api/topbiz/users/admin/{userId}/status`
- `PATCH /api/topbiz/users/admin/{userId}/authorization`
- `POST /api/topbiz/users/admin/{userId}/authorization/permissions:refresh`
- `POST /api/topbiz/users/admin/{userId}/password:reset`

### Role Admin

- `GET /api/topbiz/users/admin/roles`
- `GET /api/topbiz/users/admin/permissions`
- `POST /api/topbiz/users/admin/roles`
- `PUT /api/topbiz/users/admin/roles/{roleId}`
- `PUT /api/topbiz/users/admin/roles/{roleId}/permissions`
- `DELETE /api/topbiz/users/admin/roles/{roleId}`

### Department Admin

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

## Message Gateway APIs

### Message Use APIs

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

### Message Admin and Runtime

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

## Log Gateway APIs

### Log Query and Operation

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

## Topbiz Permission Set

- `topbiz:admin`
- `topbiz:platform:read`
- `topbiz:architecture:read`
- `topbiz:orchestration:write`
- `topbiz:message:admin`
- `topbiz:log:admin`
- `topbiz:runtime:operate`
