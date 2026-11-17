# Non-Functional Requirements

**Decision status:** PROPOSED budgets, control details and support policies;
locked source requirements (including four languages/RTL) remain requirements.
**Delivery status:** PLANNED / NOT IMPLEMENTED. All numeric targets are
UNMEASURED; there is no backend/frontend code or benchmark/test evidence.

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

- Module boundaries must be enforceable; build enforcement is PLANNED, not running.
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
- Plan controlled health, liveness/readiness endpoints and security audit access.
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

## Related documents

- [Security Governance](../00-GOVERNANCE/SECURITY-GOVERNANCE.md)
- [Deployment Architecture](../02-ARCHITECTURE/DEPLOYMENT-ARCHITECTURE.md)
- [Test Strategy](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md)
