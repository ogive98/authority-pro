# Validation Gates

**Decision status:** PROPOSED gate criteria.
**Runtime status:** L02 no-database build and its 69 tests passed locally, including an offline clean repeat. Full G2–G5, integrated application isolation and browser acceptance remain open; see [execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18).

## G0 — Documentation and contracts

Check every current-pack file and relative link/anchor, preserve archive bytes, validate the draft-2020-12 schema with a real validator, accept/reject fixtures as intended, parse Mermaid syntax and manually review architecture consistency. Confirm each approval is bounded to an actual owner decision. Record remaining proposals explicitly.

Manually check the [technical coverage matrix](../PACK-INVENTORY.md#technical-coverage-matrix): every selected row has an accountable owner, dependencies, delivery level/lot, contract gap and acceptance test. Every source engine is accounted for as a bounded baseline or explicit deferral; no business feature or demonstrator has entered the lots. Distinguish the eight-lot L1 nucleus from proposed L2/L3 capabilities. The validator does not prove semantic completeness or an acyclic runtime dependency graph.

Passing G0 proves document/fixture checks only. It is not approval of N0 mechanisms and does not certify runtime security or visual diagram rendering.

## G1 — Approved foundation design

The [six foundation orientations](../00-GOVERNANCE/DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations) are owner-approved. Before each implementation slice, obtain approval of its remaining detailed N0 contracts: session/CSRF configuration and provisioning guards, membership policy and exact role grants/context integration, audit taxonomy and transaction integration, HTTP/probe exposure and component export graph. Resolve supported Java/Spring/session/OpenAPI/Angular/TypeScript/Node/PostgreSQL versions with compatibility evidence, not merely registry publication. Approve optional Modulith/ArchUnit/Flyway selections only if adopted.

Define password, expiry, recovery and timeout policies before their implementation. Record whether privileged MFA and additional account lifecycle controls are required before production opening; an unresolved production condition cannot be relabeled as a released feature. Secret rotation, platform-data retention and operational responsibility also need explicit slice-specific requirements.

G1 records approval, never application delivery. The [2026-11-18 owner selections](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l02-skeleton-decisions--2026-11-18) settle the bounded L02 target JDK/build, five-root empty graph, boundary tooling and closed local HTTP design. Target-JDK/wrapper, package discovery, configuration, HTTP and packaged-launcher checks now provide bounded L02 implementation evidence, not full functional socle acceptance. The [L03 local-environment authorization](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l03-local-environment-authorization--2026-11-18) additionally approves the bounded dedicated PostgreSQL 16.15 environment, not Flyway adoption or application tenancy mechanics. Other applicable G1 decisions remain open. Unrelated L3 choices do not block L02; durable-publication selection becomes mandatory before L10. See the [open-decision mapping](MVP-BOUNDARIES.md#scope-decisions-still-needed).

## G2 — Executable skeleton

Fresh local dependency installation and build succeed; application starts against its new dedicated database in the authorized dedicated PostgreSQL instance with approved migration/runtime integration. The bounded environment is PostgreSQL 16.15 in a new `authority-pro-postgres` container on `127.0.0.1:5434`, new data/secrets volumes, its own network, separate operator/owner/migrator/runtime identities and 256 MiB / 0.5 CPU limits. Verify endpoint and unused resource/database/role names before provisioning, abort on collisions, and leave the other application's database, ACLs, roles and service configuration and the concurrent project untouched. Invalid configuration fails closed; module discovery reports the expected components and an intentional illegal dependency fails. No fake healthy status or default admin credentials.

L02 exits on the [approved no-database skeleton contract](../adr/ADR-0011-api-versioning.md#l02-local-http-approval--2026-11-18), not the integrated startup above: selected JDK/wrapper build, exact five-root empty graph, negative boundary fixtures, validated local startup, status-only probes with readiness 503 and denial elsewhere; no account, session, datasource or migration. An occupied port must not lead to stopping another service. None of this claims full G2. Completion of G2 requires actual PostgreSQL role/migration integration under separately approved application mechanics; L03 does not require full G2 as an entry condition. Both slices require safe errors/logs and bounded configuration; data/credential separation, backup/restore and database security checks begin with L03. L08 repeats their integrated evidence rather than introducing controls at the end.

The L01/L02 no-database evidence remains historical. The authorized L03 environment initialization is not Flyway approval or implemented tenant logic and cannot close G2. The separately authorized closed L03 application migrations/RLS/JDBC integration is IN PROGRESS: [local application qualification](../PACK-INVENTORY.md#l03-application-qualification-checkpoint--2026-11-20) passed 82 backend tests, 111 persistence checks and 24 restored-target checks, but the main database is not migrated and valid credential-bearing packaged launches remain untested. This checkpoint does not close L03 or G2; identity, audit and Angular remain unimplemented. The subsequently authorized [isolated qualification](../PACK-INVENTORY.md#l03-isolated-qualification-evidence--2026-11-19) passed technical Flyway/JDBC/RLS and same-cluster restore checks only; it does not close G2 or production recovery acceptance. The [environment evidence](../PACK-INVENTORY.md#l03-local-environment-evidence--2026-11-18) records 22 passing tooling tests, the empty dedicated database, actual host authentication, 11 negative privilege checks and a rolled-back DDL probe. Those results do not demonstrate tenant isolation or application integration.

## G3 — Identity and isolation

Run CSRF, session lifecycle, provisioning race, own-membership and authorization tests. RLS matrix covers read/insert/update/delete with actual restricted role, missing context, company/site mismatch, pool reuse, rollback and concurrency. Platform owner without tenant membership cannot read tenant data. Any leakage blocks the gate.

L03 infrastructure tests do not prove user-level authorization until L05 integration. Provisioning and sensitive identity changes require the L04 audit implementation, including failure rollback, before exposure. Test technical structures only; no accounting, HR, fleet or other business fixtures are needed.

## G4 — Audit, UI and recovery

Protected writes and audit are atomic under failure injection. Verify actual framework session/login/logout integration rather than assuming every framework operation shares a transaction. Browser golden/edge paths work, including stale tenant responses, permissions, all required locales/RTL, keyboard navigation and both themes. No UI feature is accepted solely on type checks or unit tests.

Backup restore is performed in an isolated destination and checked for state/audit consistency. Record measured recovery time/data-loss bounds, key availability and remaining production limitations. Whole-instance recovery does not close tenant-selective restore or PITR requirements.

## G5 — Repository and release readiness

Review the actual changes, secret/dependency scans, SBOM, third-party license obligations, lockfiles, repeatable CI checks and remaining risks. Attach only genuinely executed CI evidence; a workflow file is not a remote run. L08 closes the agreed L1 scope, not every matrix row or full production readiness.

The existing private repository and normal L02 commit/push are explicitly owner-authorized in the [decision log](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l02-implementation-and-publication-authorization--2026-11-18). This does not authorize a new remote, release or production deployment. Unresolved production security, data-retention or operational requirements block production release even when bounded local tests pass.

## Extension-slice acceptance

Reapply the relevant gates to each explicitly approved L2/L3 slice and add its row-specific tests; existing L1 evidence cannot certify a new engine.

- **L09:** technical fixtures prove dependency cycles/missing providers/ambiguity/incompatibility rejection, safe disablement, permission-change behavior and dry-run impact. Retained data and migration recovery limits are explicit. Opted-in command idempotency is tested separately from event inbox deduplication: reauthorize replay, reject request-fingerprint conflicts and prove atomic results/audit plus approved key expiration.
- **L10:** prove one producer publication mechanism, inbox/effect atomicity, crash recovery, ordering, stale-lease rejection, bounded retries, failure queue, authorized replay/cancellation and tenant-scoped workers. A scheduled job additionally needs restart/misfire/timezone tests.
- **L11:** accept selected SDK/UI/governance capabilities separately. Test contributions without business modules; check schema compatibility, real browser accessibility/localization, scoped export/retention, access reviews, rotation, artifact trust and environment boundaries where selected.
- **L3:** define a bounded use case, threat model, dependency and acceptance contract before implementation. Scripts/AI/third-party code require explicit authority and confinement; no general-purpose execution follows from having a descriptor.

## Visual composition architectural readiness

**PROPOSED / NOT IMPLEMENTED — separate from implementation gates.** The
[documentation-only authorization](../00-GOVERNANCE/DECISION-MANAGEMENT.md#visual-composition-documentation-authorization--2026-11-20)
permits consolidation of AP-ARCH-EVOL-001 and AP-MASTER-CONSTITUTION-001, not a
Constitution amendment, executable contract, new infrastructure or implementation
phase. Use the [coverage refinement](../PACK-INVENTORY.md#visual-composition-coverage-refinement)
and [open decisions O20–O25](../00-GOVERNANCE/DECISION-MANAGEMENT.md#4-open-decisions)
as the shared mapping, not another capability catalogue or roadmap.

Architectural readiness requires reviewed, consistent specifications with named
responsibility areas, owned/versioned public contracts, dependencies, failure and
recovery semantics, trust/tenant boundaries, compatibility/profile/resource limits
and acceptance criteria. Trace all seven [future scenarios A–G](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md#visual-composition-acceptance)
to these contracts, preserving native functionality and one authoritative owner
for each semantic, registry, execution or domain fact across studios. Record unresolved choices and their blocking scope;
a paragraph saying “extensible” or a successful document check is not full
architectural readiness. An open N0 decision cannot be silently marked approved.

The scenarios are **design-only future business acceptance**, not permission to
create orders, printers, business tables/modules or a complete ERP proof of concept.
Only inert technical fixtures may be proposed for a separately authorized technical
slice; no fixtures are created by this consolidation. Neither this review nor the
future scenarios redefine L01–L08/L1 success as the whole ERP or close G2–G5.

Before **each** selected slice:

1. Bound the scope and resolve/approve applicable detailed N0 contracts and the
   relevant O20–O25 decisions (plus existing identity, data, trust, AI, licensing or
   deployment decisions). Record approval before code, schemas, grants, migration,
   external services or physical effects; authorize execution targets separately.
2. Reuse existing owners/APIs and specify positive/negative technical cases for the
   exact revision, including authorization, compatibility, resource limits and
   rollback/reconciliation boundaries. No new role or permission follows from a
   developer mode, package signature or subscription.
3. Only after authorized implementation, attach actual executed evidence to the
   relevant G2–G5/extension gates. Separate document review, inert technical proof,
   business acceptance and production qualification; unrun cases remain NOT RUN.

**Current baseline:** L03 closed application integration is IN PROGRESS / NOT
QUALIFIED; the main `authority_pro` database is not migrated. Earlier L02 and
isolated L03 evidence remains historical and bounded, not qualification of current
L03 changes. No backend test is run by this documentation consolidation.

## Evidence format

Record date, capability row IDs, lot and delivery level, decision/approval reference, revision or content identity, command, tool versions, tested scope, expected/observed result and residual limitations. If no Git commit exists, state that and identify tested content without inventing a commit hash. Link real artifacts only.

Failed or unexecuted checks remain failed/not run; do not replace evidence with planned commands or declarations. Reviewers and approvers are named only when actually assigned. A documentation test result cannot advance an application row from NOT IMPLEMENTED.

## Related

- [Test strategy](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md)
- [Implementation phases](IMPLEMENTATION-PHASES.md)
- [Risk register](RISK-REGISTER.md)
- [Coverage matrix](../PACK-INVENTORY.md#technical-coverage-matrix)
