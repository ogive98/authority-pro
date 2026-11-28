# Phase 2 — Database Ownership and Migration Map

**Decision status:** PROPOSED policies; schema and grants are OBSERVED in V1–V5.
**Delivery status:** DESIGN ONLY for future migrations. The main database remains at V1.
**Document validation status:** DRAFT.

## 1. Schemas and ownership

| Schema | Owner context | Tables (observed) | RLS |
|---|---|---|---|
| `tenancy` | Tenancy | `tenants`, `companies`, `sites`, `memberships` | FORCE on all four |
| `audit` | Audit | `tenant_records`, `control_plane_records`, `security_attempts` | FORCE on `tenant_records` |
| `identity` | Identity | `users`, `password_credentials`, `provisioning_guard`, `spring_session`, `spring_session_attributes` | none |
| `access` | Access | `roles`, `role_permissions`, `membership_roles` | none |
| `public` | Flyway | `flyway_schema_history` | n/a |

## 2. Roles and privileges

| Role | Purpose | DDL | Data access |
|---|---|---|---|
| `authority_pro_owner` | Table owner; reached only via `SET ROLE` from migrator | yes (as owner) | full |
| `authority_pro_migrator` | Runs Flyway in approved windows | via `SET ROLE` | limited |
| `authority_pro_runtime` | Application runtime | no | SELECT/INSERT/UPDATE/DELETE only where granted; SELECT-only on memberships, credentials and access catalogue |
| `authority_pro_provisioner` | Creates the first operator account | no | bounded provisioning path only |
| `authority_pro_operator` | Bootstrap only | no | never a runtime credential |

Runtime must remain NOSUPERUSER, NOBYPASSRLS and non-owner. The qualification tooling
asserts these flags; Phase 3 adds a concurrency suite.

## 3. RLS policies (observed)

- `tenant_scope` on tenants, companies, sites and `audit.tenant_records`: row visible
  only when its tenant equals `app.tenant_id`, set transaction-locally.
- `own_membership_read/insert/update/delete`: membership rows require
  `app.user_id` and `app.tenant_id` to match.
- `member_tenant_label_read`, `member_company_label_read`: label visibility for a
  member's own tenant/company only.
- Access tables have no RLS; isolation relies on grants plus scoped queries. This is a
  known design choice to re-examine in Phase 3 (risk R-004).

## 4. Migration chain

| Version | Content | Main database | Isolated databases |
|---|---|---|---|
| V1 | tenancy schema, four tables, RLS, grants | Applied | Applied |
| V2 | audit schema and three tables | Not applied | Applied |
| V3 | identity/access schemas and tables | Not applied | Applied |
| V4 | session store and provisioning grants | Not applied | Applied |
| V5 | permission catalogue extension (`platform.inventory.read`) | Not applied | Applied |

Rules: forward-only, never rewrite an applied migration; expand/contract for
compatible change; destructive change requires backup, dry run, compatibility plan and
explicit approval; each module owns its scripts and its tables.

## 5. Decision X-01 — main database level

| Option | Consequence | Risk |
|---|---|---|
| A. Migrate main to V5 in an approved window | Application can run against the main database; unlocks integrated qualification | Writes to the production-like database; needs backup and rollback proof |
| B. Keep main at V1; operate only disposable databases | Zero risk to existing data | No integrated environment; L08 remains blocked |
| C. Rebuild main as a fresh disposable copy | Clean slate | Destroys existing (empty) V1 state and journals; requires explicit destructive approval |

Recommendation: A only after a verified backup and a rehearsed restore; B until then.
No option is executed by this blueprint.

## 6. Data retention and backup hooks

- Audit retention is undecided (U-04); no purge job exists.
- Backup/restore is undocumented in execution terms; Phase 8a will drill it on a
  disposable database before any production claim.
- Session rows expire by idle/absolute policy; cleanup is handled by the session store.

## 7. Tests to add before any migration of the main database

1. Restore drill of the current database into an isolated instance.
2. Flyway `validate` plus a checksum audit of V1–V5.
3. Migration on a copy, followed by the full security suite.
4. Rollback plan reviewed and timed.
5. Owner-signed approval referencing this map.

## Related documents

- [Contract catalogue](02-CONTRACT-CATALOGUE.md)
- [Security threat model](05-SECURITY-THREAT-MODEL.md)
- [Open risks and unknowns](../OPEN-RISKS-AND-UNKNOWNs.md)
