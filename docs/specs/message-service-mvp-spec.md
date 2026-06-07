# Spec: Message Service Historical Baseline

**Author:** Codex  
**Date:** 2026-06-07  
**Status:** Superseded  
**Reviewers:** User  
**Related docs:** `docs/contracts/message-service-contract.md`, `可复用消息通道服务设计-2024091602016-谭国照.md`, `docs/specs/message-service-full-spec.md`

## 背景

仓库中的 `message-service` 原本只是一个非常轻量的骨架，只覆盖了发送、草稿、模板预览和基础查询，距离设计文档里的“模板、变量、通道、调度、重试、记录追踪”还有明显缺口。  
本文件仅保留第一阶段历史设计基线，当前实现应以 `docs/specs/message-service-full-spec.md` 为准。

## 历史范围

- 模板管理：查询模板列表、模板详情、模板变量说明
- 变量处理：变量填充、变量完整性校验、模板预览
- 通道管理：查询可用发送通道
- 消息发送：支持即时发送与定时发送配置校验
- 草稿管理：保存草稿
- 消息查询：消息详情、状态、失败原因、记录列表、关键词搜索
- 数据承载：使用内存仓储实现模板、变量、通道和消息记录

## 当时未落地项

- 真实第三方短信/邮件/飞书投递
- 数据库持久化和分布式任务调度
- 附件上传与附件资源中心
- 管理员侧的增删改模板、变量、通道账户接口
- 真正的自动重试执行器

## 设计到代码的映射

### 核心实体

- `MessageTemplate`：模板编码、标题模板、内容模板、描述、变量编码集合
- `MessageVariable`：变量编码、名称、说明、数据类型、默认值、必填标识、自动填充标识
- `MessageChannel`：通道编码、载体类型、运营商、账户编码、发送者、可用状态
- `MessageTask`：消息主记录，包含主题、正文、变量、状态、调度方式、计划时间、失败原因、重试次数

### 服务分层

- `MessageFlowSupportService`：负责模板/通道校验、变量解析、内容渲染、接收人校验、调度校验
- `MessageCommandService`：负责发送、草稿、预览、变量填充和校验
- `MessageQueryService`：负责模板、变量、通道、消息记录和失败原因查询

## 历史接口清单

- `POST /api/messages/send`
- `POST /api/messages/drafts`
- `POST /api/messages/templates/{templateCode}/preview`
- `POST /api/messages/variables/fill`
- `POST /api/messages/variables/validate`
- `POST /api/messages/schedule/validate`
- `GET /api/messages/templates`
- `GET /api/messages/templates/{templateCode}`
- `GET /api/messages/templates/{templateCode}/variables`
- `GET /api/messages/channels`
- `GET /api/messages/{messageId}`
- `GET /api/messages/{messageId}/status`
- `GET /api/messages/{messageId}/error`
- `GET /api/messages/records`
- `GET /api/messages/search`

## 后续演进记录

1. 将 `TemplateRepository`、`VariableRepository`、`ChannelRepository` 切换为数据库实现。
2. 在 `MessageCommandService` 和独立的 `Dispatcher` 之间增加真正的异步任务队列。
3. 将 `shouldSimulateFailure` 替换为真实投递适配器和重试策略引擎。
4. 新增管理员管理端接口，支持模板、变量、通道配置维护。
