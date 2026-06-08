# user-service

`user-service` 是用户与组织能力服务，负责用户账号、角色权限、部门结构、账号状态与自助资料能力。

## Boundary

- 负责用户注册、登录、验证码、找回密码等认证基础能力。
- 负责邮箱验证码登录与第三方身份绑定登录骨架。
- 负责用户、角色、权限、部门、成员关系、部门属性定义管理。
- 负责用户自助资料修改、密码修改、冻结解冻、注销等能力。
- 不负责统一外部网关。
- 不负责跨服务编排。
- 不负责消息与日志领域数据。

## Start

```powershell
.\mvnw.cmd -pl user-service -am spring-boot:run
```

- 默认端口：`8081`

## Example Requests

登录：

```bash
curl -X POST http://localhost:8081/api/users/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"account\":\"admin\",\"password\":\"admin123\"}"
```

邮箱登录发码：

```bash
curl -X POST http://localhost:8081/api/users/auth/email/send-code ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"email.login@example.com\"}"
```

邮箱验证码登录：

```bash
curl -X POST http://localhost:8081/api/users/auth/email/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"email.login@example.com\",\"verifyCode\":\"123456\",\"autoRegister\":true,\"userName\":\"Email Login User\"}"
```

第三方身份登录：

```bash
curl -X POST http://localhost:8081/api/users/auth/third-party/login ^
  -H "Content-Type: application/json" ^
  -d "{\"provider\":\"QQ\",\"providerUserId\":\"qq_user_001\",\"providerUnionId\":\"qq_union_001\",\"account\":\"qq.demo@example.com\",\"email\":\"qq.demo@example.com\",\"userName\":\"QQ Demo User\",\"avatar\":\"https://cdn.example.com/avatar/qq-demo.png\",\"autoRegister\":true}"
```

新增用户：

```bash
curl -X POST http://localhost:8081/api/users/admin ^
  -H "X-User-Id: 1001" ^
  -H "Content-Type: application/json" ^
  -d "{\"account\":\"u1001\",\"password\":\"u1001pass\",\"userName\":\"测试用户\",\"email\":\"u1001@example.com\",\"phone\":\"13800000000\",\"departmentId\":10,\"roles\":[\"USER\"]}"
```

查询角色：

```bash
curl http://localhost:8081/api/users/admin/roles ^
  -H "X-User-Id: 1001"
```

## Placeholder And Replaceable Parts

- 仓储层当前主要是内存实现，后续可替换为关系型数据库持久化。
- 验证码发送通过 `UserVerificationCodeSender` 抽象，默认实现 `LoggingUserVerificationCodeSender` 只做日志输出。
- 第三方身份绑定主数据当前存于 `ExternalIdentityBindingRepository`，真实第三方换码和资料拉取不在本服务内实现。
- 当前登录态与密码策略是可扩展基础版，后续可接统一身份认证中心。

## Collaboration Rule

- 对外统一暴露入口在 `topbiz-service`，新增公共接口时要同步冻结契约。
- 角色权限字符串遵循 `resource:action` 规范。
