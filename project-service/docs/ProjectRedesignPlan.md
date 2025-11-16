# Project Service Redesign Plan

Derived from **“Void Squard – EAD project Users Flow”**, this plan captures the target behaviour for the revamped project-service before we start touching code. The immediate goals are:

1. Align the data model with the documented user journeys (customers raise modification requests, admins approve/assign, employees execute tasks and log time).
2. Remove legacy concepts (quotes, change requests, outbox) that are not part of the flow.
3. Re-open API surface per role (customer, employee, admin) while keeping a clear path for future integrations (appointments, time logging, notifications).

---

## 1. Core Concepts

| Concept | Description | Notes |
| --- | --- | --- |
| **Project** | Represents a customer’s modification/service request (optionally tied to an appointment). | Primary entity Admins manage. Contains metadata: title/description, vehicleId, customerId, requested dates, status. |
| **Task** | Work items under a project. Maps 1:1 to appointment entries or admin-assigned steps. | Stores serviceType, scheduled window, status, assigned employee, optional appointmentId. |
| **Activity Feed / Status History** | Audit log for project + task transitions (pending → approved → completed). | Replaces old change-request history. |
| **Appointment Intake Placeholder** | Stores external `appointmentId` and raw payload for future syncing. | No external calls yet; just fields + API contract. |
| **Time Logging Integration Point** | Placeholder references to time entries per task. | No implementation until time-logging API is ready. |

---

## 2. Roles & Permissions

| Role | Allowed Actions (per flow doc) |
| --- | --- |
| **Customer** | Submit new projects, view own projects/tasks, cancel requests before approval, view status, add notes. |
| **Employee** | View assigned tasks, update task status (Accepted → In Progress → Completed), log time (future hook). |
| **Admin** | View/approve all appointments/requests, create/edit projects, assign employees, update status, view reports/dashboards. |

Implementation implications:
- `POST /api/projects` re-opened to customers (with validation).
- Task endpoints exposed to employees (guarded so they only see their entries).
- Admin-only endpoints for global listings, approvals, and any destructive actions.

---

## 3. API Surface (Target)

### Customer-facing
- `POST /api/projects` – submit modification/service request (title, description, vehicleId, desired times, optional appointmentId).
- `GET /api/projects/mine` – list customer’s projects with tasks & status.
- `GET /api/projects/{id}` – view project details (with tasks/history).
- `PATCH /api/projects/{id}` – allow cancellation or note updates while Pending.

### Employee-facing
- `GET /api/tasks/assigned` – tasks for logged-in employee.
- `PATCH /api/tasks/{taskId}/status` – update Accepted/In Progress/Completed.
- `POST /api/tasks/{taskId}/notes` – add progress notes/photos (future S3 hook).
- **Placeholder**: `POST /api/tasks/{taskId}/time-entries` once time-logging API available.

### Admin-facing
- `GET /api/admin/appointments` – placeholder endpoint returning stub data until booking-service integration exists.
- `POST /api/admin/projects/{id}/approve` – mark project Approved + assign employees/dates.
- `GET /api/admin/projects` – paged filterable listing of all projects.
- `PATCH /api/admin/projects/{id}` – update budget/dates/status.
- `POST /api/admin/projects/{id}/tasks` – add tasks manually (non-appointment work).
- `GET /api/admin/dashboard` – future analytics stub.

### Integration placeholders
- `POST /api/admin/appointments/import` – accept payload from appointment service (stores appointment reference + auto-creates project/task). For now this can just accept JSON and persist it for later wiring.

---

## 4. Data Model Changes

1. **Remove**: `Quote`, `ChangeRequest`, `OutboxMessage`, related migrations/controllers.
2. **Project entity**:
   - Fields: `ProjectId (Guid)`, `CustomerId (long)`, `VehicleId (Guid)`, `Title`, `Description`, `Status (Pending/Approved/InProgress/Completed/Cancelled)`, `RequestedStart/End`, `ApprovedStart/End`, `AppointmentId (Guid?)`, `CreatedBy`, `CreatedAt`, `UpdatedAt`.
3. **Task entity**:
   - Fields: `TaskId (Guid)`, `ProjectId`, `ServiceType`, `Detail`, `EstimateHours?` (optional; we can drop if not needed), `AssigneeId (long?)`, `Status (Requested/Accepted/InProgress/Completed/Cancelled)`, `ScheduledStart/End`, `AppointmentId?`, `CreatedAt`, `UpdatedAt`.
4. **StatusHistory / ActivityLog**:
   - Generic table referencing `ProjectId` or `TaskId`, storing actor + role + message.
5. **Attachments / Notes**: add simple table or JSON column for now (optional; can defer).

---

## 5. Authorization & Policies

- Restore role awareness (`customer`, `employee`, `admin`) using original `RequireRole` checks.
- Add helper methods to ensure employees only see their tasks, customers only see their projects, admins bypass restrictions.
- Remove the blanket `AdminOnly` fallback policy.

---

## 6. Migration Strategy

1. Create a brand new migration that:
   - Drops obsolete tables (Quotes, ChangeRequests, Outbox, etc.).
   - Alters `Projects` and `Tasks` to new schemas (might be easier to drop/recreate in dev).
   - Adds new tables for activity log / appointment intake.
2. Because this is a breaking change, we’ll document that existing data will be lost unless migrated externally (acceptable for now).

---

## 7. Implementation Phases

1. **Phase 1 (this doc)** – define domain & API (done).
2. **Phase 2** – implement domain/migrations/entities + DTOs aligning with above.
3. **Phase 3** – rewrite controllers/services/validators/tests, reconfigure auth policies, add stub endpoints for future appointment intake.
4. **Phase 4 (future)** – wire actual integrations (appointment events, time logging, notifications).

This plan will guide the upcoming refactor so each phase is deliberate and testable. Let’s proceed to Phase 2 once this overview is approved.
