# 日志服务契约

## 目的

`log-service` 持有访问日志接入、检索、指标、告警与导出能力。

## 对外基础路径

`/api/logs`

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
    "service": "log-service",
    "boundedContext": "access-log-and-alerting",
    "coreModules": ["ingest", "search", "metrics", "alert", "export"],
    "layers": ["controller", "dto", "service", "repository", "config", "domain"],
    "publicBasePath": "/api/logs"
  }
}
```

## 为 Topbiz 预留的业务契约

- `POST /api/logs/ingest`
- `GET /api/logs/search`
- `GET /api/logs/trace/{traceId}`
- `GET /api/logs/metrics`
- `GET /api/logs/alerts`
- `POST /api/logs/alerts/{alertId}/status`
- `POST /api/logs/exports`

## 契约规则

- 所有响应统一使用 `ApiResponse`
- 服务自身持有日志主数据与衍生分析数据
- 导出与告警处理允许异步执行，但契约形态必须保持稳定
