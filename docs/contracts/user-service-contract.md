# User Service Contract

## Purpose

`user-service` owns identity and access data. It provides user, role, permission, department, and account lifecycle capabilities for `topbiz`.

## Public base path

`/api/users`

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
    "service": "user-service",
    "boundedContext": "identity-and-access",
    "coreModules": ["user", "role", "permission", "department", "account"],
    "layers": ["controller", "dto", "service", "repository", "config", "domain"],
    "publicBasePath": "/api/users"
  }
}
```

## Reserved business contracts for topbiz

- `POST /api/users/auth/login`
- `POST /api/users/auth/logout`
- `GET /api/users/me`
- `GET /api/users/{userId}`
- `GET /api/users/{userId}/permissions`
- `GET /api/users/{userId}/roles`
- `GET /api/users/departments/{deptId}`

## Contract rules

- All responses use `ApiResponse`
- Service owns user-related master data
- `topbiz` may orchestrate, but must not bypass this service's API
