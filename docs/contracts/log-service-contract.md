# Log Service Contract

## Purpose

`log-service` owns access log ingestion, search, metrics, alerting, and export capabilities.

## Public base path

`/api/logs`

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
    "service": "log-service",
    "boundedContext": "access-log-and-alerting",
    "coreModules": ["ingest", "search", "metrics", "alert", "export"],
    "layers": ["controller", "dto", "service", "repository", "config", "domain"],
    "publicBasePath": "/api/logs"
  }
}
```

## Reserved business contracts for topbiz

- `POST /api/logs/ingest`
- `GET /api/logs/search`
- `GET /api/logs/trace/{traceId}`
- `GET /api/logs/metrics`
- `GET /api/logs/alerts`
- `POST /api/logs/alerts/{alertId}/status`
- `POST /api/logs/exports`

## Contract rules

- All responses use `ApiResponse`
- Service owns log-related master data and derived analytics data
- Export and alert processing may be asynchronous but contract shape stays stable
