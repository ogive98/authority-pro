# ADR-0007 — SQL migrations with Flyway

- **Status:** APPROVED for the bounded closed L03 integration; broader deployment and upgrade workflows below remain PROPOSED.
- **Approval basis:** [Closed L03 authorization — 2026-11-19](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l03-closed-application-integration-authorization--2026-11-19).
- **Record reviewed:** 2026-11-19.
- **Accountable owner / approval authority:** Project owner.
- **Application delivery:** IN PROGRESS, not released. The [application qualification checkpoint](../PACK-INVENTORY.md#l03-application-qualification-checkpoint--2026-11-20) records 82 passing backend tests, 111 application persistence checks and 24 restored-target checks. Main-database migration and valid credential-bearing packaged launches remain unexecuted; this is not full L03 acceptance. The earlier [isolated scratch qualification](../PACK-INVENTORY.md#l03-isolated-qualification-evidence--2026-11-19) remains separate historical evidence.

## Context

The approved shared PostgreSQL strategy needs reproducible schema, constraint,
role/grant and RLS evolution. Authority Pro now has a closed backend skeleton;
the first application migration is being integrated and has not been applied to
the main database. Current work is the technical foundation, not business schemas.

## Options considered

1. Flyway with reviewed SQL migrations — direct control of PostgreSQL features
   and checksums; proposed default.
2. Liquibase — structured change metadata and tooling, with additional format
   and operational choices.
3. Hibernate schema generation — useful for experiments but not a reviewed,
   deterministic production migration history.
4. Manually executed SQL — minimal tooling, weak repeatability and auditability.

## Proposed decision

Recommend Flyway with immutable released SQL migrations. Verify its selected
edition/version, PostgreSQL adapter and Spring Boot integration under ADR-0010
before approval. If JPA is adopted, propose `ddl-auto=validate`, never runtime
schema creation/update in deployed environments.

Maintain **one deterministic migration order for the monorepo** and one canonical
history for the shared database. Propose a centrally ordered migration directory,
such as `backend/src/main/resources/db/migration`, once the backend layout exists.

- Assign globally unique, monotonically ordered migration versions in the merged
  stream. CI checks duplicate versions and ordering before release; reconcile
  concurrent branches before merging. No out-of-order application by default.
- Track component ownership in descriptive names/review responsibility, not in
  reserved per-module numerical ranges. Ranges do not provide an extensible
  dependency order and are not part of this recommendation.
- Express cross-component schema dependencies in that single sequence. Runtime
  module enablement must not independently reorder or inject migrations.
- Never edit an applied/released migration to hide drift. Add a reviewed forward
  migration; checksum repair is an exceptional, audited recovery action.
- Prefer a controlled migration step with separate credentials over DDL from
  every application replica. Runtime credentials have no migration privilege.

Roles needing elevated cluster provisioning may require a restricted operator
step before Flyway can connect. Define and review that boundary explicitly; do
not solve it by making the application or normal migrator a superuser. Changes
to existing environment permissions are not performed by this ADR revision.

## Consequences

SQL expresses RLS, constraints and indexes precisely but needs PostgreSQL-aware
review. Migrations must be tested from an empty database and supported prior
versions. A central sequence requires coordination across concurrent work;
component autonomy does not imply independent database histories. ORM validation
does not validate policy correctness or all database constraints.

## Security considerations

Separate migrator/owner and runtime roles as proposed in ADR-0003. Keep migration
credentials out of application configuration and runtime processes. Review every
new tenant table for non-null tenant identity, tenant-aware references, forced
RLS and deny-by-default policies. Migrations are trusted executable SQL; a PBC
manifest or signed package does not authorize arbitrary SQL installation.
Never embed bootstrap credentials, session secrets or production data in SQL.

## Validation and tests

Planned, not executed: migrate real PostgreSQL from empty and previous supported
schema versions; verify checksums, duplicate/order rejection, concurrent migration
locking and failed-migration recovery. Assert runtime grants/role flags, ownership,
RLS and cross-tenant constraints after migration. Validate session/provisioning
schema when ADR-0005 is implemented. Review long locks and test explicitly any
non-transactional statements rather than assuming every migration rolls back.

## Migration implications

There is no existing schema to baseline. Establish the ordered history only after
tool/version approval. Later changes should use expand-and-contract for rolling
compatibility and reviewed data backfills. PostgreSQL operations such as concurrent
index creation may need non-transactional execution. Prefer forward fixes; test
backups/restoration and application rollback against the supported schema window
before destructive changes.

## Related

- [ADR-0003 — PostgreSQL roles and RLS](ADR-0003-multi-tenancy.md)
- [ADR-0004 — Monorepo](ADR-0004-monorepo.md)
- [ADR-0005 — Identity/session persistence](ADR-0005-sessions.md)
- [ADR-0006 — Event persistence selection](ADR-0006-outbox.md)
- [ADR-0008 — Trusted package installation](ADR-0008-plugin-runtime.md)
- [ADR-0010 — Version verification](ADR-0010-java-spring-versions.md)
