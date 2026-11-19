# MVP Boundaries

**Decision status:** Socle-only scope, bounded L02 skeleton design and bounded L03 local-environment setup APPROVED; other detailed delivery slices and mechanisms PROPOSED.
**Implementation status:** L02 no-database backend skeleton IMPLEMENTED / TESTED only; see [execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18). No PostgreSQL was touched in L01/L02; that evidence remains historical. The [empty local environment](../PACK-INVENTORY.md#l03-local-environment-evidence--2026-11-18) is now provisioned and its bounded role/authentication checks passed. Application migrations, RLS, JDBC integration, identity, audit, Angular and backup remain unimplemented; G2 is open. No protected inventory or business feature is implemented.

## Authorized now

Independent Authority Pro monorepo under Documents, immutable source archive, normative documentation, explicit decisions and risks, machine-readable foundation contracts, local validation tooling and the delivered/tested L02 Spring Boot no-database skeleton. Angular and the integrated foundation remain later gated work.

The owner authorized consolidation and subsequently [approved six high-level foundation orientations](../00-GOVERNANCE/DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations). The mission remains only the socle to receive future modules, not business functionality. After authorizing environment preparation, the owner selected the dedicated-instance option in the [L03 local-environment authorization](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l03-local-environment-authorization--2026-11-18): existing `authority` database `PUBLIC CONNECT/TEMP` privileges prevent strict separation on the old server without changing its ACLs. This supersedes only the earlier same-server local topology, not shared tenants within Authority Pro's own database or the prohibition on business modules.

The approved bounded project-only Compose setup uses PostgreSQL 16.15, a new `authority-pro-postgres` container bound to `127.0.0.1:5434`, new data/secrets volumes, its own network, separate operator/owner/migrator/runtime identities and limits of 256 MiB / 0.5 CPU. Verify endpoint and unused resource/database/role names before creation; abort on collisions. Leave the existing application, its database/ACLs/service and the concurrent project untouched. No legacy assets, credentials or choices are imported. Environment initialization is not Flyway approval, application migration approval or implemented tenant logic.

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

No remote repository, commit, push, release, deployment, modification of unrelated projects or use of legacy assets follows without the owner's explicit instruction for that operation. The prior L02 authorization covers the bounded skeleton and commit/push, lifting the earlier hold; publication must be verified separately against the actual remote commit and CI outcome. The later L03 authorization permits only the bounded local environment described above. Documentation itself grants no additional database access, application L03 implementation or production deployment.

## Scope decisions still needed

The [decision log](../00-GOVERNANCE/DECISION-MANAGEMENT.md#4-open-decisions) remains the source of approval status. In addition to the six orientations, the [2026-11-18 questionnaire](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l02-skeleton-decisions--2026-11-18) approves the L02 target build, five-root empty graph and closed local HTTP/configuration design. These decisions need not be asked again; target-JDK/wrapper and real skeleton clean verification now have bounded passing evidence. The offline clean repeat also passed. The later L03 authorization settles only the dedicated local environment, its PostgreSQL 16.15 pin and bounded setup above; remaining version selections and application slice-specific policies stay open. Environment preparation may proceed within that authorization, with a verified endpoint, non-colliding project names and authorized operator access. It does not approve application migrations or tenant implementation. No changes to the old application or concurrent project are authorized.

| Decision | Approved subset / remaining proposal | Blocking point / evidence needed |
|---|---|---|
| O8 — Versions and tooling | L02 subset approved: project-local Temurin 21.0.13+9, Boot 4.1.2, Maven 3.9.16/Wrapper 3.3.4 and Modulith/ArchUnit; no database libraries. PostgreSQL 16.15 is approved for the bounded L03 dedicated environment only | L02 exact tools, clean build, actual discovery and negative tests have passing evidence. L03 environment provisioning checks passed; application database compatibility, Flyway and frontend selections remain open |
| O9 — Browser identity and provisioning | Framework-managed same-origin cookie sessions, CSRF and explicitly enabled local one-time provisioning; evaluate OIDC if needed, never public admin bootstrap or browser-stored session credentials | L05: approve ADR-0005, password/MFA/recovery readiness, expiry/revocation, initial grants and provisioning ownership; no open production rollout with unresolved account-recovery requirements |
| O10 — Tenant scope and DB roles | Non-owner restricted runtime and verified transaction-local tenant/user context; no universal platform bypass. L03 environment setup approves separate operator/owner/migrator/runtime identities in the dedicated instance | L03: environment evidence pending; approve remaining ADR-0003 application mechanics and membership/company/site contract, then test actual connection lifecycle and all DML |
| O11 — Audit and resource/retention rules | Sensitive state and critical audit commit together; anonymous failures use a distinct bounded path; specify retention and measured limits, not catch-and-ignore | L04/L05: approve the detailed transaction/session integration of the approved atomicity invariant; L08: defined retention, response/runbook ownership and tested budgets |
| HTTP and probe contract | Approved L02: loopback `skeleton-local`, only GET liveness/readiness, readiness 503, denial elsewhere, retained CSRF and RFC 9457 errors; test/build-only OpenAPI | L02 local behavior and packaged artifact have passing evidence. Later tenant/session contracts, exact codes and hosted exposure still need approval before their implementation |
| First-party execution and boundaries | Approved L02: compiled single artifact, exactly five closed roots, zero intermodule edges, Modulith/ArchUnit; no functional APIs; later named exports and consumers need explicit approval | L02 genuine discovery and positive/negative boundary tests have passing evidence. L06: approve later descriptors/lifecycle and protected inventory; no arbitrary remote code |
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
