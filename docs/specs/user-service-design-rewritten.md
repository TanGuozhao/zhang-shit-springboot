# 用户服务设计修正稿

对应原稿：`可复用用户管理服务设计-2024091602016-谭国照.md`

## 1. 文档目的

本文档用于替代原始用户服务设计稿中的过时路径、错误认证模型和过度承诺内容，作为当前仓库 `user-service` 的正式设计说明。

本文档以当前实现为准，重点冻结以下内容：

- 服务边界
- 对外与内部接口路径
- 认证与会话模型
- 用户、角色、权限、部门的主数据归属
- 已实现能力、占位实现和未来扩展的边界

## 2. 服务定位

`user-service` 是平台的身份与访问控制主数据服务，负责沉淀用户、角色、权限、部门和账号状态等领域数据，并提供注册、登录、找回密码、用户自助资料修改、后台用户管理和部门管理能力。

本服务不承担统一外部网关职责，不负责跨服务业务编排，也不直接对外暴露统一认证入口。统一对外入口由 `topbiz-service` 承担。

## 3. 核心领域对象

当前实现已形成以下核心领域对象：

- `UserAccount`：用户账号主数据，包含账号、昵称、邮箱、手机号、头像、状态、扩展字段等。
- `RoleDefinition`：角色定义，包含角色编码、角色名称、描述和权限集合。
- `PermissionDefinition`：权限定义，作为 RBAC 模型中的最小授权单元。
- `Department`：部门主数据，包含部门编码、名称、父部门、描述和部门属性。
- `DepartmentAttributeDefinition`：部门属性定义，描述字段键、名称、数据类型、默认值、是否必填、显示顺序等。
- `DepartmentMembership`：用户与部门的归属关系，包含部门路径和成员属性值。
- `VerificationCode`：验证码实体，用于注册、找回密码和解冻场景。
- `UserSession`：登录会话抽象，当前由服务内会话契约和 `sessionKey` 体现。
- `UserProfileModificationRecord`：用户资料修改记录，用于自助资料追踪。
- `LoginAttemptState`：登录失败次数和冻结状态跟踪。

## 4. 角色模型与调用方

### 4.1 平台内置角色

当前服务已预置并默认支持以下角色：

- `ADMIN`
- `OPERATOR`
- `USER`
- `AUDITOR`
- `OPS_ADMIN`
- `DEPARTMENT_ADMIN`

### 4.2 权限范围

- `ADMIN`：拥有全局用户、角色、权限、部门管理能力。
- `DEPARTMENT_ADMIN`：仅可访问自己负责部门子树范围内的数据。
- 普通用户：仅可访问自己的资料、状态、密码和注销/解冻流程。
- `topbiz-service`：作为编排层调用底层查询和管理契约，但不持有用户主数据。

### 4.3 认证上下文

当前服务解析用户身份时采用以下顺序：

1. 优先使用 `X-Session-Key`
2. 在需要时结合 `X-User-Id`

因此原稿中把 `token` 写成请求体或查询参数的做法已经作废。

## 5. 已实现能力范围

当前仓库内 `user-service` 已经实现并可直接联调的能力包括：

- 验证码发送
- 注册
- 密码登录
- 预留第三方登录请求结构
- 登出
- 当前用户资料查询
- 当前用户资料修改
- 资料修改记录查询
- 当前用户改密
- 忘记密码验证码发送
- 忘记密码重置
- 当前账号状态查询
- 账号解冻申请
- 账号注销申请
- 后台用户分页查询
- 后台新增用户
- 后台修改用户
- 后台修改用户状态
- 后台修改用户角色和直属权限
- 后台刷新用户有效权限
- 后台重置密码
- 角色列表、角色创建、角色修改、角色删除
- 权限定义查询
- 角色权限分配
- 部门分页查询
- 组织树查询
- 部门创建、修改、删除
- 部门用户查询
- 部门属性定义新增、删除、查询
- 部门属性值更新
- 成员加入部门、移出部门、跨部门转移
- 查询用户部门归属
- 成员属性更新和批量更新

