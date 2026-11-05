# RecallRelay Backend v0.3 — Auth, Registration & Approval

## Implemented account lifecycle
- `POST /auth/login`
- `PATCH /auth/password`
- `POST /auth/password-reset/request`
- `POST /auth/password-reset/confirm`
- `POST /auth/registrations/employee/start`
- `POST /auth/registrations/employee/verify`
- `POST /auth/registrations/manager`
- `GET /registrations/pending`
- `POST /registrations/{id}/approve`
- `POST /registrations/{id}/reject`
- Existing Employee Management APIs

## Employee self-registration
The request is not converted to an employee account immediately.

```text
Registration form
→ Organization/company-domain validation
→ 6-digit Company Email verification
→ PENDING_MANAGER_APPROVAL
→ manager approves
→ employees row created
→ login available
```

Service Manager approves requests in the assigned dealership. Head Office Manager approves Head Office staff. Head Office Admin may review organization-wide requests.

## Manager registration
```text
Manager registration
→ manager type
→ Employee ID
→ company-issued one-time code
→ dealership check for Service Manager
→ code hash/expiry/unused check
→ account auto-created
```
Head Office Admin is not self-registerable.

## Password reset
The request requires Organization Code + the Company Email stored on the employee record.
The API always returns a generic message. When the employee exists and is active, a random one-time token is generated, only its SHA-256 hash is stored, and a 30-minute link is emailed.

## Email configuration
Actual email delivery requires SMTP configuration:
```text
MAIL_ENABLED=true
MAIL_HOST=...
MAIL_PORT=...
MAIL_USERNAME=...
MAIL_PASSWORD=...
MAIL_SMTP_AUTH=true
MAIL_STARTTLS_ENABLE=true
MAIL_FROM=...
FRONTEND_BASE_URL=https://recallrelay.vercel.app
MAIL_DEV_EXPOSE_SECRETS=false
```
For local development you can leave `MAIL_ENABLED=false`. If `MAIL_DEV_EXPOSE_SECRETS=true`, the API may expose a test verification code/reset URL; never enable that in production.

## Database upgrade
Run:
```text
database/migrations/V003__self_registration_password_reset.sql
```
against an existing v1.4 database before starting this backend.

## Demo logins
Seeded accounts are disabled with no usable password. Configure a locally generated BCrypt hash and enable only the required account using [local DB setup](../database/README_DB.md).
- `hadmin` — Head Office Admin
- `homgr` — Head Office Manager
- `jlee` — Service Manager
- `maria` — Appointment Coordinator
- `parts01` — Parts Specialist
- `advisor01` — Service Advisor
- `tech01`, `tech02` — Technician

Organization Code: `OHC-001-001`.
