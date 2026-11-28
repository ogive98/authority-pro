# Foundation 9.5 — Adapted Plan

**Decision status:** PROPOSED programme adaptation; awaiting owner approval at Gate G0.
**Delivery status:** NOT IMPLEMENTED. This document authorizes no code, migration, CI edit, database action or deployment.
**Document validation status:** DRAFT.

## 1. Purpose

The official Foundation 9.5 pack is accepted as the governing quality framework, but
executing its eleven phases linearly on a single-owner, single-instance foundation
would trade real hardening for documents. This plan keeps the pack's controls, trims
speculative work, and adds six missing controls identified during Phase 0.

## 2. What is retained from the pack

1. Evidence levels E0–E6 and dated, revision-bound evidence.
2. Gates G0–G10 with mandatory stops and no automatic phase transition.
3. Decision status separated from delivery status.
4. Protected areas (identity, tenancy/RLS, authorization, public contracts, audit,
   migrations, secrets, deployment, recovery) requiring an ADR before change.
5. Numbered decision and risk registers.
6. Read-only baseline discipline before any edit.
7. CI minimum target including frontend, migrations on disposable PostgreSQL, SBOM,
   vulnerability, secret and license checks.
8. Restore and performance proof before any production claim.
9. Threat model and acceptance scenarios per bounded context.

## 3. What is trimmed or deferred

| Pack element | Treatment | Reason |
|---|---|---|
| Linear execution of Phases 4–10 | Design in Phase 2; implement only on a real consumer | Avoid R-014 over-engineering |
| Outbox / worker (Phase 6) | Deferred; PostgreSQL candidate only | No asynchronous use case today |
| Registry lifecycle (Phase 5) | Static inventory only | No third-party components |
| Extensions T1–T3 | Out of scope | No consumer, high risk |
| Configuration hierarchy (Phase 7) | Production profile first; tenant/user scopes deferred | Only platform scope exists |
| Full performance programme (Phase 10) | Baseline measurement early, thresholds after measurement | No workload model yet |

## 4. Phase map

| Phase | Adapted scope | Gate | Indicative effort |
|---|---|---|---|
| 0 | Read-only baseline (this package) | G0 | Done |
| 1 | Documentation truth: drift D-01…D-11, current state, validator extension | G1 | 1 day |
| Decision | X-01 main-database level; X-02 production topology; DEC-006 RPO/RTO | Owner | Decision meeting |
| 2 | Blueprint: C4, ownership matrix, contracts, OpenAPI, DB map, events, threat model, ADRs | G2 | 12–18 days |
| 3 | Security qualification: RLS concurrency, session cap, throttle, cookies/TLS, headers, audit redaction | G3 | 5–8 days |
| 9 (early) | CI: frontend job, SHA pinning, gitleaks, SBOM, disposable migration validation | G9a | 1–2 days |
| 8 (early) | Restore drill on disposable data, RPO/RTO measured | G8a | 1–2 days |
| 10 (baseline) | Workload model, p50/p95/p99, memory, soak; SLOs set after measurement | G10a | 2–3 days |
| 4–7 | Only when a consumer exists | G4–G7 | Deferred |

## 5. Additions to the pack

| ID | Addition | Where | Acceptance |
|---|---|---|---|
| A1 | Single-source status (`CURRENT-STATE.md`) plus a machine-readable evidence ledger | Phase 1 | Every normative claim links to dated evidence |
| A2 | Validator extension: required status metadata, index coverage, stale-count detection | Phase 1 | A deliberately stale count fails the check |
| A3 | Lint, formatting and coverage ratchets: ESLint/Prettier (frontend), Spotless or Checkstyle plus JaCoCo (backend) | Quality package | CI blocks on violation |
| A4 | Security headers: CSP, HSTS under TLS, frame-ancestors, Referrer-Policy, Permissions-Policy | Phase 2 design, Phase 3 tests | Header assertions in integration tests |
| A5 | Query-count assertions per endpoint and a three-scenario performance smoke | Phase 2/10 | N+1 regression fails a test |
| A6 | Supply-chain and maintenance policy: pinned action SHAs, gitleaks, osv-scanner, CycloneDX SBOM, grouped dependency updates | CI package | Reports produced on every run |

## 6. Security acceptance scenarios (Phase 3)

- Tenant A cannot read or write Tenant B data through API, restricted role or pool reuse.
- Rollback, exception and nested-transaction paths clear tenant context.
- Four concurrent logins cannot leave more than three active sessions.
- CSRF rejected without header; cookie flags asserted under the production profile.
- Throttle behavior documented per instance and bounded in memory.
- Membership revocation blocks subsequent operations deterministically.
- Audit metadata redacted; ordinary runtime credentials cannot alter audit rows.

## 7. Success metrics

1. 100% of normative status claims carry dated evidence and an E-level.
2. Zero documentation drift detected by the extended validator in CI.
3. CI covers documentation, backend, frontend, security scans and disposable migrations.
4. Six security scenarios at E5; restore drill at E5; performance baseline published.
5. Lint, format and coverage gates enforced; supply-chain reports on every run.

## 8. Explicit non-authorizations

This plan does not authorize: editing application code or tests; running migrations;
creating or deleting any database; starting, stopping or inspecting containers;
editing CI; creating branches; committing or pushing; deploying; printing secrets.

## Related documents

- [Current state](CURRENT-STATE.md)
- [Documentation drift report](DOCUMENTATION-DRIFT-REPORT.md)
- [Open risks and unknowns](OPEN-RISKS-AND-UNKNOWNs.md)
- [Progressive work programme](../13-IMPLEMENTATION/IMPLEMENTATION-PHASES.md)