## 6. 接口设计

### 6.1 架构概览接口

| 方法 | 路径 | 用途 | 备注 |
| --- | --- | --- | --- |
| `GET` | `/internal/architecture/overview` | 返回服务边界、分层、核心模块概览 | 面向内部排障与架构说明 |

### 6.2 认证接口

基础路径：`/api/users/auth`

| 方法 | 路径 | 用途 | 关键说明 |
| --- | --- | --- | --- |
| `POST` | `/verify-codes` | 发送验证码 | 适用 `REGISTER`、`FORGOT_PASSWORD`、`UNFREEZE` |
| `POST` | `/register` | 用户注册 | 要求 `agreeProtocol=true` |
| `POST` | `/login` | 登录 | 支持 `loginType=password|thirdParty` |
| `POST` | `/logout` | 登出 | 通过 `X-Session-Key` 注销当前会话 |

#### 6.2.1 登录请求 DTO

`AuthLoginRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `account` | 是 | 登录账号 |
| `password` | 否 | 密码登录时必填 |
| `loginType` | 否 | 默认为 `password`，也支持 `thirdParty` |
| `rememberLogin` | 否 | 预留记住登录态开关 |
| `thirdPartyInfo` | 否 | 第三方登录扩展信息，当前仅冻结契约，不代表已接真实第三方平台 |

#### 6.2.2 登录响应 DTO

`AuthLoginResponse`

| 字段 | 说明 |
| --- | --- |
| `userId` | 用户 ID |
| `account` | 登录账号 |
| `userName` | 用户名 |
| `roles` | 当前角色列表 |
| `permissions` | 当前有效权限列表 |
| `sessionKey` | 当前会话键 |
| `expireTime` | 会话过期时间 |

#### 6.2.3 注册请求 DTO

`UserRegistrationRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `account` | 是 | 注册账号 |
| `password` | 是 | 原始密码，由服务端做加盐哈希 |
| `userName` | 是 | 用户姓名/昵称 |
| `contact` | 是 | 联系方式，通常为邮箱或手机号 |
| `verifyCode` | 是 | 验证码 |
| `agreeProtocol` | 是 | 是否同意注册协议 |
| `extFields` | 否 | 扩展字段 |

#### 6.2.4 验证码请求/响应 DTO

`VerifyCodeSendRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `account` | 是 | 账号 |
| `contact` | 是 | 联系方式 |
| `scene` | 是 | 验证码场景 |

`VerifyCodeSendResponse`

| 字段 | 说明 |
| --- | --- |
| `account` | 账号 |
| `contact` | 联系方式 |
| `scene` | 验证码场景 |
| `expireTime` | 失效时间 |

### 6.3 用户自助接口

基础路径：`/api/users`

| 方法 | 路径 | 用途 | 关键说明 |
| --- | --- | --- | --- |
| `GET` | `/me` | 查询当前用户资料 | 依赖身份上下文 |
| `PUT` | `/me` | 修改当前用户资料 | 要求提交当前密码 |
| `GET` | `/me/modify-records` | 查询资料修改记录 | 支持分页 |
| `PUT` | `/me/password` | 修改当前密码 | 要求原密码、新密码、确认密码 |
| `POST` | `/password/forgot/send-code` | 发送忘记密码验证码 | 匿名可调用 |
| `POST` | `/password/forgot/reset` | 重置遗忘密码 | 匿名可调用 |
| `GET` | `/me/status` | 查询当前账号状态 | 依赖身份上下文 |
| `POST` | `/me/status/unfreeze` | 提交解冻申请 | 支持已登录和未登录补充解冻信息 |
| `POST` | `/me/status/cancel` | 提交注销申请 | 依赖身份上下文 |

#### 6.3.1 资料修改 DTO

`UserProfileUpdateRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `password` | 是 | 当前密码，用于确认修改权限 |
| `userName` | 否 | 新用户名 |
| `contact` | 否 | 新联系方式 |
| `avatar` | 否 | 头像地址 |
| `extFields` | 否 | 扩展字段 |

