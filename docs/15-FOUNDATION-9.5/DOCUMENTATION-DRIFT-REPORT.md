# Documentation Drift Report

**Decision status:** N/A — read-only audit output.
**Delivery status:** PHASE 0 OUTPUT. Drift is recorded, not corrected; corrections belong to Phase 1 under Gate G1.
**Document validation status:** DRAFT — awaiting Gate G0 owner approval.

## 1. Purpose

The repository currently contains more working capability than its normative documents
describe. This report lists every contradiction found between documentation and the
code at revision `27e9b14`, with the exact location and the intended correction. No
file was edited in Phase 0. Corrections must preserve historical evidence and mark
superseded claims instead of deleting them.

## 2. Drift register

| ID | Document and location | Contradiction | Evidence of truth | Severity | Phase 1 correction |
|---|---|---|---|---|---|
| D-01 | `docs/MASTER-BLUEPRINT.md` title and lines 5, 32, 38, 2217 | Project identified as "AUTHORITY ONLINE" | Owner renamed the project Authority Pro on 2026-11-17; root `README.md`, code packages and repository name | Critical | Add an identity correction block at the top; keep the immutable original title marked as historical source |
| D-02 | `README.md` line 12 | "No frontend ... exists" | 77 tracked frontend files, 16 spec files, 100 passing tests | High | Replace with the actual L07 state and link the current-state document |
| D-03 | `README.md` line 64, `docs/README.md` line 9, `docs/PACK-INVENTORY.md` line 34, `docs/13-IMPLEMENTATION/IMPLEMENTATION-PHASES.md` line 4 | Test counts of 65 unit / 4 integration, "69 passing L02 tests" | Latest verified counts: 180 unit and 7 packaged integration tests | High | Update counts with date and command; keep the earlier counts as dated historical evidence |
| D-04 | `IMPLEMENTATION-PHASES.md` line 4 | "frontend and business features remain unimplemented" | L07 screens and tests exist; business modules genuinely absent | High | Separate frontend state from business-module state |
| D-05 | `docs/adr/ADR-0005-sessions.md` line 3 | "no implementation exists" | L05 implementation qualified in isolation; code and migrations V3/V4 present | Medium | Add an implementation note without rewriting the original approval text |
| D-06 | `docs/adr/ADR-0010-java-spring-versions.md` and `README.md` L01 section | Angular 22 strict resolution failed; Angular 21 probe only | `frontend/package.json` pins Angular 22.3.0; build and tests pass | Medium | Record the later Angular 22 decision with evidence; keep the earlier probe as history |
| D-07 | `README.md` lines 12–13 | "main-database V2–V4 migration" | Five migrations exist (V1–V5); main database remains at V1 | Low | State V1–V5 and the actual main-database level |
| D-08 | `docs/PACK-INVENTORY.md` | No L07 evidence section despite commits `7e3a348` to `131823a` | Frontend commits and test evidence | High | Add a dated L07 evidence section; do not rewrite earlier sections |
| D-09 | `docs/13-IMPLEMENTATION/VALIDATION-GATES.md` line 4 | Runtime status stops at L03 | L04, L05, L06 isolated qualification passed; L07 implemented | Medium | Extend the runtime status with evidence links |
| D-10 | `.github/workflows/foundation.yml` lines 15, 16, 28, 29 | `checkout@v4` and `setup-node@v4` not pinned; no frontend/security/migration jobs | Workflow inspection; only `setup-java` is pinned by SHA | High | Separate CI package (DEC-007), not a Phase 1 edit |
| D-11 | `docs/13-IMPLEMENTATION/RISK-REGISTER.md` | Register header says NOT IMPLEMENTED for mitigations, while R01/R05/R06 have partial code | Code and isolated tests | Medium | Add per-risk implementation status without closing any risk prematurely |
| D-12 | Pack baseline `131823a` | Pack documents describe a baseline one commit behind HEAD | `27e9b14` adds the disposable database gate | Informational | Record the delta in the baseline inventory (done) |

## 3. Root causes

1. **Status is duplicated across documents.** README, docs index, pack inventory,
   phases and gates each restate delivery state, so each must be edited to stay true.
2. **No machine check of status claims.** The validator proves links, index coverage,
   Mermaid syntax and schema fixtures, but not that "65 tests" matches the build.
3. **Evidence and claim are separated.** Test counts live in prose while the real
   numbers live in build reports; nothing reconciles them.
4. **Approval text is frozen but implementation moved.** ADR status lines record the
   moment of decision, not later delivery.

## 4. Phase 1 scope (executed 2026-11-28, except CI)

- Correct D-01 to D-09 and D-11 in the listed files, preserving historical statements
  with dated supersession notes.
- Introduce `CURRENT-STATE.md` as the single authoritative status document and reduce
  duplicated status prose elsewhere to links.
- Extend `tools/validate-docs.mjs` with status-metadata and count-consistency checks.
- Re-run `npm run check:docs` and `npm test`; zero errors required before Gate G1.
- Do not modify application code, tests, migrations, manifests, lockfiles or CI.

## 5. Resolution status — 2026-11-28

| ID | Resolution |
|---|---|
| D-01 | Identity correction banner added to the blueprint; original title preserved |
| D-02, D-03, D-07 | README current-state paragraph and L02 historical counts corrected; V2–V5 stated |
| D-04 | Frontend state separated from unimplemented business modules in the phases document |
| D-05 | Implementation note added to ADR-0005 without rewriting the approval record |
| D-06 | Frontend note added to ADR-0010 and the README L01 section |
| D-08 | L07 evidence section added to the Pack Inventory |
| D-09 | Validation-gates runtime status extended with L04–L07 |
| D-11 | Implementation delta section added to the risk register |
| D-10 | Resolved 2026-11-28 at `841914b`: frontend job added (100 tests and build green remotely), `checkout`/`setup-node` pinned to commit SHAs, weekly Dependabot configured. Security scans, SBOM and migration qualification remain pending under DEC-007 |
| D-12 | Baseline delta recorded in the baseline inventory |

`npm run check:docs` reports 0 errors and `npm test` 67/67 after the corrections.
Historical counts and approval texts remain in place with dated notes; nothing was
deleted.

## Related documents

- [Current state](CURRENT-STATE.md)
- [Implementation evidence matrix](IMPLEMENTATION-EVIDENCE-MATRIX.md)
- [Foundation 9.5 adapted plan](FOUNDATION-9.5-ADAPTED-PLAN.md)
