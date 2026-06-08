# 用户服务契约

## 目的

`user-service` 持有平台的身份与访问控制主数据。
它为 `topbiz-service` 提供用户生命周期、认证支撑、RBAC、部门组织结构以及用户自助账号能力。

## 已修正的设计决策

- 对外 API 基础路径统一为 `/api/users`，不再混用 `/user/**`、`/admin/user/**`、`/org/**`。
- 正确模型名称应为 `RBAC`，原设计文档某一处写成了 `RABC`，这里已纠正。
- 密码采用服务端加盐 `PBKDF2WithHmacSHA256` 哈希。
  原文中“前端先 MD5”这一要求已被明确否决。
- 不对外暴露“删除用户”能力。原文档对此存在冲突，这里保留更早且更安全的规则：
  管理员可以新增、修改、禁用、冻结、重置，但不能删除用户。
- 部门属性定义删除实现为级联清理部门值和成员值，
  这一实现比“被使用时禁止删除”的约束更贴合后续 API 设计稿。
- 已冻结给 `topbiz-service` 使用的查询路径继续保持稳定，但现在要求必须带身份上下文。

## 通用规则

- 所有响应统一使用 `ApiResponse<T>`。
- 当前用户优先通过 `X-Session-Key` 解析，其次才使用 `X-User-Id`。
- 给 `topbiz-service` 预留的冻结查询路径仍然可用，但不属于匿名接口。
- 预置账号：
  - `admin / admin123`
  - `operator / operator123`
- 预置角色：
  - `ADMIN`
  - `OPERATOR`
  - `USER`
  - `AUDITOR`
  - `OPS_ADMIN`
  - `DEPARTMENT_ADMIN`
- 演示验证码统一为 `123456`。
- 支持的验证码场景：
  - `REGISTER`
  - `FORGOT_PASSWORD`
  - `UNFREEZE`
  - `EMAIL_LOGIN`
- 部门管理员权限范围：
  - `ADMIN` 可访问全部部门与用户。
  - `DEPARTMENT_ADMIN` 仅可访问自己部门子树范围内的数据。
- 第三方身份绑定主数据由 `ExternalIdentityBinding` 持有。
- 邮箱验证码投递当前通过 `UserVerificationCodeSender` 抽象发送，默认实现是日志占位发送。

## 内部接口

### 架构概览

- 方法：`GET`
- 路径：`/internal/architecture/overview`

## 认证接口

### 发送验证码

- 方法：`POST`
- 路径：`/api/users/auth/verify-codes`

### 注册

- 方法：`POST`
- 路径：`/api/users/auth/register`

### 登录

- 方法：`POST`
- 路径：`/api/users/auth/login`
- 说明：
  - 支持 `loginType=password|thirdParty`
  - 第三方登录必须传入 `thirdPartyInfo`
  - 返回 `sessionKey` 和 `expireTime`
  - 密码连续失败达到阈值后会自动冻结账户

### 发送邮箱登录验证码

- 方法：`POST`
- 路径：`/api/users/auth/email/send-code`
- 说明：
  - 场景固定为 `EMAIL_LOGIN`
  - 当前演示验证码统一为 `123456`
  - 当前默认只记录日志，不做真实邮件投递

### 邮箱验证码登录

- 方法：`POST`
- 路径：`/api/users/auth/email/login`
- 说明：
  - 支持 `autoRegister`
  - 当邮箱未绑定账号且允许自动注册时，会自动创建 `USER` 角色账号
  - 成功后返回底层 `sessionKey`

### 第三方身份登录

- 方法：`POST`
- 路径：`/api/users/auth/third-party/login`
- 说明：
  - 当前正式支持的 provider 主入口为 `QQ`、`WECHAT`
  - 通过 `provider + providerUserId + providerUnionId` 做绑定命中
  - 找不到绑定时会尝试按邮箱、手机号、账号归并已有用户
  - 若仍不存在用户且允许自动注册，则自动创建账号并建立绑定
  - 真实第三方平台换码不在 `user-service` 内实现，由上游 `topbiz-service` 负责

### 登出

- 方法：`POST`
- 路径：`/api/users/auth/logout`

## 与 Topbiz 兼容的冻结查询接口

- `GET /api/users/me`
- `GET /api/users/{userId}`
- `GET /api/users/{userId}/permissions`
- `GET /api/users/{userId}/roles`
- `GET /api/users/departments/{deptId}`

## 用户自助接口

### 更新当前用户资料

- 方法：`PUT`
- 路径：`/api/users/me`

### 查询资料修改记录

- 方法：`GET`
- 路径：`/api/users/me/modify-records`

### 修改密码

- 方法：`PUT`
- 路径：`/api/users/me/password`

### 发送找回密码验证码

- 方法：`POST`
- 路径：`/api/users/password/forgot/send-code`

### 重置遗忘密码

- 方法：`POST`
- 路径：`/api/users/password/forgot/reset`

### 查询当前账号状态

- 方法：`GET`
- 路径：`/api/users/me/status`

### 申请解冻

- 方法：`POST`
- 路径：`/api/users/me/status/unfreeze`
- 说明：
  - 已登录用户可以为自己的账号发起解冻
  - 未登录场景支持通过 `account + contact + verifyCode` 进行解冻

