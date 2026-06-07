# Platform Services

这是一个基于 `Spring Boot 3.5.0` 的多模块平台工程，围绕 3 个底层可复用服务和 1 个对外编排网关服务展开：

- `user-service`：用户、角色、权限、部门、认证能力
- `message-service`：消息模板、变量、发送、调度、重试、收件箱
- `log-service`：访问日志接入、检索、指标、告警、导出
- `topbiz-service`：统一对外入口，负责编排、鉴权、聚合和对外暴露

项目当前不是单纯骨架，而是一套已经具备正式分层、冻结接口契约、可本地运行、可测试验证、支持继续扩展的后端基线。

## 项目目标

- 沉淀用户、消息、日志三类可复用基础服务
- 用 `topbiz-service` 作为统一网关和跨服务编排层
- 冻结服务边界，支持多人并行开发
- 避免重复建设认证、授权、消息和日志等通用能力

## 架构总览

```text
Client / Frontend
        |
        v
 topbiz-service
   |    |    |
   |    |    +--> log-service
   |    +-------> message-service
   +------------> user-service
```

核心约束如下：

- `topbiz-service` 只做网关、编排、鉴权、聚合，不落业务主数据
- `user-service`、`message-service`、`log-service` 之间禁止直接进行业务 API 相互调用
- 对外暴露的跨服务能力统一收口到 `topbiz-service`
- 服务间调用统一使用 `OpenFeign`
- 权限字符串统一遵守 `资源:动作` 风格，例如 `user:read`、`message:send`、`topbiz:admin`

## 模块说明

| 模块 | 职责 | 当前能力 | 默认端口 |
| --- | --- | --- | --- |
| `service-common` | 公共基础设施 | `ApiResponse`、全局异常、Trace 过滤器 | - |
| `user-service` | 身份与组织能力 | 登录、会话校验、用户管理、角色权限、部门管理、自助接口 | `8081` |
| `message-service` | 消息通道能力 | 模板、变量、消息发送、草稿、任务调度、重试、收件箱、统计 | `8082` |
| `log-service` | 访问日志能力 | 日志接入、检索、Trace 查询、指标、告警、导出、运行时接口 | `8083` |
| `topbiz-service` | 对外网关与编排 | Shiro 鉴权、OpenFeign 网关、平台总览、跨服务编排、会话投影 | `8080` |

## topbiz 设计要点

`topbiz-service` 是整个仓库的公共入口，主要负责三类事情：

- `Authentication + Authorization`
- 底层服务 API 封装与按需对外暴露
- 跨服务事务编排与补偿

目前已经落地的编排流包括：

- 用户开通编排
- 部门调拨编排
- 消息发送 + 审计编排

当前事务策略采用的是更适合微服务边界的 `Saga-like compensation`，而不是 XA / 2PC。

## 当前交付状态

目前仓库已经完成以下基础交付：

- Maven 多模块结构已经搭好
- 四个业务模块都补齐了 `controller / dto / service / repository / domain / config`
- `docs/contracts` 下的服务契约已经冻结
- `topbiz-service` 已具备 `OpenFeign + Shiro + Session` 的完整链路
- `user-service` 已具备最小可用的后台管理和自助能力
- `message-service` 已具备模板、任务、重试、收件箱和统计基线
- `log-service` 已具备接入、检索、告警和导出基线
- `topbiz-service` 已补上生产可切换的 Redis Session 与 JDBC 编排持久化方案
- 全仓测试可通过

## 技术栈

- `Java 21`
- `Spring Boot 3.5.0`
- `Spring Cloud 2025.0.0`
- `OpenFeign`
- `Apache Shiro`
- `Spring Session Redis`
- `Spring JDBC + HikariCP`
- `Maven Wrapper`

## 仓库结构

```text
platform-parent
├─ service-common
├─ user-service
├─ message-service
├─ log-service
├─ topbiz-service
└─ docs
   ├─ contracts
   └─ specs
```

## 快速开始

### 环境要求

- JDK `21`
- 建议直接使用仓库内置的 `mvnw` / `mvnw.cmd`

### 先跑测试

```powershell
.\mvnw.cmd clean test
```

### 单模块测试

```powershell
.\mvnw.cmd -pl user-service -am test
.\mvnw.cmd -pl message-service -am test
.\mvnw.cmd -pl log-service -am test
.\mvnw.cmd -pl topbiz-service -am test
```

### 本地启动顺序

建议先启动底层服务，再启动 `topbiz-service`：

```powershell
.\mvnw.cmd -pl user-service -am spring-boot:run
.\mvnw.cmd -pl message-service -am spring-boot:run
.\mvnw.cmd -pl log-service -am spring-boot:run
.\mvnw.cmd -pl topbiz-service -am spring-boot:run
```

默认端口如下：

- `topbiz-service`: `8080`
- `user-service`: `8081`
- `message-service`: `8082`
- `log-service`: `8083`

## topbiz 运行模式

### 本地模式

`topbiz-service` 默认按本地开发模式启动：

- Session 存储：`servlet session`
- 编排仓储：`memory`
- 不强依赖 Redis
- 不强依赖编排持久化数据库

这保证了本地联调和测试时不需要额外基础设施即可启动。

### 生产模式

生产环境通过 `prod` profile 启用正式能力：

- Session 存储：`Redis Session`
- 编排仓储：`JDBC`
- 自动读取 Redis 与编排库连接参数

启动方式：

```powershell
.\mvnw.cmd -pl topbiz-service spring-boot:run -Dspring-boot.run.profiles=prod
```

生产相关详细说明见：

- [docs/topbiz-production-runbook.md](./docs/topbiz-production-runbook.md)

## 文档入口

建议先阅读以下文档：

- [docs/contracts/README.md](./docs/contracts/README.md)
- [docs/service-boundaries.md](./docs/service-boundaries.md)
- [docs/contracts/topbiz-service-contract.md](./docs/contracts/topbiz-service-contract.md)
- [docs/contracts/user-service-contract.md](./docs/contracts/user-service-contract.md)
- [docs/contracts/message-service-contract.md](./docs/contracts/message-service-contract.md)
- [docs/contracts/log-service-contract.md](./docs/contracts/log-service-contract.md)
- [docs/specs/topbiz-service-full-spec.md](./docs/specs/topbiz-service-full-spec.md)
- [docs/TEAM_AI_GUIDE.md](./docs/TEAM_AI_GUIDE.md)
- [user-service/README.md](./user-service/README.md)
- [message-service/README.md](./message-service/README.md)
- [log-service/README.md](./log-service/README.md)
- [topbiz-service/README.md](./topbiz-service/README.md)

## 协作规则

多人协作或交给 AI 继续开发时，请遵守这些规则：

- 不要私自修改 `docs/contracts` 下已冻结的接口契约
- 不要让底层服务之间直接相互调用
- 不要把业务主数据落到 `topbiz-service`
- 新增权限字符串时遵守 `资源:动作` 规范
- controller 只做边界，业务逻辑放到 service
- 提交前至少保证本模块测试通过

## 后续方向

当前基线已经够继续扩展，但后续仍建议沿这些方向演进：

- 补更多正式编排流
- 增强 Feign 限流、超时和失败恢复策略
- 补幂等、防重放和审计检索能力
- 将生产部署参数进一步模板化
