# ADR-0003 — Shared PostgreSQL, tenant_id and RLS

- **Status:** APPROVED — shared tenant_id/RLS strategy, restricted runtime and verified transaction-local tenant context; exact SQL/grants/policy integration below remain PROPOSED.
- **Approval basis:** Initial tenancy selection plus the [subsequent owner confirmation](../00-GOVERNANCE/DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations). The [L03 local-environment authorization](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l03-local-environment-authorization--2026-11-18) supersedes only the earlier existing-server topology: Authority Pro will use a new dedicated PostgreSQL instance and database, leaving the other application and concurrent project untouched. Shared tenants within Authority Pro's database remain approved; no implemented isolation guarantee is claimed.

- **Record reviewed:** 2026-11-17; this is not an approval date for proposed details.
- **Accountable owner / approval authority:** Project owner.
- **Tenancy delivery:** NOT IMPLEMENTED; PostgreSQL isolation tests and release evidence remain absent. The separate [L02 no-database skeleton is implemented/tested](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18); no PostgreSQL was touched in L01/L02. The [empty dedicated environment and role checks](../PACK-INVENTORY.md#l03-local-environment-evidence--2026-11-18) now have passing evidence; this is not tenant isolation. Application migrations, RLS, JDBC integration, identity, audit, Angular and backup remain unimplemented; G2 is open.

## Context

Authority Pro needs tenant isolation in a shared database. At the initial
decision, no application code existed; the current L02 skeleton deliberately has
no database or tenancy implementation. Only the technical foundation is in scope.
Future authentication must work before a tenant is selected without creating a
platform-wide tenant-data bypass.

## Options considered

1. Shared PostgreSQL with a tenant discriminator and RLS — approved strategy.
2. Schema per tenant — additional schema and migration management.
3. Database per tenant — stronger deployment separation with operational cost.
4. Application-only predicates — insufficient database defense in depth.

## Decision and approval boundary

Use option 1 for shared tenants in Authority Pro's new dedicated database and instance, not the other application's database or server. Restricted runtime access and verified transaction-local tenant context are approved orientations. The bounded local-environment approval includes separate operator/owner/migrator/runtime identities; the application SQL, role/grant mechanics, connection integration and policy design below still require validation and scoped approval.

The owner selected the dedicated-instance option after the existing `authority` database's `PUBLIC CONNECT/TEMP` privileges were explained: strict separation on that server would require changing its ACLs. The authorized replacement topology is PostgreSQL 16.15 in a new `authority-pro-postgres` container on `127.0.0.1:5434`, new data/secrets volumes, its own network and limits of 256 MiB / 0.5 CPU. This is environment initialization only, not Flyway approval or implemented tenant logic; the no-business-modules boundary is unchanged.

### Tenant data plane and database privileges

- Give every tenant-scoped row a non-null `tenant_id`. Use tenant-aware unique
  constraints and composite foreign keys so references cannot cross tenants.
- Enable and force RLS on tenant tables. Policies must constrain reads and
  writes using both `USING` and `WITH CHECK`, with no permissive catch-all policy.
- Separate the runtime role from the migration/DDL role. Runtime roles must not
  own protected tables and must have neither superuser nor `BYPASSRLS`
  privileges, nor permission to assume the migrator/owner role or alter policies.
  Keep migration credentials out of the running application.
- Do not implement `app.platform_scope`, a universal RLS bypass, a support-owner
  backdoor, or arbitrary global tenant reads. Auditing a bypass would not make
  it safe. Background work and reporting must run within an explicitly
  authorized tenant, not with a global tenant-data role.

### Identity control plane

Global users and sessions belong to an isolated identity control plane, not to
ordinary tenant tables. Propose separate schemas and narrowly scoped database
roles/grants for authentication, session access and tenant-data access. The
regular tenant-data role must not read credential/session tables.

Login and session resolution use dedicated authentication paths limited to the
submitted identifier or presented session. They must not expose general user
or session enumeration, global read APIs, or unrestricted privileged functions.
Before tenant selection, membership lookup is permitted only after authentication
and only for that authenticated user: set the trusted user context in a
transaction, then apply own-row RLS to membership rows using that user ID. This
path must not grant access to another user's memberships or tenant data.

A supplied tenant identifier is untrusted input. Check the authenticated user's
current active membership before tenant SQL. Membership administration is a
separately authorized, tenant-scoped operation, not an exception that broadens
the pre-tenant own-row policy. Neither an owner label nor a support role bypasses
isolation. Session/provisioning proposals are detailed in ADR-0005.

### Transaction context on the actual connection

For each tenant transaction, the proposed sequence is:

1. Authenticate the actor and validate the requested tenant against current
   membership; any pre-tenant membership transaction uses own-row RLS above.
2. Begin the tenant database transaction and obtain the actual JDBC connection
   that will execute its SQL.
3. On that same connection, after begin and before any tenant query, ORM flush
   or lazy load, execute parameterized `set_config('app.user_id', ..., true)`
   and `set_config('app.tenant_id', ..., true)` with server-validated values.
4. Execute tenant work; commit or roll back. Both settings are transaction-local,
   never persistent session settings.

Prove the transaction/data-access integration in a spike. A request filter or
ThreadLocal alone cannot configure pooled JDBC connections. Do not rely on a
nonexistent before-start transaction synchronization callback. Ensure ORM
connection acquisition and the setter use the same physical connection; prevent
auto-flush/tenant SQL before initialization. Disable out-of-transaction tenant
lazy loading. A new transaction, including `REQUIRES_NEW`, must establish its own
context; nested work may not switch the tenant of an existing transaction.

Policies should use missing-setting-safe lookups such as
`current_setting('app.tenant_id', true)` and explicitly deny absent/empty context.
Validate UUIDs before setting them. Missing context should be rejected at the
application boundary and denied by RLS; malformed values may raise errors, so
"missing context always returns empty results" is not a promised behavior.

## Consequences

A single migration stream can maintain tenant tables and policies. Every data
access path, including async jobs, native SQL and retries, needs transaction
context. Tenant isolation and component ownership are distinct concerns. Large
cross-tenant operational needs require a new narrowly scoped design, not an
emergency global read mechanism.

## Security considerations

RLS is defense in depth, not a defense against arbitrary SQL execution by a
compromised application that can set context. Use parameterized SQL, least
privilege, and server-derived user/tenant values. Review views, functions,
partitions and policy combinations for bypasses; prefer invoker semantics and
do not introduce a broad security-definer escape hatch. Recheck membership on
requests and define concurrent revocation behavior before implementation.

## Validation and tests

Planned, not executed: use real PostgreSQL and production-equivalent runtime
roles, not the table owner. Test cross-tenant reads/inserts/updates/deletes,
foreign keys, joins, native SQL, absent/malformed context, foreign membership
lookup, expired sessions and revocation. Assert role flags, ownership, grants
and inability to assume privileged roles. Test commit/rollback, pooled connection
reuse, errors, retries, async jobs, nested/new transactions and ORM auto-flush;
prove setters run on the actual connection before the first tenant statement.
Check that no global read or support bypass exists.

## Migration implications

Authority Pro application migrations remain unimplemented; the empty local environment and bounded setup-role checks have passed. Initialize the new project database and environment identities only in the authorized dedicated instance after verifying the endpoint and unused names; abort on collisions. Never migrate or restore into the old database, alter its ACLs, reuse its cluster-wide roles or change its server configuration. Leave the other application and concurrent project untouched. Application migrations, constraints and RLS policies still require scoped approval and validation under ADR-0007's proposed migration process; environment initialization does not approve Flyway.
Future tables need the same checks. A later move to per-tenant databases requires
a separate data migration and isolation review.

## Related

- [ADR-0002 — Component boundaries](ADR-0002-modular-monolith.md)
- [ADR-0005 — Sessions and provisioning](ADR-0005-sessions.md)
- [ADR-0006 — Tenant-scoped event delivery](ADR-0006-outbox.md)
- [ADR-0007 — Migrations](ADR-0007-flyway.md)
- [ADR-0011 — API tenant context](ADR-0011-api-versioning.md)
