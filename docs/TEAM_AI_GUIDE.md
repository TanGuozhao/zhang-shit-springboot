# 队友 AI 开发协作说明

本文档给参与本仓库开发的同学及其 AI 编码助手使用。目标是让多人并行开发时，接口不漂移、边界不混乱、提交能直接合并。

## 1. 项目定位

当前仓库包含 5 个模块：

- `service-common`：公共响应、异常、基础过滤器
- `user-service`：用户、角色、权限、部门、登录账号
- `message-service`：消息、模板、草稿、发送状态
- `log-service`：访问日志、检索、指标、告警、导出
- `topbiz-service`：编排层，对外统一入口，负责鉴权和跨服务聚合

## 2. 架构硬规则

- `topbiz-service` 只做编排，不落业务主数据
- 底层服务之间禁止直接互调
- 外部系统访问底层微服务，原则上都应通过 `topbiz-service`
- 服务间调用统一使用 `OpenFeign`
- `topbiz-service` 的认证授权使用 `Shiro`
- 当前接口契约已经冻结，禁止私自改接口路径、字段名、响应结构

## 3. 契约文件

开始写代码前，必须先阅读：

- [contracts/README.md](f:/shit/docs/contracts/README.md)
- [contracts/user-service-contract.md](f:/shit/docs/contracts/user-service-contract.md)
- [contracts/message-service-contract.md](f:/shit/docs/contracts/message-service-contract.md)
- [contracts/log-service-contract.md](f:/shit/docs/contracts/log-service-contract.md)

如果实现中发现契约缺字段：

1. 不要直接改 controller 接口
2. 先提出变更点
3. 由负责人统一决定是否升级契约

## 4. 每个模块允许做什么

### `user-service`

允许：

- 实现用户资料、角色、权限、部门、登录校验
- 完善 `repository / service / domain`
- 补充 DTO 字段映射和校验

禁止：

- 直接调用 `message-service` 或 `log-service`
- 写 `topbiz` 编排逻辑

### `message-service`

允许：

- 实现模板、草稿、发送、状态查询
- 补充消息领域对象和存储层

禁止：

- 依赖 `user-service` 做联查
- 增加跨服务聚合接口

### `log-service`

允许：

- 实现日志接收、检索、trace 查询、指标、告警、导出
- 完善日志筛选和分页查询骨架

禁止：

- 依赖其他业务服务做数据补全
- 在本服务内做平台级聚合展示

### `topbiz-service`

允许：

- 封装底层服务 API
- 做登录、鉴权、权限串校验
- 做跨服务编排和统一出口

禁止：

- 落业务主数据
- 把底层业务规则复制到 `topbiz`

## 5. AI 修改代码时必须遵守

- 优先补实现，不要重命名现有包结构
- 不要随意修改 DTO 字段名
- 不要改已有 URL 路径
- 不要把返回结构从 `ApiResponse<T>` 改成别的格式
- 不要删除现有测试
- 不要把一个服务的 domain 复制到另一个服务
- 不要引入与当前模块无关的大型框架

## 6. 代码风格约束

- Java 版本：`21`
- Spring Boot：`3.5.0`
- Spring Cloud：`2025.0.0`
- 返回统一使用 `ApiResponse`
- 公共异常优先复用 `service-common`
- controller 只做入参和出参
- 业务逻辑放 `service`
- 领域对象放 `domain`
- 请求/响应模型放 `dto`

## 7. Shiro 与权限约束

当前 `topbiz-service` 已具备基础 Shiro 骨架。

约束如下：

- 权限字符串使用冒号风格，如 `user:read`、`message:send`、`log:query`、`topbiz:admin`
- 新增权限时，先遵守“资源:动作”格式
- 不要把权限判断写死在前端
- `topbiz` controller 上优先使用 `@RequiresPermissions`

## 8. Redis Session 说明

仓库里已经保留了 `Redis Session` 依赖和配置入口。

当前状态：

- 项目骨架可运行
- 后续如果要把会话完全落到 Redis，需要在现有基础上继续补会话桥接实现

因此：

- 不要擅自删掉 Redis 相关依赖
- 也不要为了“先跑通”把鉴权整体换成 Spring Security

## 9. 提交前检查

每次提交前至少完成：

1. 本模块编译通过
2. 本模块测试通过
3. 不修改不属于自己的契约
4. 没有把别的服务的代码耦合进来

推荐命令：

```powershell
.\mvnw.cmd clean test
```

如果只验证单模块：

```powershell
.\mvnw.cmd -pl user-service -am test
.\mvnw.cmd -pl message-service -am test
.\mvnw.cmd -pl log-service -am test
.\mvnw.cmd -pl topbiz-service -am test
```

## 10. 给 AI 的标准提示词

队友如果要让 AI 继续写代码，建议直接带上这段：

```text
你正在维护一个 Spring Boot 多模块项目，包含 user-service、message-service、log-service、topbiz-service。
请严格遵守 docs/contracts 下的接口契约，不要修改接口路径、字段名和统一响应结构 ApiResponse。
topbiz 只做编排，不落业务主数据；底层服务之间禁止互调；服务间调用统一使用 OpenFeign。
只在我指定的模块内修改代码，优先补 service、repository、domain 实现，并保证 mvnw test 可通过。
```

## 11. 负责人保留项

下面这些内容不要由队友各自 AI 随意推进：

- 契约升级
- `topbiz-service` 新增对外平台接口
- 分布式事务最终选型
- Shiro 权限体系总规范
- 统一日志/trace 规范

这些应由项目负责人统一收口。
