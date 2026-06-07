# User Service Contract

## Purpose

`user-service` owns identity and access master data for the platform.
It provides user lifecycle, authentication support, RBAC, department structure,
and self-service account capabilities for `topbiz-service`.

## Corrected Design Decisions

- Public API base path is unified as `/api/users`, not mixed `/user/**`, `/admin/user/**`, and `/org/**`.
- `RBAC` is the correct model name. The source design document uses `RABC` in one section by mistake.
- Passwords are hashed on the server side with salted `PBKDF2WithHmacSHA256`.
  The source document's "frontend MD5 first" requirement was intentionally rejected.
- User deletion is not exposed. The source document has conflicting sections; the earlier and safer rule is kept:
  admins may create, update, disable, freeze, and reset, but not delete users.
- Department attribute-definition deletion is implemented as cascade cleanup of department/member values,
  matching the later API-design section better than a strict "in use cannot delete" rule.
- Query paths already frozen for `topbiz-service` remain stable, but now require identity context.

## Shared Rules

- All responses use `ApiResponse<T>`.
- The current user is resolved by `X-Session-Key` first, then `X-User-Id`.
- Frozen query paths are still available for `topbiz-service`, but they are not anonymous APIs.
- Seed accounts:
  - `admin / admin123`
  - `operator / operator123`
- Seed roles:
  - `ADMIN`
  - `OPERATOR`
  - `USER`
  - `AUDITOR`
  - `OPS_ADMIN`
  - `DEPARTMENT_ADMIN`
- Demo verification code is `123456`.
- Supported verification scenes:
  - `REGISTER`
  - `FORGOT_PASSWORD`
  - `UNFREEZE`
- Department-admin scope:
  - `ADMIN` can access all departments and users.
  - `DEPARTMENT_ADMIN` is restricted to its own department subtree.

## Internal Endpoint

### Architecture Overview

- Method: `GET`
- Path: `/internal/architecture/overview`

## Auth APIs

### Send Verification Code

- Method: `POST`
- Path: `/api/users/auth/verify-codes`

### Register

- Method: `POST`
- Path: `/api/users/auth/register`

### Login

- Method: `POST`
- Path: `/api/users/auth/login`
- Notes:
  - Supports `loginType=password|thirdParty`
  - Third-party login requires `thirdPartyInfo`
  - Returns `sessionKey` and `expireTime`
  - Repeated password failures trigger automatic freeze by threshold

### Logout

- Method: `POST`
- Path: `/api/users/auth/logout`

## Frozen Topbiz-Compatible Query APIs

- `GET /api/users/me`
- `GET /api/users/{userId}`
- `GET /api/users/{userId}/permissions`
- `GET /api/users/{userId}/roles`
- `GET /api/users/departments/{deptId}`

## Self-Service APIs

### Update Current Profile

- Method: `PUT`
- Path: `/api/users/me`

### Query Profile Modification Records

- Method: `GET`
- Path: `/api/users/me/modify-records`

### Change Password

- Method: `PUT`
- Path: `/api/users/me/password`

### Send Forgot-Password Code

- Method: `POST`
- Path: `/api/users/password/forgot/send-code`

### Reset Forgotten Password

- Method: `POST`
- Path: `/api/users/password/forgot/reset`

### Query Current Account Status

- Method: `GET`
- Path: `/api/users/me/status`

### Apply Unfreeze

- Method: `POST`
- Path: `/api/users/me/status/unfreeze`
- Notes:
  - Logged-in users may unfreeze their own account
  - Logged-out flow is supported by `account + contact + verifyCode`

### Apply Cancel

- Method: `POST`
- Path: `/api/users/me/status/cancel`

## Admin User APIs

### List Users

- Method: `GET`
- Path: `/api/users/admin`

### Create User

- Method: `POST`
- Path: `/api/users/admin`

### Update User

- Method: `PUT`
- Path: `/api/users/admin/{userId}`

### Update User Status

