# Product Vision

**Decision status:** Blueprint vision and explicit owner approvals retained;
persona details, metrics and sequencing are PROPOSED.
**Delivery status:** L02 no-database backend skeleton IMPLEMENTED / TESTED only; see [execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18). The product vision and integrated foundation remain undelivered.

## 1. Vision

Authority Pro is intended to become a universal business platform on which an
organization can progressively add independent operational domains — sales,
inventory, finance, HR, production, services — without destabilizing the
business-neutral foundation. This is a vision, not currently available software.

It is not a ready-made ERP: future ERP-grade modules would use the platform's
contracts, ownership and security boundaries. No business module is authorized
in the current foundation scope. This is an independent project, unrelated to
legacy AUTHORITY; the source blueprint's AUTHORITY ONLINE title is historical.

## 2. Problem

Business software is fragmented: each domain arrives as a separate product with
its own identity model, its own database and its own integration project.
Organizations pay for duplication, inconsistent permissions and fragile
synchronization. Small organizations cannot afford the integration effort;
large ones cannot evolve quickly.

## 3. Target users (PROPOSED persona detail)

These describe needs, not approved application roles or privilege grants. The
proposed neutral tenancy model is tenant = organization, containing companies
and sites with scoped user access; exact role policy remains open.

| Persona | Need |
|---|---|
| **Platform owner** | Operate the platform, manage tenants, licenses and support without accessing customer business data |
| **Organization administrator** | Create companies and sites, manage users, roles and configuration |
| **Business user** | Work in modules through a fast, consistent interface |
| **Module developer** | Package capabilities with explicit contracts and verified compatibility |
| **Support engineer** | Diagnose with scoped, audited access |

## 4. Product principles

1. **Business-neutral core.** The core never encodes a business rule.
2. **Progressive assembly.** Install only what is needed; add later safely.
3. **Explicit contracts.** Modules integrate through declared commands,
   queries, events and capabilities.
4. **Secure by default.** Tenant isolation and authorization are structural.
5. **Operable by one team.** The default deployment is a modular monolith.
6. **Recoverable.** Backups must be restore-tested and repairs auditable;
   neither capability is implemented yet.
7. **Premium, restrained UI.** Calm, dense, professional workspaces.

## 5. Intended differentiation (not delivered)

- Contract-first modularity: L02 has tested five-root/zero-edge build enforcement;
  functional contracts and governed extensibility remain future capabilities.
- Governed technical integration with explicit business mapping validation.
- Coherent identity, authorization and audit contracts across future modules.
- Local and hosted deployment using the same architecture; capacity and hybrid
  behavior require measurement and separate scope review, not a laptop-scale promise.

## 6. Success metrics (PROPOSED, unmeasured)

| Metric | Candidate target / evidence required |
|---|---|
| Time to create an organization tenant and first admin | < 5 minutes; reproducible operator-CLI provisioning scenario, including prerequisites |
| Cross-tenant data exposure in tests | 0; runtime-role isolation test evidence, not proof of universal absence |
| Module boundary violations in CI | 0; actual architecture-test report |
| API p95 latency, socle reads | < 300 ms; defined benchmark and raw results |
| Backup restore drill success | 100% of defined drills; actual recovery records |
| New module installation without core change | Future Phase 6 validation goal only; business modules are not authorized |

