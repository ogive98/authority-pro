# Implementation Phases

**Decision status:** PROPOSED sequencing within the owner-approved documentation plus socle scope.
**Current application state:** L02 no-database backend skeleton IMPLEMENTED / TESTED only; [execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18) records 65 unit tests and 4 packaged integration tests with no failures or skips. The offline clean repeat also passed. No PostgreSQL was touched; frontend, identity, audit, protected inventory and business features are not implemented.
**Scope reaffirmed:** Build only the technical foundation that will receive future modules; no business features, screens, tables or demonstration modules.

## Delivery levels

| Level | Intended result | Limit |
|---|---|---|
| L1 — Initial executable nucleus | Reproducible Angular/Spring Boot foundation with identity, isolation, audit, minimal compiled registry, technical shell and recovery evidence | Not the complete extensible platform; advanced identity and operational capabilities remain explicit gaps |
| L2 — Extensible socle | Governed contracts, dependency/lifecycle checks, reliable technical execution and developer/UI extension tools | Proposed subsequent technical scope, not authorization to install arbitrary code or implement business modules |
| L3 — Advanced optional platform | Separately selected shared engines and platform products | Documented candidates only; each needs a use case, threat model, design and explicit delivery decision |

These are delivery levels, not N0/N1/N2 change-governance levels or the source's priority classes. The [coverage matrix](../PACK-INVENTORY.md#technical-coverage-matrix) is the authoritative capability allocation; this file owns lot sequencing and acceptance. A capability may have a bounded baseline in L1 and a separately gated extension later.

## Dependency-aware sequence

The historical step identifiers remain for traceability. The proposed lots below refine them rather than claiming a previously approved implementation schedule.

| Step | Scope and dependency | Deliverable | Exit evidence |
|---|---|---|---|
| 0A | Audit source and retain approved choices | Corrected normative docs, scope matrix, ADRs and risk register | No invented approval or implemented claim |
| 0B | Validate documents and foundation contracts | Link/schema/diagram validator and fixtures | Clean run and intentional negative cases rejected |
| 0C | Resolve slice-specific N0 gates and verify versions | Reviewed design and tested version candidates, then approved pins | Owner decisions plus authoritative compatibility and spike evidence |
| 1A | Executable technical foundation after applicable 0C decisions | L02 no-database Maven Wrapper/backend/probes; isolated DB provisioning belongs to separately gated L03 | Bounded L02 build/start/discovery evidence does not close database-bearing G2 |
| 1B | Audit, identity and isolation after 1A | Atomic audit before protected mutations, closed provisioning, sessions, verified context, permissions and RLS | Audit rollback, CSRF/revocation/concurrency/all-DML isolation tests |
| 1C | Minimal registry and authorized Angular shell after required APIs | Compiled inventory, audit read, scope-aware UI and four locales | Descriptor rejection, browser edge cases and restore drill |
| 2 | Extensibility beyond the L1 inventory | Dependency graph, contracts, lifecycle, integration plans and extension tooling | Technical fixtures only; no business module required |
| 3 | Durable processing when explicitly selected | One publication mechanism, bounded workers/retries and consumer inbox | Crash/retry/dedup/order/tenant tests |

Minimum logging, health, security controls and backup/restore accompany the first database-bearing slice. Audit is a prerequisite of sensitive changes, not an optional later add-on. Angular browser checks accompany the first UI slice, not just final qualification.

## Eight proposed nucleus lots

These are the eight lots presented in the conversation, now made explicit. Later sequencing remains PROPOSED; the [bounded L02 design is owner-approved](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l02-skeleton-decisions--2026-11-18) and its no-database skeleton is delivered/tested. This does not deliver L03–L08 or authorize their automatic implementation. Roles below indicate responsibility, not assigned people; the project owner remains the approval authority.

