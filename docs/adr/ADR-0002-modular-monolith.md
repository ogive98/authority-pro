# ADR-0002 — Modular monolith with explicit boundaries

- **Status:** APPROVED — modular monolith with DDD, hexagonal architecture and Plugin-Based Components (PBC); bounded L02 roots, empty dependency graph and Modulith/ArchUnit selection approved on 2026-11-18. Future functional exports remain gated.
- **Approval basis:** Explicit user architecture direction plus the [L02 questionnaire answers](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l02-skeleton-decisions--2026-11-18), not evidence of implementation.

- **Record reviewed:** 2026-11-17; this is not an approval date for proposed details.
- **Accountable owner / approval authority:** Project owner.
- **Application delivery:** L02 no-database skeleton and five-root/zero-edge boundary checks IMPLEMENTED / TESTED only; see [execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18). No functional module APIs or release are delivered.

## Context

At the initial decision, the independent Authority Pro project had no
application code. Its scope remains documentation and the technical foundation
only; the L02 skeleton is now tested, but no business modules are authorized. The architecture must permit future cohesive components
without requiring distributed deployment now.

## Options considered

1. Modular monolith with explicit component APIs and dependency checks.
2. Layered monolith governed only by conventions.
3. Microservices with independent deployments and distributed coordination.
4. Dynamically loaded in-process plugins; classloader separation would not
   provide a security boundary.

## Decision and approval boundary

The approved direction is a Spring Boot modular monolith: one backend deployment
unit, organized around domain boundaries using DDD and hexagonal architecture.
The original technical term PBC means Plugin-Based Components with explicit
contracts; the later product terminology is reconciled below. Neither wording
implies hot loading or permission to run third-party code. Angular is the frontend.

Propose domain/application code behind ports, infrastructure adapters outside
those boundaries, and explicit exported APIs/events between components. Avoid
cross-component access to internal entities, repositories or owned tables.
Shared PostgreSQL does not remove component ownership.

### L02 boundary approval — 2026-11-18

The [owner explicitly selected Modulith and ArchUnit](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l02-skeleton-decisions--2026-11-18) for a single backend artifact under `com.authoritypro.platform.AuthorityProApplication`. Exactly five immediate child module roots are approved: `identity`, `tenancy`, `access`, `audit` and `runtime`. Modulith 2.1.2 and ArchUnit 1.4.2 are selected for declarations/discovery and build/test boundary enforcement; no durable event mechanism is selected.

The L02 intermodule dependency graph has **zero edges**. These roots reserve technical ownership, not implemented identity, tenancy, authorization, audit or inventory services. Do not invent exported methods or operational contracts to make the skeleton appear complete. The runtime boundary may host the actual technical startup/HTTP adapters; root discovery must not create a sixth shared/config component.

Only create `api`, `domain`, `application` and `infrastructure` packages when real code requires them. A future cross-module API must be a named export with an explicitly approved consumer; no wildcard grants, implicit root-package exports, private entity/repository imports or all-purpose shared API. Functional dependencies need a later slice decision before their introduction.

L02 must assert the exact five discovered names, then verify its empty graph and private boundaries. Test-only technical fixtures must prove that undeclared cross-module edges, cycles and private-package imports fail, while an explicitly allowed fixture contract passes. These fixtures are not production exports or business modules. Configure annotation processing/output explicitly so verification cannot write into another workspace.

`ApplicationModules.verify()` and ArchUnit checks are **IMPLEMENTED / TESTED for L02**: exactly five compiled `package-info` closed roots and zero dependency edges, with positive/negative boundary fixtures. This local build evidence does not establish remote CI execution or tenant authorization. Database schemas, future functional contracts and runtime lifecycle mechanisms remain separately gated.

### Product and logical layer clarification — 2026-11-21

**Approval basis:** the owner's explicit "integre aussi tt ca" instruction and reaffirmation of Modular Monolith, DDD, Hexagonal Architecture and **Packaged Business Capabilities**, followed by the twelve-layer supplement; see the [bounded documentation authorization](../00-GOVERNANCE/DECISION-MANAGEMENT.md#catalogue-and-layer-integration-authorization--2026-11-21). This addendum records a terminology/responsibility clarification, not an N0 mechanism replacement, completed independent review or new execution permission.

