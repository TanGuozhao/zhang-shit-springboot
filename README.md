# Platform Skeleton

这是一个基于 Spring Boot 的多模块微服务骨架项目，当前用于承接 3 个底层服务和 1 个编排层服务的并行开发。

## 模块说明

- `service-common`：公共响应、异常处理、Trace 过滤器
- `user-service`：用户、角色、权限、部门、登录账号
- `message-service`：消息模板、草稿、发送、状态
- `log-service`：访问日志、检索、指标、告警、导出
- `topbiz-service`：统一对外入口，负责编排、鉴权、聚合

## 当前架构约束

- `topbiz-service` 只做编排，不保存业务主数据
- 底层服务之间禁止直接相互调用
- 跨服务能力统一通过 `topbiz-service` 编排
- 服务间调用方式统一为 `OpenFeign`
- 登录与权限体系位于 `topbiz-service`
- `topbiz-service` 当前使用 `Shiro` 做 Authentication / Authorization

## 已完成内容

- 已改造成 Maven 多模块工程
- 已补齐 `user / message / log` 的正式分包结构
- 已补齐三类底层服务的业务 API 骨架和 DTO
- 已冻结底层服务对 `topbiz` 的接口契约
- 已完成 `topbiz` 的 `OpenFeign + Shiro` 骨架
- 已解决 Spring Boot 3 与 Shiro 的 Jakarta 兼容问题
- 全仓 `clean test` 已通过

## 契约文档

请先阅读：

- [docs/contracts/README.md](f:/shit/docs/contracts/README.md)
- [docs/contracts/user-service-contract.md](f:/shit/docs/contracts/user-service-contract.md)
- [docs/contracts/message-service-contract.md](f:/shit/docs/contracts/message-service-contract.md)
- [docs/contracts/log-service-contract.md](f:/shit/docs/contracts/log-service-contract.md)
- [docs/TEAM_AI_GUIDE.md](f:/shit/docs/TEAM_AI_GUIDE.md)

## 本地运行

全量测试：

```powershell
.\mvnw.cmd clean test
```

单模块测试示例：

```powershell
.\mvnw.cmd -pl user-service -am test
.\mvnw.cmd -pl message-service -am test
.\mvnw.cmd -pl log-service -am test
.\mvnw.cmd -pl topbiz-service -am test
```

## 端口约定

- `topbiz-service`：`8080`
- `user-service`：`8081`
- `message-service`：`8082`
- `log-service`：`8083`

## 目录结构

```text
platform-parent
├─ service-common
├─ user-service
├─ message-service
├─ log-service
├─ topbiz-service
└─ docs
   └─ contracts
```

## 推荐分工

- A：`user-service`
- B：`message-service`
- C：`log-service`
- 项目负责人：`topbiz-service`、契约收口、权限规范、集成测试

## 注意事项

- 不要私自修改 `docs/contracts` 中的接口契约
- 不要让底层服务互相调用
- 不要在 `topbiz-service` 中落业务主数据
- 新增权限字符串时遵守 `资源:动作` 格式
- 提交前至少保证本模块测试通过
