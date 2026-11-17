# Multi-Tenancy Architecture

**Decision status:** Shared PostgreSQL, tenant_id and RLS APPROVED; detailed policies PROPOSED.
**Implementation status:** NOT IMPLEMENTED.

## Purpose and hierarchy

Protect organizations from one another while supporting different company/site access within an organization.

- Tenant = organization: isolation root, organization-level configuration and commercial entitlements.
- Company: exactly one tenant; users may have different roles in different companies.
- Site: exactly one company and the same tenant; grants may narrow access further.
- User: global identity, no implicit tenant access.
- Membership: active association between a user and a tenant; company/site grants refine permissions.
- Platform owner: technical/commercial operator, not automatically a tenant member or tenant administrator.

The complete hierarchy is specified now. Its management screens and licensing are not silently included in the first foundation slice.

## Pre-tenant identity path

Sessions and accounts belong to a tightly bounded identity control plane. A successfully authenticated principal may discover only their own active membership rows using an own-user policy and validated transaction-local user context. Anonymous requests cannot enumerate organizations or memberships. Only after this lookup can a tenant context be selected.

Global identity tables do not imply a global tenant-data read path. Customer business records and tenant audit records never need a nullable tenant key or a universal bypass to support login.

## Authorized tenant operation

1. Authenticate the session independently of the tenant header.
2. Parse `X-Tenant-Id` as a single UUID and reject malformed/duplicate selection.
3. Confirm active user, tenant and membership; evaluate company/site/resource grants for the operation.
4. Begin the application transaction and bind checked user/tenant identifiers on its actual JDBC connection before tenant SQL.
5. Read/write through the owning module. RLS and composite constraints enforce row boundaries.
6. Commit/rollback and clear request context even after exceptions.

Header values are untrusted selection hints, never authority. Membership revocation must take effect on the next protected operation, not only after the session expires. Asynchronous work obtains a fresh scope and rechecks authorization as required by its command policy.

## Isolation controls

| Surface | Required control |
|---|---|
| Database | Non-owner NOSUPERUSER NOBYPASSRLS runtime; ENABLE/FORCE RLS; USING and WITH CHECK |
| Application | Principal-bound membership checks; explicit scoped transaction execution |
| API | 400 missing/malformed context, 403 inaccessible tenant, resource not-found policy preventing enumeration |
| Company/site | Permission check in addition to tenant RLS; tenant scope alone does not grant all companies |
| Cache/search | Tenant and permission scope in keys and queries, invalidation on grant changes |
| Files | Tenant-aware object references and authorized download, not a guessed path |
| Jobs/events | Tenant and actor/service authority in envelope, no ambient thread reuse |
| Audit | Distinct tenant and platform security streams; no customer data in technical summaries |

Future cache, search, file and worker adapters must satisfy these contracts before activation; they are not implemented today.

## Support and administrator limits

Support access is absent in the foundation. A future grant requires customer authorization, named actor, purpose, scope, expiry, revocation and an audit trail. Technical infrastructure administrators can potentially read storage; RLS does not cryptographically exclude a database superuser. Stronger confidentiality from the host itself would require a separate key-management/threat-model decision.

## Tests and acceptance

Two tenants, two companies per tenant and distinct grants are required fixtures. Test SELECT/INSERT/UPDATE/DELETE using the runtime role; absent context; tenant A then B on the same pool connection; exception rollback; grant revocation; malicious tenant/company/site IDs; own-membership discovery; anonymous membership denial; audit isolation. Validate role flags before tests so a superuser cannot create a false proof.

No tenant restore, schema-per-tenant conversion or offline synchronization is implied by approval of this model. Each needs its own migration and consistency design.

## Related

- [Database architecture](DATABASE-ARCHITECTURE.md)
- [ADR-0003](../adr/ADR-0003-multi-tenancy.md)
- [Identity](../04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md)
- [Validation gates](../13-IMPLEMENTATION/VALIDATION-GATES.md)
