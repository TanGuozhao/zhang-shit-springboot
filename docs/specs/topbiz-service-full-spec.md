# 规格说明：Topbiz 服务完整交付版

**作者：** Codex  
**日期：** 2026-06-07  
**状态：** 已批准  
**评审人：** 用户（委托自主交付）  
**关联文档：** `docs/contracts/topbiz-service-contract.md`、`docs/contracts/user-service-contract.md`、`docs/contracts/message-service-contract.md`、`docs/contracts/log-service-contract.md`

## 背景

`topbiz-service` 是平台统一对外的网关与编排服务。
它主要承担三类职责：

- 对外暴露底层能力接口
- 做认证与授权控制
- 做跨服务编排与平台级运行聚合

它**不持有**业务主数据。
用户主数据属于 `user-service`，消息主数据属于 `message-service`，
日志主数据属于 `log-service`。

原始笔记和设计意图比较明确，但在正式交付层面仍存在几处缺口：

- 网关边界尚未冻结
- topbiz 权限模型与 `user-service` 尚未完全对齐
- 编排行为缺少真实执行记录承载
- 登录与运行态缺少明确的 Shiro + Session 落地路径

本次交付就是为了解决这些缺口，形成一版已经可用的 Topbiz 平台网关基线。

## 已修正的设计决策

- `topbiz-service` 是跨服务编排的唯一公开入口。
- 底层服务默认不允许直接通过业务 API 互相调用。
- `topbiz-service` 使用 `OpenFeign` 做服务间集成。
- 认证通过 `user-service` 完成，再投影为 `TopbizPrincipal`。
- 授权在 `topbiz-service` 中通过 `Shiro` 权限注解执行。
- 当前本地运行态的会话实现为 `Servlet Session 桥接 + Shiro Subject 绑定`。
  `prod` profile 下切换为 `Redis Session`。
- `topbiz-service` 负责平台外层 Session，`user-service` 负责底层 `sessionKey`。
- 第三方登录对外统一走 OAuth authorize/callback，再回落到 `user-service` 第三方绑定登录接口。
- 分布式事务当前采用 `编排层 Saga-like compensation` 方案，
  而不是 XA / 2PC，这更符合当前架构边界。
- 编排执行持久化通过仓储接口抽象。
  本地模式使用内存存储，生产模式使用可持久化 JDBC 仓储。

## 架构决策

### 服务角色

- `topbiz-service` 持有：
  - 对外网关层
  - 登录态投影
  - 授权校验
  - 平台概览聚合
  - 跨服务编排
  - 跨服务审计 / trace 透传
- `topbiz-service` 不持有：
  - 用户账号与 RBAC 主数据
  - 消息模板、任务、消息记录主数据
  - 访问日志主存储与告警规则主数据

### 分层结构

- `controller`：对外 API 边界
- `dto`：稳定的请求 / 响应契约
- `service`：网关逻辑、编排逻辑、聚合逻辑
- `security`：Shiro Realm、Session Filter、权限常量、Principal 模型
- `remote`：访问底层服务的 Feign Client
- `repository`：编排执行记录持久化抽象
- `domain`：编排执行记录与步骤记录
- `config`：Feign / Shiro / Properties 等装配配置

### 调用流程

1. 客户端调用 `topbiz-service`
2. `topbiz-service` 完成认证，或恢复 `TopbizPrincipal`
3. `Shiro` 校验当前接口所需权限
4. 网关服务通过 `OpenFeign` 转发到下游底层服务
5. 透传 `X-User-Id`、`X-Session-Key` 与 trace 头
6. 对于编排流程，由 topbiz 协调多次远程调用和补偿逻辑

### 邮箱验证码登录流程

1. 客户端调用 `POST /api/topbiz/auth/email/send-code`
2. `topbiz-service` 透传到 `user-service /api/users/auth/email/send-code`
3. 客户端提交 `POST /api/topbiz/auth/email/login`
4. `topbiz-service` 调用 `user-service /api/users/auth/email/login`
5. `user-service` 返回底层 `sessionKey`
6. `topbiz-service` 建立平台外层 Session，并返回 `sessionId + sessionKey`

### 第三方登录流程

1. 客户端调用 `GET /api/topbiz/auth/oauth/{provider}/authorize`
2. `topbiz-service` 生成并保存 OAuth `state`
3. 客户端跳转第三方授权页
4. 第三方回调 `GET /api/topbiz/auth/oauth/{provider}/callback`
5. `topbiz-service` 校验并消费 `state`
6. `topbiz-service` 通过 `TopbizOAuthProviderClient` 交换用户资料
7. `topbiz-service` 调用 `user-service /api/users/auth/third-party/login`
8. `user-service` 命中绑定或自动注册后返回底层 `sessionKey`
9. `topbiz-service` 建立平台外层 Session 并返回登录结果

## 功能性需求

