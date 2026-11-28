# Phase 2 — Frontend / API Integration Map

**Decision status:** PROPOSED integration rules; current screens are OBSERVED.
**Delivery status:** DESIGN ONLY for generated contracts and E2E; the screens exist.
**Document validation status:** DRAFT.

## 1. Screen to endpoint map

| Screen | Endpoints used | States implemented | Permission dependency |
|---|---|---|---|
| Login | GET `/auth/csrf`, POST `/auth/login` | idle, invalid, throttled, unavailable | none |
| Scope selection (tenants) | GET `/auth/me` | loading, empty memberships, error | membership list only |
| Shell (post-selection) | GET `/access/bootstrap` | loading, forbidden, unavailable, ready | validated scope |
| Overview | bootstrap data, GET `/platform/inventory` | loading, empty, error | `platform.inventory.read` |
| Inventory | GET `/platform/inventory` | loading, empty, filter/sort, error | `platform.inventory.read` |
| Audit | GET `/audit/tenant-records` | loading, empty, cursor end, error | audit read permission |
| Preferences | none (local state) | ready | none |
| Not found | none | ready | none |

The frontend renders only what these endpoints return; it never derives permissions
from role names and never treats UI visibility as authorization.

## 2. Session and scope lifecycle

1. On load, the app fetches the CSRF cookie, then probes `/auth/me`.
2. Unauthenticated users are routed to login; authenticated users choose a membership.
3. Selecting a scope stores the tenant/company identifiers and calls bootstrap.
4. Every scoped request sends `X-Authority-Tenant` and, when selected,
   `X-Authority-Company`.
5. Changing scope cancels or ignores late responses from the previous scope; the
   in-flight guard must be tested against a live server.
6. Logout clears scope state and returns to login, even if the backend call fails.

## 3. Error handling rules

| Backend code | UI behavior |
|---|---|
| AUTHENTICATION_REQUIRED / SESSION_EXPIRED | clear session state, return to login with a neutral message |
| AUTHORIZATION_DENIED | show forbidden state; do not retry automatically |
| CSRF_REJECTED | renew CSRF token once, then surface failure |
| AUTHENTICATION_THROTTLED | show bounded retry guidance; no automatic retry |
| SCOPE_INVALID | return to scope selection |
| *_UNAVAILABLE | show unavailable state with retry action |

Problem `traceId` may be displayed as a support reference; it must never expose server
details.

## 4. Contract typing gap

The frontend uses hand-written TypeScript types. The OpenAPI file is not yet used to
generate or validate them. Before adding a second consumer, introduce a generated or
schema-validated client so that API drift fails a test.

## 5. Accessibility and localization

All new screens must ship with: keyboard navigation, visible focus, translated copy in
FR/EN/IT/AR, logical CSS properties for RTL, and error states that remain readable at
small widths. Preferences persist theme, density and language locally.

## 6. Testing gaps

- No E2E runner and no frontend CI job; unit tests cover components and services only.
- Authenticated journeys have never been observed against a live server (E0).
- Late-response tenant switching is implemented but not proven against real latency.

## 7. Phase 3/9 additions (proposed)

1. Contract validation test: parse the OpenAPI file and assert fixtures match.
2. Browser smoke: login → scope → overview → inventory → audit → logout, with one
   deliberate 403 and one 503 path.
3. Frontend job in CI: install, build, unit tests, lint (to be added).
4. Accessibility scan on public and authenticated states where feasible.

## Related documents

- [Contract catalogue](02-CONTRACT-CATALOGUE.md)
- [Target blueprint](00-TARGET-BLUEPRINT.md)
- [Acceptance and packages](07-ACCEPTANCE-AND-PACKAGES.md)
