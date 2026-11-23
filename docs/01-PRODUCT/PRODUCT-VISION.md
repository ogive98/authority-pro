# Product Vision

**Decision status:** Blueprint vision and explicit owner approvals retained; inclusion and traceability of all supplied catalogue capabilities are REQUIRED in the product target. Persona details, metrics, exact designs and sequencing remain PROPOSED.
**Delivery status:** L02 and bounded local technical L03 are IMPLEMENTED / TESTED; the [canonical L03 recovery evidence](../PACK-INVENTORY.md#l03-main-recovery-evidence--2026-11-20) records main V1 with four empty technical tables. Identity, audit and UI are not implemented; full G2 remains open. The product vision and integrated foundation remain undelivered.

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
`C:/Users/admin/Documents/authority-pro`, organized as one monorepo. L02 delivered
a tested no-database backend skeleton and did not itself touch PostgreSQL. The
[bounded L03 main recovery](../PACK-INVENTORY.md#l03-main-recovery-evidence--2026-11-20)
subsequently passed after the preserved failed main attempt: V1 is applied, with
four empty technical tables (`tenants`, `companies`, `sites`, `memberships`), strict
privilege checks, a packaged no-op and 31 actual data-local HTTP checks. Earlier
scratch/checkpoint results and the failed journal keep their historical meanings.
This is local technical acceptance, not authenticated membership authorization,
fresh-machine installation or production recovery acceptance. Full G2 remains open;
frontend, identity, audit and protected inventory remain unimplemented, and no
business modules are authorized. The detailed L04 atomic-audit contract is drafted
with D1–D6 approved; it remains a platform/security service, not a business module,
and implementation is not automatic. Approval, implementation, testing and release
remain separate; this documentation review runs no migration or tests and implies no
deployment.

## 9. Retained composition and developer vision

**REQUIRED target coverage / detailed design PROPOSED / NOT IMPLEMENTED.** The [two-complement coverage map](../PACK-INVENTORY.md#visual-composition-coverage-refinement) and [earlier documentation authorization](../00-GOVERNANCE/DECISION-MANAGEMENT.md#visual-composition-documentation-authorization--2026-11-20) retain their provenance. The [catalogue integration authorization](../00-GOVERNANCE/DECISION-MANAGEMENT.md#catalogue-and-layer-integration-authorization--2026-11-21) requires inclusion and traceability without replacing existing capabilities or authorizing an ERP implementation. The intended user journey is: explore what is actually installed and authorized, extend it through declared points, compose reusable features, develop missing providers, test and publish an exact reviewed revision. Native application operation must remain possible without the visual editor, AI or Marketplace.

The [UAM](../02-ARCHITECTURE/TARGET-ARCHITECTURE.md#future-semantic-application-model-and-clients) supplies common semantic contracts; visual layout is not their source of truth. Composition can add a button, field, tab, validation, menu, widget or document action only through an owned extension point. Capability discovery is not decompilation of arbitrary Java into editable graphs. New business semantics remain in separately authorized modules, never in the Core or a universal metadata store.

### Studio responsibilities and progressive experience

Retain the six previously documented composition workspaces and add **Application Studio** from the layer source. These are required product responsibilities, not seven new backend engines; ownership and final naming remain O23 design decisions. Application Studio composes the whole application over the same UAM and governed contracts, not a replacement for specialist or lifecycle studios.

| Workspace | Retained responsibilities |
|---|---|
| Application | Compose the application-wide model, features, dependencies and reviewed releases through shared UAM contracts; coordinate rather than replace other workspaces |
| Data | Model/schema design, relationships, constraints, validation, indexes and lifecycle metadata through a reviewed development/migration pipeline |
| Process | Events, workflows, human tasks, conditions, timers, subprocesses, retries, batch operations and recovery over approved capabilities |
| Interface | Forms, tables, dashboards, navigation, layouts, responsive themes and permission-aware extension contributions |
| Integration | APIs, webhooks, external/local services, permitted database adapters, devices, authentication bindings, mappings, tests and monitoring |
| Intelligence | Reports, KPIs, analytical views, alerts, decisions, prediction/classification and simulation using owned projections and approved providers |
| Development | Configuration, reusable composite creation, schema/UI development, then advanced SDK/code; testing, debugging, profiling, documentation, packaging and reviewed publication |

AP-ARCH-EVOL-001 emphasizes analytics in Intelligence Studio; AP-MASTER-CONSTITUTION-001 also places AI creation and evaluation there. The proposed reconciliation retains both: Intelligence owns the analytical/decision experience, while common AI assistance serves every studio through the same UAM, permissions and approval chain. AI model/prompt evaluation, versions, usage, costs, local/self-hosted providers and fallback remain governed capabilities, not a second application-generation runtime.

The existing Authority Studio areas **Design, Development, AI, Quality, Environment and Release** remain intact as lifecycle responsibilities across these composition workspaces. In particular, Quality, Environment and Release are not removed when adding Data or Integration. Basic, Advanced and Developer modes change presentation complexity, not stored semantics, permissions or execution authority. SDK/CLI/API clients must work independently of Studio.

Custom features may target an application, organization, application collection, private library or Marketplace under separately validated publication scopes. Reuse, cloning and import never copy permissions, approvals, credentials or business data implicitly. Node groups are encapsulated, typed, versioned and parameterized; nested reuse is limited and validated across the expanded graph.

### Required ecosystem target and selective installation

Preserve future standard modules and optionally installed industry modules, including included-active, included-inactive, custom and paid editions. Generic contacts/directories, tasks, documents, notifications, forms and workspaces remain in the target; declaring them generic does not make their implementation part of the current nucleus. Existing source business/industry catalogues are unchanged and need separate authorization before implementation.

Required Marketplace target coverage includes modules, features, nodes/composites, UI components, workflows, templates, connectors and complete applications. Retain developer/publisher verification, integrity and compatibility review, release histories, purchases/trials/subscriptions, ratings/reviews, refunds, licensing, revenue sharing/commissions and anti-abuse. Commercial workflows and portals are outside the technical socle. License availability, installed presence, scoped activation, configuration, publication and actual execution are distinct states; an entitlement never grants user permissions.

AI Studio may be visible or preinstalled in a future edition while advanced capabilities require a subscription or usage credits. Free/restricted, Creator, Professional, Enterprise and private-capacity offerings are options, not approved pricing or product commitments. Core availability and data access must not depend on an AI provider or a temporary Marketplace outage; offline license validity and revocation limits require explicit policies, not indefinite grace.

### Preserved validation journey

The [seven scenarios](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md#visual-composition-acceptance) retain automatic labels after ten validated orders, an added screen button, development of a missing printer adapter, a custom module, an inspected AI draft, native-feature preservation and failed-batch recovery. The first complement's complete customer/supplier/product/order/inventory/invoice POC also remains a future candidate, not a required business demonstrator for the technical foundation. A documented representation is architectural evidence only; actual delivery requires a separately authorized implementation and tests.

## 10. Required catalogue and architectural product model

The [required target catalogue](../PACK-INVENTORY.md#required-target-catalogue--2026-11-21) is the canonical name-level traceability map. **Every capability in both supplied sources must be included and traceable**, not held as a disposable optional idea. Required target inclusion is distinct from installed, active, configured, published, entitled and supported-in-profile states. It is neither a promise to build everything simultaneously nor a promise that every deployment supports every capability. Exact contracts, ownership allocations, implementation slices and support claims remain separately gated. Deferral must retain the requirement and its gap; removal or supersession requires an explicit recorded target decision.

For traceability below, **source A** is `588f0a05-51b8-4cb8-a527-c8ac57273301.txt` (696 lines), and **source B** is `9d756b7b-8d2c-44c6-9710-b92c7c21ed5f.txt` (601 lines). In the canonical inventory these are respectively **U** (catalogue, UC01–UC20) and **A** (architecture/layers); local source-A/source-B labels here refer to attachment order, not new requirement IDs. Both are distinct from the earlier E/M complements. Family summaries below do not replace the canonical per-name/source-line mapping or discard unlisted members of a family.

### Required capability families

| Source A family / lines | Product responsibility retained in full |
|---|---|
| 1. Universal Application Platform, 1–16 | UAM; application metadata, registry/discovery, lifecycle/versioning/compatibility/dependencies, configuration/context, templates, import/export and environments |
| 2. Visual Application Explorer, 18–35 | Actual installed application map; module/feature/data/workflow/event/API relationships, component inspection, capabilities, extension points, runtime state and change impact |
| 3. Visual Composition Engine, 37–59 | Canvas, typed nodes/ports, connections, validation/resolution/planning, properties/mapping/expressions, branching/grouping/nesting/reuse, semantic diff, import/export, simulation and preview |
| 4. LEGO Component Architecture, 61–85 | Registry/manifests/SDK/factory, lifecycle, encapsulated composite/nested components, parameters/I/O/types, compatibility/dependencies/versions, public/private libraries, authorized sharing/reuse, documentation and health |
| 5. LEGO Node Library, 87–218 | Every named node in all nine categories: Trigger (89–98), Data (100–116), Logic (118–134), Business (136–149), UI (151–164), Device (166–177), Integration (179–192), Output (194–203) and AI (205–218) |
| 6. Feature Composition, 220–237 | Builder, registry/manifests, governed injection/extension, configuration/activation/placement/permissions, dependencies/lifecycle/versioning, preview/rollback and impact |
| 7. Development Studio, 239–266 | Custom components/nodes/modules/features; schema/entity/relationship/form/table/dashboard/rule/expression/API designers, integration/device builders, controlled code/scripts/SDK, debugger/profiler/tests, packaging/docs and sandbox |
| 8. Composition Runtime, 268–290 | Graph/workflow execution, context/state/queues/scheduling/async/parallel/long-running work, history/traces, retry/timeout/idempotency/DLQ, compensation/recovery/cancellation and monitoring |
| 9. AI Application Platform, 292–319 | Provider/model/prompt abstraction and versions; understanding and generation of features/workflows/components/modules/schema/UI; integration/code/architecture assistance, diagnosis/optimization/tests/docs/recommendations/change analysis, evaluation/usage/cost and governance |
| 10. Core Platform, 321–352 | Runtime/PBC, capabilities/commands/queries/events/bus, contracts/extensions/metadata/schemas/compatibility, resources/flags/configuration/discovery/lifecycle/packages, jobs/scheduler/cache/search/notifications/localization/audit/identity/tenancy |
| 11. Data Platform, 354–373 | Owned access and schemas, controlled dynamic models/relationships/validation/transformation, lineage/provenance/governance/retention/archiving, import/export/synchronization/conflicts/quality and migrations |
| 12. Security & Governance, 375–396 | Authentication/authorization/RBAC/ABAC, federation/SSO/OAuth2/OIDC, tenant isolation, secrets/encryption/sessions/API security/rate limits/policies, extension isolation/sandbox/signatures, audit/masking and supply chain |
| 13. UI Platform, 398–419 | Runtime/registry/schema/rendering, forms/tables/dashboards/navigation/workspaces/pages/menus, themes/design system/responsive layout/RTL/accessibility, permission-aware extensions/state/localization |
| 14. Marketplace & Distribution Platform, 421–441 | Catalogue, publishers, package registry/manifests/signing/verification/dependencies/install/activate/update/rollback/compatibility, search/recommendations/reviews/payments/revenue sharing/subscriptions/licensing |
| 15. Licensing & Entitlements, 443–459 | Licenses, entitlements/subscriptions/validation, bounded offline validity, renewal/revocation, usage/quotas/trials/editions/instances/tenant/module rights and AI credits |
| 16. Integration & Connectivity Platform, 461–478 | Connectivity/API gateway/registry/SDK/webhooks/service/protocol adapters/data exchange, device discovery/management, printers/barcodes/hardware abstraction, connection health/retry/mapping |
| 17. Observability, Diagnostics & Self-Healing, 480–503 | Logs/metrics/traces/health/diagnostics/forensics, authorized repair/recovery/self-healing, capacity/profiling/dependency health/incidents/alerts, backup verification/patch/update, Ghost/Fox/Patch modes and Shield |
| 18. Deployment & Distribution Architecture, 505–525 | Deployment/profiles/instances/environments, cloud/private/standalone/hybrid/distributed/edge/offline, selective synchronization/conflicts, registration/updates/control and data planes/cells/disaster recovery |
| 19. Quality, Testing & Governance, 527–548 | Unit/integration/contract/E2E/graph/workflow simulation/performance/load/security/compatibility/regression/migration testing, test data/coverage, CI/CD/SBOM/release/change management and architecture decisions |
| 20. Cross-cutting capabilities, 551–568 | Global search, command palette, Universal Action Registry/contextual actions, notification center, separate activity and audit timelines, documents/search index/permission resolution/event correlation, cross-module coordination/process monitoring/change preview and drift detection |

Source A §§21–23 (571–696) also retain the product hierarchy, P0–P2 priorities and distinctions: module is not LEGO; feature is not workflow; editor is not runtime; UAM is not canvas; installed is not activated or entitled. Source B adds explicit SOAP adapters, currency/units and communication services, CLI, Application Studio and the layer-wide identity/provenance/recovery rules; overlapping names map to existing owners, not duplicate engines. The [priority crosswalk](../13-IMPLEMENTATION/IMPLEMENTATION-PHASES.md#catalogue-priorities-and-current-lots) reconciles sequencing without renumbering lots.

### Modular Monolith + DDD + Hexagonal + PBC

These choices solve different problems together; the [architectural foundations and logical layers](../02-ARCHITECTURE/TARGET-ARCHITECTURE.md#architectural-foundations-and-logical-layers) own the technical rules:

- **Modular Monolith:** one initial Spring Boot application with enforced internal boundaries, Angular clients and PostgreSQL. Internal contract calls do not require microservices, a broker or twelve deployables.
- **DDD:** bounded contexts own their language, invariants, data and public capabilities. The technical foundation stays business-neutral; business domains enter only after separate scope authorization.
- **Hexagonal:** application use cases and domain-facing ports define behavior; HTTP, persistence, devices and provider integrations are adapters. Source dependencies point inward: domain code must not import UI, Spring, JDBC or another module's private implementation. Logical layer numbers do not authorize imports.
- **PBC — Packaged Business Capabilities:** identifiable, versioned capabilities packaged and governed through declared contracts, manifests, permissions and lifecycle rules. This product view maps to the blueprint's historical **Plugin-Based Components** formulation; it does not make each catalogue label a Java engine, independently deployed service or arbitrary executable plugin. Reviewed first-party compiled code remains the initial trust boundary; the shared JVM is not a sandbox.

The product's shared foundation is **UAM plus governed runtime**. Universal Application Model and source B's Unified Application Model name the same UAM, not two models. UAM owns composition semantics, registries own declared identities/contracts/lifecycle facts, providers own domain data and the runtime owns execution facts. Views and caches reference those authorities, not duplicate them. Canvas, SDK/CLI/API, studios and AI all use the same validation, current authorization and exact-revision publication controls. Native/headless operation remains possible without composer, AI or Marketplace.

### Twelve logical layers

| Source B layer / role | Product-facing outcome and boundary |
|---|---|
| 1 — Infrastructure & Deployment | Spring Boot/Java, PostgreSQL, storage, process resources, installation, backup and profiles; no business rules |
| 2 — Core Platform Runtime | Contracts/registries, lifecycle, discovery/dependencies/packages, configuration/flags/compatibility and resource governance; no private business internals |
| 3 — Security & Governance | Identity, scoped permissions, tenancy, policies, audit, entitlements and trust; server-side checks across every entry point |
| 4 — Platform Services | Shared search, notifications, documents/files, cache/jobs/scheduling, localization/currency/units, templates/import/export/reporting/communications and action registry through owned contracts |
| 5 — Integration & Connectivity | REST/SOAP/GraphQL, webhooks, external systems and devices/printers through governed adapters; no private-table access |
| 6 — Data Platform | Owned persistence/schema/relationships, quality/lineage/provenance, governance/retention, migrations/synchronization/conflicts; constrained relational models, not unrestricted runtime DDL |
| 7 — Business Capability Platform | Official commands/queries/domain services/rules/events and transaction/idempotency/coordination contracts; future providers protect invariants |
| 8 — Execution & Orchestration | Validated plans, durable state/queues/workers/timers/history/traces, bounded retries/recovery/compensation; one authority per execution fact |
| 9 — Application Model & Metadata | One versioned UAM, schema/registry/dependency references and compatibility; separate semantic definitions, layout, business values and observed execution |
| 10 — Developer & AI Platform | Development/SDK/CLI/build/debug/test/package tools and governed AI model/prompt/evaluation assistance; no self-approved generated code |
| 11 — Visual Composition & Application Design | Application, Data, Process, Interface, Integration and Intelligence composition over shared typed components, libraries, simulation/diff/preview; retains Development and lifecycle areas |
| 12 — Experience & User Interaction | Angular shell, workspaces, dashboards/forms/tables, navigation/search/palette/contextual actions, preferences/themes, responsive/i18n/RTL/accessibility and notifications |

The numbers are logical responsibility labels, not a deployment plan or an unrestricted top-down call graph. Security, unique identity, metadata, versioning, compatibility, observability, auditability, reliability, performance/resource governance, lifecycle, dependencies, contract validation, testing, recovery and provenance cross all layers. Extensible components must be identifiable, discoverable, documented, authorized, versioned, monitored and recoverable.

Build and qualify the technical foundation first. Business nodes for orders, stock, invoices, payments, customers, production or accounting are **references to future authorized providers**, not permission to create their tables, rules, screens or demonstrations now. Catalogue coverage does not broaden the L1 exit or make all twenty source-A P0 foundations implemented-engine blockers. [Future acceptance](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md#catalogue-and-layer-acceptance) distinguishes documented intent from actual delivery; physical printing, distributed atomicity, universal offline support and safe automatic repair must never be inferred from a catalogue name.

## Related documents

- [Product Objectives](PRODUCT-OBJECTIVES.md)
- [Product Roadmap](PRODUCT-ROADMAP.md)
- [MVP Boundaries](../13-IMPLEMENTATION/MVP-BOUNDARIES.md)
