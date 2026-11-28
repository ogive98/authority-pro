# ADR-0013 — Production HTTP profile

- **Status:** PROPOSED — no production profile exists (risk X-02, X-07).
- **Implementation note (2026-11-28):** framing, `nosniff`, referrer, permissions and
  `frame-ancestors` headers are now emitted on every application response and tested
  in isolation; HSTS, script-restricting CSP, Secure cookies and proxy trust remain
  unimplemented. See
  [Phase 3 headers gate](../15-FOUNDATION-9.5/phase-3/00-SECURITY-HEADERS-GATE.md).
  The status above stays PROPOSED for the production profile as a whole.
- **Scope:** HTTP transport, cookie flags, security headers and proxy trust for a
  production deployment. Local development behaviour is unchanged.
- **Risk addressed:** R-007 (cookie not Secure), X-07 (security headers absent).

## Context

The local profile sets `UseSecureCookie=false`, binds same-origin, and emits only
`Cache-Control: no-store` and `X-Content-Type-Options: nosniff`. There is no HSTS,
Content-Security-Policy, frame protection, Referrer-Policy or Permissions-Policy, and
no declared rule for trusting proxy headers. A production deployment behind TLS
termination would otherwise ship a non-Secure session cookie and rely on luck for
response hardening.

## Options considered

1. **Application terminates TLS.** Simplest cookie story, but certificate lifecycle
   and key storage move into the application container; unusual for typical hosting.
2. **TLS terminated at a reverse proxy with a production Spring profile.** The app
   trusts a configured proxy for `Forwarded`/`X-Forwarded-*`, sets Secure cookies,
   and emits its own headers. Recommended.
3. **Headers emitted only by the proxy.** Fewer application changes, but the app can
   be misconfigured behind a proxy that forgets them, and tests cannot assert them
   at the application boundary.
4. **No production profile yet.** Rejected: the gap is already recorded as a risk.

## Proposed decision

Adopt option 2, with the application also emitting the headers (defence in depth).

- The production profile must refuse to start unless an HTTPS public URL is
  configured; there is no silent fallback to an insecure cookie.
- Session and CSRF cookies are `Secure`, `HttpOnly` (session), `SameSite=Lax`, with
  the existing host-only scope.
- Emit `Strict-Transport-Security` with a bounded max-age, `Content-Security-Policy`
  compatible with the built Angular bundle, `X-Frame-Options: DENY` plus
  `frame-ancestors 'none'`, `Referrer-Policy: no-referrer`, and a restrictive
  `Permissions-Policy`.
- Trust forwarding headers only from explicitly configured proxy addresses; reject
  or ignore them otherwise. Never derive the Secure flag or redirect targets from an
  untrusted `X-Forwarded-Proto`.
- Keep same-origin deployment; no credentialed wildcard CORS.
- The local profile keeps its current flags and must be visibly marked as local in
  configuration, so the two profiles cannot be confused.

## Consequences

Production configuration gains mandatory settings and a fail-closed startup check.
CSP must be tuned against the actual bundle: inline styles or scripts introduced later
will break and must be fixed rather than weakening the policy. Header tests belong in
the HTTP slice tests, not only in deployment checks.

## Security considerations

HSTS is only meaningful over HTTPS and must not be sent on plain HTTP. CSP is a
mitigation, not a replacement for output encoding. Proxy trust mistakes are a common
source of auth bypass; the allow-list is mandatory. No header may echo request data.

## Validation and tests

1. Startup fails when the production profile lacks an HTTPS public URL.
2. Cookie flags observed on login/logout/CSRF responses in the production profile.
3. Header presence and values asserted on API and static responses.
4. Forwarded headers from an unlisted source are ignored.
5. Angular application loads and authenticates under the CSP without console errors.
6. Local profile unchanged: existing tests still pass.

## Migration implications

Deployment documentation and environment templates must list the new mandatory
settings. No database change. Rollout must be rehearsed on a disposable deployment,
never on the production host.

## Related

- [ADR-0005 — Sessions](ADR-0005-sessions.md)
- [ADR-0012 — Atomic session cap](ADR-0012-atomic-session-cap.md)
- [Security threat model](../15-FOUNDATION-9.5/phase-2/05-SECURITY-THREAT-MODEL.md)
