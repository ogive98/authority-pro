# Glossary

**Status:** Source-aligned terminology; implementation-specific definitions
are PROPOSED. Platform capabilities are PLANNED / NOT IMPLEMENTED.
**Source:** [MASTER-BLUEPRINT](MASTER-BLUEPRINT.md), especially sections 2, 8 and 10.
Definitions describe the intended platform, not delivered functionality.

| Term | Definition |
|---|---|
| **Authority Pro** | Official product name. The founding blueprint calls it AUTHORITY ONLINE. |
| **Thunder Core** | Business-neutral technical foundation: bootstrap, runtime, registries, tenancy, identity, audit, contracts. |
| **Thunder Control** | Administration layer for the platform. It never bypasses module contracts or authorization. |
| **Socle** | The planned Phase 0/1 business-neutral foundation, required before any separately authorized business module; not delivered today. |
| **PBC** | Plugin-Based Components, as defined by the blueprint: independently governed components with declared contracts, ownership, dependencies and lifecycle. |
| **Module** | A bounded unit of capability owning its domain logic and data. In the modular monolith it need not be an independently deployed service. |
| **Plugin** | An extension that augments the platform or a module without owning a business domain. |
| **Extension** | Any governed addition: UI extension, adapter, automation package, template, localization pack. |
| **Capability** | A named function a module provides or requires, resolved by the platform. |
| **Contract** | A versioned public interface: command, query, event, HTTP API or capability interface. |
| **Command** | An intent to change state, dispatched to a module. |
| **Query** | A read request with an explicit result contract. |
| **Event** | A fact published after a state change. |
| **Outbox** | Table storing events in the same transaction as the state change, for reliable publication. |
| **Inbox** | Record of consumed messages used to deduplicate. |
| **Idempotency** | Property allowing safe retries of the same operation. |
| **Tenant** | Proposed business-neutral isolation boundary: one organization, including its companies and sites. Shared PostgreSQL `tenant_id` + RLS is approved; exact schema/context mechanics remain proposed. |
| **Organization** | Grouping that forms the proposed tenant boundary and may contain multiple companies; not an industry-specific domain. |
| **Company** | Entity within an organization with potentially distinct user grants, configuration and modules; not a separate tenant by default. |
| **Site** | Physical or logical location belonging to a company within an organization tenant. |
| **Membership** | Proposed association of a user with an organization and scoped grants; company/site roles and permissions remain to be approved. |
| **RLS** | PostgreSQL Row-Level Security. Verify isolation under a restricted non-owner, non-superuser, NOBYPASSRLS runtime role; bounded scratch qualification exists, while closed L03 application integration is in progress and authenticated application isolation remains unqualified. |
| **Control plane** | Proposed narrowly authorized identity and platform-metadata administration. It grants no automatic tenant business-data access and is not a global RLS bypass. |
| **Tenant plane** | Proposed tenant-scoped operations requiring verified user/tenant authorization and transaction-local database context, including company/site restrictions. |
| **Migration role** | Proposed database role for schema changes, separate from runtime credentials and unavailable to ordinary requests/jobs. |
| **Browser session** | Proposed Spring Security + Spring Session JDBC server-side session, accessed through same-origin HttpOnly cookies with CSRF protection; no custom bearer session storage in browser JavaScript. |
| **Bootstrap** | Proposed closed-default one-time operator CLI initialization with atomic guard and durable audit, not a public setup endpoint. |
| **N0 / N1 / N2** | Governance levels: critical foundation / stable platform services / business and presentation extensions. |
| **ADR** | Architecture Decision Record. |
| **RFC** | Request for Comments: proposal for an N0 change. |
| **Gate** | Objective exit criteria for a phase. |
| **Soft Glass** | Visual direction: restrained glass effects, clear hierarchy, minimal noise. |
| **Authority X** | Future global command palette and intelligent navigation; Phase 7 is proposed vision only, not implementation authorization. |
| **Authority Studio** | Future environment for building modules and extensions; Phase 7 is proposed vision only, not implementation authorization. |

## Composition terminology proposals

**PROPOSED / NOT IMPLEMENTED.** These definitions reconcile the [two supplied complements](PACK-INVENTORY.md#visual-composition-coverage-refinement); they do not rename approved concepts or implement products. Final ownership and dialects remain O20–O25 decisions.

| Term | Proposed meaning |
|---|---|
| **UAM / Application Model** | Universal Application Model: versioned semantic definition referencing owned data, UI, processes, contracts, permissions, integrations and deployment requirements; distinct from canvas layout, operational values and execution state. |
| **Visualizer** | Permission-filtered view of declared capabilities and observed application state, with real authorized navigation; not automatic conversion of arbitrary Java into editable graphs. |
| **Node / LEGO component** | Typed versioned provider contract with declared ports, configuration, errors, permissions, dependencies, provenance and budgets; not an arbitrary script or new table. |
| **Port** | Named typed input/output with direction, required status, cardinality and declared connection semantics. |
| **Composite** | Encapsulated reusable subgraph with explicit public ports/parameters, pinned child versions and validated nesting/resource limits. |
| **Extension point / feature injection** | Owned versioned contribution contract for an existing screen or capability; never arbitrary DOM, SQL or private-source mutation. |
| **Composition studios** | Data, Process, Interface, Integration, Intelligence and Development workspaces over common contracts; not replacements for Studio's existing Design/Development/AI/Quality/Environment/Release responsibilities. |
| **Intelligence Studio** | Proposed analytical/decision workspace retaining reports, KPIs, alerts, predictions and simulation; both complements' analytical and AI functions remain visible. Final naming is O23, not an approved merger. |
| **AI Studio / common AI assistance** | Optional inspected suggestions, generation, testing and diagnosis through the same model and permissions as humans; no independent runtime or self-approved publication. |
| **Basic / Advanced / Developer** | Presentation-complexity modes over the same semantics, not different permissions, formats or independent editors. |
| **Application revision / publication bundle** | Exact coherent revision and digest of model, UI, process and dependency references; approval and publication are separate from activation and execution. |
| **Installed / enabled / configured / entitled / published** | Independent availability dimensions; presence, permission, license, valid bindings and a reviewed revision must never be collapsed into one status. |
| **Architectural readiness** | Reviewed contracts, ownership, decisions and planned acceptance for a selected capability; not implementation, successful tests, release or production readiness. |

PBC continues to mean **Plugin-Based Components**. The second complement's expansion "Platform Business Capabilities" is not adopted. Resource profiles and deployment profiles are separate dimensions, and governance N0/N1/N2 is not delivery L1/L2/L3. Deferred functions remain requirements; deferral does not mean deletion.

## Naming rules

- Approved product: Authority Pro. Approved location:
  `C:/Users/admin/Documents/authority-pro`; one monorepo. This is independent of
  legacy AUTHORITY and does not reuse its architecture or implementation.
- PROPOSED identifiers: packages `com.authoritypro`, database `authority_pro`.
- PROPOSED conventions: lower-kebab-case module IDs, `domain.entity.action`
  events and `module.resource.action` permissions.
- Terminology does not imply installed packages, database objects or released
  capabilities. See [decision and delivery statuses](00-GOVERNANCE/DOCUMENTATION-STANDARDS.md#3-status-labels).

## Related documents

- [Documentation Standards](00-GOVERNANCE/DOCUMENTATION-STANDARDS.md)
- [Platform Constitution](00-GOVERNANCE/PLATFORM-CONSTITUTION.md)
