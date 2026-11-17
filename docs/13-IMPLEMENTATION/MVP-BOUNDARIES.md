# MVP Boundaries

**Decision status:** Socle-only scope APPROVED; detailed delivery slices and mechanisms PROPOSED.
**Implementation status:** NOT IMPLEMENTED application; documentary consolidation only.

## Authorized now

Independent Authority Pro monorepo under Documents, immutable source archive, normative documentation, explicit decisions and risks, machine-readable foundation contracts, local validation tooling and preparation for an Angular/Spring Boot executable foundation.

The owner's latest instruction authorizes consolidating the technical coverage matrix, delivery levels, lots and validation criteria. It explicitly reaffirms that this project builds the socle to receive future modules, not those modules or their business functionality. No legacy assets or choices are imported.

## Foundation implementation after N0 approval

Compile/run skeleton with real health state, enforced internal platform boundaries, closed operator provisioning, identity and sessions, own-membership discovery, tenant/company/site scope, permissions, RLS and audit, plus an Angular technical shell. Implement each slice completely and test it before the next. Internal platform components are not business modules.

The hierarchy is designed now; full organization management UX, invitations and company administration are not implied by a minimal runnable skeleton. Critical audit must exist before protected provisioning or other sensitive writes are exposed.

## Three delivery levels

- **L1 — Initial executable nucleus:** the eight proposed lots cover a bounded working foundation, not all source engines or complete production identity administration.
- **L2 — Extensible socle:** proposed subsequent contracts, dependency/lifecycle governance, reliable execution and extension tooling. Each selected capability requires its own contract and acceptance evidence.
- **L3 — Advanced optional platform:** technical products/engines retained as candidates, not an implementation commitment. No engine is built merely because its name appears in a catalogue.

The [coverage matrix](../PACK-INVENTORY.md#technical-coverage-matrix) owns capability allocation and gaps. [Implementation phases](IMPLEMENTATION-PHASES.md) owns lots, dependencies and exits. These levels do not replace N0/N1/N2 change governance. A detailed documentation requirement is not approval to implement every capability.

## Explicit exclusions

No sales, purchases, products, inventory, customers, accounting, HR, payroll, fleet or other business module or feature. This prohibition includes business tables, rules, APIs, screens, seed data, packages and allegedly harmless demonstration/reference modules. Business catalogues may remain reference material only; no business implementation lot is part of this mission.

No marketplace commerce, billing/subscription operations, business portals, AI execution, Studio terminal, scripts, dynamic third-party plugins, external connectors or offline writes are authorized by this documentary step. Generic technical support for a future capability is distinct from implementing its business behavior. A document service would not authorize invoices; a scheduler would not authorize payroll or fleet planning.

No remote repository, commit, push, release, deployment, modification of unrelated projects or use of legacy assets without the owner's explicit instruction for that operation. No database startup or application scaffolding is included in this documentary consolidation.

## Scope decisions still needed

The [decision log](../00-GOVERNANCE/DECISION-MANAGEMENT.md#4-open-decisions) remains the source of approval status. The following recommendations identify when a decision blocks a slice; none records new owner approval.

| Decision | Proposed recommendation and alternative | Blocking point / evidence needed |
|---|---|---|
| O8 — Versions and tooling | Verify a supported Java/Spring/Angular/PostgreSQL intersection and reproducible wrappers; evaluate Modulith/ArchUnit and Flyway rather than selecting independent latest versions | L01/L02: authoritative support evidence, scoped compatibility builds and approved pins under ADR-0010; documentation-tool pins are not the application matrix |
| O9 — Browser identity and provisioning | Framework-managed same-origin cookie sessions, CSRF and explicitly enabled local one-time provisioning; evaluate OIDC if needed, never public admin bootstrap or browser-stored session credentials | L05: approve ADR-0005, password/MFA/recovery readiness, expiry/revocation, initial grants and provisioning ownership; no open production rollout with unresolved account-recovery requirements |
| O10 — Tenant scope and DB roles | Non-owner restricted runtime, separate migration/provisioning identities and verified transaction-local tenant/user context; no universal platform bypass | L03: approve ADR-0003 mechanics and membership/company/site contract; test actual connection lifecycle and all DML |
| O11 — Audit and resource/retention rules | Sensitive state and critical audit commit together; anonymous failures use a distinct bounded path; specify retention and measured limits, not catch-and-ignore | L04/L05: approve atomicity and session integration; L08: defined retention, response/runbook ownership and tested budgets |
| HTTP and probe contract | URI versioning, validated tenant selector, RFC 9457 errors and restricted independent health probes; alternatives require explicit contract changes | L02/L05: approve applicable ADR-0011 conventions, exact OpenAPI schemas, CSRF/error behavior and production exposure |
| First-party execution and boundaries | Compiled reviewed components with explicit exports and static UI contributions; no arbitrary remote code or trust inferred from a signature | L02/L06: approve applicable ADR-0002/0008 implementation; genuine module-discovery and negative-boundary tests |
| Durable publication | Compare the persistent Modulith registry with an explicit transactional outbox and select one producer mechanism | L10 only: ADR-0006 spike and approval; not a reason to block the unrelated L1 skeleton |
| Advanced platform scope | Keep C06/C08/C09 and other L3 candidates deferred until a concrete use case and threat model exist | Before each later engine: explicit scope and acceptance; no blanket approval of scripts, marketplace or Studio |

Four languages and RTL remain required; no narrower language rollout is approved. A first-party descriptor is not a complete marketplace package contract. Security proposals must retain realistic alternatives, impact, migration/recovery constraints and review evidence in their ADRs before approval.

## Exit boundary

**L1 delivered** means the agreed nucleus has evidence of builds, tested boundaries, RLS denial, secure login, atomic audit, real browser interaction and recovery, with remaining production-readiness gaps explicitly recorded. It does not mean the complete extensible socle or every identity/operations feature is delivered.

**L2 delivered** can only be claimed for an explicitly selected set of matrix rows with contract, compatibility, failure/recovery and extension evidence. L3 is a catalogue of independent candidates, not a single automatically scheduled milestone. A set of Markdown files remains a specification, never an implemented platform.

## Related

- [Implementation phases](IMPLEMENTATION-PHASES.md)
- [Validation gates](VALIDATION-GATES.md)
- [Risk register](RISK-REGISTER.md)
- [Coverage matrix](../PACK-INVENTORY.md#technical-coverage-matrix)
