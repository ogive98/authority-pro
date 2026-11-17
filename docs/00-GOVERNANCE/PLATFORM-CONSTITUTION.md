# Platform Constitution

**Decision status:** Source principles and the explicit decisions in the
[approval record](DECISION-MANAGEMENT.md#approval-basis) are binding; additional
policy and implementation details below are PROPOSED.
**Delivery status:** PLANNED / NOT IMPLEMENTED. No backend or frontend code,
test evidence or release is present; Phase 0 is in progress.
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
where appropriate. Java 21 and Spring Boot 4.1 are candidates, not approvals.
Spring Modulith, ArchUnit and exact persistence tooling remain PROPOSED.

2.3 Approved data strategy: shared PostgreSQL with `tenant_id` and RLS.
Database versions, Flyway and migration mechanics remain PROPOSED; versioned
safe migrations and module ownership remain source requirements.

2.4 Locked contracts: REST, versioned public APIs and OpenAPI documentation.
The `/api/v1` convention and tenant selection contract remain PROPOSED.

2.5 Locked architecture: modular monolith, DDD, hexagonal architecture and
Plugin-Based Components. Replacing this baseline or adopting microservices as
the default requires explicit approval with an RFC and ADR. Tool recommendations
must not be treated as already locked decisions.

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
No CI verification exists yet. Circular module dependencies are prohibited.

4.4 Private implementation must not be imported across modules. The specific
`api` package convention is PROPOSED; published contracts are the only supported
cross-module interface.

## Article 5 — Contracts and compatibility

5.1 Every public API, event or capability is explicitly versioned.

5.2 Additive changes are preferred. Breaking changes require a deprecation
cycle (see Versioning and Compatibility).

5.3 A documented contract is the only supported integration surface.

## Article 6 — Multi-tenancy

6.1 Approved strategy: every tenant-scoped record must carry a `tenant_id` and
be protected by PostgreSQL Row-Level Security in a shared database. This is
PLANNED / NOT IMPLEMENTED, not a statement that protected tables exist.

6.2 Tenant isolation must be enforced and tested in the database, application,
API, caches, search, files and background jobs. Frontend filtering is never a
security control.

6.3 The platform owner must not automatically gain access to tenant business
data. Any future support access must be explicit, scoped, time-limited, audited
and revocable.

6.4 PROPOSED implementation: tenant = organization with a company/site hierarchy;
control-plane administration is separate from tenant-plane access and grants no
universal RLS bypass. Use a non-superuser, non-owner, NOBYPASSRLS runtime role,
separate migration credentials and transaction-local context derived from verified
tenant/user authorization. Exact schema, policies and grants require review.

## Article 7 — Security by default

7.1 Authorization must be enforced on the backend for every request. No
implemented authorization checks are claimed.

7.2 Secrets never appear in source code, committed configuration or frontend
bundles.

7.3 Untrusted code (scripts, plugins from third parties) runs only inside a
documented sandbox. Arbitrary server-privileged execution is prohibited.

7.4 Authentication, authorization and tenant-isolation changes are N0 changes.

7.5 PROPOSED foundation design: Spring Security + Spring Session JDBC,
same-origin HttpOnly browser session cookies and CSRF protection; closed-default
one-time operator CLI bootstrap, with no public setup endpoint. No custom bearer
session credentials stored in browser JavaScript.

7.6 PROPOSED audit policy: sensitive writes must commit atomically with durable
security audit or be rejected. Anonymous failures use separate bounded audit
transactions without secrets. A blanket swallow-audit-failure policy is excluded.

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

12.3 AI features are optional platform capabilities and never bypass
authorization or validation.

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
