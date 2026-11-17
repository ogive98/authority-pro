# Decision Management

**Decision status:** PROPOSED process details; locked blueprint principles and
explicit approvals are recorded separately below.
**Delivery status:** PLANNED / NOT IMPLEMENTED. Phase 0 is in progress; no
backend/frontend code, passing tests or release evidence exists.

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

| ADR | Decision | Decision status | Delivery status |
|---|---|---|---|
| 0001 | Angular + Spring Boot | APPROVED stack; numeric versions PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0002 | Modular monolith | APPROVED style; Spring Modulith/ArchUnit tooling PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0003 | Shared PostgreSQL, `tenant_id`, RLS | APPROVED strategy; context, role and policy implementation details PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0004 | One monorepo | APPROVED organization | Local Git initialized; no commit, remote or application build |
| 0005 | Spring Security + Spring Session JDBC; same-origin browser session cookies and CSRF | PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0006 | One durable publication mechanism, selected by spike: Modulith registry or explicit outbox | PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0007 | Flyway migrations | PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0008 | Plugin execution model | PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0009 | Script engine language and sandbox | PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0010 | Numeric Java / Spring Boot version baseline | PROPOSED; compatibility unverified | PLANNED / NOT IMPLEMENTED |
| 0011 | URI API versioning and tenant selection/header contract | PROPOSED | PLANNED / NOT IMPLEMENTED |

## 4. Open decisions

These questions are explicitly open. Recommendations must not be implemented
silently as though approved. Phase references are planning suggestions only.

| # | Question | Impact | Target |
|---|---|---|---|
| O1 | Plugin runtime: isolated ClassLoader, separate JVM, or declarative-only packages? | Security, contracts, operations | Before any Phase 5 authorization |
| O2 | Script engine: GraalJS, typed DSL, or no scripts in the foundation? | Security, UX, performance | Before any Phase 5 authorization |
| O3 | Search: PostgreSQL full-text vs OpenSearch | Infrastructure, cost, relevance | Before search implementation |
| O4 | Marketplace and licensing operating model | Product, legal | Future vision |
| O5 | Offline/hybrid synchronization scope | Architecture, effort | Future vision |
| O6 | Is any staged language rollout acceptable? FR/EN/IT/AR with RTL remain required; FR/EN-only is not approved | Accessibility, acceptance | Before language scope changes |
| O7 | AI providers and data-sharing policy | Compliance, cost | Future vision |
| O8 | Exact supported version matrix and boundary/migration tooling | Compatibility, maintenance | Before foundation build baseline |
| O9 | Session/CSRF policy, operator-only one-time bootstrap, role model and tenant/company/site grants | Security, authorization | Before corresponding N0 implementation |
| O10 | RLS role separation and transaction-local verified tenant/user context; control-plane boundaries | Data isolation | Before tenancy implementation |
| O11 | Audit durability/retention and measurable performance/resource budgets, including BCrypt cost | Integrity, capacity | Before acceptance criteria are finalized |

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
