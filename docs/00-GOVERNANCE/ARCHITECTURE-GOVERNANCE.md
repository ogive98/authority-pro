# Architecture Governance

**Decision status:** APPROVED bounded L02 root model and boundary tooling design
([2026-11-18 approval](DECISION-MANAGEMENT.md#l02-skeleton-decisions--2026-11-18)); future business rules remain PROPOSED. Modular monolith,
DDD, hexagonal architecture, Plugin-Based Components and strict ownership remain
binding source principles; see the [approval basis](DECISION-MANAGEMENT.md#approval-basis).
**Delivery status:** PLANNED / NOT IMPLEMENTED. Approved design is not an existing
package tree, CI enforcement or passing application tests.
**Scope:** Structural rules for the backend, frontend and module ecosystem.

## 1. Package model (APPROVED for bounded L02)

One artifact has application root `com.authoritypro.platform.AuthorityProApplication`.
Exactly `identity`, `tenancy`, `access`, `audit` and `runtime` are its immediate
module roots. No packages exist yet. Assert exact discovery from real application
classes/metadata before boundary verification; empty directories, a hard-coded
module list or an incorrectly nested model are not evidence.

L02 has ZERO intermodule edges. Introduce named `api` exports only when later
needed and explicitly approved, with separately declared and approved consumption;
no fabricated API/service makes a skeleton capability implemented. Keep `domain`,
`application` and `infrastructure` private when introduced. Domain ports preserve
inward dependencies without framework coupling. HTTP controllers are adapters,
not public cross-module contracts. Tenancy owns memberships; Identity owns global
accounts and sessions; Access owns grants and permission evaluation.

No open shared/config module is required in advance. A future business composition
root and its module discovery must be designed only after scope approval.

## 2. Allowed dependencies

L02 permits no intermodule dependency, including API-to-API edges. Later exports
and consumption need explicit approval; the business-module rows are proposals only.

| From | To | Allowed |
|---|---|---|
| `platform.<module>` | `platform.<other>.api` | Not in L02; later only via approved named export and declared consumption |
| `platform.<module>` | Any package of another module other than its explicitly exported API | No |
| `platform.*` | `modules.*` | No (inversion only through extension points) |
| `modules.<a>` | `modules.<b>.api` | Future only, after scope/export/consumption approval |
| `modules.*` | `platform.*.api` | Future only, after scope/export/consumption approval |

Rules that are not expressible as package rules are documented in an ADR; this
must not weaken strict ownership or authorize an L02 edge.

## 3. Enforcement (PLANNED / NOT IMPLEMENTED)

1. Approved L02 tools: Spring Modulith 2.1.2 and ArchUnit 1.4.2
   ([ADR-0010 pins](../adr/ADR-0010-java-spring-versions.md)); no event registry.
   `ApplicationModules.of(AuthorityProApplication.class).verify()` follows exact
   five-root discovery and an explicit empty-graph assertion, not class linkage alone.
2. ArchUnit checks must reject all L02 intermodule edges and private imports.
   Preserve no repository/entity leakage, no `platform` dependency on `modules`
   and no domain dependence on infrastructure/frameworks as capabilities arrive.
   Versioned test-only negative fixtures must demonstrate incorrect discovery and
   forbidden edges/imports fail; never add fake production services to exercise rules.
3. A boundary violation must fail the build. Nothing runs in CI yet: link actual
   application discovery, positive checks and deliberately failing fixture evidence
   before marking enforcement implemented or tested. Approval is design only.

## 4. Adding a module (proposed future procedure)

No business module is authorized today. This procedure applies only after
explicit scope approval and does not authorize Phase 6 implementation.

1. Write or update the module catalogue entry and manifest.
2. Create `modules/<name>/api` and `internal` packages.
3. Declare dependencies, capabilities, published and consumed events.
4. Add the Flyway migration under the module's ownership.
5. Add unit, integration and architecture tests.
6. Classify the change (N0/N1/N2) using the constitution.

## 5. Extension points

The source principle favors governed extension points over modifying stable
components. These candidate interfaces are PROPOSED, not implemented:

- `TenantResolver` — resolve tenant selection only after verifying authenticated
  user membership/authorization; header/domain mechanisms remain open decisions.
- `PermissionEvaluator` — evaluate scoped grants without bypassing backend or RLS
  checks; role model remains PROPOSED.
- `AuditSink` — optional export of already durable audit records; not a substitute
  for atomic sensitive-write/audit commit. Anonymous failures need a separate
  bounded audit transaction without secrets; do not silently swallow audit failures.
- `ModuleContributor` — future UI navigation/API metadata contribution; Phase 4
  is a proposed sequence, not authorization or delivered functionality.

## 6. Review process

Every N0 change requires an RFC, an ADR, impact analysis and a migration plan.
Every N1 change requires a design note and compatibility analysis. Reviews
verify boundary compliance, security controls and rollback feasibility.

## 7. Forbidden patterns

- Cross-module SQL joins in application code.
- Shared "utils" packages that accumulate business logic.
- Hidden global state or static mutable singletons carrying tenant data.
- Framework annotations from one module's internals leaking into another.
- Frontend-only authorization checks.

## Related documents

- [Platform Constitution](PLATFORM-CONSTITUTION.md)
- [Backend Architecture](../02-ARCHITECTURE/BACKEND-ARCHITECTURE.md)
- [Module Runtime](../03-CORE/MODULE-RUNTIME.md)
- [Test Strategy](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md)
