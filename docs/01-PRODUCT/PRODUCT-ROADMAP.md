# Product Roadmap

**Decision status:** PROPOSED sequencing within the owner-approved socle-only scope.
**Delivery status:** Phase-0 documentation exists and the L02 no-database backend skeleton is IMPLEMENTED / TESTED; see [execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18). The remaining L1 capabilities and later levels are not delivered.
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

## Phase 0 — Foundation (historical baseline; review remains distinct)

Historical scope: monorepo organization, normative documentation, coverage/ownership map, ADRs, build strategy and documentation CI. The no-application-build statement applied at that stage; L02 now has a delivered and tested backend skeleton.

Proposed documentary exit: review contradictions, separate approvals from proposals and link actual local documentation-test evidence. A local CI definition is not an executed remote workflow. Compatibility spikes and G1 decisions preceded L02 baseline pinning; neither those steps nor L02 alone establish delivery of the whole socle.

## Phase 1 — Platform socle (L02 skeleton tested; integrated L1 incomplete)

L1 proposed scope: backend skeleton, validated configuration and probes, separate database roles/migrations, verified tenant/company/site context, audit atomicity, closed operator provisioning, sessions/CSRF, permissions, minimal compiled registry and Angular technical shell. Audit protection is integrated before provisioning or sensitive writes are exposed. No public setup endpoint.

Minimal logs, health, security controls, data/secret lifecycle requirements and backup/restore accompany their first affected slices. FR/EN/IT/AR, RTL, keyboard access and themes are tested with the shell. Phase 3 cannot be used to postpone mandatory baseline controls.

Proposed exit: G1 is an approval gate only; implemented L1 requires evidence for [G2–G4](../13-IMPLEMENTATION/VALIDATION-GATES.md) and a bounded G5 readiness review. L02 skeleton verification passed, but full G2 remains open until the database-bearing slice and no integrated L1 acceptance is claimed. The later closed L03 integration is authorized and in progress; its current application qualification/restore must pass before the first main migration, without expanding database access or treating scratch evidence as application acceptance. Account recovery, privileged MFA and other open deployment requirements must be resolved before any production-opening decision.

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

## Deployment and distribution trajectory

The owner requested retention and progressive documentation of the additional distribution vision; [approval scope](../00-GOVERNANCE/DECISION-MANAGEMENT.md#deployment-vision-integration--2026-11-19) is not implementation authority. These are proposed milestones, not new numbered lots, deadlines or a commitment to deliver every profile.

| Stage | Retained outcome | Entry / proof boundary |
|---|---|---|
| Design now | One product with explicit deployment, topology, connectivity and resource dimensions; profile compatibility and operational contracts | [Deployment capability matrix](../02-ARCHITECTURE/DEPLOYMENT-ARCHITECTURE.md#deployment-capability-matrix); existing manifests and runtime remain unchanged |
| Finish the nucleus | L03–L08 application isolation, audit, identity, Angular and recovery | Preserve current gates and technical-only scope; the isolated development database is not a complete distribution |
| Qualify hosted and private installations | Repeatable independent installation, same-origin/TLS, supported artifacts, measured capacity and restore; scale-out when selected | Completed L1 plus selected release/environment controls; no capacity or availability claim before load/failure tests |
| Distribute and operate independently | Windows installer, controlled updates, optional desktop shell, safe fleet supervision and offline entitlement contracts | Supported OS/runtime, signed artifacts, installation identity, customer authority and interruption/recovery evidence; no business billing |
| Add selected hybrid and placement capabilities | Autonomous local/edge profiles, selective synchronization, cells and controlled tenant movement | Per-operation consistency and revocation, resource limits, identity/placement security and tested migration; not automatic table replication |
| Qualify advanced resilience | Cross-provider recovery, then multi-region or more advanced topologies where justified | Measured RPO/RTO, residency and operating-cost requirements; explicit write authority and fencing |

Portable recovery can be selected before fleet or hybrid work; independent capabilities need not wait for every earlier advanced feature. One codebase does not mean every module works in every profile: unsupported capabilities must be declared and rejected before activation. Retained candidates remain visible in the matrix until explicitly selected, rejected or superseded with a recorded reason. Kubernetes, a broker, distributed cache and multi-cloud active-active are not prerequisites. Future domain-specific synchronization belongs to future module contracts, not invented business models in the socle.

## Visual composition trajectory

**PROPOSED / NOT IMPLEMENTED.** The [retained product vision](PRODUCT-VISION.md#9-retained-composition-and-developer-vision) and [VC coverage](../PACK-INVENTORY.md#visual-composition-coverage-refinement) extend, rather than replace, the source phases and C01–C58. The [design/delivery gates](../13-IMPLEMENTATION/IMPLEMENTATION-PHASES.md#visual-composition-design-and-delivery-gates) own the sequence: finish the nucleus; select common model/extension contracts; qualify a technical slice; add a bounded visualizer/composer; then separately selected reusable development, distribution and AI capabilities. These are dependency recommendations, not dates or blanket delivery authorization.

| Source milestone | Retained placement |
|---|---|
| E P0 foundation/contracts; M discovery, gaps and constitutional consolidation | Current documentary refinement and slice-specific decisions; no constitution rewrite or universal-readiness claim |
| E P1 composition foundation; M technical blueprint completion | Selected UAM/node/extension/version contracts after O20–O25 and required existing gates |
| E P2 visual builder; E P3 data/interface composition | Later bounded visual clients and reviewed model-development pipeline; preserve native/headless operation |
| E P4 standard business modules; E full ERP POC | Future business scope only; not a technical demonstrator or automatic next lot |
| E P5 Marketplace; E P6 AI Studio | Optional independent product slices over the common governed model; retain commercial vision without adding billing to Core |
| E P7 advanced platform; M progressive implementation | Selected offline/devices/operations/advanced capabilities with measured security, resource and recovery evidence |

M's six composition studios and Basic/Advanced/Developer modes coexist with the original Studio lifecycle areas. Neither analytics nor AI is removed by terminology reconciliation. All seven M scenarios remain in [future acceptance](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md#visual-composition-acceptance); no functionality is treated as discarded merely because its implementation is deferred.

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
