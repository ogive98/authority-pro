# Implementation Evidence Matrix

**Decision status:** N/A — read-only audit output.
**Delivery status:** PHASE 0 OUTPUT. Evidence is dated and tied to revision `27e9b14`.
**Document validation status:** DRAFT — awaiting Gate G0 owner approval.

## 1. Evidence hierarchy used by the Foundation 9.5 programme

| Level | Meaning |
|---|---|
| E0 | No evidence / unknown |
| E1 | Documentation claim only |
| E2 | Source inspection |
| E3 | Automated test evidence |
| E4 | Integration / packaged test evidence |
| E5 | Isolated security, failure or performance qualification |
| E6 | Production-like operational drill and owner acceptance |

A capability is never upgraded because a document asserts it. This matrix records the
highest level actually observed on 2026-11-28.

## 2. Capability matrix

| Capability | Claim | Evidence | Level | Gaps |
|---|---|---|---|---|
| Backend skeleton | Spring Boot application with five closed Modulith roots and zero inter-module edges | `PlatformArchitectureTest`, clean `check:backend` run | E4 | Non-null verification only |
| Configuration guard | Refuses Java/command-line/security overrides; loopback-only port; fixed properties | `SkeletonSettingsTest`, `PackagedSkeletonIT` | E4 | Production profile absent |
| HTTP probes | Only liveness/readiness exposed, readiness 503 until complete | Packaged integration tests | E4 | None observed |
| Database environment | Dedicated PostgreSQL 16.15 container, distinct roles and DPAPI secret | `tools/local-environment.mjs`, README evidence sections | E4 local | Restore drill not run |
| Data model | V1–V5 migrations, RLS FORCE, tenant context checks | Isolated qualification scripts L03–L06 | E4/E5 isolated | Main database at V1 only |
| Audit engine | Atomic audit with protected state, attempt stream | L04 isolated qualification, 36 checks (2026-11-21) | E5 isolated | Retention, integrity and redaction not proven |
| Identity and access | JDBC sessions, CSRF, operator-only provisioning, scoped grants | L05 isolated qualification, `checks=36`, V1→V4 (2026-11-23) | E5 isolated | Session cap race, throttle distribution, cookie profile untested |
| Protected inventory | Compiled descriptors, permissioned read | L06 isolated qualification, 12 checks, V1→V5 (2026-11-24) | E5 isolated | Dynamic lifecycle deferred |
| Frontend shell | Login, scope selection, overview, inventory, audit, preferences, i18n, RTL | 100 unit tests at this revision | E3 | Authenticated journeys never observed in a real browser (E0) |
| Public screens | Login, not-found, responsive and accessibility checks | Earlier browser qualification, commit `4573701` | E4 local | Not reproducible in CI |
| Observability | Correlation ID and structured JSON access logs | Source inspection, unit coverage | E2/E3 | No metrics endpoint or alerting |
| Documentation tooling | Links, index, Mermaid, manifest fixtures and current counters validated | `validate-docs`, 70/70 tool tests, 0 errors | E4 local | Does not detect stale status claims |
| CI | Documentation and backend jobs defined | `foundation.yml` inspection | E2 | Remote results not verified; no frontend job; actions unpinned |
| Backend quality gates | Compiler, ArchUnit, unit and packaged tests | Clean verify | E4 local | No lint, format, coverage or mutation threshold |
| Frontend quality gates | TypeScript build and Vitest | `ng build`, `ng test` | E3/E4 local | No lint, coverage threshold or E2E |
| Supply chain | Version pins in manifests and Maven wrapper checksum | Manifest inspection | E2 | No SBOM, vulnerability, secret or license scan |
| Backup / restore | Declared requirement | Documentation only | E1 | Never drilled |
| Performance | Bundle budgets only | `angular.json` | E2 | No latency, throughput, memory or soak measurement |
| Production deployment | Not configured | `authority-skeleton.properties` | E0 | Loopback-only, no TLS profile, non-secure cookie |

## 3. Test counts at the observed revision

| Suite | Result | Command |
|---|---|---|
| Backend unit/architecture/contract | 181 passing, 0 failures (2026-11-28) | `mvnw test` |
| Backend packaged integration | 7 passing at `27e9b14`; not rerun for the headers increment | `check:backend` (clean verify) |
| Frontend unit | 100 passing across 16 files | `frontend: npm test` |
| Documentation tooling | 70 passing | `npm test` |
| Documentation validation | 0 errors, 73 documents, 903 links, 12 diagrams | `npm run check:docs` |

The backend numbers replace earlier counts of 65/4 (L02) and 151/7 (L06) still present
in some documents; the drift is recorded in the
[documentation drift report](DOCUMENTATION-DRIFT-REPORT.md).

## 4. What is explicitly not proven

- Tenant isolation under reused pool connections, rollback, exception paths and
  concurrent requests (E2 code inspection only).
- The three-session cap under concurrent logins (find-then-delete is not atomic).
- Login throttling across instances or restarts (in-memory, bounded at 1024 sources).
- Production cookie flags and HTTPS enforcement (`Secure=false` in local profile).
- Audit retention, redaction and tamper evidence.
- Backup restore, point-in-time recovery and RPO/RTO.
- Any performance characteristic beyond bundle size.
- Any authenticated browser journey.

## Related documents

- [Baseline file inventory](BASELINE-FILE-INVENTORY.md)
- [Open risks and unknowns](OPEN-RISKS-AND-UNKNOWNs.md)
- [Foundation 9.5 adapted plan](FOUNDATION-9.5-ADAPTED-PLAN.md)