### 申请注销

- 方法：`POST`
- 路径：`/api/users/me/status/cancel`

## 管理端用户接口

### 用户列表

- 方法：`GET`
- 路径：`/api/users/admin`

### 新增用户

- 方法：`POST`
- 路径：`/api/users/admin`

### 修改用户

- 方法：`PUT`
- 路径：`/api/users/admin/{userId}`

### 修改用户状态

- 方法：`PATCH`
- 路径：`/api/users/admin/{userId}/status`

### 修改用户角色与直属权限

- 方法：`PATCH`
- 路径：`/api/users/admin/{userId}/authorization`

### 刷新有效权限

- 方法：`POST`
- 路径：`/api/users/admin/{userId}/authorization/permissions:refresh`

### 重置用户密码

- 方法：`POST`
- 路径：`/api/users/admin/{userId}/password:reset`

## 管理端角色接口

### 角色列表

- 方法：`GET`
- 路径：`/api/users/admin/roles`

### 权限目录列表

- 方法：`GET`
- 路径：`/api/users/admin/permissions`

### 新增角色

- 方法：`POST`
- 路径：`/api/users/admin/roles`

### 修改角色

- 方法：`PUT`
- 路径：`/api/users/admin/roles/{roleId}`

### 设置角色权限

- 方法：`PUT`
- 路径：`/api/users/admin/roles/{roleId}/permissions`

### 删除角色

- 方法：`DELETE`
- 路径：`/api/users/admin/roles/{roleId}`

## 管理端部门接口

### 部门列表

- 方法：`GET`
- 路径：`/api/users/admin/departments`

### 组织树

- 方法：`GET`
- 路径：`/api/users/admin/departments/tree`

### 新增部门

- 方法：`POST`
- 路径：`/api/users/admin/departments`

### 修改部门

- 方法：`PUT`
- 路径：`/api/users/admin/departments/{departmentId}`

### 删除部门

- 方法：`DELETE`
- 路径：`/api/users/admin/departments/{departmentId}`

### 部门成员列表

- 方法：`GET`
- 路径：`/api/users/admin/departments/{departmentId}/users`

### 部门属性定义列表

- 方法：`GET`
- 路径：`/api/users/admin/departments/{departmentId}/attributes/definitions`

### 新增部门属性定义

- 方法：`POST`
- 路径：`/api/users/admin/departments/{departmentId}/attributes/definitions`

### 删除部门属性定义

- 方法：`DELETE`
- 路径：`/api/users/admin/departments/{departmentId}/attributes/definitions/{attributeKey}`

### 修改部门属性值

- 方法：`PUT`
- 路径：`/api/users/admin/departments/{departmentId}/attributes`

### 新增部门成员

- 方法：`POST`
- 路径：`/api/users/admin/departments/{departmentId}/members`

### 移除部门成员

- 方法：`DELETE`
- 路径：`/api/users/admin/departments/{departmentId}/members/{userId}`

### 成员跨部门调拨

- 方法：`POST`
- 路径：`/api/users/admin/departments/transfer`

### 查询用户成员归属

- 方法：`GET`
- 路径：`/api/users/admin/departments/users/{userId}/membership`

### 修改成员属性值

- 方法：`PUT`
- 路径：`/api/users/admin/departments/{departmentId}/members/{userId}/attributes`

### 批量修改成员属性值

- 方法：`POST`
- 路径：`/api/users/admin/departments/{departmentId}/members/attributes:batch`

## 当前已交付范围

当前已实现：

- 基于验证码的注册流程
- 密码登录
- 邮箱验证码登录
- 第三方身份绑定登录骨架
- 登录失败冻结与解冻流程
- 用户资料修改、资料修改记录、密码修改、忘记密码重置
- 用户状态查询与注销申请
- 管理端用户新增 / 修改 / 状态管理 / 授权 / 密码重置
- 角色目录、角色 CRUD、权限分配
- 部门 CRUD、组织树、部门属性定义与属性值
- 部门成员新增 / 移除 / 调拨 / 归属查询 / 成员属性 / 批量属性修改
- 管理员与部门管理员的范围访问控制
- 与 `topbiz` 兼容的冻结查询路径

当前仓库阶段仍故意保留为内存实现的部分：

- 用户与会话持久化
- 真实 Redis Session 落地
- 短信 / 邮件发送
- QQ / 微信真实开放平台资料拉取
- 向 `log-service` 发审计 / 事件消息
- Topbiz 侧分布式事务编排
- 基于 Shiro 的真实权限持久化模型

## 推荐调用方式

- 新代码优先调用独立接口：
  - `/api/users/auth/email/send-code`
  - `/api/users/auth/email/login`
  - `/api/users/auth/third-party/login`
- `POST /api/users/auth/login` 中的 `loginType=thirdParty|third_party` 仅作为兼容入口保留，不建议新调用方继续依赖。

## 所有权规则

- `user-service` 持有用户、角色、权限、部门、成员关系主数据。
- `topbiz-service` 可以做编排和对外暴露，但不得绕过本契约。
- 其他第二层服务不得私自实现绕开用户/权限主数据的捷径接口。
