# ADR Template — Authority Pro Foundation Decisions

**Template status: PROPOSED**  
**Capability status: NOT IMPLEMENTED — an ADR is not implementation or test evidence.**  
**Proposed owner: architecture governance maintainer; approval authority: project owner.**

## Purpose, scope and source

Use this template to propose a significant platform mechanism, record its trade-offs and obtain explicit owner approval without silently changing a locked principle. Source authority: [MASTER-BLUEPRINT](../MASTER-BLUEPRINT.md), sections 2, 5.2–5.4, 25, 30 and 32. Apply the [Thunder Core status convention](../03-CORE/THUNDER-CORE.md).

Scope: N0 architecture/security/persistence/runtime decisions and N1 decisions with cross-module consequences. Non-scope: granting operational permissions, authorizing deployment, changing credentials, creating business modules, or treating a copied template as an approved decision. Do not create an ADR file or change existing ADR status unless that documentation change is authorized.

This template's own adoption remains PROPOSED. A completed ADR starts PROPOSED with capability NOT IMPLEMENTED. Decision status, implementation status, test status and release status are separate. An approved decision can still describe an unimplemented capability. Never set an approval date or owner consent from inference or from an older document's label.

## Required decision header

Complete these fields in an actual authorized ADR. The guidance cells are requirements, not fictional evidence.

| Field | Required entry |
|---|---|
| ADR identifier and title | Unique governed ID and a specific question, not a broad product slogan |
| Decision status | Initially PROPOSED; later approved/rejected/superseded only with explicit evidence |
| Capability status | Initially NOT IMPLEMENTED; later implementation state must reference code and validation evidence |
| Proposal date | Actual date of authorship, not an assumed approval date |
| Proposal owner | Named accountable author/maintainer |
| Decision authority | Project owner for N0, plus named reviewers |
| Governance level | N0/N1 with justification; foundation security is N0 |
| Source principles | Exact source sections and owner-confirmed constraints |
| Related/superseded decisions | IDs, conflicts and proposed reconciliation; never silently overwrite history |
| Dependencies | Prerequisite ADRs, modules, contract versions and external assumptions |
| Approval evidence | Initially “None — pending owner approval”; later dated explicit decision reference |

## 1. Context and problem

Describe the current verified state, the concrete problem, who is affected and why a decision is needed now. For this foundation, explicitly state that no backend/frontend exists until verified evidence changes that fact. Separate the source's long-term vision from the authorized documentation-then-runnable-socle scope.

Identify locked constraints: independent Authority Pro identity, Angular/Spring Boot, modular monolith with DDD/hexagonal/PBC boundaries, monorepo, PostgreSQL `tenant_id` plus RLS and no business modules in this increment. If a proposal would change a locked constraint, state that it cannot proceed without explicit owner approval; do not disguise it as an implementation detail.

## 2. Scope, non-scope and ownership

List included behavior and deliberately excluded capabilities. Name the owning module(s), data owner, contract provider/consumers, operational owner and reviewers. Identify which existing responsibilities are reused so the decision does not introduce duplicate engines or a god-module.

## 3. Options and trade-offs

Compare at least two realistic options and, where useful, deferral/no-change. For each, record benefits, security/isolation impact, operational cost, framework support that is verified versus assumed, compatibility, migration/recovery complexity and testability. Cite local source evidence or a planned spike; do not claim vendor guarantees without verification.

For durable publication, compare Spring Modulith durable publications and a custom outbox as alternatives, not two parallel mechanisms. For sessions, evaluate the proposed Spring Security/Spring Session JDBC cookie/CSRF model without inheriting approval for historical custom tokens.

## 4. Proposed decision and reasons

State one precise recommendation or explicitly defer selection until a named spike passes. Label it **PROPOSED**. Explain why it fits the current socle and which trade-offs are accepted if approved. Specify measurable disqualifiers that would require reconsideration. Do not include executable code, production secrets or unsupported future capabilities as if they already exist.

