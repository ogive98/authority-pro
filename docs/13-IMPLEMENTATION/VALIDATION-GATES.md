# Validation Gates

**Decision status:** PROPOSED gate criteria.
**Runtime status:** No application build, database isolation test or browser test has passed yet.

## G0 — Documentation and contracts

Check every current-pack file and relative link/anchor, preserve archive bytes, validate the draft-2020-12 schema with a real validator, accept/reject fixtures as intended, parse Mermaid syntax and manually review architecture consistency. Confirm each approval is bounded to an actual owner decision. Record remaining proposals explicitly.

Manually check the [technical coverage matrix](../PACK-INVENTORY.md#technical-coverage-matrix): every selected row has an accountable owner, dependencies, delivery level/lot, contract gap and acceptance test. Every source engine is accounted for as a bounded baseline or explicit deferral; no business feature or demonstrator has entered the lots. Distinguish the eight-lot L1 nucleus from proposed L2/L3 capabilities. The validator does not prove semantic completeness or an acyclic runtime dependency graph.

Passing G0 proves document/fixture checks only. It is not approval of N0 mechanisms and does not certify runtime security or visual diagram rendering.

## G1 — Approved foundation design

The [six foundation orientations](../00-GOVERNANCE/DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations) are owner-approved. Before each implementation slice, obtain approval of its remaining detailed N0 contracts: session/CSRF configuration and provisioning guards, membership policy and exact role grants/context integration, audit taxonomy and transaction integration, HTTP/probe exposure and component export graph. Resolve supported Java/Spring/session/OpenAPI/Angular/TypeScript/Node/PostgreSQL versions with compatibility evidence, not merely registry publication. Approve optional Modulith/ArchUnit/Flyway selections only if adopted.

Define password, expiry, recovery and timeout policies before their implementation. Record whether privileged MFA and additional account lifecycle controls are required before production opening; an unresolved production condition cannot be relabeled as a released feature. Secret rotation, platform-data retention and operational responsibility also need explicit slice-specific requirements.

G1 records approval, never application delivery. Unrelated L3 choices do not block L02; durable-publication selection becomes mandatory before L10. See the [open-decision mapping](MVP-BOUNDARIES.md#scope-decisions-still-needed).

## G2 — Executable skeleton

Fresh local dependency installation and build succeed; application starts against its new dedicated database on the existing PostgreSQL server with approved migration/runtime roles. Verify endpoint and project names before provisioning, abort on collisions, and leave the other application's database, roles and service configuration untouched. Invalid configuration fails closed; module discovery reports the expected components and an intentional illegal dependency fails. No fake healthy status or default admin credentials.

L02 exits on its bounded skeleton checks without claiming full G2. L03 supplies the actual PostgreSQL role/migration integration and completes G2; L03 does not require full G2 as an entry condition. L02/L03 evidence includes bounded requests, safe errors/logs, restricted probes and separated development/test credentials/data. Reproducible backup/restore and security checks begin with the first database-bearing slice; L08 repeats their integrated evidence rather than introducing controls at the end.

## G3 — Identity and isolation

Run CSRF, session lifecycle, provisioning race, own-membership and authorization tests. RLS matrix covers read/insert/update/delete with actual restricted role, missing context, company/site mismatch, pool reuse, rollback and concurrency. Platform owner without tenant membership cannot read tenant data. Any leakage blocks the gate.

L03 infrastructure tests do not prove user-level authorization until L05 integration. Provisioning and sensitive identity changes require the L04 audit implementation, including failure rollback, before exposure. Test technical structures only; no accounting, HR, fleet or other business fixtures are needed.

## G4 — Audit, UI and recovery

Protected writes and audit are atomic under failure injection. Verify actual framework session/login/logout integration rather than assuming every framework operation shares a transaction. Browser golden/edge paths work, including stale tenant responses, permissions, all required locales/RTL, keyboard navigation and both themes. No UI feature is accepted solely on type checks or unit tests.

Backup restore is performed in an isolated destination and checked for state/audit consistency. Record measured recovery time/data-loss bounds, key availability and remaining production limitations. Whole-instance recovery does not close tenant-selective restore or PITR requirements.

## G5 — Repository and release readiness

Review the actual changes, secret/dependency scans, SBOM, third-party license obligations, lockfiles, repeatable CI checks and remaining risks. Attach only genuinely executed CI evidence; a workflow file is not a remote run. L08 closes the agreed L1 scope, not every matrix row or full production readiness.

Ask separately for remote name/owner/visibility and publishing operations. No commit, remote resource, release or production deployment is authorized by the current local work. Unresolved production security, data-retention or operational requirements block production release even when bounded local L1 tests pass.

## Extension-slice acceptance

Reapply the relevant gates to each explicitly approved L2/L3 slice and add its row-specific tests; existing L1 evidence cannot certify a new engine.

- **L09:** technical fixtures prove dependency cycles/missing providers/ambiguity/incompatibility rejection, safe disablement, permission-change behavior and dry-run impact. Retained data and migration recovery limits are explicit. Opted-in command idempotency is tested separately from event inbox deduplication: reauthorize replay, reject request-fingerprint conflicts and prove atomic results/audit plus approved key expiration.
- **L10:** prove one producer publication mechanism, inbox/effect atomicity, crash recovery, ordering, stale-lease rejection, bounded retries, failure queue, authorized replay/cancellation and tenant-scoped workers. A scheduled job additionally needs restart/misfire/timezone tests.
- **L11:** accept selected SDK/UI/governance capabilities separately. Test contributions without business modules; check schema compatibility, real browser accessibility/localization, scoped export/retention, access reviews, rotation, artifact trust and environment boundaries where selected.
- **L3:** define a bounded use case, threat model, dependency and acceptance contract before implementation. Scripts/AI/third-party code require explicit authority and confinement; no general-purpose execution follows from having a descriptor.

## Evidence format

Record date, capability row IDs, lot and delivery level, decision/approval reference, revision or content identity, command, tool versions, tested scope, expected/observed result and residual limitations. If no Git commit exists, state that and identify tested content without inventing a commit hash. Link real artifacts only.

Failed or unexecuted checks remain failed/not run; do not replace evidence with planned commands or declarations. Reviewers and approvers are named only when actually assigned. A documentation test result cannot advance an application row from NOT IMPLEMENTED.

## Related

- [Test strategy](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md)
- [Implementation phases](IMPLEMENTATION-PHASES.md)
- [Risk register](RISK-REGISTER.md)
- [Coverage matrix](../PACK-INVENTORY.md#technical-coverage-matrix)
