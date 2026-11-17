# Database Architecture

**Decision status:** Shared PostgreSQL with tenant_id and RLS APPROVED; schema and role design PROPOSED.
**Implementation status:** NOT IMPLEMENTED. Table names below are conceptual.

## Ownership and data boundaries

| Owner | Records | Isolation |
|---|---|---|
| Identity | Global account, password credential, framework JDBC session | Control plane; identity-owned access, no tenant-data privileges implied |
| Tenancy / Access | Tenancy owns membership; Access owns company/site grants | Own-user lookup before tenant selection; administrative access only with checked tenant permission |
| Tenancy | Tenant/organization, company, site | Tenant records and child scopes protected by RLS |
| Audit | Tenant security/administrative events | Non-null tenant_id, RLS, runtime INSERT/SELECT only |
| Audit | Pre-authentication/platform security events | Separate control-plane table, no tenant impersonation or public read |
| Migration runner | Schema history, versioned schema changes | Separate migration credential, unavailable to application runtime |

The tenant is the organization isolation root; a company belongs to exactly one tenant and a site to exactly one company. Relationships between a company's site and the tenant use composite keys so a foreign identifier cannot cross tenants. Cross-module foreign keys require a documented lifecycle dependency and do not permit repository access across modules.

## Database roles (proposal)

- Provisioning administrator: operator-controlled creation of database and roles, never an application credential.
- Migrator/table owner: DDL and reviewed migrations during deployment, not during ordinary requests.
- Runtime: LOGIN, NOSUPERUSER, NOBYPASSRLS, no schema creation, no role membership that can become the owner; only specific required DML and sequence grants.
- Backup/restore: separate operational privileges, never handed to a browser or application request.

`FORCE ROW LEVEL SECURITY` does not constrain superusers or BYPASSRLS roles. All isolation tests must assert the effective role flags and ownership before testing policies.

## Tenant policy baseline

Illustrative predicate for ordinary tenant rows (not a complete migration):

```sql
USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
```

ENABLE and FORCE RLS, appropriate operation grants, indexes and a NOT NULL tenant key are also required. Missing context denies reads/writes. The identity membership policy is distinct: SELECT only the authenticated user identified by transaction-local `app.user_id`; tenant administrators need an explicitly reviewed scoped management policy. No `OR app.platform_scope` universal bypass is permitted.

Context variables defend against accidental missing filters. Because the application can set them, they are not a protection against an attacker fully controlling the runtime process. Backend membership checks and injection prevention remain required.

## Field conventions

Use UUID technical IDs; UTC instants for timestamps and locale-independent dates for date-only concepts. Add `version` only to mutable aggregates needing optimistic concurrency. Append-only audit rows do not need `updated_at` or an optimistic version. Soft deletion is domain-specific, not global.

Unique keys include tenant scope where relevant; global account login identifiers are an explicit exception. Normalize login identifiers once with a documented policy. Index tenant filters and stable `(occurred_at, id)` audit ordering. Credentials and framework session data must never be serialized by public DTOs.

## Migrations and recovery

Flyway is a proposal in ADR-0007. Use one deterministic migration order in the monorepo, with ownership recorded per migration. Do not reserve arbitrary module number ranges as a runtime install mechanism. Hibernate may validate mappings but never auto-update production schema.

Prefer forward expand/backfill/contract migrations. A database rollback is not automatically possible when rolling back the application. Test clean creation and previous-version upgrade; test restoration of a whole backup before trusting it. Selective tenant restore requires separate referential-integrity, event-replay, shared-identity and file-consistency procedures; it is not provided by a shared database dump.

## Security, failure and acceptance

No committed password default or runtime superuser credential. Failed policy checks roll back the operation. Audit/retention jobs use separately approved roles, not runtime DELETE privileges on append-only records. Retention and legal holds require jurisdiction decisions before production.

Tests must cover all four DML operations, missing context, wrong company/site foreign keys, pooled connection reuse, effective role privileges, migration failure and restore validation. No such tests have run yet.

## Related

- [Multi-tenancy](MULTI-TENANCY-ARCHITECTURE.md)
- [Migration ADR](../adr/ADR-0007-flyway.md)
- [Audit](../04-PLATFORM-ENGINES/AUDIT-ENGINE.md)
