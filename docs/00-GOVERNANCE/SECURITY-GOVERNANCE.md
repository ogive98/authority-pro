# Security Governance

**Decision status:** PROPOSED controls and operating policies; source security
principles and the approved shared PostgreSQL `tenant_id` + RLS strategy remain
binding. See the [approval record](DECISION-MANAGEMENT.md#approval-basis).
**Delivery status:** PLANNED / NOT IMPLEMENTED. No backend/frontend code,
security tests or production-readiness evidence exists.

## 1. Principles

1. Deny by default: endpoints must require authentication unless explicitly
   public after security review. No public setup endpoint is proposed.
2. Authorization must be enforced server-side. Frontend checks are user
   experience, not security.
3. Tenant isolation must be enforced in PostgreSQL and verified by tests.
4. Least privilege applies to users, services and agents.
5. Secrets must not be committed, logged or shipped to browsers.
6. Security-relevant actions require audit; implementation must not silently
   swallow audit failures.

## 2. Socle controls (proposals, not implementations)

| Control | PROPOSED design / validation needed |
|---|---|
| Password storage | Spring Security BCrypt; cost 12 is a candidate only, pending reproducible CPU/concurrency benchmarks |
| Browser sessions | Spring Security + Spring Session JDBC; same-origin session cookies with HttpOnly, Secure outside localhost, and reviewed SameSite policy; CSRF protection for state-changing requests, including login/logout |
| Session lifecycle | Session fixation protection, expiry, logout and administrative revocation; timeout and concurrency policies remain open |
| Bootstrap | Closed by default; explicitly enabled one-time operator CLI, atomic single-use guard and durable audit; no public setup route or browser bootstrap form |
| Transport | HTTPS outside localhost; exact deployment/termination details proposed |
| Authorization | Explicit memberships and permissions; OWNER/ADMIN/MEMBER are candidate names, not approved roles or delivered behavior |
| Tenant model | Business-neutral tenant = organization; companies belong to an organization and sites to a company; company/site authorization must not be flattened into a single tenant role |
| Tenant isolation | `tenant_id` + RLS strategy approved; FORCE RLS and policy mechanics proposed; non-superuser, non-owner, NOBYPASSRLS runtime role separate from migration role |
| Context | Set transaction-local tenant/user context only from authenticated, verified membership and authorization; deny missing/invalid context; test cleanup with pooled connections |
| Audit | Sensitive writes and their durable security audit atomically commit together or the write is rejected; bounded separate transactions for anonymous failures, without secrets |
| Headers | Spring Security defaults, HSTS outside development and sanitized errors; exact header policy to review |
| Rate limiting | Authentication throttling and abuse controls must be designed and tested before exposure beyond controlled development; no production deferral implied |
| Secrets | Runtime environment or secret store; development examples must contain only labelled non-production values |

Do not issue custom bearer session tokens for browser storage. The session
credential must not be exposed in JavaScript, localStorage or sessionStorage.
An Angular-readable CSRF token is distinct from the HttpOnly session cookie;
its exact delivery/integration contract remains PROPOSED. SameSite is not a
replacement for CSRF protection.

## 3. N0 security surface

Changes to the following require security review, an ADR and explicit approval:

- authentication flow, bootstrap and session storage;
- role and permission model, including company/site scoping;
- tenant resolution, runtime database privileges and RLS policies;
- audit durability, access, pipeline and retention;
- secret storage and key management;
- package signing and plugin trust (future vision).

Control-plane operations administer identity and platform metadata through
narrowly authorized contracts. They do not grant tenant business-data access.
Tenant-plane operations require verified tenant context and tenant authorization.
There is no global `app.platform_scope` bypass. Migration credentials must not
be available to web requests or ordinary jobs. Support access, if later approved,
must remain explicit, scoped, time-limited, audited and revocable under RLS.

## 4. Tenant isolation verification (required, not run)

- **Database** — test cross-tenant SELECT/INSERT/UPDATE/DELETE, missing/invalid
  context and attempts to change tenant ownership using the actual restricted
  runtime role. Assert it cannot own protected tables, become a migration role,
  bypass RLS or disable the protection.
- **Application** — repositories and services must reject absent or mismatched
  context. Proposed context is transaction-local and based on verified identity,
  not a client-supplied user id or unchecked tenant header.
- **API** — validate tenant selection against current authorized memberships;
  proposed header/route details are not an approved contract.
- **Cache/search/files** — prove tenant isolation when these capabilities are
  introduced; tenant-prefixed keys/paths alone do not prove authorization.
- **Jobs** — explicitly restore and verify tenant/user authorization for each
  unit of work; no default privileged context.
- **Pooling** — prove tenant/user context cannot leak between transactions,
  including exceptions, rollback and connection reuse.

Any demonstrated cross-tenant leakage blocks acceptance and release. No such
test suite currently exists.

## 5. Audit and secrets

PROPOSED durability policy: a sensitive state change must commit with its durable
security audit in the same transaction, or fail without the change. Optional
external audit export must not replace this durable record. Test audit storage
failure, rollback and retry; no blanket catch-and-continue policy is acceptable.

Anonymous login failures and rejected unauthenticated requests have no successful
state-change transaction. Record them in a separate bounded transaction with
sanitized, minimal data; define rate limits and observable storage-failure handling.
Never upgrade a denial to success if that audit fails. Do not record passwords,
session credentials, CSRF secrets or raw sensitive request bodies. Retention,
redaction and exact event taxonomy remain open decisions.

Production secrets must come from the deployment environment or a secret manager.
Any future development examples must be clearly non-production. A documented,
validated rotation procedure is required before production; none is claimed here.

## 6. Vulnerability management (proposed)

- Plan CI dependency scanning; no pipeline is asserted to run today.
- Block releases with critical vulnerabilities under a reviewed exception policy.
- Produce an SBOM before public distribution.
- Deliver security fixes through the approved release/versioning process.

## 7. Threat modeling

A STRIDE review of the foundation is planned, with updates before any new trust
boundary (plugins, scripts, integrations, AI, offline synchronization). A linked
threat-model document is a specification, not evidence of completed security
review. Security acceptance requires an identified reviewer and findings record.

## 8. Incident response (proposed)

1. Detect and classify (confidentiality, integrity, availability, tenancy).
2. Contain and preserve evidence.
3. Notify the owner and affected parties according to applicable obligations.
4. Remediate, then document root cause and preventive actions.
5. Add regression tests for the failure mode.

## 9. Prohibited patterns

- Reversible/plaintext password storage or logging secrets.
- Authorization implemented only in Angular.
- Arbitrary scripts with server privileges.
- Universal RLS bypass flags or ordinary application access to migration roles.
- Treating platform ownership as permission to read tenant business records.
- Public first-user setup, custom browser bearer session storage, or disabling
  CSRF merely because the application uses cookies or is same-origin.
- Silently allowing sensitive writes after durable audit failure.
- Sharing production credentials through chat or tickets.

## Related documents

- [Platform Constitution](PLATFORM-CONSTITUTION.md)
- [Identity and Access](../04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md)
- [Multi-Tenancy Architecture](../02-ARCHITECTURE/MULTI-TENANCY-ARCHITECTURE.md)
- [Audit Engine](../04-PLATFORM-ENGINES/AUDIT-ENGINE.md)
