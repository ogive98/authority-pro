# MVP Requirements — Platform Socle

**Decision status:** Foundation scope approved; detailed requirements below are
PROPOSED unless identified as an explicit approval or source requirement.
**Delivery status:** PLANNED / NOT IMPLEMENTED. Phase 0 is in progress; no
backend/frontend code, executed acceptance tests or release exists.
**Scope:** Normative documentation plus a runnable, business-neutral foundation.
No business module is authorized. Phase labels do not approve technical proposals.

## 1. Functional requirements

### Identity and sessions

| ID | Requirement | Decision status | Delivery status |
|---|---|---|---|
| FR-01 | Closed-default one-time bootstrap through an explicitly enabled operator CLI; atomic guard and durable audit; no public setup endpoint or browser setup form | PROPOSED | PLANNED / NOT IMPLEMENTED |
| FR-02 | Email/password login via Spring Security + Spring Session JDBC; same-origin HttpOnly session cookie, Secure outside localhost and CSRF protection; no custom bearer session tokens returned for browser storage | PROPOSED | PLANNED / NOT IMPLEMENTED |
| FR-03 | Logout revokes the server-side session and clears the browser session cookie; state-changing requests require CSRF protection | PROPOSED | PLANNED / NOT IMPLEMENTED |
| FR-04 | Configurable expiry and revocation; reject expired/revoked sessions and protect against session fixation | PROPOSED | PLANNED / NOT IMPLEMENTED |
| FR-05 | An authenticated current-user endpoint returns identity and authorized memberships; exact route is an API proposal | PROPOSED | PLANNED / NOT IMPLEMENTED |

### Tenancy and access

| ID | Requirement | Decision status | Delivery status |
|---|---|---|---|
| FR-06 | Tenant selection (candidate `X-Tenant-Id`) is validated against current user membership/authorization; only verified tenant/user context is set transaction-locally | PROPOSED contract/context design | PLANNED / NOT IMPLEMENTED |
| FR-07 | Explicit role/permission model with organization, company and site scoping; OWNER/ADMIN/MEMBER are candidate names, not an approved grant hierarchy | PROPOSED | PLANNED / NOT IMPLEMENTED |
| FR-08 | Shared PostgreSQL tenant tables carry `tenant_id` and RLS; proposed runtime is non-superuser, non-owner and NOBYPASSRLS, separate from migrations; no global bypass | APPROVED strategy; role/policy mechanics PROPOSED | PLANNED / NOT IMPLEMENTED |
| FR-09 | Support organizations with multiple companies and sites and users with differing company access; proposed mapping is tenant = organization, with explicit membership scope | Source requirement; mapping/details PROPOSED | PLANNED / NOT IMPLEMENTED |

Control-plane identity/platform administration does not grant tenant business-data
access. Tenant-plane reads and writes require verified context and authorization.
Platform ownership alone cannot bypass RLS or company/site access checks.

### Audit

| ID | Requirement | Decision status | Delivery status |
|---|---|---|---|
| FR-10 | Record security events including login, logout, bootstrap and rejection; sensitive writes atomically commit with durable audit or are rejected; anonymous failures use a separate bounded transaction, without secrets | PROPOSED durability/event policy; audit is a source requirement | PLANNED / NOT IMPLEMENTED |
| FR-11 | Authorized administrators read only permitted audit records, paginated and newest first; audit-read events and their failure policy require review | PROPOSED | PLANNED / NOT IMPLEMENTED |

### Platform runtime

| ID | Requirement | Decision status | Delivery status |
|---|---|---|---|
| FR-12 | Expose authorized platform name, version, modules and health information; `/api/v1/platform/info` is a candidate route | PROPOSED | PLANNED / NOT IMPLEMENTED |
| FR-13 | Provide liveness/readiness; Actuator and exact exposure policy are candidates | PROPOSED | PLANNED / NOT IMPLEMENTED |
| FR-14 | Provide OpenAPI documentation in development without leaking secrets or opening production diagnostics | Source contract requirement; exposure policy PROPOSED | PLANNED / NOT IMPLEMENTED |

### UI shell

| ID | Requirement | Decision status | Delivery status |
|---|---|---|---|
| FR-15 | Login, tenant display, sidebar navigation and authorized audit page; bootstrap stays operator-only | PROPOSED | PLANNED / NOT IMPLEMENTED |
| FR-16 | Support FR, EN, IT and AR with RTL; selectable language and preference persistence mechanisms to design | Four-language/RTL source requirement; rollout and mechanism PROPOSED | PLANNED / NOT IMPLEMENTED |
| FR-17 | Restore authenticated state after reload using server session validation; revalidate tenant selection and never persist a session credential in JavaScript-readable storage | PROPOSED | PLANNED / NOT IMPLEMENTED |

