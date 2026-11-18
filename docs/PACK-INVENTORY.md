# Foundation Pack Inventory

**Status:** Current-pack coverage inventory, not full-platform completion.
**Application status:** L02 no-database backend skeleton locally qualified; functional socle capabilities and frontend remain NOT IMPLEMENTED.

## Source and scope

The source [blueprint](MASTER-BLUEPRINT.md) remains unchanged. The owner chose a
normative core plus executable socle approach, not full implementation or exhaustive
detailed documentation of every future product now. Every existing current-pack
file is linked from the [index](README.md); the validator checks index coverage.

## Coverage against the source tree

| Source group | Current foundation coverage | Deferred detail, retained in blueprint |
|---|---|---|
| 00 Governance | Constitution, decisions, security, compatibility and documentation rules | Release-specific governance evidence |
| 01 Product | Vision, objectives, socle requirements, targets and roadmap | Full business personas/journeys and complete domain requirements |
| 02 Architecture | Context, target, backend/frontend, DB, tenancy, API and local deployment | Detailed offline/sync, external messaging and production topology |
| 03 Core | Core/Control separation, runtime, configuration, command/query/event and durability contracts | Full registry/capability implementations, workers/scheduler, Resource Governor and entitlements |
| 04 Engines | Identity/access and audit contracts | Workflow, automation, documents, search, notifications, reporting and remaining engines |
| 05 UI | Constitution, tokens/components, locale and accessibility requirements | Dynamic forms/tables, full workspace registry and extension runtime |
| 06 Module catalogue | No business module authorized; source catalogue preserved | All per-business-domain ownership, entities and acceptance sheets |
| 07 Extensibility | First-party descriptor schema/fixtures and trust/lifecycle boundaries | Marketplace packages, signatures, dynamic installation and sandbox implementation |
| 08 Platform products | Boundaries and deferral recorded | Studio, X, marketplace, licensing, owner/partner portals and AI products |
| 09 Operations | Failure/recovery constraints and gates | Full backup/PITR, SHIELD/GHOST/FOX/PATCH procedures and operational dashboards |
| 10 Security/data | Security governance, RLS, sessions, audit constraints and risks | Complete threat model, privacy/retention/legal policy and incident runbooks before production |
| 11 Quality | Documentation validator, 69 passing L02 tests, runtime/browser/recovery gates | Database/identity/audit/browser/recovery tests and measured performance evidence |
| 12 Developer platform | Monorepo, local docs/backend tooling and CI definition | SDK/CLI, generators, contribution/release automation |
| 13 Implementation | Boundaries, phases, gates, L02 execution evidence and risk register | Later-lot implementation evidence |
| 14 Templates | ADR template and foundation manifest schema with valid/invalid fixtures | Full event/API/automation/script/migration template catalogue |
| 15 Qoder | Project boundaries and approval/evidence rules documented | Dedicated operating/prompt pack only when requested for that phase |

No deferred group is represented by an empty file or claimed implemented. The
current pack cannot claim the blueprint's full final acceptance criteria have been met.

## Technical coverage matrix

This is a requirements map, not 58 engines to create. C01–C42 preserve the source engine catalogue; C43–C58 capture cross-cutting gaps and product boundaries identified by the conversation audit. No business catalogue entry is a delivery item. Levels and lots are defined in [implementation phases](13-IMPLEMENTATION/IMPLEMENTATION-PHASES.md).

