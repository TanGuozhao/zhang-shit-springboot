# Spec: Topbiz Service Full Delivery

**Author:** Codex  
**Date:** 2026-06-07  
**Status:** Approved  
**Reviewers:** User (delegated autonomous delivery)  
**Related docs:** `docs/contracts/topbiz-service-contract.md`, `docs/contracts/user-service-contract.md`, `docs/contracts/message-service-contract.md`, `docs/contracts/log-service-contract.md`

## Context

`topbiz-service` is the platform's public gateway and orchestration service.
It is responsible for three things:

- external API exposure for lower-layer capabilities
- authentication and authorization enforcement
- cross-service orchestration and operational aggregation

It does **not** own business master data.
User master data belongs to `user-service`, message master data belongs to `message-service`,
and log master data belongs to `log-service`.

The source notes and design intent are strong, but they leave several delivery gaps:

- gateway boundaries were not yet frozen
- topbiz permissions were not fully aligned with `user-service`
- orchestration behavior was not yet backed by real execution records
- login/runtime behavior needed a concrete Shiro + session implementation path

This delivery closes those gaps with a formal topbiz delivery baseline that is already usable as the platform gateway.

## Corrected Design Decisions

- `topbiz-service` is the only public entry for cross-service orchestration.
- Lower-layer services are not expected to call each other directly through business APIs.
- `topbiz-service` uses `OpenFeign` for service-to-service integration.
- Authentication is performed through `user-service`, then projected into a `TopbizPrincipal`.
- Authorization is enforced in `topbiz-service` with `Shiro` permission annotations.
- Current runtime session implementation is `servlet session bridge + Shiro subject binding` for local mode.
  The `prod` profile switches session persistence to `Redis Session`.
- Distributed-transaction delivery is currently implemented as `Saga-like compensation at orchestration layer`,
  not XA/2PC. This is the safer fit for the current architecture.
- Orchestration execution persistence is abstracted behind repository interfaces.
  Local mode uses in-memory storage and production mode uses durable JDBC persistence.

## Architecture Decisions

### Service Role

- `topbiz-service` owns:
  - external gateway surface
  - login state projection
  - authorization enforcement
  - platform overview aggregation
  - cross-service orchestration
  - cross-service audit/log trace propagation
- `topbiz-service` does not own:
  - user accounts and RBAC master data
  - message templates, tasks, and message records as master data
  - access-log master storage and alert-rule master data

### Layering

- `controller`: external API boundary
- `dto`: stable request/response contracts
- `service`: gateway logic, orchestration logic, aggregation logic
- `security`: Shiro realm, session filter, permission constants, principal model
- `remote`: Feign clients for bottom services
- `repository`: orchestration execution persistence abstraction
- `domain`: orchestration execution and step records
- `config`: Feign/Shiro/properties wiring

### Call Flow

1. Client calls `topbiz-service`
2. `topbiz-service` authenticates or restores `TopbizPrincipal`
3. `Shiro` validates required permissions
4. Gateway service forwards request to bottom service through `OpenFeign`
5. `X-User-Id`, `X-Session-Key`, trace headers are propagated
6. For orchestration flows, topbiz coordinates multiple remote calls and compensation logic

## Functional Requirements

- FR-1: `topbiz-service` MUST expose a unified public base path `/api/topbiz`.
- FR-2: `topbiz-service` MUST support login, logout, and current-session query.
- FR-3: `topbiz-service` MUST authenticate through `user-service`.
- FR-4: `topbiz-service` MUST enforce authorization with explicit permission strings.
- FR-5: `topbiz-service` MUST proxy the frozen `user-service` public and admin contracts.
- FR-6: `topbiz-service` MUST proxy the frozen `message-service` public and admin contracts.
- FR-7: `topbiz-service` MUST proxy the frozen `log-service` public and admin contracts.
- FR-8: `topbiz-service` MUST provide platform overview aggregation.
- FR-9: `topbiz-service` MUST provide platform architecture aggregation.
- FR-10: `topbiz-service` MUST provide runtime aggregation for message/log/topbiz metrics.
- FR-11: `topbiz-service` MUST provide a user-provisioning orchestration flow.
- FR-12: `topbiz-service` MUST provide a department-transfer orchestration flow.
- FR-13: `topbiz-service` MUST provide a message-audit orchestration flow.
- FR-14: `topbiz-service` MUST persist orchestration execution records through repository abstraction.
- FR-15: `topbiz-service` MUST emit audit logs for orchestration attempts.
- FR-16: `topbiz-service` MUST propagate user identity context to lower services.

## Non-Functional Requirements

- NFR-1: All APIs MUST return `ApiResponse<T>`.
- NFR-2: Public controllers MUST remain stable for frontend and team parallel work.
- NFR-3: Permission constants MUST be centralized.
- NFR-4: Remote error handling MUST normalize downstream failures into business exceptions.
- NFR-5: Tests MUST cover login, unauthorized access, forbidden access, and orchestration happy path.
- NFR-6: Topbiz must remain stateless with respect to business master data.

## Delivered Now

### Authentication and Authorization

- Shiro realm backed by `user-service` login and permission query
- `TopbizPrincipal` session projection
- `TopbizSessionFilter` request-level authenticated subject binding
- permission constants for platform, orchestration, message admin, log admin, runtime operations
- dedicated security exception handler for `401/403` JSON responses

### Gateway Surface

- user self-service gateway
- user admin / role admin / department admin gateway
- message use APIs
- message admin and runtime APIs
- log query, admin, and runtime APIs
- platform overview, architecture, and runtime APIs

### Orchestration

- user provisioning orchestration
  - create user
  - optional welcome message
  - compensation by disabling created user on downstream failure
  - audit log write
- department transfer orchestration
  - transfer members
  - optional notification
  - compensation by reverse transfer on downstream failure
  - audit log write
- message audit orchestration
  - send message
  - ingest audit log
  - partial-success status when audit ingestion fails

### Aggregation

- current session + current user + current department overview
- architecture view for topbiz/user/message/log
- runtime view for metrics, alerts, message runtime, and log runtime
- recent orchestration list in quick view

### Testing

Current tests now cover:

- unauthenticated access returns `401`
- login creates usable topbiz session
- insufficient permission returns `403`
- admin can access platform overview
- user provisioning orchestration executes and persists execution record

## Required Permission Model

Topbiz currently depends on the following permission strings:

- `topbiz:admin`
- `topbiz:platform:read`
- `topbiz:architecture:read`
- `topbiz:orchestration:write`
- `topbiz:message:admin`
- `topbiz:log:admin`
- `topbiz:runtime:operate`

These permissions are now seeded in `user-service`, and `ADMIN` / `OPS_ADMIN` role defaults were aligned accordingly.

## Known Gaps For Next Stage

- add idempotency and replay protection for orchestration submission
- add audit search/report screens on topbiz side if frontend requires them
- add more orchestration flows once team research items are complete

## Next Hardening Direction

- continue Redis Session hardening and Shiro policy refinement
- deepen Feign resilience, timeout policy, and observability
- extend orchestration coverage only on top of the current frozen gateway contracts