#### 6.3.2 改密 DTO

`PasswordChangeRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `oldPassword` | 是 | 原密码 |
| `newPassword` | 是 | 新密码 |
| `confirmPassword` | 是 | 确认新密码 |

#### 6.3.3 忘记密码 DTO

`ForgotPasswordSendCodeRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `account` | 是 | 账号 |
| `contact` | 是 | 联系方式 |

`ForgotPasswordResetRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `account` | 是 | 账号 |
| `contact` | 是 | 联系方式 |
| `verifyCode` | 是 | 验证码 |
| `newPassword` | 是 | 新密码 |
| `confirmPassword` | 是 | 确认新密码 |

### 6.4 查询接口

| 方法 | 路径 | 用途 | 关键说明 |
| --- | --- | --- | --- |
| `GET` | `/api/users/{userId}` | 查询指定用户 | 需身份上下文 |
| `GET` | `/api/users/{userId}/permissions` | 查询指定用户有效权限 | 供后台和 topbiz 使用 |
| `GET` | `/api/users/{userId}/roles` | 查询指定用户角色 | 供后台和 topbiz 使用 |
| `GET` | `/api/users/departments/{deptId}` | 查询部门详情 | 需身份上下文 |

### 6.5 后台用户管理接口

基础路径：`/api/users/admin`

| 方法 | 路径 | 用途 | 关键说明 |
| --- | --- | --- | --- |
| `GET` | `` | 分页查询用户 | 支持 `account`、`userName`、`status`、分页参数 |
| `POST` | `` | 新增用户 | 创建时指定部门、角色、权限 |
| `PUT` | `/{userId}` | 修改用户 | 修改资料与归属 |
| `PATCH` | `/{userId}/status` | 修改状态 | 不提供物理删除 |
| `PATCH` | `/{userId}/authorization` | 修改角色与直属权限 | RBAC 管理入口 |
| `POST` | `/{userId}/authorization/permissions:refresh` | 刷新有效权限 | 获取当前汇总权限 |
| `POST` | `/{userId}/password:reset` | 重置密码 | 后台强制重置 |

#### 6.5.1 新增用户 DTO

`UserCreateRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `account` | 是 | 用户账号 |
| `password` | 是 | 初始密码 |
| `userName` | 是 | 用户姓名 |
| `email` | 否 | 邮箱，要求合法格式 |
| `phone` | 是 | 手机号 |
| `avatar` | 否 | 头像 |
| `departmentId` | 是 | 所属部门 |
| `roles` | 否 | 初始角色列表 |
| `permissions` | 否 | 直属权限列表 |
| `extFields` | 否 | 扩展字段 |

#### 6.5.2 修改授权 DTO

`UserAuthorizationUpdateRequest`

| 字段 | 说明 |
| --- | --- |
| `roles` | 角色编码列表 |
| `permissions` | 权限编码列表 |

#### 6.5.3 状态修改与密码重置 DTO

`UserStatusUpdateRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `status` | 是 | 目标状态 |
| `reason` | 否 | 变更原因 |

`AdminPasswordResetRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `newPassword` | 是 | 新密码 |

### 6.6 角色与权限接口

基础路径：`/api/users/admin`

| 方法 | 路径 | 用途 | 关键说明 |
| --- | --- | --- | --- |
| `GET` | `/roles` | 分页查询角色 | 支持按 `roleName` 搜索 |
| `GET` | `/permissions` | 查询权限定义列表 | 支持按 `permissionType` 过滤 |
| `POST` | `/roles` | 创建角色 | 创建时可附带权限 |
| `PUT` | `/roles/{roleId}` | 修改角色 | 更新名称、描述等 |
| `PUT` | `/roles/{roleId}/permissions` | 分配角色权限 | 替换角色权限集合 |
| `DELETE` | `/roles/{roleId}` | 删除角色 | 删除角色定义，不影响已归档历史记录 |