## 5. Interfaces, contracts and data impact

Define inputs/outputs, versioned named API exports, owned data, dependency edges, authorization scope, transaction boundary, idempotency/ordering where relevant and migration compatibility. Document what consumers may not access. Explain any manifest/schema revision and its effect on existing fixtures. Avoid incomplete SQL examples that omit roles, RLS or constraints.

## 6. Security and privacy review

Document actor/threat assumptions, trust boundaries and abuse cases. Address tenant versus control-plane data, company/site scope, runtime versus migration DB roles, authenticated transaction-local context, session/CSRF controls, least privilege, secret handling, payload minimization and critical audit atomicity. State explicitly that a platform owner has no implicit tenant access and that no global RLS bypass is introduced.

Specify required security reviewers and blocking findings. An ADR is not a permission grant or consent to access customer data.

## 7. Failure, recovery and operational consequences

Describe failure before/after commit, partial deployment, dependency outage, invalid configuration, crashes and concurrency where applicable. Define fail-closed behavior, bounded retries/timeouts, safe errors, rollback/roll-forward conditions, backup/restore prerequisites and retained evidence. State limits honestly: for example, at-least-once delivery rather than exactly-once transport.

List consequences, risks and mitigations with owners. Identify residual risks requiring explicit acceptance; do not mark them resolved because documentation exists.

## 8. Observability

Define actionable metrics, health/readiness behavior, safe structured logs, audit events and alerts. Specify what must never be logged, label-cardinality limits and who may inspect scope-specific diagnostics. Evidence must distinguish an unavailable capability from an empty but healthy result.

## 9. Tests, acceptance criteria and evidence

List executable unit/integration/architecture/security/contract/fault-injection tests required by the decision. Include negative cases and actual-runtime-role RLS tests when relevant. Specify measurable acceptance criteria, test environment, required review artifacts and the responsible verifier. Initially record **“Not executed — capability NOT IMPLEMENTED.”**

Keep documentation validation distinct from runtime proof. Schema fixtures parsing correctly is useful evidence, not evidence of a deployed module runtime. No check may be marked passed without an actual recorded result.

## 10. Migration, rollout and reversal

Describe compatibility with prior decisions, schema/API expansion and contraction, release ordering, feature availability, rollback limitations and data preservation. In a greenfield project, state that no existing application migration is performed, but still define future safe rollout requirements. Do not promise automatic destructive down-migrations. Record how a rejected proposal leaves the approved baseline intact.

## 11. Decision and approval record

Until approval, record the recommendation as pending and implementation authorization as absent. After an explicit owner decision, capture date, approver, scope, conditions and evidence reference. Record unresolved questions separately from accepted consequences. If replacing a conflicting ADR, create a traceable supersession/reconciliation record through an authorized documentation change; never infer that this template itself supersedes anything.

## Template validation and acceptance

An ADR using this template is ready for owner review only when:

- All required fields have substantive entries; unknowns have an owner and a blocking/non-blocking classification.
- Scope, ownership, contracts, security, failure/recovery, observability, tests and acceptance are specific to the decision.
- At least two options are compared and the recommendation is clearly PROPOSED.
- Locked principles and conflicts with existing decisions are explicit.
- Implementation/test/release status contains evidence or remains NOT IMPLEMENTED/not executed/not released.
- No secrets, operational commands, invented approvals or business-module work are included.

Missing security or acceptance evidence blocks approval; it does not justify filling the gap with a generic “must be secure” statement. The governance maintainer checks completeness; the project owner decides adoption and any N0 mechanism.

## Related contracts

- [Thunder Core](../03-CORE/THUNDER-CORE.md)
- [Module Runtime](../03-CORE/MODULE-RUNTIME.md)
- [Identity and Access](../04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md)
- [Outbox, Inbox and Idempotency](../03-CORE/OUTBOX-INBOX-IDEMPOTENCY.md)
- [Audit Engine](../04-PLATFORM-ENGINES/AUDIT-ENGINE.md)
