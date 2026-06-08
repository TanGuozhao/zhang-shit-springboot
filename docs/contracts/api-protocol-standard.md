# 统一 API 协议规范

## 1. 文档目标

本文档定义平台内各服务统一遵守的 API 协议规范，适用于：

- `topbiz-service` 对外网关接口
- `user-service`、`message-service`、`log-service` 业务接口
- 服务间调用接口与内部运行态接口

本文档以当前项目已落地实现为基准，目标是统一新增接口设计，减少字段、路径、鉴权、错误码和返回格式不一致的问题。

## 2. 适用范围

当前项目接口分为三类：

- 对外网关接口：`/api/topbiz/**`
- 基础业务服务接口：`/api/users/**`、`/api/messages/**`、`/api/logs/**`
- 内部接口：
  - 架构说明类：`/internal/**`
  - 服务内部运维/任务类：`/api/**/internal/**`

新增接口必须先归类，再按对应规则设计。

## 3. 协议总原则

- 所有接口统一返回 JSON。
- 所有业务响应统一使用 `ApiResponse<T>` 包装。
- 所有字段名统一使用 `camelCase`。
- 路径命名优先使用名词复数和层级资源表达。
- 查询使用 `GET` + query 参数；创建使用 `POST`；全量更新使用 `PUT`；局部变更使用 `PATCH`；删除使用 `DELETE`。
- 当前项目成功响应默认使用 `HTTP 200`，包括新增、修改、删除、触发类操作。
- 破坏性协议变更不得直接覆盖，必须先更新契约文档并评审。

## 4. 基础路径约定

### 4.1 对外网关路径

- 统一前缀：`/api/topbiz`
- 示例：
  - `/api/topbiz/auth/login`
  - `/api/topbiz/users/me`
  - `/api/topbiz/messages/templates`
  - `/api/topbiz/logs/search`

### 4.2 业务服务路径

- 用户服务：`/api/users`
- 消息服务：`/api/messages`
- 日志服务：`/api/logs`

示例：

- `/api/users/auth/login`
- `/api/messages/send`
- `/api/logs/trace/{traceId}`

### 4.3 内部接口路径

- 纯内部说明接口：`/internal/...`
- 内部任务/运行态接口：`/api/{service}/internal/...`

示例：

- `/internal/architecture/overview`
- `/api/messages/internal/runtime`
- `/api/logs/internal/tasks/flush`

说明：

- `/internal/**` 不作为外部业务契约暴露。
- `/api/**/internal/**` 仅用于内部任务、调试、运行态查询和运维动作。

## 5. HTTP 方法规范

### 5.1 标准资源操作

- `GET`：查询资源，不产生副作用
- `POST`：创建资源或触发一次性命令
- `PUT`：整体更新资源
- `PATCH`：局部更新资源状态或局部字段
- `DELETE`：删除资源

### 5.2 当前项目允许的动作型接口

当前项目已存在部分命令式接口，新增接口可在以下场景继续沿用：

- 难以抽象为资源创建的动作触发
- 批处理动作
- 校验、预览、解析、刷新类命令

推荐形式：

- 子路径动作：`POST /api/messages/send`
- 资源动作：`POST /api/users/admin/{userId}/authorization/permissions:refresh`
- 批处理动作：`POST /api/users/admin/departments/{departmentId}/members/attributes:batch`

约束：

- 动作路径必须表达明确业务语义。
- 优先资源化设计，只有资源语义不自然时才允许动作型接口。
- 批量动作建议使用 `:batch`、`:refresh`、`:reset` 这类后缀，避免随意扩散。

## 6. 请求头规范

### 6.1 通用请求头

- `Content-Type: application/json`
- `Accept: application/json`
- `X-Trace-Id`：链路追踪 ID，可由调用方传入；未传入时服务自动生成并回写
- `X-Request-Id`：请求级唯一 ID，建议由网关或调用方透传

### 6.2 认证与上下文请求头

当前项目实际使用以下上下文字段：

- `X-User-Id`：当前登录用户 ID
- `X-Session-Key`：底层用户会话标识

说明：

- `topbiz-service` 作为网关，会向下游服务透传 `X-User-Id` 和 `X-Session-Key`。
- `topbiz-service` 会透传 `X-Trace-Id` 和 `X-Request-Id`。
- 底层服务一般不直接解析浏览器 Session，而以请求头上下文为准。

### 6.3 响应头规范

- `X-Trace-Id`：服务端必须在响应头中返回最终使用的追踪 ID