#### 6.6.1 角色创建 DTO

`AdminRoleCreateRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `roleCode` | 是 | 角色编码 |
| `roleName` | 是 | 角色名称 |
| `description` | 否 | 角色描述 |
| `permissions` | 否 | 初始权限编码列表 |

### 6.7 部门管理接口

基础路径：`/api/users/admin/departments`

| 方法 | 路径 | 用途 | 关键说明 |
| --- | --- | --- | --- |
| `GET` | `` | 分页查询部门 | 支持 `deptName`、`parentDepartmentId`、分页参数 |
| `GET` | `/tree` | 查询组织树 | 返回树形结构 |
| `POST` | `` | 创建部门 | 支持初始化部门属性 |
| `PUT` | `/{departmentId}` | 修改部门 | 修改名称、父级、属性等 |
| `DELETE` | `/{departmentId}` | 删除部门 | 删除前做约束检查 |
| `GET` | `/{departmentId}/users` | 查询部门成员 | 支持按账号、用户名分页筛选 |
| `GET` | `/{departmentId}/attributes/definitions` | 查询部门属性定义 | 返回当前部门定义的属性集合 |
| `POST` | `/{departmentId}/attributes/definitions` | 新增部门属性定义 | 写入定义层 |
| `DELETE` | `/{departmentId}/attributes/definitions/{attributeKey}` | 删除部门属性定义 | 当前实现按级联清理落地 |
| `PUT` | `/{departmentId}/attributes` | 更新部门属性值 | 只修改值，不修改定义 |
| `POST` | `/{departmentId}/members` | 将成员加入部门 | 支持批量加入 |
| `DELETE` | `/{departmentId}/members/{userId}` | 将成员移出部门 | 返回新的归属结果 |
| `POST` | `/transfer` | 跨部门转移成员 | 批量转移 |
| `GET` | `/users/{userId}/membership` | 查询用户部门归属 | 返回部门路径与成员属性 |
| `PUT` | `/{departmentId}/members/{userId}/attributes` | 修改成员属性 | 更新单个成员属性 |
| `POST` | `/{departmentId}/members/attributes:batch` | 批量修改成员属性 | 返回批量处理结果 |

#### 6.7.1 创建部门 DTO

`AdminDepartmentCreateRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `departmentCode` | 是 | 部门编码 |
| `departmentName` | 是 | 部门名称 |
| `parentDepartmentId` | 是 | 父部门 ID |
| `description` | 否 | 描述 |
| `attributes` | 否 | 初始化属性值 |

#### 6.7.2 成员转移 DTO

`DepartmentTransferRequest`

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `userIds` | 是 | 待转移用户 ID 列表 |
| `fromDepartmentId` | 是 | 原部门 ID |
| `toDepartmentId` | 是 | 目标部门 ID |

### 6.8 关键响应对象

#### 6.8.1 用户资料响应

`UserProfileResponse`

| 字段 | 说明 |
| --- | --- |
| `userId` | 用户 ID |
| `account` | 账号 |
| `userName` | 用户名 |
| `email` | 邮箱 |
| `phone` | 手机 |
| `avatar` | 头像 |
| `status` | 状态编码 |
| `statusDesc` | 状态描述 |
| `departmentId` | 所属部门 ID |
| `roles` | 角色列表 |
| `permissions` | 权限列表 |
| `extFields` | 扩展字段 |
| `createTime` | 创建时间 |
| `updateTime` | 更新时间 |

#### 6.8.2 后台用户摘要

`AdminUserSummaryResponse`

