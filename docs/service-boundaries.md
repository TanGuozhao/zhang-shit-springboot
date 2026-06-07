# Service Boundaries And Delivery Baseline

本文档用于冻结当前仓库四个服务的职责边界、启动方式、示例请求以及占位实现说明，作为第一版可复用微服务基线。

## Overall Rules

- `topbiz-service` 是唯一对外公开入口，负责鉴权、授权、编排、聚合与网关转发。
- `user-service`、`message-service`、`log-service` 是底层能力服务，不承担统一对外网关职责。
- 底层服务之间禁止直接进行业务 API 相互调用。
- 跨服务调用统一经由 `topbiz-service` 编排，底层服务契约以 `docs/contracts/*.md` 为准。
- 当前仓库定位是“可复用微服务能力底座”，不是“已接入真实外部系统的生产业务系统”。

## user-service

职责边界：

- 负责用户、角色、权限、部门、账号状态、自助资料修改、密码能力。
- 负责身份认证基础能力，例如注册、登录、验证码、找回密码。
- 负责组织关系与部门属性管理。
- 不负责统一对外鉴权入口，不负责跨服务业务编排。

启动说明：

```powershell
.\mvnw.cmd -pl user-service -am spring-boot:run
```

- 默认端口：`8081`

示例请求：

```bash
curl -X POST http://localhost:8081/api/users/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"loginName\":\"admin\",\"password\":\"admin123\"}"
```

```bash
curl http://localhost:8081/api/users/admin/roles
```

占位实现 / 可替换实现：

- 当前仓储层以本地内存实现为主，后续可替换为 MySQL / PostgreSQL / ORM 持久化实现。
- 验证码、密码找回、账号冻结规则为可演进模板实现，后续可接短信、邮件、统一身份源。
- 当前会话与登录态逻辑主要服务于平台内部联调，生产环境可接统一 SSO / OAuth2 / CAS。

## message-service

职责边界：

- 负责消息模板、变量、渠道、发送、草稿、调度、重试、站内信收件箱与统计能力。
- 对外提供消息发送与查询契约，对内提供调度和运行时接口。
- 不负责真实第三方消息供应商接入编排，不负责统一用户认证入口。

启动说明：

```powershell
.\mvnw.cmd -pl message-service -am spring-boot:run
```

- 默认端口：`8082`

示例请求：

```bash
curl -X POST http://localhost:8082/api/messages/send ^
  -H "Content-Type: application/json" ^
  -d "{\"templateCode\":\"NOTICE\",\"receivers\":[\"u1001\"],\"variables\":{\"name\":\"test\"}}"
```

```bash
curl http://localhost:8082/api/messages/templates
```

占位实现 / 可替换实现：

- 当前发送链路是可复用消息域骨架，不绑定真实短信、邮件、公众号、推送厂商。
- 渠道账号、调度执行、重试策略为可替换模板实现，后续可接 MQ、定时任务平台、供应商 SDK。
- 当前记录与收件箱主要基于本地仓储抽象，后续可替换为数据库、ES、消息中间件。

## log-service

职责边界：

- 负责访问日志接入、检索、Trace 查询、指标统计、告警规则、导出任务与运行时信息。
- 负责日志脱敏、缓冲、导出与告警评估等日志域能力。
- 不负责统一 API 网关，不负责真实业务主数据落库。

启动说明：

```powershell
.\mvnw.cmd -pl log-service -am spring-boot:run
```

- 默认端口：`8083`

示例请求：

```bash
curl -X POST http://localhost:8083/api/logs/ingest ^
  -H "Content-Type: application/json" ^
  -d "{\"traceId\":\"trace-demo-001\",\"serviceName\":\"topbiz-service\",\"path\":\"/api/topbiz/platform/overview\",\"method\":\"GET\",\"status\":200}"
```

```bash
curl "http://localhost:8083/api/logs/search?serviceName=topbiz-service"
```

占位实现 / 可替换实现：

- 当前日志存储支持本地内存抽象，已预留可替换 ClickHouse 存储实现入口。
- 当前告警通知为平台内模板实现，后续可接企业微信、钉钉、邮件、短信。
- 当前导出任务为基础流程骨架，后续可替换为对象存储、异步任务中心、报表系统。

## topbiz-service

职责边界：

- 负责统一登录入口、Shiro 权限控制、OpenFeign 转发、能力聚合与跨服务编排。
- 负责对外暴露用户、消息、日志相关公共 API。
- 负责 Saga-like 编排记录与补偿流程承载。
- 不沉淀用户、消息、日志主业务数据。
- 不替代底层服务的领域职责，只做网关与 orchestration。

启动说明：

本地模式：

```powershell
.\mvnw.cmd -pl topbiz-service -am spring-boot:run
```

生产配置模式：

```powershell
.\mvnw.cmd -pl topbiz-service spring-boot:run -Dspring-boot.run.profiles=prod
```

- 默认端口：`8080`

示例请求：

```bash
curl -X POST http://localhost:8080/api/topbiz/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"loginName\":\"admin\",\"password\":\"admin123\"}"
```

```bash
curl http://localhost:8080/api/topbiz/platform/overview
```

```bash
curl -X POST http://localhost:8080/api/topbiz/orchestrations/message-audit ^
  -H "Content-Type: application/json" ^
  -d "{\"operatorUserId\":1,\"templateCode\":\"NOTICE\",\"receivers\":[\"u1001\"],\"variables\":{\"name\":\"demo\"}}"
```

占位实现 / 可替换实现：

- 本地模式默认使用内存编排仓储和 Servlet Session，方便演示和联调。
- `prod` 模式已切换到 Redis Session 与 JDBC 编排仓储，但仍属于通用模板，需要你按环境补齐真实连接参数。
- 分布式事务采用 Saga-like compensation 思路，不使用 XA / 2PC；后续可替换为 Seata 或统一事务平台。
- OpenFeign 远程调用当前是服务模板内直连配置，后续可接注册中心、服务网格或 API 网关体系。

## Startup Order

建议启动顺序：

1. `user-service`
2. `message-service`
3. `log-service`
4. `topbiz-service`

全量测试：

```powershell
.\mvnw.cmd clean test
```

## Delivery Positioning

当前版本适合作为：

- 课程设计 / 毕设 / 方案原型
- 可复用微服务模板基线
- 团队后续扩展真实接入前的统一开发底座

当前版本不等于：

- 已完成真实短信、邮件、SSO、对象存储、监控告警平台接入
- 已完成生产部署、压测、监控、容灾和安全加固的最终上线系统
