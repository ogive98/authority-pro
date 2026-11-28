# Phase 2 — Security Threat Model

**Decision status:** PROPOSED threat analysis; no risk is accepted or closed here.
**Delivery status:** DESIGN AND PLANNING ONLY. Tests listed are not executed.
**Document validation status:** DRAFT.

## 1. Assets and trust boundaries

| Asset | Sensitivity | Where it lives |
|---|---|---|
| Credentials and password hashes | Critical | `identity.password_credentials` |
| Sessions | Critical | `identity.spring_session*`, browser cookie |
| Tenant data and membership | Critical | `tenancy` schema |
| Authorization catalogue | High | `access` schema |
| Audit records | High | `audit` schema |
| Configuration and secrets | Critical | process environment, DPAPI file, future vault |

Boundaries: browser ↔ HTTP process; process ↔ database; operator ↔ provisioning CLI;
future process ↔ external integrations. The browser and all request headers are
untrusted; the database is trusted only through the restricted runtime role.

## 2. STRIDE by context

| Context | Threat | Existing control | Gap | Planned test |
|---|---|---|---|---|
| Identity | Spoofing: credential stuffing | bcrypt verification, per-source throttle (5/15 min) | in-memory, per instance, resets on restart | Multi-instance and restart behavior test (X-08) |
| Identity | Session hijacking | HttpOnly, SameSite Lax, 30 min idle, 8 h absolute, id rotation at login | `Secure=false`; no TLS profile | Cookie/tag assertions under production profile (R-007) |
| Identity | Concurrent session abuse | Cap of three, oldest evicted | advisory-lock cap, lock-timeout fail-closed and rotation proven 2026-11-28 (L05 `checks=42`) | No cap test outstanding; distributed throttle remains (X-08) |
| Identity | CSRF | CSRF cookie + header on unsafe methods | none observed | Negative CSRF tests per unsafe route |
| Tenancy | Cross-tenant read/write | RLS FORCE, transaction-local context, own-membership policies | pool reuse, rollback, nested `REQUIRES_NEW` isolation with a refused joined scope switch, cross-tenant write denial and four simultaneous tenant-scoped transactions proven 2026-11-28 (L05 `checks=47`, `131eb81`) | No RLS test outstanding; load-level concurrency not measured |
| Access | Privilege escalation | Deny-by-default, DB-resolved permissions, scope validated per request | scope-invalid status inconsistency (400 vs 403) | Contract test for every route |
| Access | Stale permission after revocation | revision returned in bootstrap; authorization reads join `identity.users` with `status = 'active'` | proven 2026-11-28 (L05 `checks=46`, `55ba979`): a disabled account loses profile, scope and audit grant, a disabled membership loses its scope, and `/auth/me` ends the session on an empty profile | Session-store purge after revocation not implemented; multi-instance caches untested |
| Audit | Tampering / deletion | runtime granted INSERT/SELECT only on audit tables | no tamper evidence; retention undecided | Privilege and integrity test (R-008) |
| Audit | Sensitive metadata leak | bounded metadata, redaction expected | redaction not proven | Adversarial metadata test |
| Inventory | Unauthorized catalogue read | `platform.inventory.read` resolved from DB | none observed | Filter bypass attempts |
| Runtime | Configuration override | strict environment allowlist, fixed properties | production profile absent | Negative startup tests |

## 3. HTTP hardening gaps

| Control | Current (2026-11-28) | Target (PROPOSED) |
|---|---|---|
| Content-Security-Policy | API: `default-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'` | strict policy for the Angular document; no inline script |
| Strict-Transport-Security | absent | only under TLS, with a reviewed max-age |
| X-Frame-Options / frame-ancestors | `DENY` / `frame-ancestors 'none'` on all application responses | deny framing (met; TRACE is container-terminated) |
| Referrer-Policy | `no-referrer` on all application responses | `no-referrer` (met) |
| Permissions-Policy | unused features denied on all application responses | deny unused features (met) |
| X-Content-Type-Options | `nosniff` on all application responses | present on all responses (met) |
| Cache-Control | `no-store` on authenticated payloads | keep, test every route |

## 4. Threat scenarios that must pass before any production claim

1. Tenant A cannot read or write Tenant B data via API, restricted role or reused pool connection.
2. Rollback, exception and nested-transaction paths clear tenant context.
3. Four concurrent logins never leave more than three active sessions.
4. CSRF is rejected without the header for login and logout.
5. Production cookie flags are asserted (`Secure`, `HttpOnly`, `SameSite`).
6. Revoked membership blocks subsequent bootstrap and audit calls deterministically.
7. Ordinary runtime credentials cannot UPDATE or DELETE audit rows.
8. Audit metadata is redacted and bounded; no secret appears in logs.
9. Login throttling behavior is documented per instance and bounded in memory.
10. Security headers are present on every response, including problems. Partial:
    application responses are covered and tested; TRACE is container-terminated and
    the Angular document CSP/HSTS remain open (see
    [headers gate](../phase-3/00-SECURITY-HEADERS-GATE.md)).

## 5. Explicit non-goals

No claim of protection against a database superuser or host administrator; RLS is not
cryptography. No WAF, DDoS protection, MFA, SSO or external identity provider in this
programme.

## Related documents

- [ADR-0012 atomic session cap](../../adr/ADR-0012-atomic-session-cap.md)
- [ADR-0013 production HTTP profile](../../adr/ADR-0013-production-http-profile.md)
- [Database ownership map](03-DB-OWNERSHIP-AND-MIGRATION-MAP.md)
- [Open risks and unknowns](../OPEN-RISKS-AND-UNKNOWNs.md)
