# ADR-0002 — Modular monolith with explicit boundaries

- **Status:** APPROVED — modular monolith with DDD, hexagonal architecture and Plugin-Based Components (PBC); enforcement tooling and package layout are PROPOSED.
- **Approval basis:** Explicit user architecture direction, not approval of particular libraries or an implemented design.

- **Record reviewed:** 2026-11-17; this is not an approval date for proposed details.
- **Accountable owner / approval authority:** Project owner.
- **Application delivery:** NOT IMPLEMENTED; test and release evidence absent.

## Context

The independent Authority Pro project has no application code. Its current
scope is documentation and the technical foundation only; no business modules
are authorized. The architecture must permit future cohesive components
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
PBC means Plugin-Based Components with explicit contracts; it does not imply
hot loading or permission to run third-party code. Angular is the frontend.

Propose domain/application code behind ports, infrastructure adapters outside
those boundaries, and explicit exported APIs/events between components. Avoid
cross-component access to internal entities, repositories or owned tables.
Shared PostgreSQL does not remove component ownership.

Spring Modulith and ArchUnit are proposed tools for dependency, cycle and
boundary checks. Their compatibility, module discovery, exported-package rules
and CI integration need a spike. `ApplicationModules.verify()` is a candidate
check, not code that exists or a guarantee of runtime security. Exact package
names, build modules and database schemas are not approved here.

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

Planned, not executed: verify the candidate tooling against ADR-0010; deliberately
introduce a dependency cycle and an internal-package import and require checks
to fail; exercise ports with adapter tests. Prove tenant isolation separately
against PostgreSQL. No tooling or CI enforcement is currently implemented.

## Migration implications

Greenfield: establish a minimal technical boundary convention before code is
created. A later component extraction requires contract versioning, data
ownership and migration planning, authorization review and an explicit decision.
Do not introduce a distributed runtime merely to anticipate that possibility.

## Related

- [ADR-0001 — Stack](ADR-0001-stack.md)
- [ADR-0003 — Multi-tenancy](ADR-0003-multi-tenancy.md)
- [ADR-0006 — Durable events](ADR-0006-outbox.md)
- [ADR-0008 — PBC execution model](ADR-0008-plugin-runtime.md)
- [ADR-0010 — Version verification](ADR-0010-java-spring-versions.md)
