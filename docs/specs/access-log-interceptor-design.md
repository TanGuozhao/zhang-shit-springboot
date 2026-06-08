# 可复用访问日志拦截器设计

**作者**: Codex  
**日期**: 2026-06-07  
**状态**: Draft for implementation  
**参考文档**: `可复用访问日志管理服务设计-2024090907024-刘新宇.md`、`docs/specs/log-service-p2-spec.md`、`docs/specs/log-service-p2-implementation-plan.md`

## 1. 现状结论

当前仓库**不是完全没有拦截能力**，但**没有真正的访问日志拦截器**。

当前已存在的相关能力：

- `service-common` 中有 [TraceIdFilter.java](f:\shit\service-common\src\main\java\com\example\platform\common\web\TraceIdFilter.java)，负责生成或透传 `X-Trace-Id`，并写入 `MDC`
- `topbiz-service` 中有 [TopbizSessionFilter.java](f:\shit\topbiz-service\src\main\java\com\example\platform\topbiz\security\TopbizSessionFilter.java)，负责会话鉴权
- `topbiz-service` 中有 [TopbizFeignConfig.java](f:\shit\topbiz-service\src\main\java\com\example\platform\topbiz\config\TopbizFeignConfig.java)，负责下游调用时透传 `X-Trace-Id`、`X-Request-Id`
- `topbiz-service` 中有 [TopbizAuditService.java](f:\shit\topbiz-service\src\main\java\com\example\platform\topbiz\service\TopbizAuditService.java)，但它是手工调用日志服务，并不是统一入口拦截器

所以结论很明确：

- 我们已经有“链路标识传播”
- 我们还没有“统一访问日志采集拦截器”
- 这部分应该设计，而且应该做成**可复用组件**，优先落在 `service-common`，供 `topbiz-service`、`user-service`、`message-service` 统一接入

## 2. 设计目标

基于刘新宇文档，本拦截器需要承担以下职责：

1. 在 HTTP 请求入口统一采集访问日志
2. 统一补齐链路字段、基础信息、结果与耗时字段
3. 支持异常分类、慢请求识别、敏感数据脱敏
4. 与现有 `log-service` 对接，异步上报日志
5. 对业务线程影响可控，不能因为日志写入失败拖垮主流程
6. 保持可配置、可关闭、可灰度启用

## 3. 与刘新宇文档的对齐

刘新宇文档中对拦截器要求的核心点包括：

- 入口读取或生成 `trace_id`
- 记录 `service_name`、`client_ip`、`method`、`uri`
- 记录 `cost_ms`、`http_status`、`biz_code`
- 支持 `req_params`、`res_body` 开关采集
- 支持异常分类
- 支持敏感字段脱敏
- 支持慢请求输出与统计

本设计完全保留这些目标，但对当前仓库做两点工程化纠偏：

### 3.1 纠偏一：拦截器不直接承担全部统计计算

刘新宇文档中部分指标是“日志记录时统计（预聚合）”。  
当前仓库已经把预聚合和分析能力放在 `log-service`，因此拦截器只负责：

- 采集结构化访问日志
- 标准化字段
- 异步投递

而不在拦截器本身内做复杂聚合。

### 3.2 纠偏二：当前日志服务 DTO 字段还不够完整

当前 `log-service` 的 `LogIngestRequest` 字段为：

- `serviceName`
- `traceId`
- `level`
- `message`
- `path`
- `statusCode`
- `latencyMs`
- `requestId`
- `clientIp`
- `timestamp`
- `tags`

这意味着刘新宇文档中的以下字段，短期需要放入 `tags`：

- `method`
- `uri`
- `biz_code`
- `span_id`
- `user_agent`
- `exception_type`
- `req_params_summary`
- `res_body_summary`

也就是说：

- **Phase 1**：拦截器先通过 `tags` 补齐结构化信息，兼容当前 `log-service`
- **Phase 2**：再考虑扩展 `log-service` 的日志接入模型

## 4. 总体方案

推荐采用“`Filter + HandlerInterceptor + AsyncPublisher`”三层方案。

### 4.1 Trace 入口层

继续复用现有 `TraceIdFilter`：

- 读取请求头 `X-Trace-Id`
- 若为空则生成 UUID
- 写入 `MDC`
- 写回响应头

必要时补充：

- `X-Request-Id`
- `spanId`

### 4.2 访问日志采集层

新增 `AccessLogInterceptor`，基于 Spring MVC `HandlerInterceptor`：

- `preHandle` 记录开始时间
- `afterCompletion` 计算耗时、采集状态、异常信息
- 从请求和响应中提取结构化字段
- 将日志事件投递到异步发布器

### 4.3 内容缓存层