## 7. 统一响应格式

### 7.1 响应包装结构

所有业务接口统一返回如下结构：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {}
}
```

字段定义：

- `success`：是否成功
- `code`：业务结果码，不等同于 HTTP 状态码
- `message`：结果说明
- `data`：业务数据，失败时通常为 `null`

### 7.2 成功响应示例

#### 查询类

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "userId": 1001,
    "account": "admin",
    "displayName": "System Admin"
  }
}
```

#### 无返回体操作类

```json
{
  "success": true,
  "code": "OK",
  "message": "logout success",
  "data": null
}
```

### 7.3 失败响应示例

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "email must not be blank",
  "data": null
}
```

## 8. 分页协议

当前项目统一分页结果结构为：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "total": 125,
    "list": [
      {
        "userId": 1001,
        "account": "admin"
      }
    ]
  }
}
```

字段定义：

- `total`：总记录数
- `list`：当前页数据列表

分页请求参数统一建议：

- `pageNum`：页码，从 `1` 开始
- `pageSize`：每页条数

约束：

- 分页查询统一使用 query 参数，不放入 body。
- 非分页列表接口仅在数据量可控时返回数组。

## 9. 参数设计规范

### 9.1 Path 参数

- 用于资源唯一标识
- 参数名必须具备明确业务语义

示例：

- `/api/users/{userId}`
- `/api/messages/{messageId}`
- `/api/logs/trace/{traceId}`

### 9.2 Query 参数

用于：

- 分页
- 筛选
- 搜索
- 排序

示例：

- `GET /api/users/admin?pageNum=1&pageSize=20`
- `GET /api/messages/templates?channel=email`
- `GET /api/messages/inbox?receiver=alice@example.com`

约束：

- 参数名统一使用 `camelCase`
- 布尔值使用 `true/false`
- 时间范围建议显式命名，如 `startTime`、`endTime`

### 9.3 Body 参数

- `POST`、`PUT`、`PATCH` 默认使用 JSON body
- 请求体对象命名统一为 `XXXRequest`
- 响应体对象命名统一为 `XXXResponse`

约束：

- 不允许把复杂查询条件拆散进大量自定义 header
- 不允许同一语义同时出现在 path、query、body 三处

## 10. 字段命名与类型规范

### 10.1 命名规范

- JSON 字段统一使用 `camelCase`
- 布尔字段优先使用可读语义，如 `enabled`、`required`
- ID 字段统一使用 `xxxId`
- 编码型字段统一使用 `xxxCode`
- 名称型字段统一使用 `xxxName`
- 时间字段统一使用 `xxxTime`、`createdAt`、`updatedAt` 等稳定命名

### 10.2 类型规范

- ID 使用数值或字符串，但同一资源类型必须统一
- 金额禁止使用浮点；如涉及金额，统一使用字符串或最小货币单位整数
- 时间统一使用 ISO-8601 字符串，建议 UTC 或带时区偏移

示例：

```json
{
  "createdAt": "2026-06-07T23:10:00+08:00"
}
```

### 10.3 枚举规范

- 枚举值统一使用稳定字符串
- 枚举字段文档必须说明可选值

示例：

```json
{
  "status": "ACTIVE"
}
```

## 11. 错误处理规范

### 11.1 HTTP 状态码

当前项目统一规则如下：

- `200 OK`：业务成功
- `400 Bad Request`：参数错误、通用业务异常
- `401 Unauthorized`：认证失败
- `403 Forbidden`：无权限
- `500 Internal Server Error`：服务内部异常

如确有必要，允许扩展：

- `404 Not Found`：资源不存在
- `409 Conflict`：状态冲突、重复创建

### 11.2 统一错误码

当前项目已出现且建议复用的错误码包括：

- `OK`
- `VALIDATION_ERROR`
- `INTERNAL_ERROR`
- `AUTHENTICATION_FAILED`
- `FORBIDDEN`
- `REMOTE_CALL_FAILED`

业务模块新增错误码时遵循以下原则：

- 大写英文加下划线
- 语义稳定
- 同一类错误不要重复造多个近义码

推荐示例：

- `USER_NOT_FOUND`
- `ROLE_NOT_FOUND`
- `DEPARTMENT_HAS_CHILDREN`
- `MESSAGE_TEMPLATE_DISABLED`

### 11.3 校验错误

