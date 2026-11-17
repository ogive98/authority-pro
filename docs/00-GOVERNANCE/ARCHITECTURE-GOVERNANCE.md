# Architecture Governance

**Decision status:** PROPOSED implementation conventions. Modular monolith,
DDD, hexagonal architecture, Plugin-Based Components and strict ownership are
binding source principles; see the [approval record](DECISION-MANAGEMENT.md#approval-basis).
**Delivery status:** PLANNED / NOT IMPLEMENTED. The package tree, tooling and
CI checks below are design proposals, not existing code or passing tests.
**Scope:** Structural rules for the backend, frontend and module ecosystem.

## 1. Package model (PROPOSED)

Proposed application root: `com.authoritypro.platform.AuthorityProApplication`.
Its immediate child packages `identity`, `tenancy`, `access`, `audit` and `runtime`
are the candidate module roots. No packages exist yet. Assert these exact discovered
modules before invoking boundary verification; a nonempty but incorrectly nested
model is not proof of modularity.

Each module exposes an explicitly declared named `api` interface and keeps
`domain`, `application` and `infrastructure` private. Domain ports must preserve
inward dependencies without framework coupling. HTTP controllers are adapters,
not public cross-module contracts. Tenancy owns memberships; Identity owns global
accounts and sessions; Access owns grants and permission evaluation.

No open shared/config module is required in advance. A future business composition
root and its module discovery must be designed only after scope approval.

## 2. Allowed dependencies

| From | To | Allowed |
|---|---|---|
| `platform.<module>` | `platform.<other>.api` | Yes, if declared |
| `platform.<module>` | Any package of another module other than its explicitly exported API | No |
| `platform.*` | `modules.*` | No (inversion only through extension points) |
| `modules.<a>` | `modules.<b>.api` | Yes, if declared |
| `modules.*` | `platform.*.api` | Yes |

Rules that are not expressible as package rules are documented in an ADR.

## 3. Enforcement (PLANNED / NOT IMPLEMENTED)

1. Proposed Spring Modulith verification, such as
   `ApplicationModules.of(AuthorityProApplication.class).verify()`, requires a
   compatible version and validated discovery configuration. Nothing runs in CI yet.
2. Proposed ArchUnit tests would assert:
   - no class outside a module imports its private domain/application/infrastructure packages;
   - no JPA repository or entity is referenced outside its owning module;
   - no dependency from `platform` to `modules`;
   - domain logic does not depend on infrastructure adapters/frameworks.
3. Intended gate: a boundary violation fails the build. Approval of modularity
   is not evidence of enforcement; link actual test code and passing/failing
   test results before marking this implemented or tested.

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
