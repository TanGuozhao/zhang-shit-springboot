# Spec: User Service Full Delivery

**Author:** Codex  
**Date:** 2026-06-07  
**Status:** Approved  
**Reviewers:** User (delegated autonomous delivery)  
**Related specs:** `docs/contracts/user-service-contract.md`, `可复用用户管理服务设计-2024091602016-谭国照.md`

## Context

`user-service` is the identity and access master-data service for the platform.
It is the bottom-layer owner of users, roles, permissions, departments, and department membership.
`topbiz-service` orchestrates and exposes external APIs, but does not own identity master data.

The source design document is directionally strong, but it contains several contradictions and unsafe details.
This delivery corrects those issues and lands a complete repository-stage service baseline:

- stable public contract under `/api/users`
- scoped admin and department-admin access control
- verification-code-based register/reset/unfreeze flows
- RBAC and department-management delivery
- organization tree, department attributes, and member attribute operations
- clean test coverage for the main lifecycle and admin flows

The current repository stage still uses in-memory repositories, but the API boundary, service layering,
permission model, and domain split are now strong enough for the team to continue on DB/Redis/Shiro integration
without rebreaking contracts.

## Source Document Corrections

The following source-document issues were corrected during implementation:

- `RABC` was corrected to `RBAC`.
- "Frontend MD5 then transmit" was rejected.
  Password hashing is now server-side with salted `PBKDF2WithHmacSHA256`.
- The document mixes `/user/**`, `/admin/user/**`, and `/org/**`.
  The delivered contract is unified under `/api/users`.
- The document both says "admin has no delete permission" and later defines user delete APIs.
  Delivered implementation keeps the safer earlier rule: no user delete API.
- The document later recommends idempotent resource-style APIs.
  The delivered implementation follows that guidance where practical.
- Department attribute-definition deletion was landed as cascade cleanup of attribute values,
  because that matches the later interface-design section better than a strict reject-on-use rule.

## Architecture Decisions

### Service Boundary

- `user-service` owns:
  - user accounts
  - account status
  - login sessions contract
  - roles
  - permission catalog
  - department structure
  - department-member relationship
  - department attribute definitions and values
  - member attribute values
- `topbiz-service` owns:
  - orchestration
  - external aggregation
  - authentication/authorization gateway behavior
  - Shiro session and cross-service permission use
- `message-service` and `log-service` do not own identity master data.

### Layering

- `controller`: transport boundary only
- `dto`: request/response contract
- `service`: business orchestration and access checks
- `repository`: in-memory persistence abstraction, ready for DB replacement
- `domain`: master-data models

### Access-Control Model

- `ADMIN`: full scope
- `DEPARTMENT_ADMIN`: limited to own department subtree
- self-read/self-write flows bypass admin requirements but remain identity-bound
- frozen topbiz-compatible query paths are no longer anonymous

### Security Baseline

- passwords use salted `PBKDF2WithHmacSHA256`
- verification-code scenes are explicit and isolated:
  - `REGISTER`
  - `FORGOT_PASSWORD`
  - `UNFREEZE`
- repeated password failure triggers automatic freeze
- session invalidation happens on password change/reset and status changes

## Functional Requirements

- FR-1: The service MUST keep a unified public base path `/api/users`.
- FR-2: The service MUST support verification-code-based registration.
- FR-3: The service MUST support password login and a stable third-party-login request contract.
- FR-4: The service MUST support logout and server-side session invalidation.
- FR-5: The service MUST support self profile query/update and modification-record query.
- FR-6: The service MUST support known-password change and forgot-password reset.
- FR-7: The service MUST support account status query, auto-freeze on repeated failure, unfreeze, and cancel apply.
- FR-8: The service MUST support admin user create/update/status/authorization/password-reset operations.
- FR-9: The service MUST support RBAC role management and permission allocation.
- FR-10: The service MUST support department CRUD with subtree-safe parent checks.
- FR-11: The service MUST support organization tree query.
- FR-12: The service MUST support department attribute-definition CRUD and department attribute-value update.
- FR-13: The service MUST support department member add/remove/transfer/query operations.
- FR-14: The service MUST support per-member department attribute update and batch update.
- FR-15: The service MUST enforce scoped access for `DEPARTMENT_ADMIN`.
- FR-16: Frozen query paths used by `topbiz-service` MUST remain stable.

