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

Those six orientations alone did not approve numeric versions, SQL/grants, identity policies, bootstrap guards, session/audit integration, module tooling, tenant headers or exact HTTP/probe contracts. The subsequent L02 decision below approves only its bounded subset. Other G1 implementation/readiness conditions remain open; the existing database is not assumed compatible merely because a client or draft Compose image exists.

| ADR | Decision | Decision status | Delivery status |
|---|---|---|---|
| 0001 | Angular + Spring Boot | APPROVED stack; L02 backend subset approved through ADR-0010, other numeric versions PROPOSED | L02 backend qualified; Angular NOT IMPLEMENTED |
| 0002 | Modular monolith | APPROVED style and bounded L02 package graph/Modulith/ArchUnit selection; future functional dependencies separately gated | Five closed roots/zero edges qualified; functional services NOT IMPLEMENTED |
| 0003 | Shared PostgreSQL, `tenant_id`, RLS | APPROVED strategy, restricted runtime and verified transaction-local context; exact SQL/grants/integration PROPOSED; new dedicated database, existing application untouched | PLANNED / NOT IMPLEMENTED |
| 0004 | One monorepo | APPROVED organization | Existing private repository and locally qualified L02 backend |
| 0005 | Spring Security + Spring Session JDBC; browser cookie sessions and CSRF; local operator bootstrap | APPROVED orientations; detailed policies/integration PROPOSED | L02 deny-all/CSRF boundary tested; identity/JDBC sessions/provisioning NOT IMPLEMENTED |
| 0006 | One durable publication mechanism, selected by spike: Modulith registry or explicit outbox | PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0007 | Flyway migrations | PROPOSED; excluded from L02 | PLANNED / NOT IMPLEMENTED |
| 0008 | Plugin execution model | APPROVED compiled first-party orientation and L02 single-artifact boundary; descriptor lifecycle/distribution details remain PROPOSED; third-party execution deferred | L02 artifact boundary qualified; plugin lifecycle NOT IMPLEMENTED |
| 0009 | Script engine language and sandbox | PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0010 | Numeric Java / Spring Boot version baseline | APPROVED L02 subset; database/frontend matrix remains gated | Portable target JDK/wrapper and online/offline clean L02 verification passed |
| 0011 | URI API versioning and tenant selection/header contract | APPROVED orientations plus bounded L02 local probes/error envelope; tenant headers, identity integration and hosted exposure remain PROPOSED | L02 probes/errors qualified; functional API/tenant selection NOT IMPLEMENTED |

### L02 skeleton decisions — 2026-11-18

**Decision status: APPROVED for the bounded L02 design below. Delivery: L02 implemented and locally qualified; see [execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18).** After requesting finalization of L02 decisions, the owner explicitly selected all three recommended answers in the decision questionnaire. The available approval evidence is transcribed here; no stable conversation URL or separate human technical reviewer was supplied. Assistant research and metadata checks are not independent security certification.

| Approval question | Exact selected answer | Approved scope |
|---|---|---|
| Quelle chaîne de compilation approuvez-vous pour le squelette L02 ? | Cible isolée (Recommandé) | Java 21 Temurin 21.0.13+9 portable for this project, Spring Boot 4.1.2 BOM, Maven 3.9.16 and Wrapper 3.3.4 with integrity checking; no global Java replacement |
| Approuvez-vous les cinq composants techniques et leur contrôle automatique pour L02 ? | Modulith et ArchUnit (Recommandé) | One backend artifact, root `com.authoritypro.platform.AuthorityProApplication`, exactly `identity`, `tenancy`, `access`, `audit`, `runtime`; Modulith 2.1.2 and ArchUnit 1.4.2; no intercomponent calls in L02, no event engine or dynamic loading |
| Approuvez-vous le démarrage local fermé et sans base de données décrit ci-dessous ? | Local fermé (Recommandé) | Explicit `skeleton-local` mode on `127.0.0.1`, port 8080 only if free; only GET liveness/readiness, readiness 503 while incomplete; deny other access, retain CSRF, validated configuration and RFC 9457 with `code`/`traceId`; springdoc 3.2.0 for tests/build only |

The selected alternatives were presented against retaining the installed Microsoft JDK, using ArchUnit alone, omitting the HTTP listener, or deferring approval. The isolated target keeps this project reproducible without altering other applications, at the cost of a new local JDK download and verification. Modulith/ArchUnit reduce bespoke boundary tooling but are not a hostile-code sandbox. Minimal local probes permit diagnosis without claiming an authenticated application is ready.

