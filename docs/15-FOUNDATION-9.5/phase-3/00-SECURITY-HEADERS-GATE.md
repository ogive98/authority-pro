# Phase 3 — Security headers qualification (increment 1)

**Decision status:** Implementation of previously unspecified hardening; no new
owner decision.
**Delivery status:** IMPLEMENTED for the local profile.
**Verification status:** TESTED at unit/slice level on 2026-11-28; no database used,
no packaged integration rerun.
**Document validation status:** DRAFT.

## 1. Scope of this increment

Address the application-emitted part of X-07: every response produced by the backend
now carries a fixed hardening set. This does not create the production profile and
does not claim HSTS or a script-restricting CSP.

## 2. Implemented controls

| Header | Value | Applies to |
|---|---|---|
| `X-Frame-Options` | `DENY` | all application responses |
| `Content-Security-Policy` | `default-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'` | all application responses (JSON/API only) |
| `X-Content-Type-Options` | `nosniff` | all application responses |
| `Referrer-Policy` | `no-referrer` | all application responses |
| `Permissions-Policy` | `camera=(), geolocation=(), microphone=(), payment=(), usb=()` | all application responses |

The filter is registered ahead of the session and problem filters and writes the
headers before the chain runs, so rejected, unauthenticated and error responses carry
them too. A CSP for the Angular document and HSTS remain in ADR-0013: HSTS requires a trusted
HTTPS scheme, which only the production profile can establish.

## 3. Container-terminated TRACE

Tomcat terminates `TRACE` before the servlet filter chain. Its 403 response does not
carry the hardening headers. A dedicated test asserts that it does not echo request
headers and returns the standard problem envelope; this is recorded as a bounded
exception, not as header coverage.

## 4. Files changed

- `backend/src/main/java/com/authoritypro/platform/runtime/infrastructure/http/SecurityHeadersFilter.java`
- `backend/src/main/java/com/authoritypro/platform/runtime/infrastructure/http/HttpSecurityConfiguration.java` (registration)
- `backend/src/test/java/com/authoritypro/http/HttpBoundaryTest.java`

## 5. Evidence

| Check | Command | Result |
|---|---|---|
| HTTP boundary slice | `mvnw -Dtest=HttpBoundaryTest test` | 49 tests, 0 failures |
| Full backend unit suite | `mvnw test` | 182 tests, 0 failures (`55ba979`) |

Executed with the portable Temurin 21.0.13+9 JDK, Maven offline, no
`AUTHORITY_PRO_DATABASE` environment, no container started or restarted. The packaged
integration suite (7 tests) was not rerun in this increment.

## 6. Not proven

- HSTS emission under TLS and trusted proxy headers (ADR-0013).
- A CSP for the Angular document served by the frontend host; the API policy above does not protect it.
- `Secure` session/CSRF cookies in a production profile.
- Runtime scenarios requiring PostgreSQL: distributed throttling. Revocation, nested
  transactions, RLS under pooling and the atomic session cap are proven on a disposable
  database (L05 `checks=47`, `131eb81`).
- Header behavior through the frontend dev proxy or a real reverse proxy.

## 7. Next qualification increments

1. Production profile with fail-closed HTTPS configuration and `Secure` cookies.
2. Full CSP tuned against the built Angular bundle, verified in a browser.
3. PostgreSQL-dependent scenarios from the threat model, on disposable databases,
   under an explicit environment authorization.

## Related documents

- [Security threat model](../phase-2/05-SECURITY-THREAT-MODEL.md)
- [ADR-0013 — Production HTTP profile](../../adr/ADR-0013-production-http-profile.md)
- [Current state](../CURRENT-STATE.md)