## Non-Functional Requirements

- NFR-1: All APIs MUST return `ApiResponse<T>`.
- NFR-2: The service MUST start and pass tests without external infrastructure.
- NFR-3: The current repository-stage implementation MUST preserve contract stability for `topbiz-service`.
- NFR-4: Access checks MUST be centralized in service-layer support utilities, not scattered across controllers.
- NFR-5: Domain and DTO split MUST remain stable so teammates can parallelize DB, Redis, and topbiz integration.
- NFR-6: Password storage MUST avoid unsalted one-way digests.

## Delivered Now

### Authentication and Self-Service

- verification-code send API
- register API
- password login
- third-party login request shape
- logout
- self profile update
- profile modification records
- password change
- forgot-password code send and reset
- self status query
- unfreeze flow
- cancel apply flow
- login freeze threshold

### Admin and RBAC

- user list/create/update/status/authorization/password-reset
- role list/create/update/delete
- permission catalog query
- effective permission refresh

### Department and Organization

- department list/create/update/delete
- subtree-safe parent validation
- organization tree query
- department-user list query
- department attribute-definition list/create/delete
- department attribute update
- member add/remove/transfer
- user membership query
- member attribute update
- member attribute batch update

### Testing

`user-service` now passes:

```powershell
.\mvnw.cmd -pl user-service -am clean test
```

Covered regression flows include:

- login and session key
- self resolution by header
- department query with identity context
- admin create/update/status/authorization flows
- verification-code-based registration
- self profile + password + forgot-password lifecycle
- repeated-login freeze and unfreeze
- role and department admin flows
- organization tree, department attributes, and membership operations

## Acceptance Mapping to Source Document

### Source 3.2 User Use Cases

- 3.2.1 Register: implemented
- 3.2.2 Login/Logout: implemented as password login plus third-party login contract
- 3.2.3 Personal information management: implemented
- 3.2.4 Password management: implemented
- 3.2.5 Account status management: implemented

### Source 3.3 Admin Use Cases

- 3.3.1 User management: implemented except delete
- 3.3.2 RBAC: implemented as role/permission management baseline
- 3.3.3 Department management: implemented including tree, attribute definition/value, membership flows

### Source 4 Data Model Direction

Domain abstractions now exist for:

- `UserAccount`
- `RoleDefinition`
- `PermissionDefinition`
- `Department`
- `DepartmentAttributeDefinition`
- `VerificationCode`
- `LoginAttemptState`
- `UserSession`
- `UserProfileModificationRecord`

## Current Gaps vs Full Production Target

These are known remaining items, but they no longer block parallel development:

- real database persistence and migration scripts
- Redis-backed session storage
- real SMS/email verification delivery
- audit/event emission to `log-service`
- full topbiz Shiro integration against persistent identity data
- distributed transaction orchestration in `topbiz-service`
- richer third-party identity binding model
- password-policy configuration center and risk rules

## Implementation Rules

- do not change frozen API paths under `docs/contracts/user-service-contract.md`
- do not add direct calls from `message-service` or `log-service` into identity internals
- do not move orchestration logic into `user-service`
- do not reintroduce frontend-side MD5 assumptions
- do not weaken password hashing back to plain digest storage

## Immediate Next Step

The codebase is now ready for the next production-hardening stage.
The safest implementation order is:

1. DB schema and repository replacement in `user-service`
2. Redis Session and Shiro bridge refinement in `topbiz-service`
3. Audit/event integration with `log-service`
4. Optional third-party identity binding expansion
