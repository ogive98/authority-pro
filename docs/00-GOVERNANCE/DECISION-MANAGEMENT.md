# Decision Management

**Decision status:** PROPOSED process details; locked blueprint principles and
explicit approvals are recorded separately below.
**Delivery status:** PLANNED / NOT IMPLEMENTED. Phase 0 is in progress; no
application backend/frontend code, application acceptance or release evidence exists.
Documentation-validator tests and disposable compatibility probes are separate evidence.

## 1. Decision types

| Type | Use | Approval |
|---|---|---|
| **ADR** | Architecture or technology choice with lasting impact | Explicit owner decision after appropriate technical review |
| **RFC** | N0 change proposal with impact analysis | Explicit owner decision after review |
| **Design note** | N1 implementation approach | Technical review; approval process proposed |
| **Ticket** | N2 work item within authorized scope | Proposed team review; no independent scope authorization |

Product ownership does not establish technical expertise or technical review.
Record the author, reviewer and approver separately, only when evidenced.

## 2. ADR lifecycle and delivery evidence

Decision lifecycle:

```text
PROPOSED -> APPROVED -> (SUPERSEDED | DEPRECATED)
    |
    +-> REJECTED
```

- One ADR per decision, numbered sequentially, never silently replaced.
- Decision approval does not imply implementation, testing, release or permission
  to execute actions outside the approved scope.
- Record status changes with the actual date, decision maker, exact approved
  scope and evidence link. Do not invent an approval date or reviewer.
- Superseding an ADR requires a new ADR referencing the old one.
- ADRs live in [`docs/adr/`](../adr/).

Required sections follow the [ADR template](../14-TEMPLATES/ADR-TEMPLATE.md):
context, problem, options, recommendation/decision, reasons, consequences,
risks, migration implications, decision status, delivery status and evidence.