| Lot | Scope / proposed lead | Dependencies | Deliverable and exit evidence |
|---|---|---|---|
| L01 — Technical decisions | Platform/security: compatibility, security design and build strategy | Documentation review; applicable open decisions | Authoritative version/support evidence, bounded compatibility spikes, documented approvals before implementation; G1, not application delivery |
| L02 — Backend skeleton | Platform: Spring Boot entry point, module boundaries, configuration, errors and probes; bounded design approved 2026-11-18, skeleton IMPLEMENTED / TESTED | Approved L02 subset of ADR-0002/0010/0011; exact project-portable tools verified | Clean verify passed: 65 unit + 4 packaged integration tests, no failures/skips; exactly five compiled closed roots and zero intermodule edges with positive/negative fixtures; real local no-DB main startup, invalid configuration/occupied port rejected; only exact GET liveness 200/UP and readiness 503/OUT_OF_SERVICE, other HTTP denied, CSRF retained, no sessions/default user; test-only OpenAPI absent from production JAR; full G2 remains open until L03 |
| L03 — Data and isolation | Tenancy/Access plus database operations: migrations, roles, organization/company/site structure, transaction-local context and RLS | L02 skeleton checks, not full G2; approved isolation/migration design | Complete G2 with actual PostgreSQL build/start and roles; restricted-role read/insert/update/delete, missing-context, pool-reuse, rollback and concurrency tests; technical fixtures only; isolation part of G3 |
| L04 — Fundamental audit | Audit plus security: protected-state/audit atomicity and bounded failed-attempt stream | L03; approved audit semantics | Fault-injected audit failure rolls back the associated protected change; no secret payloads; audit part of G4 |
| L05 — Identity and access | Identity/Tenancy/Access: closed provisioning, sessions, permissions, membership selection and revocation | L03 and L04; approved authentication design | Provisioning races, CSRF, session expiry/revocation, unauthorized scope and tenant-owner separation tested; G3 plus integrated audit evidence |
| L06 — Minimal runtime | Runtime: compiled descriptors, provided/required contracts and capability inventory | L02 boundaries; L05 for protected inventory access | Reject malformed declarations, missing required providers and incompatible contracts; inventory reflects real components; no dynamic installation |
| L07 — Angular shell | UI platform: login, context, navigation, audit view, themes, preferences and FR/EN/IT/AR with RTL | L05/L06 APIs and audit read contract | Real browser golden/edge paths, late tenant responses, permissions, keyboard/accessibility and all locales/themes; UI part of G4 |
| L08 — Integrated qualification | Quality/security/operations: build, CI, performance baseline, supply-chain checks and recovery | L02–L07; evidence gathered continuously | Integrated G2–G4 evidence, clean-install repeatability, isolated restore and G5 readiness review; no implicit remote publication |

L01 may run separately authorized, disposable technical spikes to establish compatibility; these are not production scaffolding or approval of a dependency matrix. With bounded L02 verification complete, the next step is L03 facts/design and approval: establish the server/target facts and proposed isolation mechanics without automatically connecting, provisioning or migrating PostgreSQL. L03 cannot claim end-to-end user authorization before L05. L04 must be integrated and retested with provisioning/login/session behavior in L05. L08 does not postpone protection, audit or recovery until the end.

## Proposed extensions after the nucleus

| Lot / level | Scope / proposed lead | Dependencies | Acceptance before completion |
|---|---|---|---|
| L09 / L2 — Governed extensibility | Runtime/API platform: exact ownership and dependency matrix, optional dependencies where justified, contract/schema compatibility, lifecycle and integration planning | L06, approved lifecycle/API contracts and completed L1 baseline | Technical fixtures cover cycles, absent required dependencies, optional absence, multiple providers, permission changes and incompatible versions. Opted-in command idempotency proves current replay authorization, fingerprint conflicts, atomic result/audit and bounded key retention. Dry-run reports consumers and data-retention impact; disabling never silently deletes data |
| L10 / L2 — Reliable execution | Runtime execution plus Audit/operations: one durable publication mechanism, inbox, bounded jobs, retries, failure queue and recovery | L09; ADR-0006 selection; scoped worker authority; approved resource budgets | Crash windows, duplicates, order, stale leases, cancellation, retry exhaustion, authorized replay and tenant isolation tested; scheduler added only for a specified technical job |
| L11 / L2 — Technical tools and governance | UI/developer/security/data/operations: extension SDK/CLI, UI contributions, API governance, data lifecycle, access reviews, environment and release controls | L09; L10 only for capabilities needing durable jobs | Each selected matrix row has its own contract and tests; critical baseline controls already exist in L1. No claim that a generator or dashboard implements an engine |
| L3 / Unscheduled | Optional workflow/automation, generic services, advanced operations, Studio, AI, packages, connectivity and offline capabilities | Per-capability use case, approval, prerequisites and security review | Split into bounded future lots before implementation; no placeholder executable engines or blanket L3 completion claim |

