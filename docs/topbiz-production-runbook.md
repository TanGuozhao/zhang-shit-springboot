# Topbiz Production Runbook

## Runtime Modes

- Default mode keeps `topbiz-service` easy to run locally:
  - orchestration repository: `memory`
  - session store: servlet session
- Production mode is enabled with `--spring.profiles.active=prod`:
  - orchestration repository: `jdbc`
  - session store: `Redis Session`

## Required Production Environment Variables

### Redis Session

- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT` default `6379`
- `SPRING_DATA_REDIS_USERNAME` optional
- `SPRING_DATA_REDIS_PASSWORD` optional
- `SPRING_DATA_REDIS_DATABASE` default `0`
- `SPRING_DATA_REDIS_TIMEOUT` default `3s`
- `SPRING_DATA_REDIS_SSL_ENABLED` default `false`

### Orchestration JDBC Store

- `TOPBIZ_ORCH_JDBC_URL`
- `TOPBIZ_ORCH_JDBC_USERNAME`
- `TOPBIZ_ORCH_JDBC_PASSWORD`
- `TOPBIZ_ORCH_JDBC_DRIVER` default `com.mysql.cj.jdbc.Driver`
- `TOPBIZ_ORCH_JDBC_TABLE` default `topbiz_orchestration_execution`
- `TOPBIZ_ORCH_JDBC_INIT_SCHEMA` default `true`
- `TOPBIZ_ORCH_JDBC_POOL_SIZE` default `12`
- `TOPBIZ_ORCH_JDBC_TIMEOUT` default `3s`

## Start Commands

### Local

```powershell
.\mvnw.cmd -pl topbiz-service spring-boot:run
```

### Production Profile

```powershell
.\mvnw.cmd -pl topbiz-service spring-boot:run -Dspring-boot.run.profiles=prod
```

## Notes

- `application-prod.yml` keeps Redis Session behind the `prod` profile, so local development does not need Redis.
- `TopbizSessionFilter` now runs after the Spring Session filter, allowing Redis-backed sessions to be restored before Shiro authorization.
- JDBC orchestration storage auto-creates the execution table and two query indexes when schema initialization is enabled.
