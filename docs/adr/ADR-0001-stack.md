# ADR-0001 — Angular and Spring Boot stack

- **Status:** APPROVED — Angular frontend and Spring Boot backend; the bounded L02 backend/tool subset is approved in ADR-0010. Other numeric/library selections remain PROPOSED, and frontend/database/session integration remains unverified.
- **Approval basis:** Explicit user direction for an independent Authority Pro documentation and foundation project in `C:/Users/admin/Documents/authority-pro`.

- **Record reviewed:** 2026-11-17; this is not an approval date for proposed details.
- **Accountable owner / approval authority:** Project owner.
- **Application delivery:** L02 no-database backend skeleton IMPLEMENTED / TESTED only; see [execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18). Frontend and database-bearing integration remain unimplemented; no release is claimed.

## Context

At the initial decision, Authority Pro was a greenfield platform with no
application code. The authorized scope remains documentation and a technical
foundation, not business modules; current L02 delivery is recorded above. The
user explicitly selected Angular and Spring Boot; this record makes no claim
about the owner's prior experience or approval of implementation mechanisms.

## Options considered

1. Angular + Spring Boot — explicitly selected core stack.
2. React + a Node.js backend — alternative, not selected.
3. Angular + .NET — alternative, not selected.

Deployment topology is a separate decision in ADR-0002, not a competing stack.

## Decision and approval boundary

Use Angular for the frontend and Spring Boot for the backend. Shared PostgreSQL
is approved separately in ADR-0003. The approved architecture direction is a
modular monolith with DDD, hexagonal boundaries and Plugin-Based Components (PBC).

The [L02 subset in ADR-0010](ADR-0010-java-spring-versions.md#l02-baseline-approval--2026-11-18) now approves the project-local Java 21 target, Boot/Maven baseline and boundary tooling. Those L02 pins and target-JDK skeleton tests now have bounded passing evidence. Angular/TypeScript, persistence/JPA, database and session-store integration remain separate slice decisions; do not import the whole candidate matrix into L02. No floating "latest" policy is approved.

## Consequences

- Frontend and backend can evolve together within the approved monorepo.
- Framework conventions do not replace domain and application boundaries.
- Only backend-controlled paths may access PostgreSQL; browser database access
  is not part of the proposal.
- A future service extraction would require explicit contracts and data
  migration; the stack does not make extraction automatic.

## Security considerations

Framework selection alone does not establish authentication or isolation.
ADR-0005 proposes browser sessions and CSRF protection; ADR-0003 proposes the
RLS implementation. Dependency vulnerability review and supported-version checks
are required before selecting versions. No business modules, secrets, or
application code are introduced by this ADR revision.

## Validation and tests

L02 clean verification passed 65 unit tests and 4 packaged integration tests with
no failures or skips on the approved project-portable toolchain. See the canonical
evidence above and ADR-0010; the offline clean repeat also passed.
Frontend, PostgreSQL connectivity/migration, session-store and broader dependency/
security acceptance remain separately gated and unverified. No PostgreSQL was
touched. A registry lookup alone is not a compatibility test.

## Migration implications

The initial greenfield decision required no existing application migration. The
approved L02 baseline is now built and tested without a database or data migration.
Later major upgrades need release-note review, integration tests and a rollback
plan; L03 facts/design and approval precede any database implementation.

## Related

- [ADR-0002 — Modular monolith](ADR-0002-modular-monolith.md)
- [ADR-0003 — Multi-tenancy](ADR-0003-multi-tenancy.md)
- [ADR-0004 — Monorepo](ADR-0004-monorepo.md)
- [ADR-0005 — Browser sessions](ADR-0005-sessions.md)
- [ADR-0010 — Version verification](ADR-0010-java-spring-versions.md)
