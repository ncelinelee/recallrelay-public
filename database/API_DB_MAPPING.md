# API → Database mapping

This is the practical path from the existing OpenAPI contract to the MariaDB schema.

| API | Main tables | Important rule |
|---|---|---|
| POST /auth/login | organizations, employees | Organization Code + employee_login_id + BCrypt password; active only |
| GET /dashboard | recall_cases, case_parts, appointments, case_activities, dealership_campaigns | Apply JWT organization/dealership scope first; calculate nextAction |
| GET /search | customers, vehicles, recall_campaigns, case_parts, recall_cases | Search only inside authenticated organization/dealership scope |
| POST /customers | customers, vehicles, recall_targets, recall_cases | VIN match creates/links cases; no direct POST /cases |
| POST/GET/PATCH /campaigns | recall_campaigns, dealership_campaigns | New campaign creation = Head Office Admin only; Service Manager can manage existing campaigns within assigned scope |
| GET/PATCH /cases/{id} | recall_cases + joined case tables | Case status system-managed; customer requested service date is a structured demand signal; eligibility/admin fields remain role-scoped |
| POST/PATCH /parts | case_parts | Parts Specialist or Service Manager; ARRIVED triggers recalculation |
| POST/PATCH /appointments | appointments | Coordinator / Advisor / Service Manager; UNIQUE recall_case_id; second POST = 409 |
| GET /appointments/availability | appointments, employees | technician_id + scheduled_at index |
| GET/POST /cases/{id}/activities | case_activities | Contact/memo is append-only timeline; callback completion links to source activity |

## Derived response values
The service layer calculates these after loading the scoped case data:

1. `recallOutreachEligibility`
   - `ownership_valid && service_area_valid && phone_valid`
2. `partReadiness`
   - linked parts >= 1 and every status = `ARRIVED` → `READY`
   - otherwise `NOT_READY`
3. `supplyAlignment`
   - requested service date + part ETA/status are compared deterministically
   - `ARRIVED` -> `ALIGNED`
   - ETA unavailable or ETA later than requested date -> `AT_RISK`
   - ETA on/before requested date but part not physically received -> `WATCH`
4. `nextAction`
   - `REVIEW_APPOINTMENT`
   - `CALLBACK_CUSTOMER`
   - `CONTACT_CUSTOMER`
   - `CHECK_PART_STATUS`
   - return only the highest-priority matching action

## Backend implementation order
Recommended order for a first working vertical slice:

1. Auth/JWT + organization/dealership scope
2. GET `/cases/{caseId}`
3. GET `/search`
4. GET `/dashboard`
5. Part PATCH
6. Appointment POST/PATCH
7. Activity POST
8. Campaign/Employee admin endpoints

This order makes the Vue prototype become real incrementally instead of trying to implement all 33 operations at once.

## Employee Management v0.2

- `GET /employees` -> `employees` + `dealerships`, scoped by JWT organization/dealership.
- `PATCH /employees/{employeeId}` -> `employee_name`, `company_email`, `role`, `dealership_id`, `active`.
- Account removal is not exposed as a normal workflow. Use `PATCH /employees/{employeeId}` with `active=false` to deactivate while preserving history.
- Service Manager is the dealership-level superuser within the assigned dealership; historical foreign-key references remain intact.
- Service Manager cannot create or assign a Head Office Admin account.
