# ADR-0005 — Browser authentication, sessions and initial provisioning

- **Status:** APPROVED — Spring Security/JDBC browser cookie sessions with CSRF and local operator CLI initial provisioning, never a public setup page. Exact policies, guards, grants and framework integration remain PROPOSED; no implementation exists.
- **Approval basis:** [Bounded owner confirmation](../00-GOVERNANCE/DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations), not blanket approval of every recommendation below.
- **Scope:** Browser access to the technical foundation. Machine-to-machine authentication is separately deferred.

- **Record reviewed:** 2026-11-17; this is not an approval date.
- **Accountable owner / approval authority:** Project owner.
- **Identity/session delivery:** NOT IMPLEMENTED; integrated authentication/session tests and release evidence remain absent. The separate [L02 no-database skeleton is implemented/tested](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18), with CSRF retained but no session store, user or login.

## Context

A user may belong to multiple tenants. Browser sessions need expiration,
revocation, safe credential handling and per-request membership validation.
At the initial decision, Authority Pro had no application code; custom bearer-token
storage was not an existing constraint. The current L02 skeleton still has no
identity/session implementation. Future authentication must operate before tenant
selection without opening a global tenant-data path.

## Options considered

1. **External OIDC provider with Spring Security and local browser sessions.**
   Delegates credentials, federation and possibly MFA to a provider. Adds provider
   availability/configuration, redirect validation and identity-linking concerns.
   OIDC login can still end in a local cookie session; it is not the opposite of
   server-side sessions. Provider tokens should remain server-side.
2. **Local authentication with Spring Security + Spring Session JDBC.**
   Keeps browser sessions revocable in PostgreSQL without adding an identity
   service. Requires responsible password storage, abuse prevention, account
   recovery and session lifecycle management. This is the recommended initial
   foundation candidate, subject to a security/compatibility spike.
3. **Custom opaque bearer tokens or custom JWT/refresh-token storage.**
   Introduces bespoke issuance, rotation, revocation, persistence and client
   storage rules. Browser-readable tokens increase exposure to XSS; JWTs do not
   remove revocation requirements. Not recommended for the initial browser flow.

## Proposed decision

Spring Security with JDBC-backed browser cookie sessions and CSRF, plus local operator CLI provisioning, are approved orientations. The recommended Spring Session JDBC implementation, same-origin deployment, detailed cookie/expiry/guard behavior and framework integration must still be verified against the selected versions. External OIDC remains a possible separately reviewed extension; its addition must preserve the browser session/security contract rather than silently replace the approved orientation.

- Send the opaque session identifier only in an `HttpOnly`, `Secure`, host-only
  cookie with an explicit `SameSite` policy (propose `Lax`, subject to login-flow
  testing) and the narrowest workable path. Do not put authentication tokens in
  localStorage, sessionStorage, URLs or a custom browser bearer header.
- Require HTTPS. Do not enable broad credentialed CORS; same-origin is the
  proposed default. Trust forwarding headers only from configured proxies.
- Keep Spring Security CSRF protection enabled for unsafe requests, including
  login and logout. Logout is an authenticated, CSRF-protected POST, not a GET.
  Provide a narrowly scoped pre-login CSRF-token acquisition path; Angular sends
  the CSRF token in the agreed header. Refresh it after authentication/logout as
  required by the verified framework behavior. A readable CSRF token, if used,
  is distinct from the always-HttpOnly session cookie. SameSite is not a CSRF
  replacement.
- Rotate the session ID on authentication/privilege changes, define idle and
  absolute expiry, and invalidate sessions on logout, account disablement and
  security-sensitive credential changes. Verify role/membership changes against
  current authorization rather than trusting stale session snapshots.
- Store only necessary session attributes. Spring Session's persistence and
  identifier format must be verified; do not claim it hashes identifiers like
  the discarded custom-token design. Restrict session-table and backup access.