| 字段 | 说明 |
| --- | --- |
| `userId` | 用户 ID |
| `account` | 账号 |
| `userName` | 用户名 |
| `status` | 当前状态 |
| `departmentId` | 所属部门 |
| `createTime` | 创建时间 |
| `updateTime` | 更新时间 |

#### 6.8.3 部门摘要

`AdminDepartmentSummaryResponse`

| 字段 | 说明 |
| --- | --- |
| `departmentId` | 部门 ID |
| `departmentCode` | 部门编码 |
| `departmentName` | 部门名称 |
| `parentDepartmentId` | 父部门 ID |
| `description` | 描述 |
| `attributes` | 当前部门属性值 |
| `createTime` | 创建时间 |
| `updateTime` | 更新时间 |

#### 6.8.4 部门属性定义响应

`DepartmentAttributeDefinitionResponse`

| 字段 | 说明 |
| --- | --- |
| `attributeId` | 属性定义 ID |
| `departmentId` | 所属部门 ID |
| `attributeKey` | 属性键 |
| `attributeName` | 属性名 |
| `dataType` | 数据类型 |
| `defaultValue` | 默认值 |
| `required` | 是否必填 |
| `rules` | 校验规则描述 |
| `displayOrder` | 显示顺序 |
| `createTime` | 创建时间 |
| `updateTime` | 更新时间 |

#### 6.8.5 用户部门归属响应

`DepartmentMembershipResponse`

| 字段 | 说明 |
| --- | --- |
| `userId` | 用户 ID |
| `departmentId` | 部门 ID |
| `departmentCode` | 部门编码 |
| `departmentName` | 部门名称 |
| `departmentPath` | 部门路径 |
| `memberAttributes` | 成员属性值 |

### 6.9 典型请求示例

#### 登录

```json
POST /api/users/auth/login
{
  "account": "admin",
  "password": "admin123",
  "loginType": "password"
}
```

#### 新增用户

```json
POST /api/users/admin
{
  "account": "u1001",
  "password": "Init@123",
  "userName": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800000000",
  "departmentId": 1001,
  "roles": ["USER"],
  "permissions": [],
  "extFields": {
    "employeeNo": "E1001"
  }
}
```

#### 部门成员转移

```json
POST /api/users/admin/departments/transfer
{
  "userIds": [1001, 1002],
  "fromDepartmentId": 2001,
  "toDepartmentId": 2002
}
```

## 7. 工程设计

### 7.1 服务边界

- 本服务负责什么：
  - 用户主数据
  - 角色与权限主数据
  - 部门与成员归属
  - 用户自助资料与密码能力
  - 注册、登录、找回密码、解冻等身份支撑能力
- 本服务不负责什么：
  - 统一外部网关
  - 跨服务业务编排
  - 消息投递
  - 日志采集与告警
  - 统一对外单点登录门户
- 主数据是否由本服务持有：
  - 是。用户、角色、权限、部门均由本服务持有。
- 是否允许其他底层服务直接调用：
  - 不建议通过业务 API 直接互调。
  - 平台设计要求跨服务业务调用统一经由 `topbiz-service` 编排。

### 7.2 运行模式

#### 本地默认模式

- 启动命令：

```powershell
.\mvnw.cmd -pl user-service -am spring-boot:run
```

- 默认端口：`8081`
- 默认仓储：内存实现
- 默认会话：服务内会话契约
- 可在无数据库、无 Redis、无短信邮件平台前提下启动和联调

#### 可替换生产模式

- 用户、角色、部门仓储可替换为 MySQL 或 PostgreSQL
- 会话能力可由 `topbiz-service` 的 Redis Session 统一承接
- 验证码发送可替换为短信、邮件或企业通知通道
- 第三方登录可替换为微信开放平台、QQ互联或企业统一身份源

#### 外部依赖是否可选

- 当前阶段全部可选
- 没有短信、邮件、数据库时，本服务仍能跑通主流程

### 7.3 契约冻结说明

#### 已冻结接口