The four principles answer different questions: one backend deployment unit; cohesive domain boundaries and language; inward code dependencies through ports/adapters; and identifiable, versioned, governed capability packaging. The original blueprint's **Plugin-Based Components** remains the historical technical formulation. **Packaged Business Capabilities** is the requested product meaning; the correspondence preserves the existing explicit-contract, compiled-first-party and data-ownership boundaries rather than introducing a second PBC runtime. The earlier source variant "Platform Business Capabilities" is not a separate adopted mechanism. No `kind`, manifest version, API identifier, package path or trusted-execution setting changes here.

The [twelve logical layers](../02-ARCHITECTURE/TARGET-ARCHITECTURE.md#architectural-foundations-and-logical-layers) classify responsibilities, not twelve modules, Maven artifacts, applications or microservices. A feature is not necessarily a workflow; a module can provide several reusable nodes/capabilities; a library or adapter need not become a PBC. UAM is the composition definition, not graphical layout or a copy of all domain/runtime state. The editor and runtime remain separate, and installed, activated, entitled, configured, published and authorized are independent conditions.

**Impact:** documentation and source traceability only; the existing five roots and their allowed graph remain unchanged. Retain Angular, the single Spring Boot artifact and PostgreSQL; no source folder reorganization, shared-kernel creation, new infrastructure or business examples are implemented. Alternatives rejected are replacing the baseline with a global twelve-tier call chain, creating one module per engine name, and adopting microservices or in-process untrusted plugins by inference.

**Acceptance and migration:** [module internals](../02-ARCHITECTURE/BACKEND-ARCHITECTURE.md#module-internals-and-dependency-direction) specify the proposed private structure; [catalogue/layer acceptance](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md#catalogue-and-layer-acceptance) retains exact export, domain-inward and single-authority tests for later slices. No database/code migration or new architectural test execution occurs in this clarification. Any later extraction needs its own measured justification, ADR, ownership/data migration, authorization/consistency review and recovery plan; this record does not approve it.

## Consequences

- One backend deployment avoids distributed infrastructure for the foundation.
- Components share the JVM and database; failures and resource exhaustion can
  affect the whole application. Logical boundaries are not process isolation.
- Explicit ports and contracts aid testing but do not guarantee inexpensive
  future service extraction.
- Only technical foundation components may be scaffolded; business modules are
  outside the current scope.

## Security considerations

Dependency rules do not enforce tenant authorization. Apply ADR-0003 at data
access boundaries and ADR-0005 at authentication boundaries. Trusted compiled
first-party components have application-process privileges; untrusted executable
extensions require a separate future execution model under ADR-0008.

## Validation and tests

L02 real discovery, zero-edge verification and positive/negative boundary fixtures
passed as part of the canonical clean verification linked above. Future functional
ports and exported contracts need their own tests. Later [closed L03 evidence](../PACK-INVENTORY.md#l03-main-recovery-evidence--2026-11-20)
qualifies bounded PostgreSQL/JDBC/RLS integration, not authenticated membership
checks or full application security. Neither local evidence nor this clarification
claims remote CI execution.

## Migration implications

The initial greenfield decision required a minimal boundary convention before
code creation; L02 now implements and tests that bounded structure. A later
component extraction requires contract versioning, data ownership and migration
planning, authorization review and an explicit decision.
Do not introduce a distributed runtime merely to anticipate that possibility.

## Related

- [ADR-0001 — Stack](ADR-0001-stack.md)
- [ADR-0003 — Multi-tenancy](ADR-0003-multi-tenancy.md)
- [ADR-0006 — Durable events](ADR-0006-outbox.md)
- [ADR-0008 — PBC execution model](ADR-0008-plugin-runtime.md)
- [ADR-0010 — Version verification](ADR-0010-java-spring-versions.md)
