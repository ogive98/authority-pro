# ADR-0011 — API versioning, tenant header and problem details

- **Status:** APPROVED — /api/v1, controlled tenant selection and standardized errors as orientations; bounded L02 local probe/error contract approved on 2026-11-18 and implemented/tested. Tenant headers, identity integration and hosted exposure remain PROPOSED / NOT IMPLEMENTED.
- **Approval basis:** [Bounded owner confirmation](../00-GOVERNANCE/DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations) and [L02 questionnaire answers](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l02-skeleton-decisions--2026-11-18), not blanket approval of later contracts.

- **Record reviewed:** 2026-11-17; this is not an approval date.
- **Accountable owner / approval authority:** Project owner.
- **Application delivery:** L02 local no-database probe/error boundary IMPLEMENTED / TESTED only; see [execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18). No functional identity/tenant/inventory API, frontend or release is delivered.

## Context

The future Angular frontend and Spring Boot backend need an explicit, testable
HTTP contract. Tenant selection must not become authorization. No API code
existed at the initial decision; current L02 delivery is limited to the local
probe/error boundary below, with no functional APIs or business modules. Tenant
examples remain proposals. Future browser authentication follows the session
design in ADR-0005, not a custom bearer-token protocol.

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

The owner initially approved `/api/v1`, controlled tenant selection and standardized errors. The L02 section below subsequently approves the local probe policy and RFC 9457 envelope only for the bounded skeleton. `X-Tenant-Id`, tenant UUID/status mappings, session integration and hosted exposure remain proposals; illustrative future responses are not available routes.

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

Return `application/problem+json`. The envelope approved for L02 below, and proposed for later integrated APIs, includes
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

## L02 local HTTP approval — 2026-11-18

The [owner selected the local closed skeleton](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l02-skeleton-decisions--2026-11-18). Approval covered the following L02 contract, not hosted exposure, tenant/header conventions or a login flow. Its subsequent implementation/test evidence is linked above; approval alone was not delivery.

| Boundary | Approved L02 behavior |
|---|---|
| Activation/listener | Explicit `skeleton-local` mode; bind only `127.0.0.1`; port 8080 only if available. Fail on collision, never terminate another service or fall back to a public address |
| Anonymous allowlist | GET `/actuator/health/liveness` and GET `/actuator/health/readiness` only, on that local listener; no wildcard `/actuator/**`, alternate management listener or public proxy |
| Probe semantics | Status-only JSON; liveness 200/UP only when the process is live; readiness 503/OUT_OF_SERVICE while the nucleus cannot serve protected application traffic. No database/network checks or tenant queries |
| Other requests | Deny without executing application handlers. No generated default user, HTTP Basic, form login, session cookie, public bootstrap, administrative operation or permissive CORS |
| CSRF | Retain protection for unsafe methods; denying all mutations is not authorization to disable CSRF. No CSRF/session bootstrap route is introduced in L02 |
| Application errors | The RFC 9457 envelope described above, including `code` and server-generated opaque `traceId`, for filter/controller failures; HTTP and body status agree, no raw input or secret leakage |
| OpenAPI | springdoc 3.2.0 in test/build scope only; no Swagger UI or OpenAPI endpoint in the serving artifact. The generated local contract describes only actual skeleton behavior, never future available login/tenant routes |

Typed startup configuration is validated before listening. Safe packaged settings plus narrowly allowed environment overrides must not permit an unknown mode, non-loopback binding, weakened security or an unexpected property source to open the application. Reject unknown application keys/invalid values with a safe category, not echoed values. Command-line/remote security overrides and other applications' configuration/secret files are outside this mode. This is not a generic configuration service or a selectable production authentication bypass.

The health responses are Actuator status contracts, not Problem Details errors: an expected readiness 503 has a minimal health status body. Problem Details covers rejected application/security requests and unexpected failures; unavailable probes must not be rewritten as healthy. No authentication-success path or fake principal is needed to test the denial boundary.

L02 clean verification passed 65 unit tests and 4 packaged integration tests with no failures or skips, covering exact route/method matching, denial outside the two GET paths, retained CSRF, no session/default user, invalid configuration and occupied-port refusal, RFC 9457 redaction and no JDBC/Flyway/session initialization. Real main startup was exercised on loopback port 8080; controller-error fixtures remain test-only and OpenAPI is excluded from the production JAR. These are bounded executed skeleton checks, not later identity/tenant acceptance; the offline clean repeat also passed.

The existing identity, tenant-selection, pagination and protected-resource mappings remain proposed for their later slices. Once those capabilities exist, changing readiness and exposing their routes requires their own integrated acceptance; this local exception never opens them automatically. Production network/TLS/authorization rules and all other ADR-0011 decisions remain gated.

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

L02 probe, denial, RFC 9457 and packaged startup checks have the bounded passing
evidence above. Still planned, not executed: functional `/api/v1` routing, tenant
header validation, missing/expired sessions, unauthorized tenants and per-request
membership revocation. Retest the error contract in those integrated paths and
exercise login/logout and unsafe APIs with Angular CSRF handling. Later readiness
must be tested against actual database/dependency failure without widening probe
exposure. Tenant cache separation remains untested.

## Migration implications

No deployed API or clients need migration. The bounded L02 local HTTP/security
contract is approved above; later integrated OpenAPI/security contracts still
require approval before implementation. Future breaking changes require
parallel-version/deprecation planning and compatible clients; a later M2M API
must be designed separately rather than weakening session CSRF. Probe changes
require coordinated deployment configuration, not application URI versioning.

## Related

- [ADR-0001 — Stack](ADR-0001-stack.md)
- [ADR-0003 — Tenant context and own-row membership](ADR-0003-multi-tenancy.md)
- [ADR-0005 — Browser sessions, CSRF and provisioning](ADR-0005-sessions.md)
- [ADR-0006 — Independent event compatibility](ADR-0006-outbox.md)
- [ADR-0010 — Framework/toolchain verification](ADR-0010-java-spring-versions.md)
