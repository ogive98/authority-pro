# Phase 2 — Contract Catalogue

**Decision status:** PROPOSED envelope and compatibility rules; endpoint descriptions are OBSERVED at `27e9b14`.
**Delivery status:** DESIGN ONLY for envelopes and future operations; the `/api/v1` endpoints exist.
**Document validation status:** DRAFT.

## 1. Sources of truth

- Machine-readable observed contract: [OpenAPI 0.1.0](openapi/authority-pro-v1.yaml).
- Error envelope: RFC 9457 `application/problem+json`, identical shape for all failures.
- Session: cookie `AUTHORITY_PRO_SESSION`, HttpOnly, SameSite Lax, 30 minutes idle,
  8 hours absolute, maximum three concurrent sessions.
- CSRF: readable `XSRF-TOKEN` cookie plus `X-XSRF-TOKEN` header on unsafe methods.

## 2. Error catalogue

| Code | HTTP | Type suffix | Meaning |
|---|---:|---|---|
| AUTHENTICATION_REQUIRED | 401 | authentication-required | No valid session |
| SESSION_EXPIRED | 401 | session-expired | Session or profile no longer valid |
| AUTHORIZATION_DENIED | 403 | authorization-denied | Authenticated but not permitted |
| CSRF_REJECTED | 403 | csrf-rejected | Missing or invalid CSRF header |
| AUTHENTICATION_THROTTLED | 429 | authentication-throttled | Source blocked after repeated failures |
| SCOPE_INVALID | 400 or 403 | scope-invalid | Missing/malformed selector; status differs by endpoint |
| SESSION_STORE_UNAVAILABLE | 503 | session-store-unavailable | Session persistence failure |
| ACCESS_UNAVAILABLE | 503 | access-unavailable | Authorization store failure |
| INVENTORY_UNAVAILABLE | 503 | inventory-unavailable | Catalogue or authorization failure |
| AUDIT_QUERY_INVALID | 400 | audit-query-invalid | Invalid filter, cursor or limit |
| AUDIT_UNAVAILABLE | 503 | audit-unavailable | Audit store failure |

Every problem carries `code`, `instance` (`urn:uuid:...`) and a 32-hex `traceId`. No
SQL, class name, stack trace or rejected input value may appear. `SCOPE_INVALID` is 400
on bootstrap/audit and 403 on authentication-chain refusals; this is an observed
inconsistency to resolve in Phase 3, not a documented target.

## 3. Endpoint contracts

| Operation | Auth | Scope headers | Success | Principal failure modes |
|---|---|---|---|---|
| GET `/api/v1/auth/csrf` | none | none | 204 | none observed |
| POST `/api/v1/auth/login` | none | none | 204 | 400, 401, 403 CSRF, 429, 503 |
| POST `/api/v1/auth/logout` | session | none | 204 | 401, 403 CSRF |
| GET `/api/v1/auth/me` | session | none | 200 profile | 401, 503 |
| GET `/api/v1/access/bootstrap` | session | tenant required; company optional; site rejected | 200 bootstrap | 400, 401, 403, 503 |
| GET `/api/v1/platform/inventory` | session | none | 200 inventory | 401, 403, 503 |
| GET `/api/v1/audit/tenant-records` | session | tenant required; company optional; site rejected | 200 page | 400, 401, 403, 503 |

Payload shapes are in the OpenAPI file; the request/response records in code remain the
normative implementation until generated types are introduced.

## 4. Envelope for future commands, queries, events and jobs

Where a transport needs metadata, it carries: `contractId`, `contractVersion`,
`correlationId`, `causationId`, `operationId`, `actorId` or explicit system actor,
optional tenant scope, occurred/received timestamps, trace context and an idempotency
key for retryable state changes. HTTP may carry the correlation id in a header; the
frontend and backend currently use a request-scoped correlation id for logs only.

## 5. Versioning and compatibility

1. `/api/v1` is the only public version; additive fields are allowed, removals and
   semantic changes require a new version or an expand/contract migration.
2. Error codes are part of the contract; renaming a code is breaking.
3. Permission codes are contract identifiers; removal requires consumer review.
4. The OpenAPI file is validated against observed responses in tests before any
   generated client is adopted.
5. No client may depend on field order or on absence of unknown fields.

## 6. Idempotency

- Reads are naturally idempotent and non-cacheable (`no-store`).
- Login creates a session per call; repeating it is allowed and subject to the session
  cap.
- Logout is safe to repeat but is not guaranteed 204 once unauthenticated.
- Future mutating operations must define an idempotency key before implementation.

## 7. Contract tests required

- Schema validation of every 2xx and problem response against the OpenAPI file.
- A deliberately invalid request rejected with the documented code.
- A stale session producing SESSION_EXPIRED, not AUTHENTICATION_REQUIRED, when the
  cookie is presented.
- CSRF rejection exercised for every unsafe method.
- Cursor paging with an invalid cursor producing AUDIT_QUERY_INVALID.

## Related documents

- [OpenAPI observed contract](openapi/authority-pro-v1.yaml)
- [Module ownership matrix](01-MODULE-OWNERSHIP-MATRIX.md)
- [Frontend/API map](06-FRONTEND-API-MAP.md)