The [version subset](../adr/ADR-0010-java-spring-versions.md#l02-baseline-approval--2026-11-18), [package boundary](../adr/ADR-0002-modular-monolith.md#l02-boundary-approval--2026-11-18) and [local HTTP contract](../adr/ADR-0011-api-versioning.md#l02-local-http-approval--2026-11-18) carry the detailed acceptance constraints. Later exports/consumers need their own declared dependency approval; no placeholder identity, audit or registry service may pretend those capabilities exist.

No JDBC/JPA/driver, embedded database, Flyway migration, session store, default account, login or protected mutation belongs to L02. Database/RLS design and provisioning remain L03, audit L04, identity L05, protected inventory L06 and Angular L07. No change to the existing PostgreSQL application, role or service, not even a connection preflight, is authorized by this decision.

The questionnaire step recorded design decisions only. The portable JDK, wrapper and real skeleton must be verified during separately started L02 implementation; a failed integrity, compatibility or security check stops acceptance rather than authorizing an unsupported override. No download, application code, database access, new commit, push or deployment was performed by that questionnaire record. At that point the previous push remained on hold. This resolved the stated L02 design choices, not all of G1, the database-bearing G2 gate or production readiness.

### L02 implementation and publication authorization — 2026-11-18

The owner subsequently instructed: “continue seule , enchaine avec le lot prochain  commit et pousse , prends les bon choix pour moi”. This authorizes autonomous implementation and qualification of the next lot, L02, followed by a new verified commit and push to the existing repository. It lifts the prior publication hold for the pending documentation and this verified skeleton; it is not evidence that a push or any application test has succeeded.

The bounded work includes project-local tools/build configuration, startup/security adapters, architecture and artifact tests, repeatable qualification commands and the corresponding CI checks. Only the project's own loopback test processes may be started and stopped; an occupied port must not be freed by terminating another service. No business module, PostgreSQL connection/provisioning/migration, other-application change, deployment or automatic L03 implementation follows from this instruction.

## 4. Open decisions

These questions are explicitly open. Recommendations must not be implemented
silently as though approved. Phase references are planning suggestions only.

| # | Question | Impact | Target |
|---|---|---|---|
| O1 | Compiled first-party execution and bounded L02 single-artifact packaging/boundary tools approved; later descriptor lifecycle/distribution and any declarative or isolated third-party execution remain open. A classloader is not an untrusted-code sandbox | Security, contracts, operations | Before later lifecycle/distribution implementation or any third-party execution authorization |
| O2 | Script engine: GraalJS, typed DSL, or no scripts in the foundation? | Security, UX, performance | Before any Phase 5 authorization |
| O3 | Search: PostgreSQL full-text vs OpenSearch | Infrastructure, cost, relevance | Before search implementation |
| O4 | Marketplace and licensing operating model | Product, legal | Future vision |
| O5 | Offline/hybrid synchronization scope | Architecture, effort | Future vision |
| O6 | Is any staged language rollout acceptable? FR/EN/IT/AR with RTL remain required; FR/EN-only is not approved | Accessibility, acceptance | Before language scope changes |
| O7 | AI providers and data-sharing policy | Compliance, cost | Future vision |
| O8 | L02 JDK/build and Modulith/ArchUnit subset approved and locally qualified. Migration/data selections and frontend findings remain open | Compatibility and maintenance; bounded L02 evidence cannot qualify other stacks | Data decisions before L03 and frontend review before L07; repeat L02 checks when inputs change |
| O9 | Sessions/CSRF and local operator CLI approved as orientations; exact cookie/expiry/recovery/MFA policies, guard, role model and grants remain open | Security, authorization | Before corresponding implementation or production readiness |
| O10 | Restricted runtime and verified transaction-local context approved; endpoint/version, new database/role names, operator authority, exact SQL/grants and connection integration remain open; old application untouched | Data isolation and coexistence | Before provisioning and tenancy implementation |
| O11 | Critical sensitive-state/audit atomicity approved; action taxonomy, framework integration, retention and measurable budgets including password-hash cost remain open | Integrity, capacity | Before corresponding implementation and acceptance |
| O12 | Which bounded workflow dialect/engine, definition/instance ownership, task/deadline rules and running-version/external-effect protocol satisfy the deferred execution contracts? | Durability, tenant isolation, operating and migration cost | Before any selected L3 workflow/automation slice; not an L02 blocker |
| O13 | What agent/service identity, attenuated delegation, exact-action approval, memory/provenance and execution-budget policies apply, alongside O7 provider/export decisions? | Authority, privacy, audit and human control | Before selected L3 agent tools or memory; no approval through model confidence |
| O14 | Which governed UI schema dialect, compiled widgets, API bindings and publication/version rules are supported? Runtime entity/table/code generation remains outside this proposal | Contract evolution, accessibility and data ownership | Before C12/C38/C39 schema support in L2/L11; visual/AI builders remain L3 |

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
