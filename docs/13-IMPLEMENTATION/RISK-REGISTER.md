# Risk Register

**Decision status:** PROPOSED mitigations; risks identified during foundation audit.
**Implementation status:** NOT IMPLEMENTED. A documented mitigation is not a closed runtime risk.

| ID | Severity | Risk | Required mitigation / closure evidence |
|---|---|---|---|
| R01 | Critical | Runtime DB role bypasses RLS | Separate non-owner NOSUPERUSER NOBYPASSRLS credential; assert flags and prove all-DML isolation |
| R02 | Critical | Global scope flag opens all tenants | No blanket policy bypass; bounded identity-plane own-membership lookup |
| R03 | Critical | Public bootstrap account takeover or race | Operator-only disabled-default provisioning; atomic guard and concurrency/audit tests |
| R04 | High | Custom browser token/session handling weakens auth | Framework sessions with HttpOnly cookies, CSRF and lifecycle tests; owner approval pending |
| R05 | Critical | Tenant context applied too late or leaked through pool | Same-connection transaction initialization before tenant SQL; rollback/reuse/async tests |
| R06 | High | Audit failure silently loses critical evidence | Protected state and audit same transaction, tested failure rollback; bounded separate attempt stream |
| R07 | High | Nested packages make module verification vacuous | Assert actual module names and reject intentional illegal dependency |
| R08 | High | Untrusted plugin treated as safe due to signature/classloader | Compiled reviewed first-party only; external sandbox design before executable third-party packages |
| R09 | High | Duplicate durable mechanisms cause double processing | Spike and select one publication mechanism; dedup and side effects same consumer transaction |
| R10 | High | Shared DB backup mistaken for tenant restore | Whole restore drill first; separate selective-restore consistency design |
| R11 | High | Company/site grants absent under tenant-only isolation | Define hierarchy and compound constraints now; test in-tenant privilege boundaries |
| R12 | Medium | Version metadata mistaken for compatibility | Official support matrix + clean build + database/browser tests before pinning |
| R13 | High | Premature APPROVED/IMPLEMENTED labels mislead later agents | Separate decision and delivery status; real evidence for each gate |
| R14 | Medium | Extensive vision delays or expands the socle | Preserve blueprint, tier documentation, exclude all business modules and speculative engines |
| R15 | Medium | Unrealistic machine/performance promises | Measure current environment and workload; targets unmeasured until benchmark evidence |
| R16 | High | Stale request/cache shows another tenant after switch | Cancel/ignore late responses and clear scope-specific client state; browser race test |
| R17 | High | Secret defaults or database port exposed to LAN | No usable committed password; loopback-only dev publish; runtime never bootstrap credential |
| R18 | Medium | Owner confidentiality overclaimed against infrastructure admins | Explicit trust model; RLS not cryptographic protection from a host/DB superuser |
| R19 | Medium | Unapproved language/design reduction | Preserve FR/EN/IT/AR and RTL; no imported legacy theme assumptions |
| R20 | High | Minimal nucleus presented as the complete extensible socle | Distinguish L1/L2/L3 and row-level coverage; G0 scope review and L08 qualification cannot close deferred capabilities |
| R21 | High | Technical demonstration introduces business tables/screens/rules | Technical-only providers/consumers and inert test fixtures; reject business features at every lot review, including purported reference modules |
| R22 | High | Audit arrives after protected provisioning or mutations | L04 audit before L05 exposure; integration fault injection must prove rollback; G3/G4 cannot pass separately on incompatible implementations |
| R23 | High | Capabilities enumerated without ownership or dependency/failure contracts | Track matrix gaps, concrete public contracts and negative cases; L06 validates actual baseline graph, L09 extends it without cyclic private access |
| R24 | High | Release lacks data lifecycle, key rotation or incident response | C45/C47–C50 must have slice-specific baseline requirements before L08 and complete production-readiness decisions before deployment; exercise recovery and revocation |
| R25 | High | Optional engine or phase label mistaken for implementation authority | All L09–L11/L3 allocations remain proposals; record approval per selected scope and N0 decision before code |
| R26 | High | Supply-chain or third-party license obligations confused with commercial licensing | C48 SBOM, vulnerability and dependency-license evidence before distribution; signing/provenance checks separately from C36 entitlement behavior |
| R27 | Critical | Multi-instance saturation or stale sessions/grants defeat availability and authorization | Bound aggregate database pools and queues across nodes/workers; enforce backpressure and cross-node session/CSRF/revocation behavior. Prove node-loss recovery, stale-grant denial and overload behavior; exercise supported mixed versions, migration locking and drain without duplicate writers/jobs |
| R28 | High | Installer, updater or uninstall damages unrelated services or user data | Preflight port/path/service collisions; never replace unrelated resources. Reject corrupted, unsigned or wrong-platform artifacts; interrupt install/update and restore a consistent supported version/data pair. Prove data-preserving uninstall with deletion separately authorized |
| R29 | Critical | Compromised fleet control or cloned installation identity widens remote authority and leaks metadata | Installation-bound credentials, managed-restore re-enrollment/rekey and revocation; duplicate detection is limited to observable connected behavior, not guaranteed for offline copies of credentials without separately approved hardware trust. Explicit consent, least-privilege scoped remote administration and audit, with no implicit tenant access. Prove stale/expanded authority denial, metadata redaction and safe local operation during central outage; bound telemetry and test disk saturation |
| R30 | Critical | Synchronization resurrects deleted data or replays revoked offline authority | Separate local operation from synchronization; durable bounded queues/checkpoints, idempotent replay, schema/conflict handling and current reauthorization. Test tombstones and resnapshot after long disconnection; signed bound entitlements require expiry and clock-rollback handling, with no promise of immediate offline revocation |
| R31 | Critical | Cell split-brain or stale/spoofed routing creates competing tenant writers or cross-tenant access | Authenticate routing and reject stale maps; fence writers across tenant migration and rollback. Prove consistent identity/files/jobs/audit and single-writer cutover; identity-plane discovery must never become a universal isolation bypass |
| R32 | High | Update/artifact mismatch or unavailable backup secrets/keys makes cross-provider recovery unusable | Preserve verified signed platform-compatible artifacts and schema/version compatibility with controlled recoverable encryption keys/secrets. Perform isolated cross-provider restore and interruption/rollback drills, fail explicitly on missing keys or incompatible versions, verify data/files/identity/jobs/audit consistency and measure achieved RPO/RTO |
| R33 | High | UAM or a studio becomes a second authority for capabilities, permissions or business state | Reuse owning registries/APIs and one versioned semantic model; forbid studio-private grants and direct module-table access. Prove equivalent native/visual/SDK/AI behavior with inert fixtures. Next decision: approve authoritative ownership and public adaptation contracts before model/runtime work (O20/O25) |
| R34 | High | Cross-studio edits or dependency updates drift versions and activate an unreviewed composition | Pin semantic, node/schema/contract and dependency versions; optimistic revision checks, exact-revision approval and controlled publication; preserve in-flight versions. Test stale/concurrent edits, round-trip imports and invalidated approvals. Next decision: approve revision/compatibility and cross-studio conflict policy (O20/O21/O23) |
| R35 | Critical | Visualizer metadata exposes hidden tenants, schemas, devices, configuration or implementation details | Filter metadata, relationship edges, counts/search, errors, traces, exports and caches under current tenant/company/site permissions, not only record data. Test revocation and late tenant-switch responses. Next decision: approve metadata visibility/redaction and retention contracts (O22/O25 with O9/O13) |
| R36 | Critical | Imported/generated code or prompt/UI injection escapes trust boundaries or self-assigns permissions | Treat all generated/imported content as untrusted; no grant creation by developer modes or manifests. Require separately approved isolated execution/build trust model, effect-free simulations, exact-revision human approval and bounded network/filesystem/secrets access. Signature/classloader is not a sandbox. Test injection/escape and stale authority denial. Next decision: approve supported code/trust classes and isolation evidence (O24/O25 with O1/O2/O7/O13) |
| R37 | Critical | A device timeout or partial batch retry duplicates physical effects or loses outcome evidence | Durable atomic counter/claim and dispatch intent, scoped event/command deduplication, stable per-item identities and reconciliation; preserve successful items and expose UNKNOWN outcomes before authorized retry/reprint. No exactly-once printing or automatic physical rollback promise. Next decision: approve adapter acknowledgement, retry/resume, manual reconciliation and compensation contracts (O21/O24 with O12/O15/O18) |
| R38 | High | Graph parsing, composite expansion or execution exhausts CPU, memory, queues, connections or paid providers | Bound size/depth/fan-out/steps/time/concurrency and transitive references at validation and execution; aggregate quotas, admission/backpressure and tenant fairness include retries/delegations. Measure p95 and node/device/provider usage on declared workloads. Next decision: approve enforceable profile budgets and cost/overload policy (O21/O23 with O11/O16) |
| R39 | Critical | Generated DDL silently changes ownership, bypasses RLS or destroys data | Schema generation produces reviewed proposals only; owning migration process, approved N0 database contracts, restricted runtime, expand/contract and isolated restore tests precede execution. No table per node, arbitrary SQL or automatic destructive migration. Next decision: approve supported schema changes, ownership and migration/recovery policy (O20/O24/O25 with O10/O14) |
| R40 | Critical | Incompatible or hostile imports exploit archives/parsers/references or fetch arbitrary dependencies through SSRF | Reject unsupported versions/digests/contracts, path traversal/escaping links, duplicate entries, archive bombs and unbounded parser/reference expansion before mutation. Resolve pinned approved local/registry references without arbitrary URL or automatic dependency fetching; separately approve allowlisted acquisition and SSRF/redirect/DNS checks. Test rejection without side effects. Next decision: approve package/import dialect, limits, compatibility and acquisition policy (O20/O24 with O1/O15) |