**Status legend:** P = partial specification or principles exist; D = explicitly deferred detail; G = identified gap without a sufficient contract. These are documentation states, never test results. Detailed row-level mechanisms and allocations remain **PROPOSED** except the six high-level [foundation orientations](00-GOVERNANCE/DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations) and the [bounded L02 design decisions](00-GOVERNANCE/DECISION-MANAGEMENT.md#l02-skeleton-decisions--2026-11-18). Functional capability rows remain **NOT IMPLEMENTED**; the [L02 execution evidence](#l02-execution-evidence--2026-11-18) qualifies only their shared no-database skeleton, not identity, tenancy, audit or other complete row contracts. Executable JSON documentation fixtures are not functional runtime evidence. L1 means a bounded baseline; L2/L3 entries are subsequent candidates, not implementation authorization.

**Ownership:** Identity owns accounts/sessions; Tenancy owns organization/company/site and memberships; Access owns grant decisions; Audit owns audit contracts/data; Runtime owns technical descriptors and lifecycle. UI, API, Data, Developer, Security and Operations below are proposed responsibility areas, not automatically new modules or assigned people. Shared standards do not grant those areas access to another component's private data. Lead owners remain accountable for their row; participants retain their own contracts.

| ID / capability | Existing basis / state | Proposed owner | Dependencies | Proposed level / lot and boundary | Acceptance evidence required |
|---|---|---|---|---|---|
| C01 Identity | Identity/access, ADR-0005 / P | Identity | C02, C03, C23, C43 | L1 L05: provisioning and sessions; expanded identity C46 later | CSRF, fixation, expiry/revocation, provisioning race and audit failure tests |
| C02 Access control | Identity/access / P | Access | C01, C03–C05, C23 | L1 L03/L05: scoped grants; governance C46 later | Deny missing/revoked grants and unauthorized company/site access |
| C03 Tenant | Tenancy architecture, ADR-0003 / P | Tenancy | C02, C43 | L1 L03/L05: verified scope and RLS | Restricted-role all-DML, pooled connection, rollback and concurrent isolation tests |
| C04 Organization/company | Tenancy architecture / P | Tenancy | C03, C43 | L1 L03: hierarchy and membership structure, not complete organization-admin UX | Tenant-safe constraints; no cross-tenant parent or reference |
| C05 Site/branch | Tenancy architecture / P | Tenancy | C04, C02 | L1 L03: site scope, not business locations/workflows | Site belongs to the same company/tenant; grants checked |
| C06 Policy | Source, security governance / D | Access | C02, C23 | L3: contextual security policies beyond initial RBAC | Deny by default, deterministic decisions, revocation and policy-version tests; no business rules engine |
| C07 Configuration | Configuration engine / P | Runtime: environment; Tenancy: scoped persistence; each component: typed definitions | C02, C43, C47 | L1 L02: validated environment config; L2 L11: governed scoped changes | Invalid values fail startup; secrets not returned; authorized changes audited |
| C08 Feature flags | Source, Core boundaries / D | Runtime | C02, C07, C23 | L3: technical feature lifecycle, never an authorization bypass | Safe defaults, scoped evaluation, expiry/removal and rollback |
| C09 Entitlements | Source, Core boundaries / D | Runtime | C02, C07, C36 | L3: technical entitlement enforcement; no billing module | Expiry/revocation and offline-grace rules; license never grants identity privileges |
| C10 Capabilities | Module runtime, manifest / P | Runtime | C11, C13 | L1 L06: static inventory; L2 L09: resolution | Missing, ambiguous and incompatible providers rejected; selection explicit |
| C11 Contracts | Command/query/event, compatibility governance / P | API / component owners | C28, C43 | L1 L02/L06: typed contracts; L2 L09: evolution checks | Breaking schemas rejected; public exports and owner boundaries verified |
| C12 Metadata | Manifest, database architecture, [settings/schema boundary](03-CORE/CONFIGURATION-ENGINE.md#settings-interface-schemas-and-generated-models) / P | Runtime / Data | C11, C43 | L1 L06: descriptor metadata only; L2 L11: governed data metadata | Bounded schema, owner and lifecycle per field; no universal EAV model or runtime generation of entities/tables through UI settings |
| C13 Registry | Module runtime / P | Runtime | C28, C42 | L1 L06: actual compiled components; L2 L09: dependency graph | Expected components discovered; duplicate IDs, cycles and missing requirements rejected |
| C14 Commands | Command/query/event and durability command-idempotency contract / P | Each owning component | C02, C11, C23 | L1 L02/L05: typed application calls; L2 L09: governed dispatch and explicitly opted-in command idempotency | Reauthorize first execution and replay; same key/different fingerprint conflicts; concurrent mutation/result/audit atomicity and approved key retention tested; distinct from event inbox deduplication |
| C15 Queries | Command/query/event / P | Each owning component | C02, C03, C11 | L1 L02/L05: owned reads; L2 L09: explicit cross-component read contracts | Pagination and scope enforced; no private repository/table access |
| C16 Events | Durability proposal, ADR-0006 / P | Runtime execution | C03, C11, C23 | L2 L10: select one durable publication mechanism | Commit/crash windows, inbox/effect atomicity, duplicates, ordering and scoped worker authority |
| C17 Workflow | [Deferred runtime contracts](03-CORE/MODULE-RUNTIME.md#9-deferred-workflow-and-automation-contracts) / P | Runtime execution | C02, C16, C30, C31 | L3: versioned generic definitions, instances and human tasks; no business process or selected engine | Pinned versions, claim/completion/revocation and timer races, atomic audit, scoped recovery, external-effect reconciliation and explicit compensation |
| C18 Automation | [Deferred runtime contracts](03-CORE/MODULE-RUNTIME.md#9-deferred-workflow-and-automation-contracts), ADR-0009 / P | Runtime execution | C10, C17, C32, C42 | L3: approved capability graphs and bounded authenticated triggers; no unrestricted scripts | Grammar/cycle/budget validation, isolated dry-run, exact-version publication, current authority, replay safety and bounded stop behavior |
| C19 Notifications | Source only / D | Notification service | C02, C03, C16, C24 | L3: generic delivery adapters, not business alerts | Recipient authorization, retries/dedup, privacy and delivery outcomes |
| C20 Documents | Source only / D | Document service | C02, C03, C54 | L3: document metadata/version contracts, separate from file storage | Version access, retention and tenant isolation; no invoices/contracts business logic |
| C21 Search | Source; decision O3 / D | Search service | C02, C03, C45 | L3: owned authorized projections | Revoked access removed from results; scoped indexing/rebuild and stale-result handling |
| C22 Reporting | Source only / D | Reporting service | C02, C15, C45 | L3: generic report execution; analytics separate | Query/time/output budgets, permission recheck and cancellation; no financial or HR reports |
| C23 Audit | Audit engine, security governance / P | Audit | C03, C43 | L1 L04/L05: durable append and authorized read | Protected change and audit both commit or both roll back; separate bounded failed-attempt path |
| C24 Integration | Source; partial capability contracts / G | Runtime integration | C10, C11, C16, C25 | L2 L09: plan/validate technical bindings; L3: external connectors | Separate dependency and integration resolution; provider loss, permission changes, mapping quarantine and recovery |
| C25 Data mapping | Source only / D | Integration / data owners | C11, C28 | L2 L09: mapping contract only; L3: executor | Typed transformation, provenance and rejected ambiguity; no inferred legally significant action |
| C26 Templates | Source only / D | Template service | C02, C27, C28 | L3: bounded technical templates | Output escaping, parser limits and versioning; no hidden script execution |
| C27 Localization | UI constitution/design system / P | UI platform | C07, C38 | L1 L07: FR/EN/IT/AR and RTL; L2 L11: translation workflow | All shell states translated; locale formats, terminology and RTL checked; no tax packs |
| C28 Validation | API, manifest schema and validator / P | API / component owners | C11 | L1 L02/L06: boundary validation; L2 L09: schema evolution | Positive/negative schemas, bounded input and safe errors; current fixture tests prove documents only |
| C29 Cache | Source; API cache constraints / D | Each owning component | C02, C03 | L3 only when measured need exists | Tenant/user-aware keys, revocation/invalidation, bounded memory; never a source of authorization |
| C30 Scheduling | Source; durability authority limits / D | Runtime execution | C16, C31, C32 | L2 L10 if a technical job needs it | Persistent schedule, timezone/misfire policy, cancellation and restart tests |
| C31 Queue/workers | Durability proposal / P | Runtime execution | C16, C32 | L2 L10: bounded queue, retry/failure queue and recovery | Backpressure, lease fencing, retry exhaustion, authorized replay and shutdown drain |
| C32 Resource governor | Non-functional requirements / P | Operations / Runtime | C07, C33 | L1 L02/L08: explicit limits; L2 L10: worker budgets; L3: adaptive quotas | Measured time/size/concurrency limits and tenant fairness; no invented capacity claims |
| C33 Observability | API probes, non-functional requirements / P | Operations | C07, C43 | L1 L02/L08: logs/health/correlation; L2 L10/L11: traces and execution metrics | Minimal protected diagnostics, safe labels, failure signals and trace propagation |
| C34 Backup | Deployment, test gates / P | Operations | C43, C47 | L1 L03/L08: whole-instance procedure and restore; L3: scheduled/partial/PITR options | Restore into isolated destination; verify data/audit consistency and measured recovery targets |
| C35 Recovery | Deployment, test gates / P | Operations | C34, C47 | L1 L08: recovery drill; L2 L11: runbooks; L3: advanced repairs | Recovery credentials and approval tested; shared backup never represented as tenant-only restore |
| C36 Licensing | Source only / D | Licensing platform | C02, C23, C47 | L3: technical license validation; commerce/billing excluded | Activation/expiry/revocation and grace policy; no tenant-data access for owner console |
| C37 Marketplace | Source only / D | Distribution platform | C36, C41, C48 | L3: package distribution design only | Trust, compatibility and permission review before installation; no sales/subscription business feature |
| C38 UI registry | Module runtime, [frontend schema boundary](02-ARCHITECTURE/FRONTEND-ARCHITECTURE.md#deferred-schema-driven-interfaces) / P | UI platform | C02, C10, C28 | L1 L06/L07: static contributions; L2 L11: governed contribution schemas | Declared routes only, visible navigation, versioned compiled widget registry, backend denial and tenant-switch race tests |
| C39 Dynamic components | [Schema-driven interface contracts](02-ARCHITECTURE/FRONTEND-ARCHITECTURE.md#deferred-schema-driven-interfaces) / P | UI platform | C28, C38 | L2 L11: allowlisted schemas; L3: visual/AI builders | Exact schema/API revisions, reviewed publication, server validation and field authorization; accessible FR/EN/IT/AR/RTL forms, no arbitrary JavaScript or generated database models |
| C40 Developer SDK | Architecture conventions / P | Developer platform | C11, C13, C28 | L1 L02: conventions/tests; L2 L11: SDK/CLI and technical fixtures | Generated component respects ownership and contracts; works without Studio |
| C41 Package/lifecycle management | Module runtime, ADR-0008 / P | Runtime | C11, C13, C42 | L1 L06: descriptor validation; L2 L09: compiled lifecycle; L3: distribution | Dry-run impact, explicit permission approval, dependency-safe disablement and retained data; no automatic destructive uninstall |
| C42 Security/integrity | Security governance, ADRs / P | Security / component owners | C02, C23, C47, C48 | L1 throughout: controls/threat analysis; L3: full SHIELD product | Actual attack-boundary tests and remediation evidence; signatures alone never establish safe execution |
| C43 Persistence and migration ownership | Database/backend, ADR-0007 / P | Each data owner / Operations | C07, approved DB roles | L1 L02/L03: owned tables and migrations; L2 L09: compatibility review | Real PostgreSQL migrations/constraints; restricted runtime; expand/contract and recovery plan; no business schemas |
| C44 API lifecycle/schema registry | API architecture, ADR-0011 / P | API / contract owners | C11, C28 | L1 L02: OpenAPI/errors/bounds; L2 L09/L11: deprecation and compatibility | HTTP/filter error consistency, consumer fixtures, version retirement and quota behavior |
| C45 Data governance/portability | Ownership/privacy principles; no complete lifecycle contract / G | Data / each data owner | C02, C23, C43, C47 | L1 L01/L08: classify platform data and retention requirements; L2 L11: controlled export/deletion; L3: lineage/quality tooling | Purpose, lawful basis/consent where applicable, legal holds, export scope, derived-data and backup retention; no raw destructive DB scripts |
| C46 Identity/access governance | Own-membership/provisioning detailed, governance partial / G | Identity / Access | C01, C02, C23 | L1 L01: production-readiness decision for MFA/recovery; L2 L11: scoped identity lifecycle/reviews | Groups/teams, invitations, temporary support grants, service identities and segregation of duties require separate contracts; expiry and revocation tested |
| C47 Secrets and keys | Security governance / P | Security / Operations | Approved environment/trust model | L1 L02/L08: protected injection and rotation/recovery procedure; L2 L11: provider integration | No committed/logged secret; rotation/revocation without privilege broadening; no homegrown cryptographic vault |
| C48 Supply chain and license compliance | Security governance requires SBOM; details partial / P | Security / Developer platform | Build baseline, C47 | L1 L08: SBOM/scans/license inventory; L2 L11: artifact provenance and signing | Reproducible inventory, disclosed findings, license obligations and tamper rejection; dependency licenses differ from C36 |
| C49 SLOs and incidents | Unmeasured targets and incident outline / P | Operations / Security | C33, C34, C35 | L1 L08: measured baseline and recovery runbook; L2 L11: SLI/SLO/escalation | Define windows, alert thresholds, ownership and incident drill; no availability claims without measurements |
| C50 Environment governance | Local topology only / G | Operations | C07, C43, C47 | L1 L02/L08: isolated development/test data; L2 L11: promotion and previews | Separate credentials/data/network assumptions; no production data copied into test without approved protection |
| C51 Cost governance | Source audit; no complete contract / G | Operations | C32, C33 | L2 L11: technical usage accounting; L3: cost budgets | Attributed bounded telemetry, estimate versus billed-cost distinction; no accounting module |
| C52 Feature/architecture governance | Decisions/boundaries specified; feature lifecycle partial / P | Platform / Quality | C11, C13, C48 | L1 L01/L02: approvals and boundary tests; L2 L09/L11: impact/deprecation checks | Discover actual boundaries, track affected consumers and retire safely; experiments stay L3 |
| C53 Localization/accessibility operations | UI targets, no complete review workflow / P | UI / Quality | C27, C38 | L1 L07/L08: locale/keyboard/RTL checks; L2 L11: review workflow | Translation completeness and accessible interaction evidence; target is not compliance certification |
| C54 File storage | Document/storage separation not specified / G | Storage service | C02, C03, C45, C47 | L3: tenant-safe storage adapter, not document ownership | Upload limits/type checks, malware policy, protected downloads, retention and signed-link expiry |
| C55 Authority X | Source product, [agent authority contracts](00-GOVERNANCE/SECURITY-GOVERNANCE.md#10-deferred-agent-authority-contracts) / P | UI platform | C02, C10, C38 | L2 L11 candidate: non-AI command palette; L3: optional language interpretation | Permission-aware navigation/actions, versioned allowlisted tools and exact-action confirmation; model confidence cannot waive sensitive approval |
| C56 Studio/developer-partner ecosystem | [Agent authority boundary](00-GOVERNANCE/SECURITY-GOVERNANCE.md#10-deferred-agent-authority-contracts) added; complete six-area contracts still absent / G | Developer platform | C40, C48, C50 | L3: Design, Development, AI, Quality, Environment, Release | Independent application over contracts; external Git support, revocable narrower delegations, isolated reviewed builds and human release approval; no runtime code loading; partner certification later |
| C57 Advanced platform products | Source, [agent authority boundary](00-GOVERNANCE/SECURITY-GOVERNANCE.md#10-deferred-agent-authority-contracts); other product detail deferred / P | Operations / Security / platform owners | C24, C32–C35, C42, C47 | L3: Server Manager, SHIELD/GHOST/FOX/PATCH, isolated scripts, connectors/devices, optional AI and offline/hybrid | Separate threat/authority model per product; agent approval/replay, isolated revocable memory, provenance and execution budgets; repair approval, per-operation sync conflict policy and no default confidential-data export |
| C58 Advanced data/experimentation | Extended conversation audit; no detailed contracts / G | Data / platform owners | C16, C45, C51, C52 | L3: master-data/quality/lineage infrastructure, analytics, simulation, process mining, digital twin, experiments | Separate use case, owned projections, privacy, budgets and acceptance per candidate; domain models/processes remain excluded |

Dependencies identify collaborating capabilities, not a legal Java import graph. Apparent cycles such as Identity/Access must be resolved into directed public contracts before code, without moving private data into a shared module. L09 owns the complete concrete provider/consumer graph; L1 already requires a non-circular graph for its actual components.

### Evidence and coverage boundaries

The existing normative bases referenced above are indexed under [architecture](README.md#architecture), [Core contracts](README.md#core-and-platform-contracts), [ADRs](README.md#architecture-decision-records), [quality](11-QUALITY-AND-TESTING/TEST-STRATEGY.md) and [security governance](00-GOVERNANCE/SECURITY-GOVERNANCE.md). A G row is a requirement discovery, not a completed specification. The immutable blueprint and supplied extended conversation provide vision, not technical approval.

Before implementing a row, add its concrete public contract, owner, dependency direction, failure behavior and negative tests to the appropriate existing specification. Record any remaining gap rather than creating an empty engine or document. The initial cross-component acceptance scenario uses only Identity, Tenancy, Access, Audit and Runtime or isolated test fixtures; no business module, entity or screen is allowed.

For each implemented slice, record row IDs, lot, decision reference, tested revision, commands, observed results and limitations. No row currently has runtime evidence; the historical documentation checks below cannot close any application acceptance cell.

The subsequent owner-requested documentation enrichment adds proposed workflow/task, agent-authority and UI-schema contracts with a [deferred acceptance matrix](11-QUALITY-AND-TESTING/TEST-STRATEGY.md#deferred-capability-acceptance). Changes from D to P indicate partial written contracts only. All 58 identifiers and delivery allocations remain unchanged: workflow/automation and AI agents stay L3; governed schemas stay L2/L11; visual/AI builders stay L3. [Open decisions O12–O14](00-GOVERNANCE/DECISION-MANAGEMENT.md#4-open-decisions) and [future design gates](13-IMPLEMENTATION/IMPLEMENTATION-PHASES.md#deferred-capability-design-gates) still require separate approval. No engine, vendor, new infrastructure or business module is selected.

## Approval provenance

The renamed product, independent Documents folder, monorepo, stack principles,
shared tenant_id/RLS strategy and socle-only scope are explicit owner decisions.
The owner subsequently approved the six high-level session/provisioning, restricted
runtime/context, critical-audit atomicity, compiled-contract and API orientations.
The [2026-11-18 L02 decisions](00-GOVERNANCE/DECISION-MANAGEMENT.md#l02-skeleton-decisions--2026-11-18)
also approve the bounded build/tool subset, five-root empty graph and closed local
HTTP/configuration design; target-tool and application verification remain pending.
Other mechanisms, schema/grants, framework integration, numeric versions and subsequent
feature phases remain proposals or verification gates. PostgreSQL already serves another
application; the new dedicated project database must not overwrite or alter its database,
roles or service configuration. Host/port, server version and name availability remain
unverified. The old Compose draft must not be started for this selected topology.

## Verification boundaries

Automated checks cover current links/anchors, index coverage, JSON Schema plus
selected manifest semantic constraints, and Mermaid parsing. Graph-wide dependency
resolution, actual module isolation, database policies, authentication, browser UI,
backup restore and visual diagram rendering need separate evidence.

## Local verification evidence — 2026-11-17

Historical snapshot from the initial pack validation, before the coverage-matrix consolidation; counts below are not the current document revision's check results.

- Node.js v24.19.0 and npm 11.17.0 were observed in the execution shell.
- `npm test`: 18 validator tests passed, zero failures.
- `npm run check:docs`: 53 documentation/fixture files, 317 Markdown links,
  9 Mermaid blocks parsed, zero reported errors. Parsing is not visual rendering.
- Ajv draft-2020-12 validation accepts the valid manifest and rejects the invalid
  fixture for the intended missing identifier and malformed version.
- Source and archive SHA-256 both equal
  `515c25639d140f78c5b2bd9f75f0c81d79cdc50d9a5f11e108cadfb6d39da2b7`.
- Compose quiet validation correctly refuses the blank bootstrap password.
- Local Git has no remote. No application, database startup, migration, browser
  check, commit, push or remote CI run is part of this evidence.

## Coverage consolidation verification — 2026-11-17

- Scope: eight existing Markdown files updated; no new application file, business feature, database migration or executable module.
- Runtime: Node.js v24.19.0, npm 11.17.0; local repository still has no commit, so no commit identifier is claimed for this working-tree check.
- `npm test`: 18 documentation-validator tests passed, zero failures.
- `npm run check:docs`: 53 documentation/fixture files, 50 checked Markdown files, 338 local links and 9 Mermaid blocks, zero reported errors.
- A separate read-only check verified exactly 58 unique, contiguous matrix identifiers C01–C58 and confirmed the archive SHA-256 still matches the original attachment.
- Cross-document review corrected the L02/L03 G2 exit dependency, command-idempotency allocation and scoped-configuration ownership. This review is not approval of the proposed technical mechanisms.
- No backend/frontend directory exists. No application, RLS, browser, performance or recovery test has run. No database/container startup, commit, remote creation, push or release was performed.

These are documentation and scope-review results, not runtime acceptance. The automated validator does not establish architectural completeness or security.

## L01 compatibility checkpoint — 2026-11-17

The [version ADR](adr/ADR-0010-java-spring-versions.md#compatibility-evidence--2026-11-17)
records the exact candidate matrix, sources, executed commands and bounded outcomes:

- Java 21 / Boot 4.1.2: five disposable tests passed, including an offline rerun;
  effective POM and resolved dependency tree inspected. No application context,
  server, database connection or migration was started.
- Angular 22: strict npm resolution rejected an upstream optional WASM peer conflict.
  Angular 21 LTS: strict install, AOT/reactivity assertions and a negative compiler
  case passed; 16 moderate development-graph entries represent two advisory roots.
  No production-graph findings were reported by `npm audit --omit=dev`; no application
  build or browser acceptance is inferred.
- Probes, generated reports and downloaded tools remain Git-ignored scratch inputs,
  not versioned application fixtures. No root dependency, CI or Compose change.
- Documentation recheck for this working-tree checkpoint based on `f5d2914`:
  `npm test` passed 18 tests; `npm run check:docs` reported 53 documents/fixtures,
  50 Markdown files, 387 links, nine diagrams and zero errors. Link checks do not
  fetch external sources; Mermaid parsing is not a visual review.
- The 58 contiguous capability identifiers remain intact. The archive still matches
  the original attachment's SHA-256 recorded above. No application directory exists.
- The compatibility investigation is recorded; remaining numeric/tool approvals,
  security contracts and integration tests keep full G1/G2 open. No capability row
  is promoted to IMPLEMENTED or TESTED on this basis.

## L02 decision finalization checks — 2026-11-18

- The three explicit owner selections are recorded as bounded design approvals,
  not delivery or permission to operate PostgreSQL or publish changes.
- `npm test`: 18 documentation-validator tests passed, zero failures.
- `npm run check:docs`: 53 documentation/fixture files, 50 Markdown files,
  460 local links and nine Mermaid blocks, zero reported errors.
- `git diff --check` passed. Including the preceding deferred-contract enrichment,
  the working tree changes only 21 existing Markdown documents; no staged or
  untracked files, and no backend/frontend application directory exists.
- A separate read-only check confirmed C01–C58 are contiguous, their delivery
  allocations match HEAD and the source/archive SHA-256 still matches the value
  recorded above. HEAD remains the local unpushed checkpoint `e27f3af`.
- No target-JDK download, real backend build/start, database operation or browser
  test was performed. These results validate documentation, not runtime security,
  full G1/G2 acceptance or any capability's implementation.

## L02 execution evidence — 2026-11-18

The [subsequent owner instruction](00-GOVERNANCE/DECISION-MANAGEMENT.md#l02-implementation-and-publication-authorization--2026-11-18) authorized this bounded implementation and publication, superseding the earlier hold. The snapshots above describe their respective earlier steps, not the current application state.

- The real [backend](../backend/pom.xml) uses project-portable Temurin 21.0.13+9, release 21, Boot 4.1.2, Maven 3.9.16 and Wrapper 3.3.4. Global Java remains the existing Microsoft installation; no global PATH/JAVA_HOME change was made.
- Publisher-verified Windows x64 Temurin ZIP SHA-256: `a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9`. Maven ZIP SHA-256 in the wrapper: `5af3b743dd8b876b5c45da33b676251e5f1687712644abb4ee519ca56e1d89ce`. Tools and dependency caches remain ignored and project-local.
- `JAVA_HOME=<portable-jdk> npm run check:backend` passed a clean Maven verify: 65 unit/HTTP/architecture/contract tests and four packaged-JAR integration tests; zero failures, errors or skipped tests. `npm run check:backend -- --offline`, with the same process-local JAVA_HOME, repeated the complete clean verification successfully.
- Both clean builds produced JAR SHA-256 `50be3e30deba500077e0f793ee2c4096abcad05d9de8b22adf94c887535320f6`. This is observed local reproducibility, not a claim of cross-OS byte identity or a published release. Surefire/Failsafe reports, dependency inventory and `openapi.json` are generated under ignored `backend/target/`.
- Real production bytecode discovers exactly five closed immediate roots, explicit empty allowed-dependency declarations, no implicit/named API exports and zero intermodule edges. Positive and negative fixtures prove package-only discovery, named-contract authorization, private-import rejection, cycle rejection and inward layering. The entrypoint owns its nested bootstrap settings without importing a private runtime type.
- The packaged launcher requires `AUTHORITY_PRO_MODE=skeleton-local`, ignores ambient configuration files, rejects environment/system/CLI overrides and binds only loopback port 8080. A reserved-port fixture proves the specific port-collision failure without displacing its owner. Tests stop only their own child processes; Windows temporary files are confined to each test directory. Shutdown is verified by child termination, not a nonportable immediate socket rebind.
- Exact GET liveness returns 200/UP and readiness returns 503/OUT_OF_SERVICE, without component details. Other routes and methods are denied; malformed firewall requests return sanitized 400 responses. CSRF remains active, a valid CSRF token grants no authority, no default user/login/session exists, and failures expose fixed RFC 9457 envelopes with server-generated occurrence IDs.
- Springdoc generates only the two-probe contract in tests and cannot serve documentation over the application HTTP boundary. JAR inspection rejects persistence/session-store/driver/Flyway/documentation/test-engine dependencies and test fixtures. No database, identity, audit, inventory, event engine or frontend capability is inferred from reserved package names.
- The resolved graph contains 104 dependencies: 62 compile/runtime and 42 test-only. OSV querybatch at 2026-11-18T07:54:04Z returned zero known-advisory findings for those exact coordinates. All 104 had POM license metadata, including dual-license choices for Logback and Jakarta annotations. Ignored reports are `tmp/l02/dependency-advisories.json` and `tmp/l02/dependency-licenses.json`; this is a point-in-time dependency/license inventory, not legal clearance, a standardized release SBOM or security certification. Recheck on dependency changes.
- Nonblocking test-tool warnings remain: Mockito's test-only dynamic agent attachment and unchecked Swagger schema fluent calls. No permissive JVM flag or security-check bypass was added; these libraries do not expose a production docs/test endpoint.
- Read-only code review identified the nonportable rebind assertion (corrected and retested) and the need to commit the Unix Maven wrapper executable. The publication check must verify mode 100755. The CI backend job runs real qualification; remote results must be observed separately rather than inferred from the workflow file.
- Final documentation validation: 18 validator tests passed; 53 documents/fixtures, 50 Markdown files, 497 local links and nine Mermaid blocks validated with zero errors. Whitespace checks passed after correcting Markdown line breaks; the immutable source hash and all 58 row allocations were rechecked. No generated binaries, tool caches or credential files are selected for publication; the existing environment example is unchanged.
- No PostgreSQL connection, metadata query, provisioning, migration, restart, other-application modification, browser test, deployment or release was performed. Capability rows C01–C58 retain their allocations and future functional status. This completes L02 only, not full G1/G2, RLS, authentication, audit or production readiness.

## Next gate

L02 is locally qualified. Before separately starting L03, establish the existing PostgreSQL endpoint/version, unused dedicated database/role names, operator authority and safe provisioning plan without altering the other application. No database operation is authorized merely by closing L02. Review the frontend findings again before L07; deferred L3 choices do not block unrelated nucleus work.

Initial commit `f5d2914` was pushed to the private repository `ogive98/authority-pro`. The verified L01 checkpoint `e27f3af` was initially held locally for review. The latest owner instruction lifts that hold and authorizes a new verified L02 commit and normal push to the existing `main`; actual publication and remote CI outcomes are reported after execution. Future lots, releases, deployment and database operations remain outside that authorization.

- [Validation gates](13-IMPLEMENTATION/VALIDATION-GATES.md)
- [Decision log](00-GOVERNANCE/DECISION-MANAGEMENT.md)
