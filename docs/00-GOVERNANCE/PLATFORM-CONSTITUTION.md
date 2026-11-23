# Platform Constitution

**Decision status:** Source principles and the explicit decisions in the
[approval record](DECISION-MANAGEMENT.md#approval-basis), including the
[owner-confirmed foundation orientations](DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations), are binding; additional
policy and implementation details below remain PROPOSED.
**Delivery status:** L02 and bounded closed L03 are locally IMPLEMENTED / TESTED;
[main recovery evidence](../PACK-INVENTORY.md#l03-main-recovery-evidence--2026-11-20)
records the applied V1 and four empty technical tables. The full foundation remains
incomplete: authenticated identity/membership enforcement, audit, protected inventory,
frontend and business features are not delivered. Full G2–G5 and production remain
open; documentary coverage, isolated probes and this clarification cannot close them.
**Scope:** Every developer, AI agent, module, plugin and extension of Authority Pro.
**Authority:** Locked [blueprint](../MASTER-BLUEPRINT.md) principles and explicit
owner approvals prevail. This document does not approve its own proposals.

## Preamble

Authority Pro is intended as a universal business platform. Its core must be
technical and business-neutral; future independent modules would declare their
capabilities, contracts and data ownership. The authorized scope is normative
documentation plus a runnable foundation, not business-module implementation.
The platform must remain stable as separately authorized capabilities are added.

## Article 1 — Identity

1.1 The official product name is **Authority Pro**. The approved project location
is `C:/Users/admin/Documents/authority-pro`, organized as one monorepo. The
founding blueprint is titled AUTHORITY ONLINE; this independent platform is
unrelated to legacy AUTHORITY. Do not import legacy code or assumptions.

1.2 PROPOSED identifiers: `authoritypro` (packages), `authority_pro` (database)
and `authority-pro` (artifacts). The monorepo decision is not evidence of an
initialized repository or runnable foundation.

## Article 2 — Technology stack and architecture

2.1 Locked frontend baseline: Angular, TypeScript, Angular Router, Reactive Forms.
Numeric versions remain PROPOSED until compatibility review and approval.

2.2 Locked backend baseline: Java, Spring Boot, Spring Security, Spring Data
where appropriate. The bounded L02 toolchain and Modulith/ArchUnit pins are
approved and tested under [ADR-0010](../adr/ADR-0010-java-spring-versions.md).
Persistence tooling remains separately gated and is excluded from L02.

2.3 Approved data strategy: shared PostgreSQL with `tenant_id` and RLS within
Authority Pro's dedicated instance, never a shared server with another project.
PostgreSQL 16.15 and the bounded closed L03 JDBC/Flyway integration have their
[recorded approvals](DECISION-MANAGEMENT.md#l03-closed-application-integration-authorization--2026-11-19);
broader operational and authenticated policies remain separately gated. Versioned
safe migrations and module ownership remain source requirements.

2.4 Locked contracts: REST, versioned public APIs and OpenAPI documentation.
Approved orientation: `/api/v1`, controlled tenant selection and standardized
errors. The bounded L02 RFC 9457 envelope is approved and tested; later
selector/header and integrated error contracts remain PROPOSED.

2.5 Locked architecture: modular monolith, DDD, hexagonal architecture and
the blueprint's Plugin-Based Components approach. The owner's later product term
**Packaged Business Capabilities** is explicitly reconciled in
[ADR-0002](../adr/ADR-0002-modular-monolith.md#product-and-logical-layer-clarification--2026-11-21),
without changing this technical boundary. Approved foundation orientation: compiled
first-party components with explicit contracts, not arbitrary hot-loaded plugins.
The twelve logical responsibility layers complement these principles; they are not
twelve deployables or a replacement dependency graph. Replacing this baseline or
adopting microservices as the default requires explicit approval with an RFC and ADR.
Tool recommendations remain PROPOSED.

## Article 3 — Business-neutral core

3.1 Thunder Core contains platform infrastructure only. Accounting, payroll,
sales, inventory, production, CRM or any industry rule is prohibited inside
the core.

3.2 A capability may enter the core only if it is required by every module and
has no business semantics.

3.3 Core changes are N0 changes (Article 9).

## Article 4 — Module ownership and boundaries

4.1 A module owns its domain logic, its data and its schema objects.

4.2 No module may read or write another module's tables, repositories or
entities. Cross-module communication uses published contracts (commands,
queries, events, capability interfaces).

4.3 Module dependencies must be explicit and verified in CI when implemented.
L02 local build tests verify the exact five closed module roots and zero edges;
this is not evidence of remote CI execution. Circular dependencies are prohibited.

4.4 Private implementation must not be imported across modules. L02 exports no
functional APIs. Later cross-module contracts require named exports and explicitly
approved consumers; the root declarations do not grant access to internals.

## Article 5 — Contracts and compatibility

5.1 Every public API, event or capability is explicitly versioned.

5.2 Additive changes are preferred. Breaking changes require a deprecation
cycle (see Versioning and Compatibility).

5.3 A documented contract is the only supported integration surface.

## Article 6 — Multi-tenancy

6.1 Approved strategy: every tenant-scoped record must carry a `tenant_id` and
be protected by PostgreSQL Row-Level Security in the project's shared-tenant
database. Bounded L03 implements four empty technical tables with RLS; this does
not establish authenticated authorization or complete isolation across future services.

6.2 Tenant isolation must be enforced and tested in the database, application,
API, caches, search, files and background jobs. Frontend filtering is never a
security control.

6.3 The platform owner must not automatically gain access to tenant business
data. Any future support access must be explicit, scoped, time-limited, audited
and revocable.

6.4 Approved orientation: restricted PostgreSQL runtime privileges, RLS and
transaction-local tenant context derived from verified tenant/user authorization.
The bounded technical organization/company/site/membership schema and separated
non-superuser, non-owner, NOBYPASSRLS runtime role are qualified in L03. Deriving
context from authenticated membership and enforcing company/site grants still
requires the separately approved L05 contracts. Broader schema/operational policies
remain PROPOSED. Control-plane administration grants no universal RLS bypass.

## Article 7 — Security by default

7.1 Authorization must be enforced on the backend for every request. L02 tests
its closed local HTTP allowlist and denial boundary only; user, membership and
permission-based authorization remain unimplemented.

7.2 Secrets never appear in source code, committed configuration or frontend
bundles.

7.3 Untrusted code (scripts, plugins from third parties) runs only inside a
documented sandbox. Arbitrary server-privileged execution is prohibited.

7.4 Authentication, authorization and tenant-isolation changes are N0 changes.

7.5 Approved orientations: Spring Security/JDBC-backed cookie sessions with CSRF,
and first-administrator provisioning through a local operator CLI, not a public
page or setup endpoint. Exact Spring Session JDBC integration, same-origin/cookie
configuration, expiry and closed-default single-use bootstrap guards remain
PROPOSED. No custom bearer session credentials stored in browser JavaScript.

7.6 Approved invariant: critical sensitive changes and their durable security
audit both commit or both roll back. Exact transaction/session integration and
separate bounded audit transactions for anonymous failures remain PROPOSED.
Secrets and a blanket swallow-audit-failure policy are excluded.

## Article 8 — Data ownership and integrity

8.1 Each module declares the data it owns and the contracts by which others
may access it.

8.2 A universal EAV model is prohibited as a substitute for modelled domain
data.

8.3 Database changes use expand-and-contract migration strategies. Destructive
migrations require approval and a tested restore path.

## Article 9 — Governance levels

| Level | Content | Required to change |
|---|---|---|
| **N0 — Critical foundation** | Identity, tenancy, security, module contracts, core runtime, audit, persistence boundaries | RFC + ADR + impact analysis + security review + migration plan + owner approval |
| **N1 — Stable platform services** | Engines, SDK, UI platform, integration infrastructure, tooling | Design note + tests + compatibility analysis + review |
| **N2 — Business and presentation extensions** | Business modules, screens, reports, workflows, plugins | Tests + contract and security compliance |

## Article 10 — Change control

The following actions require explicit owner approval before execution:
changing the stack, replacing an approved architecture, converting to
microservices, modifying N0 foundations, destructive migrations, deleting
project files, moving major directories, pushing commits to a shared remote,
publishing releases, deploying to production, changing production secrets,
executing dangerous scripts, exporting confidential data.

Every such request states: what changes, why, affected files, risks,
alternatives and rollback plan.

## Article 11 — Quality gates

11.1 A future implementation change is done only when its build, tests and
module-boundary verification have linked evidence and documentation agrees.
Approval, implementation, testing and release are distinct statuses; none may
advance by inference. Documentation corrections alone do not close runtime gates.

11.2 Coverage percentage alone is not proof of quality. Critical paths require
explicit tests: authentication, authorization, tenant isolation, audit and
migration.

## Article 12 — AI agents

12.1 AI agents follow this constitution like any contributor.

12.2 Agents have explicit, scoped tool permissions. Human approval is required
for N0 changes, destructive migrations, package publication, production
deployment and access to sensitive data.

12.3 AI use and installation remain optional; the [required target catalogue](../PACK-INVENTORY.md#required-target-catalogue--2026-11-21)
retains the requested AI capabilities as product requirements without authorizing
their implementation now. AI never bypasses authorization or validation, and Core
operation must not depend on an AI provider.

## Article 13 — Amendments

Amendments follow the ADR process and record their actual author, review and
approval evidence separately. Do not attribute authorship, technical review or
approval to the owner without evidence. An agent-written proposal is not an
owner-approved amendment; locked source decisions are never changed silently.

## Compliance checklist

- [ ] No business rule in the core.
- [ ] No cross-module repository or entity access.
- [ ] Tenant-scoped tables carry `tenant_id` and RLS.
- [ ] Backend authorization on every endpoint.
- [ ] No secret in the repository.
- [ ] Tests updated, module boundaries verified.
- [ ] Documentation and ADR updated.

## Related documents

- [Architecture Governance](ARCHITECTURE-GOVERNANCE.md)
- [Security Governance](SECURITY-GOVERNANCE.md)
- [Decision Management](DECISION-MANAGEMENT.md)
- [Multi-Tenancy Architecture](../02-ARCHITECTURE/MULTI-TENANCY-ARCHITECTURE.md)