L09–L11 are proposals, not three promises to finish every platform capability. Independent L11 controls need not wait for L10. Prefer ordinary compiled components, typed calls and the existing PostgreSQL boundary until a concrete requirement justifies more infrastructure.

## Deferred capability design gates

The additional contracts below are documentation refinements, **PROPOSED / NOT IMPLEMENTED**, not new scheduled lots or approval to extend L01–L08. Workflow/automation and agents remain L3; governed interface schemas remain L2/L11, with visual/AI builders in L3. Unrelated deferred decisions do not block the bounded L02 skeleton.

| Candidate | Required decision before a corresponding future slice | Scoped acceptance |
|---|---|---|
| Workflow and human tasks, C17/C18 | Definition dialect/subset, state ownership, assignment/deadline semantics, engine comparison, running-instance evolution and external-effect protocol | [Runtime contracts](../03-CORE/MODULE-RUNTIME.md#9-deferred-workflow-and-automation-contracts); durable delivery/service authority prerequisites must be proven, not inferred from a queue or compiler probe |
| Agent-assisted operations, C55–C57 | Agent identity/delegation, exact-action approvals, tenant memory/provider data policy, tool allowlist and budgets | [Agent authority contracts](../00-GOVERNANCE/SECURITY-GOVERNANCE.md#10-deferred-agent-authority-contracts); begin with authorized reads/proposals, then validated drafts; enable bounded effectful actions only after their own acceptance |
| Governed UI schemas, C12/C38/C39 | Schema dialect/versioning, compiled widget set, existing API/data owners, publication/revision rules and accessibility | [Frontend contracts](../02-ARCHITECTURE/FRONTEND-ARCHITECTURE.md#deferred-schema-driven-interfaces); forms never authorize runtime database/entity/code generation |

Use the [deferred test matrix](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md#deferred-capability-acceptance) with technical fixtures only. Compare operating cost, licensing, isolation, persistence/recovery and migration feasibility before adopting an external workflow/agent engine. An adapter is not proof that engines or their histories are interchangeable. No new broker, cache, model provider, infrastructure or business module is selected by this sequencing.

## Future vision only

The source retains business and industry catalogues for context. They are outside this mission and are not a later automatic phase of this delivery plan. Do not create accounting, HR, fleet or other business rules, screens, tables, packages or demonstration modules to prove extensibility. Use test-only technical providers/consumers with inert bounded payloads and no production business data.

A shared notification, document or reporting service would be a separately approved technical capability; payroll notifications, invoices and financial reports remain excluded business features. Advanced products are not necessary to claim the bounded L1 nucleus delivered.

## Change and rollback discipline

Documentation consolidation does not approve N0 implementation. Keep the source archive unchanged. Application rollback must distinguish compatible code rollback from data recovery; a migration cannot promise universal automatic reversal. No commit, remote creation, push, release or production deployment follows implicitly from any gate.

## Related

- [MVP boundaries](MVP-BOUNDARIES.md)
- [Validation gates](VALIDATION-GATES.md)
- [Coverage matrix](../PACK-INVENTORY.md#technical-coverage-matrix)
- [Product roadmap](../01-PRODUCT/PRODUCT-ROADMAP.md)
- [Decision management](../00-GOVERNANCE/DECISION-MANAGEMENT.md)
