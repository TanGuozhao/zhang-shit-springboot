# log-service

`log-service` 是访问日志能力服务，负责日志接入、检索、指标、Trace、告警和导出。

## Boundary

- 负责访问日志写入与查询。
- 负责 Trace 维度检索、指标聚合、告警规则和导出任务。
- 负责日志脱敏、缓冲刷新和运行态可视化基础能力。
- 不负责统一 API 网关。
- 不负责用户主数据和消息主数据。
- 不直接承担跨服务编排。

## Start

```powershell
.\mvnw.cmd -pl log-service -am spring-boot:run
```

- 默认端口：`8083`

## Example Requests

写入日志：

```bash
curl -X POST http://localhost:8083/api/logs/ingest ^
  -H "Content-Type: application/json" ^
  -d "{\"traceId\":\"trace-demo-001\",\"serviceName\":\"topbiz-service\",\"path\":\"/api/topbiz/platform/overview\",\"method\":\"GET\",\"status\":200}"
```

查询日志：

```bash
curl "http://localhost:8083/api/logs/search?serviceName=topbiz-service"
```

查询运行态：

```bash
curl http://localhost:8083/api/logs/internal/runtime
```

## Placeholder And Replaceable Parts

- 默认实现偏向可演示和可测试，不是强依赖真实日志基础设施。
- 当前已预留内存存储与 ClickHouse 风格存储抽象，后续可落地真实日志库。
- 告警通知和导出任务是模板实现，后续可接企业 IM、邮件、对象存储。

## Collaboration Rule

- 日志字段、检索条件、导出格式属于契约层，不要随意破坏兼容性。
- 对外公开能力优先经由 `topbiz-service` 暴露。
