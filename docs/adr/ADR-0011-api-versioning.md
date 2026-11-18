# ADR-0011 — API versioning, tenant header and problem details

- **Status:** APPROVED — /api/v1, controlled tenant selection and standardized errors as orientations. Exact tenant header, RFC 9457 fields/codes, routes and probe exposure remain PROPOSED; nothing is implemented.
- **Approval basis:** [Bounded owner confirmation](../00-GOVERNANCE/DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations), not blanket approval of the detailed contracts below.

- **Record reviewed:** 2026-11-17; this is not an approval date.
- **Accountable owner / approval authority:** Project owner.
- **Application delivery:** NOT IMPLEMENTED; test and release evidence absent.

## Context

The Angular frontend and Spring Boot backend need an explicit, testable HTTP
contract. Tenant selection must not become authorization. Authority Pro has no
API code or business modules; example shapes below illustrate the foundation
proposal only. Browser authentication follows the proposed session design in
ADR-0005, not a custom bearer-token protocol.

## Options considered

1. URI major version `/api/v1` and a per-request `X-Tenant-Id` header — recommended
   for visible API versions and explicit tenant selection.
2. Media-type or custom-header versioning — keeps URLs stable but makes routing,
   tooling and diagnostics less explicit.
3. Tenant in the URI/host, or bound only to the session — possible alternatives;
   session-only selection complicates simultaneous tabs and tenant switching.
4. Unversioned application APIs — minimal initial routing but no explicit major
   compatibility boundary.

For errors, prefer RFC 9457 Problem Details to a bespoke
`code/message/details/traceId` envelope; stable application codes remain extensions.

## Proposed decision

The owner approved `/api/v1`, controlled tenant selection and standardized errors. The remaining recommendations here refine those orientations; the approval does not select `X-Tenant-Id`, a specific UUID/error schema or probe exposure policy.

### Application API and browser security

- Place application APIs under `/api/v1/...`; breaking HTTP contract changes
  require a reviewed new major version and a documented support/deprecation
  window. Additive changes still need consumer compatibility tests.
- Propose `X-Tenant-Id` as a single UUID header on tenant-scoped operations.
  Validate syntax and reject ambiguous/multiple values. It is a requested scope,
  never a trusted identity, grant or instruction to bypass RLS.
- Authenticate the session, resolve current active membership, then initialize
  tenant and user context as described in ADR-0003 before tenant SQL. The Angular
  interceptor may supply the selected tenant but cannot confer authorization.
- For authenticated tenant requests, propose `400` with `TENANT_REQUIRED` when
  absent, `400` with `TENANT_INVALID` when malformed, and the same `403` with
  `TENANT_FORBIDDEN` for unknown or unauthorized tenants. Avoid revealing whether
  another tenant exists. Missing/expired authentication normally produces `401`;
  CSRF rejection produces `403` according to the security filter flow.
- Classify identity/control-plane endpoints explicitly: login/session handling
  does not require a selected tenant. Pre-tenant membership discovery requires
  authentication and own-row RLS, never global reads. No public
  `/auth/bootstrap` is introduced.
- Use the same-origin, HttpOnly/Secure/SameSite session cookie and CSRF protection
  from ADR-0005. Unsafe browser requests, including login and logout, require
  CSRF validation; logout is POST. Cookie-based endpoints do not gain a CSRF
  exception just because they are JSON APIs. M2M authentication remains deferred.

### RFC 9457 errors

Return `application/problem+json`. Propose a consistent contract including
`type`, `title`, `status`, `detail` and `instance`, plus stable `code` and opaque
`traceId` extensions. `status` must agree with the actual HTTP status. `type` is a
stable problem-type URI; `title` describes the type, while `detail` is a safe,
occurrence-specific explanation. `instance` identifies the occurrence/request
without secrets. These are project contract requirements; do not imply RFC 9457
requires every field in every possible implementation.

Illustrative response for an authenticated request missing its tenant header:

```json
{
  "type": "urn:authority-pro:problem:tenant-required",
  "title": "Tenant required",
  "status": 400,
  "detail": "Select a tenant and provide X-Tenant-Id.",
  "instance": "/api/v1/example",
  "code": "TENANT_REQUIRED",
  "traceId": "7f3d05c26a294b51"
}
```

This example defines no implemented endpoint. Error adapters must cover
controller validation as well as authentication, access denial and CSRF failures
in Spring Security filters. Do not expose stack traces, SQL, credentials,
cookies, raw request headers or unauthorized tenant metadata. Clients branch on
stable `type`/`code`, not human-readable `detail` text.

### Management probes are separately configured

Management probes are not application API versions or tenant endpoints. Propose
explicit configuration and verification of the full paths:

- `/actuator/health/liveness`
- `/actuator/health/readiness`

Do not rewrite them under `/api/v1`, abbreviate readiness to a relative path, or
assume they are enabled/exposed by default. Configure probe enablement, management
base path/port, endpoint exposure and access policy separately. Prefer a restricted
management network/port and minimal status-only responses. Any unauthenticated
probe access must be narrowly limited to these read-only paths and deployment
controls, not blanket access to `/actuator/**`.

Liveness should reflect process health rather than transient database failures;
readiness may include dependencies necessary to serve requests. Decide and test
the exact health groups before deployment. Probes need no tenant header and must
not query tenant data through a privileged/global path. They do not disable CSRF
for application or administrative operations.

## Consequences

URI versions simplify routing and diagnostics but still require deprecation and
consumer policy. A tenant header creates a cache dimension: default authenticated
tenant responses to private/no-store behavior and do not enable shared caching
without a reviewed tenant/user-aware key and appropriate `Vary` behavior. HTTP
versioning does not define event payload versions in ADR-0006.

## Security considerations

Test proxy preservation of the tenant header, cookie flags and CSRF headers.
Tenant IDs in headers are not secrets or protection against leakage; minimize
logging. Correlation IDs must be generated or validated server-side, not reflected
as arbitrary untrusted text. Identity, tenant API and management security paths
need explicit matching so probe exposure does not accidentally expose the API.

## Validation and tests

Planned, not executed: contract-test URI routing, header validation, missing/
expired session, unauthorized tenants and per-request membership revocation.
Check `application/problem+json`, every agreed field, HTTP/body status agreement,
`code`/`traceId`, and sanitization for controller and filter errors. Test login,
logout and unsafe APIs with missing/invalid/valid CSRF tokens from Angular.
Exercise both full probe paths with the intended management port/access rules,
no tenant header, minimal output, database failure and application startup;
confirm unrelated Actuator endpoints are not exposed. Test tenant cache separation.

## Migration implications

No deployed API or clients need migration. Approve and document the concrete
OpenAPI/security contract before implementation. Future breaking changes require
parallel-version/deprecation planning and compatible clients; a later M2M API
must be designed separately rather than weakening session CSRF. Probe changes
require coordinated deployment configuration, not application URI versioning.

## Related

- [ADR-0001 — Stack](ADR-0001-stack.md)
- [ADR-0003 — Tenant context and own-row membership](ADR-0003-multi-tenancy.md)
- [ADR-0005 — Browser sessions, CSRF and provisioning](ADR-0005-sessions.md)
- [ADR-0006 — Independent event compatibility](ADR-0006-outbox.md)
- [ADR-0010 — Framework/toolchain verification](ADR-0010-java-spring-versions.md)
