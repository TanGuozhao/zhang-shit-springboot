# topbiz-service

`topbiz-service` 是平台统一对外入口，承担网关、鉴权、授权、聚合和跨服务编排职责。

## Boundary

- 负责统一登录入口与 Session 管理。
- 负责平台邮箱验证码登录入口与第三方 OAuth authorize/callback。
- 负责基于 Shiro 的 Authentication 和 Authorization。
- 负责通过 OpenFeign 转发 user、message、log 三个底层服务能力。
- 负责编排流程、聚合接口、平台总览与运行态接口。
- 不沉淀用户、消息、日志主业务数据。
- 不替代底层服务领域实现。

## Start

本地模式：

```powershell
.\mvnw.cmd -pl topbiz-service -am spring-boot:run
```

生产模板模式：

```powershell
.\mvnw.cmd -pl topbiz-service spring-boot:run -Dspring-boot.run.profiles=prod
```

- 默认端口：`8080`

## Example Requests

登录：

```bash
curl -X POST http://localhost:8080/api/topbiz/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"account\":\"admin\",\"password\":\"admin123\"}"
```

平台邮箱登录发码：

```bash
curl -X POST http://localhost:8080/api/topbiz/auth/email/send-code ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"email.login@example.com\"}"
```

平台邮箱验证码登录：

```bash
curl -X POST http://localhost:8080/api/topbiz/auth/email/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"email.login@example.com\",\"verifyCode\":\"123456\",\"autoRegister\":true,\"userName\":\"Email Login User\"}"
```

发起 QQ OAuth：

```bash
curl http://localhost:8080/api/topbiz/auth/oauth/qq/authorize
```

平台总览：

```bash
curl http://localhost:8080/api/topbiz/platform/overview ^
  -b "TOPBIZSESSION=<sessionId>"
```

消息审计编排：

```bash
curl -X POST http://localhost:8080/api/topbiz/orchestrations/message-audit ^
  -b "TOPBIZSESSION=<sessionId>" ^
  -H "Content-Type: application/json" ^
  -d "{\"operatorUserId\":1,\"templateCode\":\"NOTICE\",\"receivers\":[\"u1001\"],\"variables\":{\"name\":\"demo\"}}"
```

## Placeholder And Replaceable Parts

- 本地默认使用内存编排仓储与 Servlet Session，便于开发和演示。
- `prod` 模式启用 Redis Session 与 JDBC 编排仓储，但仍是模板化生产配置，需要按环境填入真实参数。
- 第三方登录通过 `TopbizOAuthProviderClient` 抽象接入，默认实现 `MockTopbizOAuthProviderClient` 只做 mock 换码和 mock 用户资料构造。
- 分布式事务当前采用 Saga-like compensation，不使用 XA。
- 远程服务发现当前以静态配置和 Feign 模板为主，后续可接注册中心、服务网格或统一网关。

## Collaboration Rule

- `topbiz-service` 只做 orchestration，不要把业务主数据存进来。
- 新增对外能力时优先复用底层契约，不要绕开底层服务直接写重复业务逻辑。