参数校验失败统一返回：

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "field A invalid; field B must not be blank",
  "data": null
}
```

说明：

- 当前实现将多个字段错误拼接为单个 `message`
- 如未来需要结构化字段错误，必须在全项目层面统一升级，不得单个服务自行扩展不同格式

## 12. 认证与权限规范

### 12.1 网关层

- 对外客户端优先调用 `topbiz-service`
- `topbiz-service` 负责登录态管理、权限校验和下游上下文透传

### 12.2 下游服务

- 下游服务通过 `X-User-Id`、`X-Session-Key` 获取调用身份
- 需要操作人语义的后台接口，必须校验操作用户身份和权限

### 12.3 匿名接口

匿名接口必须显式收敛，仅允许登录、验证码、注册、密码找回、OAuth 回调等场景。

## 13. 链路追踪规范

- 调用方可传入 `X-Trace-Id`
- 若未传入，由服务端自动生成 UUID
- 服务端必须在响应头回传 `X-Trace-Id`
- 网关调用下游时应透传 `X-Trace-Id` 和 `X-Request-Id`

## 14. 资源建模规范

### 14.1 优先资源化

优先使用资源表达：

- `GET /api/users/{userId}`
- `PUT /api/users/admin/{userId}`
- `DELETE /api/users/admin/roles/{roleId}`

### 14.2 允许子资源

当存在明确从属关系时，允许子资源设计：

- `/api/users/admin/departments/{departmentId}/users`
- `/api/messages/templates/{templateCode}/variables`

### 14.3 动作接口约束

以下情况可设计为动作接口：

- 登录、发送验证码、触发任务、批量操作、预览、校验、刷新

示例：

- `POST /api/topbiz/auth/login`
- `POST /api/messages/templates/{templateCode}/preview`
- `POST /api/logs/internal/tasks/exports/run`

## 15. 版本管理规范

当前项目路径中未引入 `/v1`、`/v2` 版本号，因此采用“契约冻结 + 评审变更”策略：

- 已发布接口默认视为稳定契约
- 修改 path、method、字段名、字段类型、返回结构、错误码语义，都属于协议变更
- 协议变更前必须先更新 `docs/contracts/` 下的契约文档
- 非兼容变更必须先给出迁移方案，再安排落地

在未统一决定版本策略前，新增接口不得自行引入 `/v1`

## 16. 文档编写规范

每个接口文档至少应包含：

- 接口名称
- 请求方法
- 请求路径
- 权限要求
- 请求头
- Path 参数
- Query 参数
- Body 示例
- 成功响应示例
- 失败响应示例
- 错误码说明

## 17. 标准示例

### 17.1 登录接口

`POST /api/topbiz/auth/login`

请求：

```json
{
  "account": "admin",
  "password": "******"
}
```

响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "userId": 1001,
    "sessionId": "8f8c1d6b...",
    "sessionKey": "usk_9f2c..."
  }
}
```

### 17.2 分页查询接口

`GET /api/users/admin?pageNum=1&pageSize=20`

响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "total": 2,
    "list": [
      {
        "userId": 1001,
        "account": "admin",
        "displayName": "System Admin"
      },
      {
        "userId": 1002,
        "account": "operator",
        "displayName": "Ops User"
      }
    ]
  }
}
```

### 17.3 校验失败示例

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "account must not be blank; password must not be blank",
  "data": null
}
```

## 18. 新增接口落地检查清单

- 是否放在正确的服务和路径前缀下
- 是否优先采用资源化路径
- 是否统一返回 `ApiResponse<T>`
- 是否使用 `camelCase` 字段
- 是否复用 `pageNum`、`pageSize`、`total`、`list`
- 是否明确需要 `X-User-Id`、`X-Session-Key`
- 是否支持 `X-Trace-Id` 透传
- 是否复用已有错误码，而不是重复发明
- 是否已更新对应契约文档

## 19. 当前项目结论

从当前代码实现出发，平台统一 API 协议基线如下：

- 成功返回统一为 `ApiResponse<T>`
- 分页返回统一为 `PagedResult<T> = { total, list }`
- 认证上下文统一通过 `X-User-Id`、`X-Session-Key` 透传
- 链路追踪统一通过 `X-Trace-Id` 透传并回写
- 外部接口统一走 `/api/topbiz/**`
- 底层服务统一走 `/api/users/**`、`/api/messages/**`、`/api/logs/**`
- 内部接口统一使用 `/internal/**` 或 `/api/**/internal/**`

后续新增或重构接口，均以本规范为统一协议基准。
