# Product Roadmap

**Decision status:** PROPOSED sequencing within the owner-approved socle-only scope.
**Delivery status:** Phase 0 documentation exists; application code NOT IMPLEMENTED.
**Boundary:** Build the foundation to receive future modules, never business modules or their tables, rules, screens or demonstrations in this mission.

## Delivery map

The [implementation lots](../13-IMPLEMENTATION/IMPLEMENTATION-PHASES.md) define the executable plan; the [coverage matrix](../PACK-INVENTORY.md#technical-coverage-matrix) records each technical capability, owner, dependencies and acceptance. Source phase names below remain for traceability, not as automatic authorization.

| Delivery level | Proposed allocation | Limit |
|---|---|---|
| L1 — Initial executable nucleus | Lots L01–L08; source Phase 1 plus necessary basic UI and operations | Does not claim complete extensibility or production readiness without evidence |
| L2 — Extensible socle | Proposed lots L09–L11; selected contracts, execution, governance and tools from Phases 2–5 | Technical capabilities only; scope and N0 decisions must be approved |
| L3 — Advanced optional platform | Selected technical products from later source phases | Unscheduled candidates, not an obligation to implement the entire catalogue |
| Business ecosystem | Source Phase 6 and business/industry portions of Phase 9 | Outside this mission; no implementation lots or demonstration modules |

These levels are distinct from governance N0/N1/N2. Four languages (FR, EN, IT, AR with RTL) remain required; no reduced language rollout is approved.

## Phase 0 — Foundation (current, IN PROGRESS)

Scope: monorepo organization, normative documentation, coverage/ownership map, ADRs, build strategy and documentation CI. No application build has been delivered.

Proposed exit: review contradictions, separate approvals from proposals and link actual local documentation-test evidence. A local CI definition is not an executed remote workflow. Application compatibility spikes and G1 decisions precede application baseline pinning; they are not evidence that the socle has shipped.

## Phase 1 — Platform socle (PLANNED / NOT IMPLEMENTED)

L1 proposed scope: backend skeleton, validated configuration and probes, separate database roles/migrations, verified tenant/company/site context, audit atomicity, closed operator provisioning, sessions/CSRF, permissions, minimal compiled registry and Angular technical shell. Audit protection is integrated before provisioning or sensitive writes are exposed. No public setup endpoint.

Minimal logs, health, security controls, data/secret lifecycle requirements and backup/restore accompany their first affected slices. FR/EN/IT/AR, RTL, keyboard access and themes are tested with the shell. Phase 3 cannot be used to postpone mandatory baseline controls.

Proposed exit: G1 is an approval gate only; implemented L1 requires evidence for [G2–G4](../13-IMPLEMENTATION/VALIDATION-GATES.md) and a bounded G5 readiness review. No application gate is claimed passed. Account recovery, privileged MFA and other open deployment requirements must be resolved before any production-opening decision.

## Later phases — vision only, not authorized

Phases 2–9 retain source context, all NOT IMPLEMENTED. The technical portions may enter explicitly approved L2/L3 slices. Business portions remain outside this mission; passing a platform gate never authorizes them.

## Phase 2 — Module infrastructure

Scope: extend the L1 compiled inventory into capability/contract compatibility, concrete dependency graphs, governed lifecycle and integration plans. For durable events, select one producer mechanism after ADR-0006's spike, with consumer inbox and idempotence; do not require both a Modulith publication registry and a custom outbox on the same path.

Proposed exit: technical test providers/consumers prove valid contracts and rejection of missing/ambiguous/incompatible dependencies without private cross-component persistence access. No business reference module, business table or screen is created. Durable execution has separate crash/retry/order/isolation evidence under L10.

## Phase 3 — Operational foundation

Scope: deepen the L1 baseline with traces, worker/queue monitoring, resource budgets, incident/SLO governance, backup automation and later advanced recovery. Supply-chain checks and rotation procedures already accompany L1 readiness; later signing/provenance and operational dashboards extend them.

Proposed exit: measured limits, observable failures and tested recovery/runbooks for the selected slice. Whole-instance recovery is not proof of tenant-selective restore or PITR. Advanced repair requires a separate authority and backup/recovery design.

## Phase 4 — UI foundation

Scope: extend the existing L1 shell with versioned contributions, allowlisted form/table schemas, preferences and accessibility/localization operations. Visual designers are optional later scope. A non-AI Authority X command palette can be evaluated separately from AI interpretation.

Proposed exit: a technical contribution is registered, navigable and permission-checked, with all four languages/RTL and accessibility tests. Arbitrary remote JavaScript is not an extension mechanism.

## Phase 5 — Extensibility

Scope: separate compiled component lifecycle, dependency resolution, integration resolution, distribution trust and optional automation. L2 contracts do not authorize arbitrary package installation, scripts or third-party execution.

Proposed exit: authorized technical lifecycle dry-run identifies dependencies, permissions, data retention and rollback limits. Installation/signature/sandbox execution, if later selected, requires its own threat model and tests. Disabling/uninstalling cannot silently remove data; data recovery is not equivalent to reversing every migration.

## Phase 6 — First business modules

Retained solely as a source-blueprint reference. Accounting, HR, fleet and all other business modules/features are excluded from this mission. There is no scheduled Phase 6 implementation or business demonstrator; technical fixtures validate extensibility instead.

## Phase 7 — Platform products

Unscheduled L3 candidates: Studio's Design/Development/AI/Quality/Environment/Release areas, technical distribution/licensing tools and optional AI-assisted Authority X. Developer SDK/CLI should work without Studio. Commerce, billing and business portals are not socle features and remain excluded.

## Phase 8 — Advanced capabilities

Unscheduled candidates: external connectivity, hardware agents, optional AI, offline/hybrid synchronization, sandboxed automation, forensics and advanced recovery. Each needs a bounded use case, explicit authority, resource limits and an acceptance contract. No automatic general-purpose script runtime or universal synchronization is promised.

## Phase 9 — Expansion

Source business/industry expansion and tax packs are outside this mission. Translation/format support does not authorize fiscal rules. Advanced technical analytics, lineage, simulation and experimentation are separate L3 candidates, not a reason to construct domain-specific data models now.

## Rules

- Preserve the source archive, approved architecture and strict data ownership.
- Scope consolidation does not approve technical N0 proposals; record decisions before corresponding implementation.
- No business feature, including a reference demonstration, can enter a technical lot.
- Record row IDs, lot, revision, actual tests and residual gaps before claiming delivery; no gate closes on a documentation assertion.
- Never hide a security regression or swallow critical-audit failure to pass a performance target.
- No commit, remote creation, push, release or deployment follows automatically from readiness.

## Related documents

- [Implementation Phases](../13-IMPLEMENTATION/IMPLEMENTATION-PHASES.md)
- [MVP Boundaries](../13-IMPLEMENTATION/MVP-BOUNDARIES.md)
- [Risk Register](../13-IMPLEMENTATION/RISK-REGISTER.md)
