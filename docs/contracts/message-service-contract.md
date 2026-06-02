# Message Service Contract

## Purpose

`message-service` owns message template, variable, dispatch, channel, retry, and inbox capabilities.

## Public base path

`/api/messages`

## Frozen initial endpoints

### 1. Query architecture overview

- Method: `GET`
- Path: `/internal/architecture/overview`
- Response:

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

## Reserved business contracts for topbiz

- `POST /api/messages/send`
- `POST /api/messages/drafts`
- `GET /api/messages/{messageId}`
- `GET /api/messages/{messageId}/status`
- `GET /api/messages/templates`
- `GET /api/messages/templates/{templateId}`
- `POST /api/messages/templates/{templateId}/preview`

## Contract rules

- All responses use `ApiResponse`
- Service owns message-related master data
- Retry, scheduling, and inbox semantics are internal to this service