- Method: `PATCH`
- Path: `/api/users/admin/{userId}/status`

### Update User Roles and Direct Permissions

- Method: `PATCH`
- Path: `/api/users/admin/{userId}/authorization`

### Refresh Effective Permissions

- Method: `POST`
- Path: `/api/users/admin/{userId}/authorization/permissions:refresh`

### Reset User Password

- Method: `POST`
- Path: `/api/users/admin/{userId}/password:reset`

## Admin Role APIs

### List Roles

- Method: `GET`
- Path: `/api/users/admin/roles`

### List Permission Catalog

- Method: `GET`
- Path: `/api/users/admin/permissions`

### Create Role

- Method: `POST`
- Path: `/api/users/admin/roles`

### Update Role

- Method: `PUT`
- Path: `/api/users/admin/roles/{roleId}`

### Set Role Permissions

- Method: `PUT`
- Path: `/api/users/admin/roles/{roleId}/permissions`

### Delete Role

- Method: `DELETE`
- Path: `/api/users/admin/roles/{roleId}`

## Admin Department APIs

### List Departments

- Method: `GET`
- Path: `/api/users/admin/departments`

### Get Organization Tree

- Method: `GET`
- Path: `/api/users/admin/departments/tree`

### Create Department

- Method: `POST`
- Path: `/api/users/admin/departments`

### Update Department

- Method: `PUT`
- Path: `/api/users/admin/departments/{departmentId}`

### Delete Department

- Method: `DELETE`
- Path: `/api/users/admin/departments/{departmentId}`

### List Department Users

- Method: `GET`
- Path: `/api/users/admin/departments/{departmentId}/users`

### List Department Attribute Definitions

- Method: `GET`
- Path: `/api/users/admin/departments/{departmentId}/attributes/definitions`

### Create Department Attribute Definition

- Method: `POST`
- Path: `/api/users/admin/departments/{departmentId}/attributes/definitions`

### Delete Department Attribute Definition

- Method: `DELETE`
- Path: `/api/users/admin/departments/{departmentId}/attributes/definitions/{attributeKey}`

### Update Department Attribute Values

- Method: `PUT`
- Path: `/api/users/admin/departments/{departmentId}/attributes`

### Add Department Members

- Method: `POST`
- Path: `/api/users/admin/departments/{departmentId}/members`

### Remove Department Member

- Method: `DELETE`
- Path: `/api/users/admin/departments/{departmentId}/members/{userId}`

### Transfer Members Between Departments

- Method: `POST`
- Path: `/api/users/admin/departments/transfer`

### Query User Membership

- Method: `GET`
- Path: `/api/users/admin/departments/users/{userId}/membership`

### Update Member Attribute Values

- Method: `PUT`
- Path: `/api/users/admin/departments/{departmentId}/members/{userId}/attributes`

### Batch Update Member Attribute Values

- Method: `POST`
- Path: `/api/users/admin/departments/{departmentId}/members/attributes:batch`

## Current Delivered Scope

Implemented now:

- Verification-code-based register flow
- Password login and third-party login request contract
- Login failure freeze and unfreeze flow
- Self profile update, profile modification records, password change, forgot-password reset
- User status query and cancel apply
- Admin user create/update/status/authorization/password-reset
- Role catalog, role CRUD, permission allocation
- Department CRUD, organization tree, department attribute definitions/values
- Department member add/remove/transfer/membership/member attributes/batch updates
- Scoped access checks for admin and department-admin
- Frozen topbiz query paths kept compatible

Still intentionally in-memory for this repository stage:

- User/session persistence
- Real Redis Session backing
- SMS/email delivery
- Audit/event emission to `log-service`
- Topbiz-side distributed transaction orchestration
- Real Shiro-driven permission persistence model

## Ownership Rules

- `user-service` owns user, role, permission, department, and membership master data.
- `topbiz-service` may orchestrate and expose external APIs, but must not bypass this contract.
- Other second-layer services must not implement private shortcuts around user/permission master data.
