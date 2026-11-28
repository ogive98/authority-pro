# Phase 2 — Acceptance Criteria and Package Sequencing

**Decision status:** PROPOSED.
**Delivery status:** DESIGN ONLY; only S-03 is delivered (2026-11-28, `dfaf1f8`,
L05 `checks=42`); every other package below is not started.
**Document validation status:** DRAFT.

## 1. Package model

A package is a bounded, verifiable unit of work. Every package must declare its
entry gate, its deliverables, its proof commands and its exit evidence. A package
that cannot produce runtime evidence is not complete; a design package may close
on document checks only.

| Id | Package | Entry gate | Exit evidence |
|---|---|---|---|
| S-01 | Security qualification harness (Phase 3) | G2 approved | executed scenarios from the threat model with recorded results |
| S-02 | Production HTTP profile (ADR-0013) | S-01 designed | Secure cookie, proxy trust and headers proven in a non-production profile |
| S-03 | Atomic session cap (ADR-0012) | S-01 designed | DELIVERED 2026-11-28 at `dfaf1f8`: four parallel logins, lock timeout and rotation proven by L05 `checks=42` |
| S-04 | Distributed login throttle decision | S-01 designed | accepted risk or implemented shared limiter |
| C-01 | Contract validation in CI | G2 approved | OpenAPI parsed, fixtures validated, drift fails the build |
| C-02 | CI hardening (DEC-007) | C-01 | backend + frontend jobs green on a clean checkout |
| C-03 | Lint and coverage floors | C-02 | configured thresholds enforced by CI |
| D-01 | Documentation validator extension | C-01 | status tokens and counters validated, tests updated |
| R-01 | Restore drill | G2 approved | documented restore of an isolated database from backup |
| P-01 | Performance baseline | S-02 | measured latency/throughput baseline with stated environment |
| P-02 | Module edge rules (ADR-0014) | S-01 | named exports enforced by lint/ArchUnit |

## 2. Ordering constraints

1. S-01 before S-02/S-03/S-04: qualification must exist before hardening claims.
2. C-01 before C-02: a job that cannot detect contract drift is not hardening.
3. R-01 before any decision that assumes recoverable data (X-01 option A).
4. P-01 after S-02: measuring a profile that will change is wasted effort.
5. P-02 is independent but must not break the existing module-boundary tests.

## 3. Exit criteria per gate

| Gate | Required condition |
|---|---|
| G0 | Phase 0 baseline accepted, open decisions recorded |
| G1 | Phase 1 documentation drift resolved and indexed |
| G2 | Phase 2 blueprint, contracts and ADRs approved |
| G3 | Security scenarios executed with evidence; residual risks accepted |
| G4 | CI enforces build, tests, lint and contract checks |
| G5 | Restore drill and performance baseline recorded |
| G6+ | Business capabilities only after the foundation gates |

## 4. Proof commands

| Check | Command | Expected |
|---|---|---|
| Documentation | `npm run check:docs` | 0 errors, all documents indexed |
| Tool tests | `npm test` | 70 passing, 0 failing |
| Backend | `npm run check:backend` | 181 unit (2026-11-28); 7 packaged at `27e9b14`, not rerun since |
| Frontend | `npm --prefix frontend test` | 100 passing |

These commands prove repository consistency, not runtime security or availability.

## 5. Non-goals of this phase

- No code, migration, CI workflow or production configuration is changed.
- No package above is started, estimated in days, or assigned an owner.
- No gate beyond G2 is claimed.

## Related documents

- [Frontend / API map](06-FRONTEND-API-MAP.md)
- [Security threat model](05-SECURITY-THREAT-MODEL.md)
- [Adapted Foundation 9.5 plan](../FOUNDATION-9.5-ADAPTED-PLAN.md)
