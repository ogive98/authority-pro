# Database Architecture

**Decision status:** Shared tenant_id/RLS strategy and restricted runtime/context orientations APPROVED; the later [closed L03 integration](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l03-closed-application-integration-authorization--2026-11-19) approves its bounded technical subset. Authenticated membership authorization, broader schema/operational policies and future composition details remain separately gated or PROPOSED.
**Implementation status:** Bounded closed L03 JDBC/RLS integration is locally IMPLEMENTED / TESTED; [main recovery evidence](../PACK-INVENTORY.md#l03-main-recovery-evidence--2026-11-20) records V1 with four empty technical tables, strict privilege/catalog checks, a packaged no-op and actual closed runtime HTTP checks. Identity/audit schemas and authenticated membership authorization below remain conceptual or proposed, not implemented. Full clean-machine and production acceptance remain open.

## Dedicated server and new project database

The owner explicitly [approved a separate local instance](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l03-local-environment-authorization--2026-11-18) after the shared-server preflight found PUBLIC CONNECT/TEMPORARY privileges on the old database. "Shared" still means Authority Pro tenants share THEIR project database, not another application's server, database or tables. The `authority_pro` database now contains V1's four empty technical tenancy tables on its own PostgreSQL 16.15 container, exposed only at `127.0.0.1:5434`; see [environment evidence](../PACK-INVENTORY.md#l03-local-environment-evidence--2026-11-18). All pre-existing databases, users, service configuration, data directories, volumes and concurrent projects remain outside the mutation boundary.

Follow the [safe provisioning boundary](DEPLOYMENT-ARCHITECTURE.md#safe-provisioning-boundary): confirm endpoint/version and operator authority, abort on database/role name collisions, and positively verify the target before any migration. PostgreSQL roles are cluster-wide; create only separately approved dedicated roles, never alter existing roles or global privileges. Review inherited/PUBLIC access without mutating the old application's security; if stronger isolation needs existing changes, stop for approval. Separate database names alone do not prove workload or privilege isolation.

Only the new cluster's ACLs were restricted during environment setup; no existing server was changed or restarted. The guarded Compose preparation does not introduce application migrations or alter the L02 no-database backend. Do not start or configure another project's services.

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

## Future dynamic fields and model development

**PROPOSED / NOT IMPLEMENTED.** The [documentation-only authorization](../00-GOVERNANCE/DECISION-MANAGEMENT.md#visual-composition-documentation-authorization--2026-11-20), decisions O20–O25 and [coverage refinement](../PACK-INVENTORY.md#visual-composition-coverage-refinement) do not authorize schema execution or change approved database privileges.

The baseline remains owner-managed relational tables, explicit relationships, constraints and reviewed migrations. Future dynamic fields or JSONB may be used only within a bounded, versioned schema owned by the resource provider. Define allowed keys/types, required fields, cardinality, size/depth limits and server-side validation before persistence; unknown or incompatible fields are rejected. JSONB is not a permission-free escape from relational integrity or an unrestricted business-data bag.

Each such design must specify indexing and supported query/filter/sort patterns, query cost and result-size budgets, tenant/company/site authorization, RLS and tenant-safe references, sensitivity, retention/deletion and legal-hold behavior. JSONB contents must not hide foreign-resource access, secrets or unbounded history. Schema evolution, backfill and old/new reader compatibility need owner review and measurable acceptance evidence, including negative validation, isolation and query-budget tests.

There is no universal EAV store, table-per-node scheme or runtime DDL. A visual node, UI field, setting key or UAM publication cannot create/alter tables, APIs or privileges. Model development is separate from [configuration values and UI schemas](../03-CORE/CONFIGURATION-ENGINE.md#settings-interface-schemas-and-generated-models).

If a future data-model compiler is approved, it produces reviewable versioned migration artifacts through a controlled development/build pipeline. Approval covers ownership, generated diff, tenant constraints/RLS, indexes, compatibility, backfill, failure/recovery and clean/upgrade tests before the deployment migrator applies anything. The runtime credential never receives DDL or migration authority. UI configuration publication remains a separate gate and must block on missing approved schema prerequisites; it cannot silently run generated SQL.

## Migrations and recovery

Flyway is approved for the [bounded closed L03 integration](../adr/ADR-0007-flyway.md); broader deployment/upgrade workflows remain proposed. Use one deterministic migration order in the monorepo, with ownership recorded per migration. Do not reserve arbitrary module number ranges as a runtime install mechanism. Hibernate may validate mappings but never auto-update production schema.

Prefer forward expand/backfill/contract migrations. A database rollback is not automatically possible when rolling back the application. Test clean creation and previous-version upgrade; test restoration of a whole backup before trusting it. Selective tenant restore requires separate referential-integrity, event-replay, shared-identity and file-consistency procedures; it is not provided by a shared database dump.

## Security, failure and acceptance

No committed password default or runtime superuser credential. Failed policy checks roll back the operation. Audit/retention jobs use separately approved roles, not runtime DELETE privileges on append-only records. Retention and legal holds require jurisdiction decisions before production.

Tests must cover all four DML operations, missing context, wrong company/site foreign keys, pooled connection reuse, effective role privileges, migration failure and restore validation. [Isolated L03 qualification](../PACK-INVENTORY.md#l03-isolated-qualification-evidence--2026-11-19) already supplies bounded scratch evidence for these technical areas; it is not authenticated application integration or a main migration baseline. Main-application acceptance remains open, including current membership/permission enforcement and durable runtime writes. Future model-development checks also follow [visual-composition acceptance](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md#visual-composition-acceptance); no new test execution is claimed by this documentation refinement.

## Related

- [Multi-tenancy](MULTI-TENANCY-ARCHITECTURE.md)
- [Migration ADR](../adr/ADR-0007-flyway.md)
- [Audit](../04-PLATFORM-ENGINES/AUDIT-ENGINE.md)