No FR/EN-only foundation has been approved. A staged language rollout requires an
explicit scope decision; the four-language requirement remains in force.

## 2. Explicit non-scope

No business module or feature belongs to this mission, including accounting, HR,
fleet, their tables/screens/rules or a reference business demonstrator. Retaining
source catalogue names does not authorize their implementation. Commercial
billing, subscriptions and business portals remain excluded.

Technical capabilities beyond L1 include expanded identity, durable execution,
extension tooling and generic platform services. The [coverage matrix](../PACK-INVENTORY.md#technical-coverage-matrix)
distinguishes L2 proposals from optional L3 candidates. Registration/invitations,
MFA, social login, password reset and service identities need explicit contracts;
MFA/account-recovery production requirements must be decided before opening the
service, not silently postponed. No technical row is implemented or automatically
authorized by being listed.

### Requirement-to-lot traceability

| Existing requirement | Proposed lot / coverage row | Acceptance reference |
|---|---|---|
| FR-01 | L04/L05; C01, C23 | Criteria 1–2 and 6; atomic audit must precede provisioning |
| FR-02–FR-05 | L05; C01, C02, C46 | Criteria 3, 6 and 8; actual session integration tested |
| FR-06–FR-09 | L03/L05; C02–C05, C43 | Criteria 1, 4–5 and 8; user authorization verified after identity integration |
| FR-10–FR-11 | L04/L05/L07; C23 | Criteria 6–8; failed-attempt stream distinct from protected transactions |
| FR-12 | L06/L07; C10, C13, C38 | Criterion 8 plus actual-component inventory and unauthorized access rejection |
| FR-13–FR-14 | L02/L08; C33, C44 | Criterion 8 plus restricted probes, safe filter/controller errors and schema checks |
| FR-15–FR-17 | L07; C27, C38, C53 | Criteria 7–8; browser tenant-switch, locale, RTL, theme and session-reload checks |

L01 establishes applicable decisions; L08 gathers and reruns integrated evidence.
The [lot definitions](../13-IMPLEMENTATION/IMPLEMENTATION-PHASES.md#eight-proposed-nucleus-lots)
and [gates](../13-IMPLEMENTATION/VALIDATION-GATES.md) include baseline operations,
recovery and supply-chain requirements beyond these functional FR identifiers.

## 3. Acceptance criteria (proposed, not executed)

1. A fresh database migrates using a separate migration role. Normal runtime
   uses restricted credentials and has no public setup route.
2. Bootstrap is disabled by default; explicit operator activation permits one
   atomic CLI initialization. Repeated and concurrent attempts are rejected;
   required durable-audit failure rolls back the initialization.
3. Login uses the protected cookie session, rotates session identity as needed,
   and requires CSRF protection. Missing/invalid CSRF is rejected on protected
   writes, login and logout; revoked/expired sessions no longer authenticate.
4. Tenant A cannot read or modify tenant B data under the actual restricted
   runtime role. Control-plane privileges do not bypass tenant-plane checks.
5. Missing/invalid tenant selection is denied, never used as a global context;
   exact error codes remain part of the proposed API contract. Pooled-connection
   tests prove context is transaction-local across success, error and rollback.
6. Sensitive writes and their durable audit commit together or neither commits.
   Anonymous failures use bounded separate audit transactions; audit storage
   failures remain observable and never change denial into success.
7. After CLI provisioning, the browser completes login, tenant selection and
   authorized audit navigation. FR/EN/IT/AR and RTL checks cover the shell.
8. Actual backend/frontend builds, architecture tests, security tests and CI
   results are linked for the tested revision. No criterion is passed solely
   because it appears in documentation.

## 4. Out of scope by design

The socle must not gain business tables or rules. Any business-module proposal
requires separate owner authorization; placement in a future phase is not consent.

## Related documents

- [MVP Boundaries](../13-IMPLEMENTATION/MVP-BOUNDARIES.md)
- [API Architecture](../02-ARCHITECTURE/API-ARCHITECTURE.md)
- [Identity and Access](../04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md)
- [Security Governance](../00-GOVERNANCE/SECURITY-GOVERNANCE.md)