- For local credentials, propose a supported adaptive Spring Security
  `PasswordEncoder`, rate limiting and non-enumerating login responses. Recovery
  and MFA policy require review before exposure beyond the foundation.

Global users/sessions are an isolated identity control plane under ADR-0003.
Authentication paths are narrowly scoped; pre-tenant membership queries run as
the authenticated user with own-row RLS. Each tenant request validates current
membership and initializes tenant/user context on its actual JDBC transaction.
An authenticated session is neither a tenant grant nor a platform-wide bypass.

### Initial provisioning recommendation

Initial administrator provisioning is operator-only through a CLI on a secured
deployment environment, closed by default. There is no public `/auth/bootstrap`,
startup auto-provisioning or application endpoint that creates the first admin.
The operator must explicitly enable the operation through a restricted execution
path with short-lived, narrowly scoped provisioning access. Runtime credentials
must not possess that access.

Use a persistent, atomic single-use database guard: lock or conditionally claim
a singleton provisioning record and create the initial identity/assignment in
the same transaction. A unique guard and database constraints must make exactly
one concurrent attempt succeed. Failure rolls back both the guard and identity;
once committed, all replays fail closed. An environment flag, empty-table check
or local file alone is not a concurrency-safe guard. Existing or inconsistent
provisioning state must not silently overwrite an administrator.

Read bootstrap secrets from a protected interactive prompt or a restricted secret
source, never command-line arguments, shell history, logs, source files or CLI
output. Log a non-secret audit result. Recovery after provisioning is a separate,
reviewed operator procedure; resetting the guard is not a runtime backdoor.
Provisioning creates no exemption from tenant RLS.

### Deferred machine-to-machine authentication

M2M identities, credentials, OAuth grants, token formats, scopes and rotation
are unapproved and out of this foundation decision. Do not reuse browser cookies,
add custom bearer tokens, or disable browser CSRF in anticipation of M2M. A later
M2M design needs a separate trust model and explicitly separated security paths.

## Consequences

The session database becomes part of browser availability and requires cleanup,
retention and capacity planning. Standard framework handling reduces bespoke
security code but does not eliminate security review. Same-origin deployment
simplifies browser policy; cross-origin or federated flows need further tests.
The operator CLI adds deployment work but avoids a public bootstrap attack path.

## Security considerations

Protect credentials, cookies and CSRF values from logs, traces and error bodies.
Use generic authentication failures and abuse controls. HttpOnly limits cookie
reading but does not prevent XSS from issuing authenticated requests; output
encoding and a reviewed content-security policy remain necessary. Restrict
session serialization types and evaluate fixation, replay, invalidation and
concurrent-session behavior. No support or owner role receives global data reads.

## Validation and tests

Planned, not executed: verify cookie flags and same-origin Angular CSRF handling;
reject missing/invalid CSRF on login, logout and every unsafe browser endpoint;
verify session rotation, idle/absolute expiry, revocation and multiple instances.
Test identity/membership isolation, forged tenant headers and stale permissions.
Run simultaneous provisioning attempts, rollback/crash/replay cases, closed
mode and already-provisioned cases. Confirm `/auth/bootstrap` is absent and no
secret appears in process arguments, output, audit, logs or repository content.
Test JDBC session schema/serialization against the versions chosen in ADR-0010.

## Migration implications

No live bearer tokens, users or sessions need conversion. Before implementation,
approve the provider choice, verify session schema migrations through ADR-0007,
and review provisioning permissions and recovery. A future OIDC migration needs
identity linking and session invalidation planning; M2M is not implicitly enabled.

## Related

- [ADR-0003 — Identity and tenant isolation](ADR-0003-multi-tenancy.md)
- [ADR-0007 — Session and provisioning schema migrations](ADR-0007-flyway.md)
- [ADR-0010 — Security/session dependency verification](ADR-0010-java-spring-versions.md)
- [ADR-0011 — Browser API contract](ADR-0011-api-versioning.md)
