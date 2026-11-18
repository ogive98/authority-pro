# API Architecture

**Decision status:** `/api/v1`, controlled tenant selection and standardized errors are [owner-approved orientations](../00-GOVERNANCE/DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations); exact transport and error contracts remain PROPOSED. Versioned REST/OpenAPI are required by the blueprint.
**Implementation status:** NOT IMPLEMENTED. All routes below are proposed contracts.

## Conventions

Approved orientations: `/api/v1` APIs and Spring Security/JDBC-backed browser cookie sessions with CSRF protection; credentials never enter JavaScript storage. Tenant selection must be independently authorized by the backend. JSON, UUID identifiers, ISO-8601 UTC timestamps and `X-Tenant-Id` as the selector remain PROPOSED contract details, as do exact session/CSRF integration and configuration (ADR-0005). Request identifiers are proposed to be server-generated or strictly bounded and validated before logging.

## Planned foundation routes

| Method | Full path | Scope and behavior |
|---|---|---|
| GET | `/api/v1/auth/csrf` | Obtain CSRF token; no sensitive user data |
| POST | `/api/v1/auth/login` | Framework authentication, CSRF, throttling; session rotation |
| POST | `/api/v1/auth/logout` | CSRF; revoke/invalidate current session |
| GET | `/api/v1/auth/me` | Authenticated user and own authorized scopes only |
| GET | `/api/v1/platform/info` | Authenticated minimal platform version and real component state |
| GET | `/api/v1/audit-events` | Active tenant and `platform.audit.read`; paginated tenant records |
| GET | `/actuator/health/liveness` | Restricted deployment probe, minimal status |
| GET | `/actuator/health/readiness` | Restricted deployment probe, no public infrastructure details |

No public `/auth/bootstrap` route. Initial first-administrator provisioning through a local operator CLI is approved; its guard and workflow details remain PROPOSED. No registration, support impersonation, business resource, marketplace or package-execution route belongs to the first slice.

## Errors

Standardized errors are an approved orientation. PROPOSED format: `application/problem+json` with RFC 9457 fields and stable `code`/`traceId` extensions; exact fields, status mappings and filter/controller integration remain unapproved details. Example only:

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

400 covers invalid input/context; 401 unauthenticated session; 403 authorization or CSRF failure; 404 non-visible resources where enumeration must be prevented; 409 optimistic concurrency; 429 throttling with retry guidance; 503 unavailable dependency. Security-filter failures must use the same format as controller failures. Never return SQL, stack traces, credentials or a confirmation that a login email exists.

## Pagination and compatibility

Proposal: limit default 25/max 100; nonnegative offset with a bounded maximum; stable `(occurred_at desc, id desc)` ordering for audit. Response includes `items`, `total`, `limit`, `offset`. Reject unsupported sort fields via allowlist. Consider cursors before large offsets become operationally costly.

API versions change only for breaking contracts. Deprecation windows are proposed in governance, not a current service commitment. OpenAPI documents request, response, authorization, CSRF and error contracts; generator/library compatibility needs verification before selecting a version.

## Security and operational behavior

Prefer same-origin frontend/proxy deployment rather than permissive CORS. If cross-origin hosting is approved later, exact origin allowlists and credential handling need review. Rate limiting and bounded request bodies are required for any externally accessible login, not deferred behind a production label. Swagger and detailed Actuator output are not anonymous production APIs.

## Tests and acceptance

Contract fixtures cover valid and malformed UUIDs, membership revocation, permission denials, CSRF rejection, expired session, all pagination bounds and stable error media type. Confirm cookie flags and that logout invalidates the server session. No route is documented as available until an integration test proves it.

## Related

- [ADR-0011](../adr/ADR-0011-api-versioning.md)
- [Backend](BACKEND-ARCHITECTURE.md)
- [Frontend](FRONTEND-ARCHITECTURE.md)