Track delivery separately: **PLANNED / NOT IMPLEMENTED**, **IMPLEMENTED**,
**TESTED**, **RELEASED**, or **DEPRECATED**. Before advancement, link respectively
to actual code/build artifacts, reproducible test commands and results for the
identified revision/environment, and release approval plus the published artifact.
A diagram, generated specification, unchecked checklist or future path is not
implementation evidence. Test execution and release must never be inferred from
an approved ADR. See [status rules](DOCUMENTATION-STANDARDS.md#3-status-labels).

## 3. Current decision log

### Approval basis

The owner explicitly approved, in the instruction commissioning these corrections:
**Authority Pro**, **C:/Users/admin/Documents/authority-pro**, **one monorepo**,
**Angular + Spring Boot**, **modular monolith**, **shared PostgreSQL with tenant_id
and RLS**, and **normative documentation plus a runnable foundation, with no
business modules**. These approvals are limited to that scope. The instruction
supplies no stable conversation URL or separate technical review; this paragraph
records the available evidence without fabricating either. Preserve these known
approvals; future decision-status advances require a durable approval reference.

[MASTER-BLUEPRINT](../MASTER-BLUEPRINT.md) sections 2 and 5 are the locked source
for DDD, hexagonal architecture, Plugin-Based Components, business-neutral core,
strict ownership, explicit contracts and secure-by-default principles. Section 15
requires FR, EN, IT and AR/RTL. The source's AUTHORITY ONLINE title does not
connect this independent project to legacy AUTHORITY.

### Owner confirmation of foundation orientations

The owner's subsequent conversational response explicitly validated the six presented orientations: "validée sauf que le postgres tourne deja pour une autre application ... creer une nouvelle base de donnes sans toucher a l'ancienne". This is bounded approval of the following principles, not of every implementation detail in the linked ADRs. The approval is recorded from this conversation; no separate technical reviewer, stable conversation URL or executed verification is invented.

1. Spring Security with JDBC-backed browser cookie sessions and CSRF protection.
2. Initial administrator creation through a local operator CLI, never a public setup page.
3. Restricted PostgreSQL runtime privileges, RLS and server-verified transaction-local tenant context.
4. Critical sensitive state changes and their audit commit together or roll back together.
5. Compiled first-party components with explicit contracts; no arbitrary hot-loaded plugins.
6. Application APIs under `/api/v1`, controlled tenant selection and standardized errors.

**Database constraint:** PostgreSQL already serves another application, according to the owner. Authority Pro must use a newly created dedicated database, not that application's database or schemas. A separate database on the existing server is the intended deployment; database name `authority_pro` is a candidate pending a collision check. No existing database, role, service configuration, data directory or volume may be overwritten or repurposed. No restart, server upgrade, drop/recreate, restore over existing data or global privilege change is authorized. Dedicated project roles need their own approved names and narrowly scoped creation plan because PostgreSQL roles are cluster-wide.

This authorizes the new-database direction, not execution against an unidentified endpoint or elevation of privileges. Host/port, actual server version, unused database/role names, operator authority and safe secret handling must be established before provisioning. A collision or need to alter the other application's security/configuration stops the operation for owner review. No database connection, creation or migration is claimed by this record.

Exact numeric versions, SQL/schema/grants, passwords/expiry/MFA/recovery policies, bootstrap guard, session/audit integration, module-verification tooling, tenant header format, RFC 9457 fields and probe exposure remain PROPOSED or unverified. These approved orientations narrow O1/O9/O10/O11; they do not close all G1 implementation/readiness conditions. The existing database is not assumed compatible merely because a client or draft Compose image exists.

| ADR | Decision | Decision status | Delivery status |
|---|---|---|---|
| 0001 | Angular + Spring Boot | APPROVED stack; numeric versions PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0002 | Modular monolith | APPROVED style; Spring Modulith/ArchUnit tooling PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0003 | Shared PostgreSQL, `tenant_id`, RLS | APPROVED strategy, restricted runtime and verified transaction-local context; exact SQL/grants/integration PROPOSED; new dedicated database, existing application untouched | PLANNED / NOT IMPLEMENTED |
| 0004 | One monorepo | APPROVED organization | Initial commit f5d2914 pushed to private ogive98/authority-pro; no application build |
| 0005 | Spring Security + Spring Session JDBC; browser cookie sessions and CSRF; local operator bootstrap | APPROVED orientations; detailed policies/integration PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0006 | One durable publication mechanism, selected by spike: Modulith registry or explicit outbox | PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0007 | Flyway migrations | PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0008 | Plugin execution model | APPROVED compiled first-party orientation and explicit contracts; packaging/tooling PROPOSED; third-party execution deferred | PLANNED / NOT IMPLEMENTED |
| 0009 | Script engine language and sandbox | PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0010 | Numeric Java / Spring Boot version baseline | PROPOSED; official metadata and bounded probe outcomes recorded; application/database compatibility still gated | PLANNED / NOT IMPLEMENTED |
| 0011 | URI API versioning and tenant selection/header contract | APPROVED /api/v1, controlled selection and standardized errors; exact headers/error/probe contracts PROPOSED | PLANNED / NOT IMPLEMENTED |

## 4. Open decisions

These questions are explicitly open. Recommendations must not be implemented
silently as though approved. Phase references are planning suggestions only.

| # | Question | Impact | Target |
|---|---|---|---|
| O1 | Compiled first-party execution approved; what packaging/tooling is needed, and is any later declarative or isolated third-party execution justified? A classloader is not an untrusted-code sandbox | Security, contracts, operations | Before corresponding implementation or any third-party execution authorization |
| O2 | Script engine: GraalJS, typed DSL, or no scripts in the foundation? | Security, UX, performance | Before any Phase 5 authorization |
| O3 | Search: PostgreSQL full-text vs OpenSearch | Infrastructure, cost, relevance | Before search implementation |
| O4 | Marketplace and licensing operating model | Product, legal | Future vision |
| O5 | Offline/hybrid synchronization scope | Architecture, effort | Future vision |
| O6 | Is any staged language rollout acceptable? FR/EN/IT/AR with RTL remain required; FR/EN-only is not approved | Accessibility, acceptance | Before language scope changes |
| O7 | AI providers and data-sharing policy | Compliance, cost | Future vision |
| O8 | Candidate matrix and bounded probes recorded in ADR-0010; approve actual JDK/pins and boundary/migration tooling, resolve or explicitly review frontend toolchain findings before use | Compatibility, maintenance; no blanket G1 closure from disposable tests | Before each applicable build baseline; frontend review before L07 |
| O9 | Sessions/CSRF and local operator CLI approved as orientations; exact cookie/expiry/recovery/MFA policies, guard, role model and grants remain open | Security, authorization | Before corresponding implementation or production readiness |
| O10 | Restricted runtime and verified transaction-local context approved; endpoint/version, new database/role names, operator authority, exact SQL/grants and connection integration remain open; old application untouched | Data isolation and coexistence | Before provisioning and tenancy implementation |
| O11 | Critical sensitive-state/audit atomicity approved; action taxonomy, framework integration, retention and measurable budgets including password-hash cost remain open | Integrity, capacity | Before corresponding implementation and acceptance |

Open decisions need realistic options, trade-offs and a PROPOSED recommendation,
followed by explicit approval evidence. No universal RLS bypass is an acceptable
substitute for separately authorized control-plane and tenant-plane operations.

## 5. Emergency changes

PROPOSED procedure: an emergency may shorten design review to restore service,
with an ADR and incident record within 48 hours. It does not waive explicit
approval, security boundaries or evidence requirements. No service is released today.

## 6. Traceability

Every future approved N0 change must link its approval, RFC and ADR to the
implementation review and changelog. Implementation, tests and releases retain
separate evidence links; documentation approval alone cannot close a phase gate.

## Related documents

- [Platform Constitution](PLATFORM-CONSTITUTION.md)
- [Versioning and Compatibility](VERSIONING-AND-COMPATIBILITY.md)
- [Risk Register](../13-IMPLEMENTATION/RISK-REGISTER.md)
