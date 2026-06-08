# 消息服务契约

## 目的

`message-service` 持有消息模板、变量、投递、渠道、重试与收件箱能力。

## 对外基础路径

`/api/messages`

## 第一版冻结接口

### 1. 查询架构概览

- 方法：`GET`
- 路径：`/internal/architecture/overview`
- 响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "service": "message-service",
    "boundedContext": "message-channel",
    "coreModules": ["template", "variable", "dispatch", "channel", "inbox"],
    "layers": ["controller", "dto", "service", "repository", "config", "domain"],
    "publicBasePath": "/api/messages"
  }
}
```

## 为 Topbiz 预留的业务契约

- `POST /api/messages/send`
- `POST /api/messages/drafts`
- `GET /api/messages/{messageId}`
- `GET /api/messages/{messageId}/status`
- `GET /api/messages/templates`
- `GET /api/messages/templates/{templateId}`
- `POST /api/messages/templates/{templateId}/preview`

## 契约规则

- 所有响应统一使用 `ApiResponse`
- 服务自身持有消息领域主数据
- 重试、调度、收件箱语义由本服务内部维护