No metric is measured or owner-approved. Workload and hardware requirements,
including BCrypt benchmarking, are defined in the proposed
[non-functional requirements](NON-FUNCTIONAL-REQUIREMENTS.md#1-performance).
No first-year delivery commitment is established.

## 7. Non-goals

- Replacing the stack or adopting microservices.
- Delivering all business modules at once.
- Offline-first operation in the current foundation; Phase 8 is vision only,
  not an approved schedule.
- Marketplace, licensing and AI in the socle.
- A generic low-code database that bypasses domain modelling.

## 8. Product status

The earlier Phase-0 no-code status is historical. The approved scope remains
normative documentation plus a runnable foundation in
`C:/Users/admin/Documents/authority-pro`, organized as one monorepo. L02 has a
built and tested no-database backend skeleton, not an integrated socle or functional
module APIs. L02 itself did not touch PostgreSQL. Subsequent dedicated-environment
and isolated scratch qualification have separate evidence; the later closed L03
application integration is authorized and IN PROGRESS, not delivered or qualified
as a complete slice. No main-database migration is established by this documentation
review. Frontend, identity, audit and protected inventory remain unimplemented,
and no business modules are authorized. Approval, implementation, testing and
release remain separate; no deployment follows from these records.

## 9. Retained composition and developer vision

**PROPOSED / NOT IMPLEMENTED.** The [two-complement coverage map](../PACK-INVENTORY.md#visual-composition-coverage-refinement) and [documentation authorization](../00-GOVERNANCE/DECISION-MANAGEMENT.md#visual-composition-documentation-authorization--2026-11-20) extend the target without replacing existing capabilities or authorizing an ERP implementation. The intended user journey is: explore what is actually installed and authorized, extend it through declared points, compose reusable features, develop missing providers, test and publish an exact reviewed revision. Native application operation must remain possible without the visual editor, AI or Marketplace.

The [UAM](../02-ARCHITECTURE/TARGET-ARCHITECTURE.md#future-semantic-application-model-and-clients) supplies common semantic contracts; visual layout is not their source of truth. Composition can add a button, field, tab, validation, menu, widget or document action only through an owned extension point. Capability discovery is not decompilation of arbitrary Java into editable graphs. New business semantics remain in separately authorized modules, never in the Core or a universal metadata store.

### Studio responsibilities and progressive experience

The following six composition workspaces are candidates, not six new backend engines. Their ownership and final naming remain O23 decisions.

| Workspace | Retained responsibilities |
|---|---|
| Data | Model/schema design, relationships, constraints, validation, indexes and lifecycle metadata through a reviewed development/migration pipeline |
| Process | Events, workflows, human tasks, conditions, timers, subprocesses, retries, batch operations and recovery over approved capabilities |
| Interface | Forms, tables, dashboards, navigation, layouts, responsive themes and permission-aware extension contributions |
| Integration | APIs, webhooks, external/local services, permitted database adapters, devices, authentication bindings, mappings, tests and monitoring |
| Intelligence | Reports, KPIs, analytical views, alerts, decisions, prediction/classification and simulation using owned projections and approved providers |
| Development | Configuration, reusable composite creation, schema/UI development, then advanced SDK/code; testing, debugging, profiling, documentation, packaging and reviewed publication |

AP-ARCH-EVOL-001 emphasizes analytics in Intelligence Studio; AP-MASTER-CONSTITUTION-001 also places AI creation and evaluation there. The proposed reconciliation retains both: Intelligence owns the analytical/decision experience, while common AI assistance serves every studio through the same UAM, permissions and approval chain. AI model/prompt evaluation, versions, usage, costs, local/self-hosted providers and fallback remain governed capabilities, not a second application-generation runtime.

The existing Authority Studio areas **Design, Development, AI, Quality, Environment and Release** remain intact as lifecycle responsibilities across these composition workspaces. In particular, Quality, Environment and Release are not removed when adding Data or Integration. Basic, Advanced and Developer modes change presentation complexity, not stored semantics, permissions or execution authority. SDK/CLI/API clients must work independently of Studio.

Custom features may target an application, organization, application collection, private library or Marketplace under separately validated publication scopes. Reuse, cloning and import never copy permissions, approvals, credentials or business data implicitly. Node groups are encapsulated, typed, versioned and parameterized; nested reuse is limited and validated across the expanded graph.

### Optional ecosystem and commercial candidates

Preserve future standard modules and optional industry modules, including included-active, included-inactive, custom and paid editions. Generic contacts/directories, tasks, documents, notifications, forms and workspaces remain visible candidates; declaring them generic does not make their implementation part of the current nucleus. Existing source business/industry catalogues are unchanged.

Marketplace candidates include modules, features, nodes/composites, UI components, workflows, templates, connectors and complete applications. Retain developer/publisher verification, integrity and compatibility review, release histories, purchases/trials/subscriptions, ratings/reviews, refunds, licensing, revenue sharing/commissions and anti-abuse. Commercial workflows and portals are outside the technical socle. License availability, installed presence, scoped activation, configuration, publication and actual execution are distinct states; an entitlement never grants user permissions.

AI Studio may be visible or preinstalled in a future edition while advanced capabilities require a subscription or usage credits. Free/restricted, Creator, Professional, Enterprise and private-capacity offerings are options, not approved pricing or product commitments. Core availability and data access must not depend on an AI provider or a temporary Marketplace outage; offline license validity and revocation limits require explicit policies, not indefinite grace.

### Preserved validation journey

The [seven scenarios](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md#visual-composition-acceptance) retain automatic labels after ten validated orders, an added screen button, development of a missing printer adapter, a custom module, an inspected AI draft, native-feature preservation and failed-batch recovery. The first complement's complete customer/supplier/product/order/inventory/invoice POC also remains a future candidate, not a required business demonstrator for the technical foundation. A documented representation is architectural evidence only; actual delivery requires a separately authorized implementation and tests.

## Related documents

- [Product Objectives](PRODUCT-OBJECTIVES.md)
- [Product Roadmap](PRODUCT-ROADMAP.md)
- [MVP Boundaries](../13-IMPLEMENTATION/MVP-BOUNDARIES.md)
