# Non-Functional Requirements

**Decision status:** PROPOSED budgets, control details and support policies;
locked source requirements (including four languages/RTL) remain requirements.
**Delivery status:** L02 no-database backend skeleton IMPLEMENTED / TESTED;
see [bounded execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18).
All numeric targets below remain UNMEASURED: skeleton verification is not a
performance benchmark, frontend test or integrated foundation acceptance.
Closed L03 technical local integration is IMPLEMENTED / TESTED within bounded
scope; see [current main recovery evidence](../PACK-INVENTORY.md#l03-main-recovery-evidence--2026-11-20).
Main V1 was already applied (count 1); the latest authorized recovery passed with a
no-op migration (count 0) and 31 real `data-local` HTTP checks. All four technical
tables remain empty. This is not a performance benchmark or production-readiness
claim and does not implement user authentication, membership validation, audit,
Angular or business functionality. Full G2 clean-machine provision reproduction is
not established; G3–G5 remain open. No backend test or performance measurement is
run by this documentation update.

Targets below are candidates for review, not guarantees, approved service levels
or proof that a 2 GB deployment or 500 ms login is achievable.

## 1. Performance

| Metric | PROPOSED unmeasured target | Required measurement |
|---|---|---|
| API p95, socle reads | < 300 ms | Warm JVM, defined authenticated read mix and tenant dataset |
| Login p95 | < 500 ms | Full Spring Security/session/audit path; BCrypt cost 12 is a candidate to benchmark, not an approved setting |
| API p99 | < 1 s | Candidate 50-concurrent-user load; app CPU allocation and resource contention recorded |
| Page size (lists) | Maximum 100 rows, default 25 | Proposed API contract and payload-size tests |
| Backend startup | < 30 s | Defined cold/warm environment; distinguish migration time and readiness |

A reproducible workload must record the code revision, pinned dependency versions,
hardware/OS/container limits, JVM flags, database configuration, dataset size and
shape (tenants, users, records), request mix, payloads, concurrency/arrival rate,
warm-up, duration, repetitions, network topology and error rate. Include successful
and rejected authentication, session persistence, CSRF, RLS and durable audit costs.
Publish commands/scripts and raw results with percentiles; no result is claimed yet.

Benchmark BCrypt independently and in the full login path on the reference CPU,
including contention and abuse/throttling load. Review work factor, security and
capacity together rather than weakening hashing to assert a 500 ms promise.

## 2. Resource profile

- Candidate development budgets: JVM heap 512 MB and PostgreSQL container
  256 MB. These are unvalidated starting hypotheses, not recommended limits.
- Candidate small deployment: 2 vCPU, 2 GB RAM, 20 GB disk. Validate full-process
  RSS, database/cache/WAL, OS/proxy overhead, backups, concurrency and headroom;
  heap size alone cannot establish capacity. Increase sizing if measurements
  require it; no minimum viable deployment is certified.
- Preserve the source principle of operational simplicity: do not introduce an
  external broker, cache or search cluster solely for theoretical scalability.
- Deployment modes (local/hosted/hybrid) are distinct from resource profiles.

## 3. Availability and recovery

| Deployment vision | PROPOSED availability target | PROPOSED RPO | PROPOSED RTO |
|---|---|---|---|
| Local (Lite) | Best effort, single node | 24 h | 4 h |
| Hosted (Standard) | 99.5% monthly | 15 min, subject to WAL/PITR design | 1 h |
| Hybrid | To define per installation and offline scope | 24 h candidate | 4 h candidate |

None is approved, measured or backed by implemented backup automation. Define the
failure model, dataset, backup cadence, retention, restore procedure and timed
restore drill before accepting a target. A backup is not reliable until restoration
has been tested; no successful drill is asserted here.

## 4. Security

- No secrets in source, committed configuration, logs or frontend bundles.
- Enforce and test backend authorization and tenant isolation.
- Proposed transport policy: TLS outside localhost; reviewed vulnerability gates
  before release.
- Proposed authentication: Spring Security + Spring Session JDBC, same-origin
  HttpOnly session cookies, Secure outside localhost and CSRF protection. No
  custom browser bearer session storage or claimed SHA-256 token persistence.
- Proposed bootstrap: closed-default, one-time operator CLI; no public setup route.
- Proposed database runtime: non-superuser, non-owner, NOBYPASSRLS role, separate
  migration role, transaction-local verified tenant/user context, no global RLS bypass.
- Proposed audit durability: sensitive writes commit atomically with security
  audit or are rejected; anonymous failure events use bounded separate transactions
  without secrets. Do not silently swallow audit failures.

See [Security Governance](../00-GOVERNANCE/SECURITY-GOVERNANCE.md) for the proposed
controls and tests. These statements are not an implemented security certification.

## 5. Scalability

- Multiple backend instances are a validation scenario, not a proven capability.
- Proposed JDBC-backed sessions avoid process-local session authority; verify
  revocation, concurrent requests, CSRF behavior and transaction-local tenant
  context with actual multi-instance tests before claiming support.
- Future workers/schedulers need a reviewed coordination design and duplicate
  execution tests. Database locks or leader election remain proposals.

## 6. Maintainability

- L02 build tests enforce exactly five closed module roots and zero intermodule
  edges, including negative fixtures. Later functional exports, consumers and
  persistence boundaries require their own approved contracts and tests.
- Public contracts must be documented and versioned; cyclic dependencies are prohibited.
- Immutable released migrations and forward corrections with restore procedures
  are proposed policy; no released migration history exists.

## 7. Accessibility and localization

- WCAG 2.2 AA is a PROPOSED acceptance target, not a conformance claim.
- Keyboard navigation is required for navigation, dialogs and forms.
- FR, EN, IT and AR with RTL are required by blueprint section 15. FR/EN-first
  and IT/AR-later rollout is unapproved and cannot silently reduce acceptance scope.
- Proposed implementation externalizes user-facing text through an i18n layer;
  test direction, layout, formatting and language selection for all four languages.

## 8. Observability

- Plan structured logs with correlation id and verified tenant id where applicable;
  anonymous/control-plane events must not invent a tenant or disclose secrets.
- L02 implements only exact loopback GET liveness (200/UP) and readiness
  (503/OUT_OF_SERVICE); other requests are denied. Integrated dependency health
  and protected security-audit access remain planned.
- Request latency, error rate, database pool and future queue metrics remain
  planned; the Phase 3 allocation is proposed, not a delivered monitoring system.

## 9. Data integrity

- Approved strategy: `tenant_id` + RLS for tenant-scoped data in shared PostgreSQL.
- Proposed organization model: tenant = organization, with companies and sites;
  this is business-neutral infrastructure, not an industry module.
- Audit timestamps, optimistic-lock fields, soft deletion and UUID strategy are
  proposed per-entity conventions. Document purpose and lifecycle; do not add
  every metadata field universally without a need.

## 10. Browser and device support

PROPOSED support matrix: current and previous major Chromium, Firefox and Safari;
candidate desktop viewport 1280×720, with responsive behavior to validate.
This is not tested support or an approved exclusion of other devices. Review
accessibility and RTL layouts at defined viewports before fixing the matrix.

## 11. Composition resource and independence budgets

**PROPOSED / NOT IMPLEMENTED / UNMEASURED.** These AP-ARCH-EVOL-001 /
AP-MASTER-CONSTITUTION-001 refinements are covered by the
[documentation authorization](../00-GOVERNANCE/DECISION-MANAGEMENT.md#visual-composition-documentation-authorization--2026-11-20)
and [coverage refinement](../PACK-INVENTORY.md#visual-composition-coverage-refinement).
[O20–O25](../00-GOVERNANCE/DECISION-MANAGEMENT.md#4-open-decisions), applicable N0
contracts and measured acceptance must precede each selected implementation;
no numeric quota, capacity claim or new execution service is approved here.

| Boundary | Required bounded workload and enforcement proposal |
|---|---|
| Validation/import | Cap encoded/expanded bytes, nodes/edges, schema and reference sizes, parser/reference depth, composite nesting, transitive expansion and fan-out; reject recursive/unbounded structures before execution. Limit validation CPU, memory, elapsed time and concurrent requests, not just final graph size |
| Execution | Bound steps/iterations, depth, branch fan-out, input/output and intermediate-state size, CPU/memory, wall-clock and per-node deadlines; count nested composites, retries and delegated calls against the parent budget rather than resetting it |
| Asynchronous work | Long or effectful batches use bounded work units/checkpoints, not an indefinitely blocked HTTP request. Bound queue length/bytes/age, workers, concurrent runs, attempts and retry time; apply admission control, backpressure and tenant fairness. Explicit overload/pending/failure and cancellation semantics must preserve already committed or unknown effects |
| Shared resources | Bound aggregate database connections, device dispatches and provider calls across all workers/nodes, including recovery and rollout overlap. Limit telemetry cardinality/storage, metadata retrieval and caches; no per-node process or mandatory broker follows from a graph |
| Usage/cost | Attribute CPU/time, bytes, steps, retries, queue wait and external calls to authorized tenant/company/site, application/revision, execution and node; attribute device operations and AI/provider/model usage separately. Separate estimated/reserved cost, observed usage and billed cost; bound labels and redact payloads |

Benchmark graph validation and execution separately under the section 1 protocol.
Record graph depth/fan-out/composite expansion, payload sizes, port/schema checks,
dataset and tenant mix, synchronous/asynchronous ratio, device/provider latency,
concurrency, cache state, rejected work and fault windows. Measure p95 (and tail
latency), throughput, memory, saturation, recovery and isolation with authorization,
RLS and audit enabled where applicable. A small graph or ten-item scenario is not
proof of arbitrary graph size or concurrent-user capacity; reject unsupported
workloads rather than asserting a magic capacity. Cost/resource telemetry must
remain useful during offline buffering within declared storage limits.

Core/headless operation and native module APIs must remain independent of optional
Visual/Development Studio, AI providers and Marketplace availability. Their outage
must not stop unrelated, already authorized native or published headless operations
within declared dependency/entitlement validity windows; an actually dependent
capability must show its bounded blocked/degraded state, never false success.
Do not load every installed package to discover it. Qualify cold/lazy loading,
metadata pagination, studio-disabled startup, offline operation and reconnection
on each supported profile. Verify these budgets and outage cases through the
[future composition acceptance](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md#visual-composition-acceptance),
not documentation or L02 unit-test success.

## Related documents

- [Security Governance](../00-GOVERNANCE/SECURITY-GOVERNANCE.md)
- [Deployment Architecture](../02-ARCHITECTURE/DEPLOYMENT-ARCHITECTURE.md)
- [Test Strategy](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md)
