# Documentation Standards

**Decision status:** PROPOSED conventions except requirements inherited from
[MASTER-BLUEPRINT](../MASTER-BLUEPRINT.md), notably truthful status reporting.
**Validation status:** Review in progress; this document does not certify pack
completeness or approval. Delivery is bounded to the IMPLEMENTED / TESTED L02
no-database backend skeleton, not the complete platform; see
[execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18).

## 1. Language and format

- Documentation is written in English. Conversation and clarification happen in
  French.
- Markdown is the primary format for human-readable specifications, as required
  by the blueprint; machine-readable contracts remain separate.
- Machine-readable contracts use JSON, JSON Schema, OpenAPI or AsyncAPI.
- Diagrams use Mermaid and must reflect the written specification.

## 2. Required document structure

Every specification states, in this order:

1. Purpose
2. Scope and non-scope
3. Responsibilities and non-responsibilities
4. Dependencies
5. Interfaces and contracts (where applicable)
6. Security considerations
7. Failure modes
8. Observability
9. Tests
10. Acceptance criteria
11. Related documents

Not every governance document needs all sections; the structure applies to
technical specifications.

## 3. Status labels

Each document separates **decision status**, **delivery status** and, where
relevant, **document validation status**. A mixed document identifies which
clauses are locked source requirements, explicit approvals or proposals.

| Dimension | Label | Meaning / evidence needed |
|---|---|---|
| Specification | SPECIFIED | Described in a document; no approval or delivery implied |
| Decision | PROPOSED | Recommendation awaiting an explicit decision |
| Decision | APPROVED | Exact scope approved with approver/date and approval evidence link; not implementation or technical validation |
| Decision | REJECTED / SUPERSEDED | Recorded decision or replacement reference |
| Delivery | PLANNED / NOT IMPLEMENTED | Intended work; no working capability demonstrated, even if the decision is approved |
| Delivery | IMPLEMENTED | Actual code/build artifacts linked for an identified revision; does not mean tested or released |
| Verification | TESTED | Reproducible commands, environment, revision and passing results linked for the stated coverage |
| Delivery | RELEASED | Approved release record and published artifact linked; not inferred from tests |
| Lifecycle | DEPRECATED | Replacement/removal policy and affected version identified |

Evidence links are required before status advances. A future file path, template,
unchecked acceptance list or generated claim is not evidence. If approval was
explicit in the owner's instruction but has no stable URL, preserve only that
bounded approval and record the provenance gap; see the
[approval record](DECISION-MANAGEMENT.md#approval-basis). Do not invent dates,
approvals, implementation, expert review or successful tests. Authors, reviewers
and approvers are distinct roles; ownership alone proves no technical expertise.

A documented feature is not an implemented feature. The earlier Phase-0
no-application status is historical: L02 now has bounded backend build and test
evidence. Frontend and integrated identity, tenancy, audit and inventory remain
PLANNED / NOT IMPLEMENTED. Neither a documentation review nor L02 skeleton
verification can close later integration or deployment gates.

## 4. Writing rules

- One authoritative definition per concept; link instead of duplicating.
- No empty headings and no placeholder sections.
- No vague requirements: each security or performance statement names a
  control, a threshold or a test.
- No invented standards or unverified tool claims.
- Use relative Markdown links between documents.
- Use the product name Authority Pro consistently.

## 5. Naming

| Kind | Convention | Example |
|---|---|---|
| Document file | `UPPER-KEBAB-CASE.md` | `MULTI-TENANCY-ARCHITECTURE.md` |
| ADR file | `ADR-NNNN-short-title.md` | `ADR-0003-multi-tenancy.md` |
| Event | `domain.entity.action` | `hr.payroll.approved` |
| Module id | `lower-kebab-case` | `inventory` |
| Permission | `module.resource.action` | `inventory.product.read` |

## 6. Change process

- A document change that alters an approved decision requires the matching ADR
  change.
- Contradictions are resolved before merging; the resolution is recorded.
- The documentation index in `docs/README.md` is updated with every new
  document.

## 7. Validation

Documentation is validated by review against this checklist:

- [ ] Purpose, scope and non-scope are explicit.
- [ ] Contracts and failure modes are described.
- [ ] Security and tests are actionable.
- [ ] Links resolve.
- [ ] Terminology matches the glossary.
- [ ] No contradiction with the constitution or an approved ADR.

## Related documents

- [Platform Constitution](PLATFORM-CONSTITUTION.md)
- [Glossary](../GLOSSARY.md)
- [Documentation index](../README.md)