若需要采集请求体/响应体摘要，单靠 `HandlerInterceptor` 不够，建议额外引入 `AccessLogBodyCachingFilter`：

- 使用 `ContentCachingRequestWrapper`
- 使用 `ContentCachingResponseWrapper`
- 只缓存配置允许的接口
- 只保留摘要，不保留无限长度原文

### 4.4 异步发布层

新增 `AccessLogPublisher`：

- 将拦截器采集出的日志事件放入内存队列
- 后台线程池异步调用 `log-service`
- 失败时本地降级记录 `WARN`
- 与业务线程解耦

## 5. 推荐模块划分

推荐把通用能力放入 `service-common`：

- `TraceIdFilter`
- `AccessLogInterceptor`
- `AccessLogBodyCachingFilter`
- `AccessLogContext`
- `AccessLogProperties`
- `AccessLogPublisher`
- `SensitiveDataMasker`
- `ExceptionClassifier`

业务服务只做最小接入：

- 注册拦截器
- 注入服务名
- 配置日志服务地址或发布适配器
- 自定义 `bizCode` 提取策略和用户身份提取策略

## 6. 日志模型设计

### 6.1 拦截器内部标准模型

建议新增内部模型 `AccessLogEvent`：

```java
public record AccessLogEvent(
        String traceId,
        String requestId,
        String spanId,
        String serviceName,
        String method,
        String uri,
        String clientIp,
        String userAgent,
        String userId,
        String bizCode,
        Integer httpStatus,
        Long costMs,
        String level,
        String outcome,
        String exceptionType,
        String exceptionCode,
        String message,
        String reqParamsSummary,
        String resBodySummary,
        Instant timestamp,
        Map<String, String> tags
) {}
```

### 6.2 与当前 `log-service` 的映射

当前先映射为：

```text
serviceName -> serviceName
traceId -> traceId
level -> level
message -> message
uri -> path
httpStatus -> statusCode
costMs -> latencyMs
requestId -> requestId
clientIp -> clientIp
timestamp -> timestamp
其余字段 -> tags
```

### 6.3 tags 推荐字段

建议统一写入以下 `tags`：

- `method`
- `uri`
- `spanId`
- `bizCode`
- `userId`
- `userAgent`
- `outcome`
- `exceptionType`
- `exceptionCode`
- `slowRequest`
- `reqParamsSummary`
- `resBodySummary`

## 7. 处理流程

### 7.1 正常请求

1. `TraceIdFilter` 生成或透传 `traceId`
2. `AccessLogBodyCachingFilter` 包装请求与响应
3. `AccessLogInterceptor.preHandle` 记录开始时间
4. 业务控制器执行
5. `AccessLogInterceptor.afterCompletion` 采集日志
6. 进行脱敏、摘要截断、异常分类
7. 投递给 `AccessLogPublisher`
8. 发布器异步上报到 `log-service`

### 7.2 异常请求

1. 业务代码或框架抛出异常
2. 全局异常处理器返回统一响应
3. `afterCompletion` 拿到异常对象
4. `ExceptionClassifier` 计算异常类型
5. 输出 `ERROR` 或 `WARN` 级访问日志
6. 异步投递到 `log-service`

## 8. 异常分类设计

建议统一分类为：

- `BIZ`: 业务异常，如参数错误、用户不存在
- `AUTH`: 认证鉴权异常
- `RPC`: 远程调用异常
- `DB`: 数据库异常
- `SYS`: 系统异常
- `TIMEOUT`: 超时异常
- `UNKNOWN`: 未识别异常

分类规则建议做成独立组件 `ExceptionClassifier`，避免写死在拦截器中。

## 9. 慢请求策略

按刘新宇文档要求，需要支持慢请求监控。

建议配置项：

- `slowThresholdMs`: 默认 `3000`
- `errorStatusThreshold`: 默认 `500`

规则：

- `costMs >= slowThresholdMs` 时标记 `slowRequest=true`
- 慢请求默认记录为 `WARN`
- `5xx` 或捕获异常默认记录为 `ERROR`
- 普通请求记录为 `INFO`

## 10. 请求体与响应体采集策略

这部分不能默认全开，必须做护栏。

### 10.1 默认策略

- 默认不记录完整请求体和响应体
- 默认只记录参数摘要
- 仅在异常请求、慢请求或显式配置接口白名单时采集详细摘要

### 10.2 摘要策略

- `req_params` 最长 `2KB`
- `res_body` 最长 `2KB`
- 超出长度截断并追加 `...truncated`

### 10.3 不采集对象

以下类型默认禁止落日志正文：

- 文件上传
- 二进制流
- 图片
- Excel/PDF
- 大文本下载

## 11. 脱敏设计

必须在拦截器侧做一次脱敏，不能把明文敏感数据直接送到 `log-service`。

