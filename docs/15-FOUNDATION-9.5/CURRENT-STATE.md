# Authority Pro — Current State

**Decision status:** Status snapshot; no new decision is taken here.
**Delivery status:** Authoritative status summary for revision `27e9b14`, 2026-11-28.
**Document validation status:** DRAFT — awaiting Gate G0 owner approval. Until approved, the Pack Inventory remains the historical evidence record and this document is a proposed single source of truth.

## 1. Purpose

This is the one place that states what is true today. Every other document should link
here instead of restating delivery status. When a status changes, this file changes in
the same package, with its evidence link and date.

## 2. Status by area

| Area | Decision | Delivery | Verification | Evidence |
|---|---|---|---|---|
| L01 stack decisions | APPROVED (bounded) | n/a | n/a | ADR-0001, ADR-0010 |
| L02 backend skeleton | APPROVED (bounded) | IMPLEMENTED | TESTED locally (E4) | Pack Inventory L02 section |
| L03 dedicated PostgreSQL and data model | APPROVED (bounded) | IMPLEMENTED | TESTED locally; main database at V1 only | Pack Inventory L03 sections |
| L04 audit | APPROVED (contract D1–D6) | IMPLEMENTED | TESTED in isolation (E5 isolated) | Audit Engine §9.11 |
| L05 identity and access | APPROVED (contract) | IMPLEMENTED | TESTED in isolation (E5 isolated) | Identity and Access §10.13 |
| L06 protected inventory | APPROVED (three owner decisions) | IMPLEMENTED | TESTED in isolation (E5 isolated) | Pack Inventory L06 section |
| L07 Angular application | Per-slice approvals | IMPLEMENTED | 100 unit tests (E3); public screens checked in a browser (E4 local); authenticated journeys NOT VERIFIED | Frontend tree, this revision |
| L08 integrated qualification | NOT APPROVED | NOT IMPLEMENTED | NOT VERIFIED | Implementation phases |
| CI frontend/security/migrations | NOT APPROVED (DEC-007) | NOT IMPLEMENTED | NOT VERIFIED | `foundation.yml` |
| Production deployment | NOT APPROVED | NOT IMPLEMENTED | NOT VERIFIED | Skeleton properties |
| Backup / restore | Required by programme | NOT IMPLEMENTED | NOT VERIFIED | No drill evidence |
| Performance baseline | Not set | NOT IMPLEMENTED | NOT VERIFIED | Bundle budgets only |

## 3. What is true at this revision

- The backend builds and passes 181 unit/architecture/contract tests (headers
  increment, 2026-11-28), with five closed Modulith roots and zero inter-module
  edges; the 7 packaged integration tests were last verified at `27e9b14` and were
  not rerun for this increment.
- The frontend builds and passes 100 unit tests across 16 files, with FR/EN/IT/AR and
  RTL support.
- Documentation checks pass with 0 errors: 73 documents, 903 links, 12 diagrams, plus
  70 tool tests.
- The Phase 2 blueprint exists as DESIGN ONLY in
  [phase-2](phase-2/00-TARGET-BLUEPRINT.md); nothing in it is approved, and Gate G2
  remains pending.
- Phase 3 increment 1 is implemented and tested: every application response carries
  framing, `nosniff`, referrer and permissions headers; HSTS, script CSP, Secure
  cookies and all database-dependent scenarios remain open
  ([headers gate](phase-3/00-SECURITY-HEADERS-GATE.md)).
- The main database remains at V1; V2–V5 exist only as migrations and in isolated
  qualification databases.
- No production profile, TLS termination, backup drill, performance measurement or
  authenticated browser evidence exists.

## 4. What is not true, despite older documents

- The project is not called AUTHORITY ONLINE. The blueprint title is historical.
- The repository is not frontend-free. L07 screens exist and are unit-tested.
- Test counts of 65/4 or 69 are obsolete.
- The main database is not migrated to V5.

Detailed contradictions and their corrections are in the
[documentation drift report](DOCUMENTATION-DRIFT-REPORT.md).

## 5. Open owner decisions

| Decision | Question | Impact if unresolved |
|---|---|---|
| X-01 | Migrate the main database to V5, or keep strict disposable-database operation? | Any default runtime use fails |
| X-02 | Production topology: TLS termination, cookie domain and secure profile | No deployable profile |
| DEC-006 | RPO/RTO targets | Restore design cannot be accepted |
| DEC-007 | CI hardening (frontend, security, SBOM, migrations) | Quality claims stay local only |
| U-04 | Audit retention policy | Audit cannot be released |

## 6. Update rule

1. A change that alters a status above must edit this file in the same package and cite
   the command, date and revision that prove it.
2. No status may advance on a document claim alone; use the evidence levels of the
   [implementation evidence matrix](IMPLEMENTATION-EVIDENCE-MATRIX.md).
3. Contradictions are resolved here first, then propagated by links.

## Related documents

- [Pack inventory](../PACK-INVENTORY.md)
- [Implementation phases](../13-IMPLEMENTATION/IMPLEMENTATION-PHASES.md)
- [Validation gates](../13-IMPLEMENTATION/VALIDATION-GATES.md)
- [Open risks and unknowns](OPEN-RISKS-AND-UNKNOWNs.md)
- [Foundation 9.5 adapted plan](FOUNDATION-9.5-ADAPTED-PLAN.md)