- `/api/users/auth/**`
- `/api/users/me`
- `/api/users/me/modify-records`
- `/api/users/me/password`
- `/api/users/password/forgot/**`
- `/api/users/me/status/**`
- `/api/users/{userId}`
- `/api/users/{userId}/permissions`
- `/api/users/{userId}/roles`
- `/api/users/departments/{deptId}`
- `/api/users/admin/**`
- `/api/users/admin/departments/**`
- `/internal/architecture/overview`

#### 已冻结 DTO

- `AuthLoginRequest`
- `AuthLoginResponse`
- `UserRegistrationRequest`
- `UserRegistrationResponse`
- `VerifyCodeSendRequest`
- `VerifyCodeSendResponse`
- `UserProfileUpdateRequest`
- `PasswordChangeRequest`
- `ForgotPasswordSendCodeRequest`
- `ForgotPasswordResetRequest`
- `UserCreateRequest`
- `UserAuthorizationUpdateRequest`
- `UserStatusUpdateRequest`
- `AdminPasswordResetRequest`
- `AdminRoleCreateRequest`
- `AdminDepartmentCreateRequest`
- `DepartmentTransferRequest`

#### 仅为预留扩展点

- `loginType=thirdParty`
- `thirdPartyInfo`
- 真实 SSO / OAuth2 / CAS 接入
- 第三方账号绑定模型

### 7.4 占位实现与可替换实现

- 当前密码安全已正式落地，不属于占位实现。
- 当前验证码投递能力仍是模板实现，可替换为真实短信/邮件通道。
- 当前仓储为内存实现，可替换为数据库实现。
- 当前会话是底层服务级契约，会由 `topbiz-service` 的统一会话体系接管外部登录态。
- 当前第三方登录只冻结请求结构，未直接接入微信、QQ 等真实平台。

### 7.5 默认配置与约束

当前 `application.yml` 已冻结以下默认约束：

- `session-ttl-hours: 8`
- `password-min-length: 8`
- `password-history-limit: 3`
- `login-failure-freeze-threshold: 5`
- `verify-code-ttl-minutes: 5`
- `verify-code-cooldown-seconds: 60`

工程含义如下：

- 会话默认 8 小时过期
- 新密码长度至少 8 位
- 最近 3 次密码不可重复使用
- 连续登录失败达到 5 次自动冻结
- 验证码有效期 5 分钟
- 同账号同场景发送验证码需要至少间隔 60 秒

## 8. 对原稿的具体修正

本次修正明确替换了原稿中的以下错误或冲突内容：

- 原稿中的 `/user/**`、`/admin/user/**`、`/org/**` 路径全部废止，统一改为 `/api/users/**`
- 原稿中的 `RABC` 术语错误统一修正为 `RBAC`
- 原稿中的“前端先 MD5 再传输”已废止，改为服务端加盐 `PBKDF2WithHmacSHA256`
- 原稿中的 `token` 请求参数模型废止，改为 `X-Session-Key` 与 `X-User-Id`
- 原稿中“删除用户”与“管理员无删除权限”互相冲突，现统一为不提供用户删除接口，只做状态管理
- 原稿中把 QQ、微信等第三方接入写成已完成功能，现统一降级为未来扩展；当前仅冻结 `thirdParty` 契约入口
- 原稿中部门能力描述过粗，当前实现已经细化到部门属性定义、部门属性值、成员属性和批量成员属性更新，修正稿已按实现补齐

## 9. 后续扩展建议

后续可以在不破坏当前契约的前提下继续扩展：

- 用户、角色、部门数据落库
- Redis 会话共享
- 与 `topbiz-service` 的 Shiro 鉴权联动
- 微信开放平台、QQ互联、邮箱验证码免密登录
- 审计事件写入 `log-service`
- 通过 `message-service` 发送验证码和安全通知

但这些能力进入实现前，不应再改动本文档中已冻结的对外路径和 DTO 结构。