### 11.1 默认脱敏项

- `password`
- `token`
- `authorization`
- `secret`
- `idCard`
- `phone`
- `email`

### 11.2 脱敏规则

- 手机号：`138****5678`
- 邮箱：用户名部分打码
- 密码：固定输出 `***`
- Token：固定输出 `***`
- 身份证：中间位掩码

### 11.3 实现方式

推荐 `SensitiveDataMasker` 支持两种模式：

- 基于字段名的通用 JSON 脱敏
- 基于注解的对象字段脱敏

本仓库第一阶段建议先上**字段名规则脱敏**，复杂度更低。

## 12. 用户身份与业务码提取

### 12.1 用户身份

不同服务提取方式不同：

- `topbiz-service`: 可从 `TopbizPrincipal` 或会话上下文中取 `userId`
- `user-service`: 优先取 `X-User-Id`，没有则留空
- `message-service`: 通常没有用户态，可留空

### 12.2 业务码

`biz_code` 不应由拦截器硬编码解析业务响应。

建议定义扩展接口：

```java
public interface BizCodeResolver {
    String resolve(HttpServletRequest request, HttpServletResponse response, Object handler);
}
```

默认实现返回空值，业务服务按需覆盖。

## 13. 配置设计

建议新增配置前缀：`platform.access-log`

```yaml
platform:
  access-log:
    enabled: true
    service-name: topbiz
    slow-threshold-ms: 3000
    capture-request-body: false
    capture-response-body: false
    capture-on-error-only: true
    max-body-length: 2048
    exclude-paths:
      - /actuator/**
      - /error
      - /favicon.ico
    publisher:
      queue-capacity: 5000
      batch-size: 100
      flush-interval-ms: 1000
      fail-fast: false
```

## 14. 与当前仓库的集成建议

### 14.1 第一阶段落点

优先在 `topbiz-service` 接入，因为它：

- 已有对 `log-service` 的 Feign 客户端
- 已有 `traceId` 透传
- 已有用户态和会话上下文
- 已有部分手工日志上报逻辑

### 14.2 第二阶段扩展

再接入：

- `user-service`
- `message-service`

### 14.3 与现有 `TopbizAuditService` 的关系

`TopbizAuditService` 不应删除，但应重新定位为：

- 负责“业务审计事件”
- 不负责“通用 HTTP 访问日志”

即：

- 访问日志 -> 统一走 `AccessLogInterceptor`
- 审计日志 -> 继续走 `TopbizAuditService`

## 15. 出站调用链路设计

当前 `TopbizFeignConfig` 已经透传：

- `X-Trace-Id`
- `X-Request-Id`

后续建议统一约定：

- 入站 `TraceIdFilter` 负责 traceId
- 出站 Feign 拦截器负责透传
- 如后续引入消息队列，也需要透传 `traceId`

这样查询链路日志时，`GET /api/logs/trace/{traceId}` 才有完整价值。

## 16. 性能与可靠性要求

### 16.1 性能要求

- 拦截器主线程处理不得包含远程同步调用
- 访问日志采集本身额外开销目标 `< 5ms`
- 请求体/响应体采集默认关闭

### 16.2 可靠性要求

- 日志发布失败不能影响业务响应
- 发布器队列满时允许丢弃低优先级日志，但必须计数
- 错误日志和慢请求日志优先级高于普通日志

## 17. 验收标准

满足以下条件时，认为拦截器设计可进入实现阶段：

1. 普通请求能自动生成访问日志，无需业务手写
2. 异常请求能自动生成错误分类日志
3. 慢请求会被正确标记
4. `traceId` 能在上下游服务间贯通
5. 敏感字段不会明文进入日志服务
6. 日志发布失败不会影响主业务请求
7. `topbiz-service` 能先行灰度接入

## 18. 实施顺序建议

### Phase A

- 在 `service-common` 增加 `AccessLogInterceptor` 基础框架
- 接入 `topbiz-service`
- 先采集基础字段，不采集 body

### Phase B

- 增加异常分类、慢请求标记、脱敏
- 增加可配置请求体/响应体摘要

### Phase C

- 抽象 `BizCodeResolver`
- 统一接入 `user-service`、`message-service`
- 增加日志采样和限流

## 19. 结论

当前仓库**确实缺少真正的访问日志拦截器**，而这部分正是刘新宇文档中“访问日志记录、异常分类、脱敏、慢请求监控、链路关联”落地的关键入口。

因此建议马上补这个设计，并按“`service-common` 通用组件 + `topbiz-service` 先接入 + `log-service` 继续作为接收与分析后端”的方式推进。这个方案既和刘新宇文档一致，也和当前仓库已经完成的 `P2 log-service` 方向不冲突。