R27–R32 track the proposed [deployment contract](../02-ARCHITECTURE/DEPLOYMENT-ARCHITECTURE.md#deployment-capability-matrix); their acceptance evidence remains NOT RUN. These additions authorize neither infrastructure changes nor test execution.

R33–R40 are contiguous additions for AP-ARCH-EVOL-001 / AP-MASTER-CONSTITUTION-001,
**PROPOSED / NOT IMPLEMENTED**, under the
[documentation-only authorization](../00-GOVERNANCE/DECISION-MANAGEMENT.md#visual-composition-documentation-authorization--2026-11-20).
Use the [coverage refinement](../PACK-INVENTORY.md#visual-composition-coverage-refinement),
[O20–O25 decision register](../00-GOVERNANCE/DECISION-MANAGEMENT.md#4-open-decisions)
and [future acceptance matrix](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md#visual-composition-acceptance)
for ownership, scoped decisions and closure evidence, without duplicating their
contracts. No mitigation or test for these proposed composition contracts is claimed
implemented/executed. Bounded closed L03 is now locally IMPLEMENTED / TESTED:
the [recovery evidence](../PACK-INVENTORY.md#l03-main-recovery-evidence--2026-11-20)
records exact default/type/column-privilege checks, a packaged no-op and 31 actual
runtime HTTP checks after the retained default-ACL comparison failure. Main's four
technical tables remain empty. These checks do not close user-authorization,
clean-machine provisioning, production recovery or advanced-composition risks.

Responsibility defaults to the project owner for approval and future platform/security maintainers for execution; no team assignment is invented. Close a risk only with evidence linked from a validation gate, or explicitly accept it with an owner decision and bounds.

## Implementation delta — 2026-11-28

The table above states required mitigations, not their current delivery state. At revision `27e9b14`:

- R01, R02 and R05 have partial code and isolated tests (restricted non-owner runtime role, bounded own-membership lookup, same-connection tenant context). Pool-reuse, rollback and concurrency proof is missing.
- R03 and R04 have operator-only provisioning and framework sessions implemented and isolated-tested.
- R06 has atomic audit tested in isolation; retention, redaction and integrity remain open.
- R10, R11 and R15 have no drill and no benchmark.
- R13 is now supported by the single status source in [CURRENT-STATE](../15-FOUNDATION-9.5/CURRENT-STATE.md).

No entry above is closed by this note. See the [open risks register](../15-FOUNDATION-9.5/OPEN-RISKS-AND-UNKNOWNs.md) for labels and unknowns.

## Related

- [Validation gates](VALIDATION-GATES.md)
- [Decision management](../00-GOVERNANCE/DECISION-MANAGEMENT.md)
- [Source blueprint](../MASTER-BLUEPRINT.md)
