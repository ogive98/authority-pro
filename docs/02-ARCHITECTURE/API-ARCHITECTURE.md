# API Architecture

**Decision status:** `/api/v1`, controlled tenant selection and standardized errors are [owner-approved orientations](../00-GOVERNANCE/DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations). Bounded L02 HTTP/error and test/build-only OpenAPI design is [APPROVED on 2026-11-18](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l02-skeleton-decisions--2026-11-18); tenant/session route details remain PROPOSED. Versioned REST/OpenAPI remain blueprint requirements.
**Implementation status:** NOT IMPLEMENTED. Approval is design only; no route is available or tested.

## Conventions

Approved integrated orientations: `/api/v1` APIs and Spring Security/JDBC-backed browser cookie sessions with CSRF protection; credentials never enter JavaScript storage. These are not L02 session or tenant capabilities. Tenant selection must be independently authorized by the backend. JSON, UUID identifiers, ISO-8601 UTC timestamps and `X-Tenant-Id` as the selector remain PROPOSED tenant/session contract details, as do exact session/CSRF integration and configuration (ADR-0005). L02 uses sanitized errors with server-generated trace identifiers; broader request-correlation transport remains proposed.

## Bounded L02 HTTP design

The [canonical L02 approval](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l02-skeleton-decisions--2026-11-18) and [ADR-0011](../adr/ADR-0011-api-versioning.md) bound explicit `skeleton-local` mode to `127.0.0.1:8080`, only if the port is free; never stop another service.

- Allow only `GET /actuator/health/liveness` (process `UP` when live) and `GET /actuator/health/readiness` (`503 OUT_OF_SERVICE` while the nucleus is incomplete), with status-only bodies and no infrastructure details.
- Deny every other request, including other methods and wildcard Actuator/docs access. Retain CSRF without provisioning sessions; no credentials, account or login capability is introduced.
- Springdoc 3.2.0 is TEST/BUILD ONLY and excluded from the production artifact; no Swagger UI or published OpenAPI endpoint. Pins and toolchain scope belong to [ADR-0010](../adr/ADR-0010-java-spring-versions.md).

## Planned integrated foundation routes (not L02)

| Method | Full path | Scope and behavior |
|---|---|---|
| GET | `/api/v1/auth/csrf` | Obtain CSRF token; no sensitive user data |
| POST | `/api/v1/auth/login` | Framework authentication, CSRF, throttling; session rotation |
| POST | `/api/v1/auth/logout` | CSRF; revoke/invalidate current session |
| GET | `/api/v1/auth/me` | Authenticated user and own authorized scopes only |
| GET | `/api/v1/platform/info` | Authenticated minimal platform version and real component state |
| GET | `/api/v1/audit-events` | Active tenant and `platform.audit.read`; paginated tenant records |

No public `/auth/bootstrap` route. Initial first-administrator provisioning through a local operator CLI is approved; its guard and workflow details remain PROPOSED. No registration, support impersonation, business resource, marketplace or package-execution route belongs to the first slice.

## Errors

L02 approves `application/problem+json` errors with the RFC 9457 shape (`type`, `title`, `status`, `detail`, `instance`) and stable `code`/opaque `traceId` extensions, sanitized without secrets. Denial/validation handling must preserve HTTP/body status agreement across applicable filter/controller paths. Health probes retain status-only health bodies, including readiness 503, not this error envelope. Tenant/session codes, mappings and integration remain PROPOSED; the following tenant example is not an L02 endpoint:

```json
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access to the requested tenant is not permitted.",
  "instance": "/api/v1/audit-events",
  "code": "TENANT_FORBIDDEN",
  "traceId": "example-trace"
}
```

For later integrated routes, proposed mappings are: 400 invalid input/context; 401 unauthenticated session; 403 authorization or CSRF failure; 404 non-visible resources where enumeration must be prevented; 409 optimistic concurrency; 429 throttling with retry guidance; 503 unavailable dependency. Security-filter failures must use the same error format as controller failures. Never return SQL, stack traces, credentials or a confirmation that a login email exists.

## Pagination and compatibility

Proposal: limit default 25/max 100; nonnegative offset with a bounded maximum; stable `(occurred_at desc, id desc)` ordering for audit. Response includes `items`, `total`, `limit`, `offset`. Reject unsupported sort fields via allowlist. Consider cursors before large offsets become operationally costly.

API versions change only for breaking contracts. Deprecation windows remain proposed, not a current service commitment. The approved Springdoc 3.2.0 selection is L02 test/build-only tooling, not a runtime API or proof of generation compatibility. Generated contracts must describe only real surfaces; no invented login, tenant or service API makes the skeleton appear implemented. Later OpenAPI contracts will document approved request, response, authorization, CSRF and error behavior.

## Security and operational behavior

Prefer same-origin frontend/proxy deployment rather than permissive CORS. If cross-origin hosting is approved later, exact origin allowlists and credential handling need review. Rate limiting and bounded request bodies are required for any externally accessible login, not deferred behind a production label. L02 has no login, Swagger or published OpenAPI endpoint; detailed Actuator output remains unavailable.

## Tests and acceptance

L02 requires evidence for the exact probe/method allowlist, status-only bodies, readiness 503, all-other-request denial, retained CSRF without sessions, sanitized error shape and build-only OpenAPI tooling exclusion from the production artifact. No such application tests have run.

Later integrated contract fixtures cover valid/malformed UUIDs, membership revocation, permission denials, CSRF rejection, expired sessions, pagination bounds and stable error media type. Confirm cookie flags and server-session invalidation on logout. No route is documented as available until an integration test proves it.

## Related

- [ADR-0011](../adr/ADR-0011-api-versioning.md)
- [Backend](BACKEND-ARCHITECTURE.md)
- [Frontend](FRONTEND-ARCHITECTURE.md)