- FR-1：`topbiz-service` 必须统一对外基础路径为 `/api/topbiz`
- FR-2：`topbiz-service` 必须支持登录、登出、当前会话查询
- FR-3：`topbiz-service` 必须通过 `user-service` 完成密码、邮箱验证码、第三方绑定三类认证
- FR-4：`topbiz-service` 必须通过显式权限串执行授权校验
- FR-5：`topbiz-service` 必须代理已冻结的 `user-service` 公开与管理契约
- FR-6：`topbiz-service` 必须代理已冻结的 `message-service` 公开与管理契约
- FR-7：`topbiz-service` 必须代理已冻结的 `log-service` 公开与管理契约
- FR-8：`topbiz-service` 必须提供平台概览聚合能力
- FR-9：`topbiz-service` 必须提供平台架构聚合能力
- FR-10：`topbiz-service` 必须提供消息 / 日志 / topbiz 运行态聚合能力
- FR-11：`topbiz-service` 必须提供用户开通编排流程
- FR-12：`topbiz-service` 必须提供部门调拨编排流程
- FR-13：`topbiz-service` 必须提供消息审计编排流程
- FR-14：`topbiz-service` 必须通过仓储抽象持久化编排执行记录
- FR-15：`topbiz-service` 必须为编排尝试写入审计日志
- FR-16：`topbiz-service` 必须向下游服务透传用户身份上下文

## 非功能性需求

- NFR-1：所有 API 必须返回 `ApiResponse<T>`
- NFR-2：公开 Controller 必须保持稳定，支持前后端与多人并行开发
- NFR-3：权限常量必须集中维护
- NFR-4：远程调用失败必须统一归一化为业务异常
- NFR-5：测试必须覆盖登录、未认证、无权限、编排主流程成功路径
- NFR-6：Topbiz 对业务主数据必须保持无状态

## 当前已交付内容

### 认证与授权

- 基于 `user-service` 登录与权限查询的 Shiro Realm
- `TopbizPrincipal` 会话投影
- `TopbizSessionFilter` 请求级认证主体绑定
- 平台密码登录入口
- 平台邮箱验证码发码与登录入口
- OAuth authorize/callback 入口
- OAuth state 管理
- 第三方登录后的平台外层 Session 建立
- 平台、编排、消息管理、日志管理、运行时操作等权限常量
- 专门处理 `401/403` JSON 响应的安全异常处理器

### 外部认证扩展点

- `TopbizOAuthProviderClient`
  - 对第三方 provider 换码与用户资料拉取做抽象
- `MockTopbizOAuthProviderClient`
  - 当前默认实现
  - 在 `mock-enabled=true` 时返回 mock 资料
  - 属于可替换实现，不代表真实开放平台已接入

### 网关能力面

- 用户自助网关
- 用户管理 / 角色管理 / 部门管理网关
- 消息使用接口
- 消息管理与运行时接口
- 日志查询、管理与运行时接口
- 平台概览、架构、运行态接口

### 编排能力

- 用户开通编排
  - 创建用户
  - 可选发送欢迎消息
  - 下游失败时通过禁用已创建用户进行补偿
  - 写入审计日志
- 部门调拨编排
  - 调拨成员
  - 可选发送通知
  - 下游失败时通过反向调拨进行补偿
  - 写入审计日志
- 消息审计编排
  - 发送消息
  - 写入审计日志
  - 如果日志写入失败，则返回部分成功状态

### 聚合能力

- 当前会话 + 当前用户 + 当前部门概览
- topbiz / user / message / log 架构视图
- 指标、告警、消息运行态、日志运行态聚合视图
- 最近编排记录的快捷视图

### 测试覆盖

当前测试已覆盖：

- 未认证访问返回 `401`
- 登录后可创建有效 topbiz 会话
- 邮箱验证码发码会透传到 `user-service`
- 邮箱验证码登录可建立平台外层 Session
- OAuth authorize 可返回授权地址与 state
- OAuth callback 可完成第三方登录并建立平台外层 Session
- 权限不足返回 `403`
- 管理员可以访问平台概览
- 用户开通编排可执行并写入执行记录

## 必需权限模型

Topbiz 当前依赖以下权限串：

- `topbiz:admin`
- `topbiz:platform:read`
- `topbiz:architecture:read`
- `topbiz:orchestration:write`
- `topbiz:message:admin`
- `topbiz:log:admin`
- `topbiz:runtime:operate`

这些权限已经在 `user-service` 中完成预置，同时 `ADMIN` / `OPS_ADMIN` 默认角色也已完成对齐。

## 下一阶段已知缺口

- QQ / 微信真实开放平台换码、token 校验、用户资料拉取尚未接入
- 邮件验证码真实投递能力尚未接入
- 为编排提交增加幂等与重放保护
- 如果前端需要，可在 topbiz 侧补充审计检索 / 报表界面接口
- 等团队调研项完成后，再扩展更多正式编排流程

## 后续加固方向

- 继续完善 Redis Session 与 Shiro 策略细节
- 强化 Feign 的容错、超时策略与可观测性
- 后续新增编排流程时，只能建立在当前已冻结的网关契约之上
