# user-service

`user-service` 是用户与组织能力服务，负责用户账号、角色权限、部门结构、账号状态与自助资料能力。

## Boundary

- 负责用户注册、登录、验证码、找回密码等认证基础能力。
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
  -d "{\"loginName\":\"admin\",\"password\":\"admin123\"}"
```

新增用户：

```bash
curl -X POST http://localhost:8081/api/users/admin ^
  -H "Content-Type: application/json" ^
  -d "{\"loginName\":\"u1001\",\"displayName\":\"测试用户\",\"phone\":\"13800000000\"}"
```

查询角色：

```bash
curl http://localhost:8081/api/users/admin/roles
```

## Placeholder And Replaceable Parts

- 仓储层当前主要是内存实现，后续可替换为关系型数据库持久化。
- 验证码发送是模板能力，后续可接短信或邮件网关。
- 当前登录态与密码策略是可扩展基础版，后续可接统一身份认证中心。

## Collaboration Rule

- 对外统一暴露入口在 `topbiz-service`，新增公共接口时要同步冻结契约。
- 角色权限字符串遵循 `resource:action` 规范。
