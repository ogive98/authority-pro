# Open Risks and Unknowns

**Decision status:** N/A — read-only audit output.
**Delivery status:** PHASE 0 OUTPUT. No mitigation is implemented or closed by this document.
**Document validation status:** DRAFT — awaiting Gate G0 owner approval.

## 1. Labels

Each entry uses one of: CONFIRMED FROM SOURCE, CONFIRMED BY TEST EVIDENCE,
DOCUMENTED CLAIM ONLY, INFERRED — VERIFY, UNKNOWN, NOT VERIFIED.

## 2. Foundation risk register mapped to observed evidence

| ID | Risk | Severity | Observed evidence | Label | Required closure |
|---|---|---:|---|---|---|
| R-001 | Blueprint identifies AUTHORITY ONLINE | Critical | Drift D-01 | CONFIRMED FROM SOURCE | Phase 1 identity correction with preserved history |
| R-002 | README claims no frontend | High | Drift D-02 | CONFIRMED FROM SOURCE | Phase 1 correction |
| R-003 | Status drift between code, tests and documents | High | Drift D-03, D-04, D-09 | CONFIRMED FROM SOURCE | Single current-state source plus validator checks |
| R-004 | RLS unproven under pool reuse and concurrency | Critical | Partially mitigated 2026-11-28: L05 isolation checks prove no stale tenant context after commit/rollback on a reused single-connection pool and cross-tenant writes rejected; parallel concurrency still unproven | CONFIRMED BY TEST EVIDENCE (partial) | Phase 3 restricted-role concurrency suite |
| R-005 | Concurrent session cap race | High | CLOSED 2026-11-28: advisory-lock cap proven by L05 `checks=42` (`56e5662`, `dfaf1f8`) — four parallel logins leave the three newest, lock timeout fails closed with no inserted row, rotation keeps three | CONFIRMED BY TEST EVIDENCE (closed) | None for the cap; distributed throttling remains R-006 |
| R-006 | Login throttle is not distributed | High | `LoginThrottle` is an in-memory bounded map, 1024 sources, reset on restart | CONFIRMED FROM SOURCE | Bounded policy decision and multi-instance test |
| R-007 | Production cookie profile incomplete | Critical | `Secure=false`, SameSite Lax, loopback HTTP only | CONFIRMED FROM SOURCE | TLS profile, secure cookie, browser test |
| R-008 | Audit retention, redaction and integrity unproven | High | Atomicity tested in isolation only | DOCUMENTED CLAIM ONLY | Retention policy, redaction tests, tamper evidence |
| R-009 | Runtime described beyond implementation | High | Outbox ADR is PROPOSED; no worker code | CONFIRMED FROM SOURCE | Code-to-contract status map; defer implementation |
| R-010 | No frontend CI job | High | `foundation.yml` has two jobs only | CONFIRMED FROM SOURCE | CI package under DEC-007 |
| R-011 | Backup and restore never proven | Critical | No drill evidence | UNKNOWN | Disposable restore drill with RPO/RTO |
| R-012 | No performance evidence | High | Only bundle budgets configured | CONFIRMED FROM SOURCE | Workload model and measured baseline |
| R-013 | Supply-chain policy incomplete | High | Unpinned actions; no SBOM, vulnerability, secret or license scanning | CONFIRMED FROM SOURCE | CI security package |
| R-014 | Scope could over-engineer the foundation | High | Eleven-phase programme against a small dedicated instance | INFERRED — VERIFY | Phase gates and deferral policy |
| R-015 | Tooling could overwrite uncommitted work | Critical | Clean tree, zero untracked files at baseline | CONFIRMED BY TEST EVIDENCE | Keep file allowlists and Git-state checks |

## 3. Additional risks found during Phase 0

| ID | Risk | Severity | Evidence | Label | Required decision or closure |
|---|---|---:|---|---|---|
| X-01 | Main database is at V1 while code expects V2–V5 | Critical | Migrations V1–V5 exist; documentation records main at V1 | CONFIRMED FROM SOURCE | Owner decision: migration window or strict disposable-database mode |
| X-02 | No production profile exists | Critical | Loopback address, fixed port, non-secure cookie, no TLS assumptions | CONFIRMED FROM SOURCE | Phase 2 design and Phase 3 tests |
| X-03 | CI actions not pinned to immutable SHAs | High | `checkout@v4`, `setup-node@v4` | CONFIRMED FROM SOURCE | CI package with verified SHAs |
| X-04 | Authenticated browser journeys never observed | High | No test account, no session evidence | CONFIRMED FROM SOURCE | Separate authorized disposable environment |
| X-05 | Validator cannot detect stale status claims | Medium | `validate-docs.mjs` checks links, index, Mermaid, fixtures only | CONFIRMED FROM SOURCE | Phase 1 validator extension |
| X-06 | No lint, formatting or coverage gate in either application | Medium | No ESLint, Checkstyle/Spotless or JaCoCo/Vitest coverage thresholds configured | CONFIRMED FROM SOURCE | Quality package |
| X-07 | Security response headers incomplete | High | Partially mitigated 2026-11-28: frame, referrer, permissions, nosniff and `frame-ancestors` on all application responses (tested); script CSP and HSTS still absent | CONFIRMED FROM SOURCE | Phase 3 headers gate; ADR-0013 for CSP/HSTS |
| X-08 | Session throttling has no persistence or edge limit | High | Same as R-006 plus no reverse-proxy configuration | CONFIRMED FROM SOURCE | Deployment profile decision |

## 4. Unknowns that must not be guessed

| ID | Unknown | Label | How it will be resolved |
|---|---|---|---|
| U-01 | Actual behavior of RLS under a reused pooled connection across tenants | RESOLVED FOR ONE CONNECTION (2026-11-28, L05 `checks=39`); parallel connections still unknown | Phase 3 concurrency suite with restricted role |
| U-02 | Maximum concurrent sessions under parallel logins | RESOLVED (2026-11-28, L05 `checks=42`): four parallel fresh sessions leave exactly three; timeout and rotation covered at `dfaf1f8` | Closed |
| U-03 | Restore duration and integrity for the real data volume | UNKNOWN | Isolated restore drill |
| U-04 | Audit retention needs and legal constraints | UNKNOWN | Owner decision, then policy |
| U-05 | Production topology and RPO/RTO | UNKNOWN | Owner decision (DEC-006) |
| U-06 | Remote CI results at HEAD | NOT VERIFIED | Observe the workflow run separately |
| U-07 | Angular advisory status at the pinned versions | NOT VERIFIED | Dependency scan in the CI security package |
| U-08 | API latency, throughput and memory under representative load | UNKNOWN | Phase 10 baseline |
| U-09 | Whether the disposable `authority_pro_l07_ui_test` database exists | NOT VERIFIED | Checked only inside an authorized L07 package |
| U-10 | Whether any container or database is running | NOT VERIFIED | Phase 0 prohibits access; verify before any later run |
| U-11 | Dependency update cadence and owner availability for upgrades | UNKNOWN | Maintenance policy in the adapted plan |

## 5. Stop conditions

No Phase 3 test, migration, container action or deployment may begin until the owner
approves it explicitly. X-01 must be decided before any use of the main database, and
the disposable-database gate does not authorize creating that database.

## Related documents

- [Existing risk register](../13-IMPLEMENTATION/RISK-REGISTER.md)
- [Implementation evidence matrix](IMPLEMENTATION-EVIDENCE-MATRIX.md)
- [Current state](CURRENT-STATE.md)
