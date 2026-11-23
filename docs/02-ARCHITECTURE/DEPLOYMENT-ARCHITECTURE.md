# Deployment Architecture

**Decision status:** APPROVED bounded L02 local/no-database design and subsequently a [dedicated local PostgreSQL environment](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l03-local-environment-authorization--2026-11-18), replacing the earlier same-server local topology. Other projects remain outside the mutation boundary.
**Implementation status:** L02 skeleton, the dedicated local PostgreSQL environment and closed L03 technical local integration are IMPLEMENTED / TESTED within bounded scope. The latest authorized [`recover-main` passed](../PACK-INVENTORY.md#l03-main-recovery-evidence--2026-11-20) at `2026-11-20T13:13:56.917Z`, id `0ba0c512-e618-4aa3-8267-204907fc1f27`, bound to preserved `main.failed` `356dcc34-3a3a-4227-ba84-630691755f8c` and qualification `d1af7045-a3f7-4042-b750-1839aedb32e8`. Main V1 was already applied (count 1); recovery was a no-op migration (count 0), and 31 real `data-local` HTTP checks passed; PID 16796 was stopped. All four technical tables remain empty. The [initial empty environment](../PACK-INVENTORY.md#l03-local-environment-evidence--2026-11-18), failed attempt and [isolated qualification/restore evidence](../PACK-INVENTORY.md#l03-isolated-qualification-evidence--2026-11-19) remain historical, not overwritten successes. Other containers, database identities, artifacts and original journals are unchanged. Hosted deployment and production readiness remain unestablished. This documentation update runs no backend test or infrastructure operation.

## Modes and current scope

Lite (local), Standard (hosted) and Hybrid are blueprint targets. LITE/STANDARD/POWER/ADAPTIVE are separate resource profiles. The foundation first validates a local development topology; no hosted environment or production release is created automatically.

## Approved bounded L02 mode (implemented and tested locally)

Explicit `skeleton-local` mode binds only `127.0.0.1`, using port `8080` only if free; otherwise stop the attempted startup, never another service. Typed, validated configuration must reject invalid values and omit secrets from diagnostics. The only allowed HTTP requests are the two status-only GET probes in [API architecture](API-ARCHITECTURE.md#bounded-l02-http-design): live process `UP`, readiness `503 OUT_OF_SERVICE` while the nucleus is incomplete; every other request is denied, with CSRF retained and no sessions provisioned.

No datasource, JDBC, JPA, Flyway, database/driver (including embedded DB), session store, credentials, account/login, broker, containers or external calls belong to the L02 application. Project-portable Temurin `21.0.13+9`, Boot `4.1.2` BOM and Maven `3.9.16` / Wrapper `3.3.4` are the installed and tested bounded L02 baseline; Microsoft `21.0.12.1` was only the historical L01 probe runtime. [ADR-0010](../adr/ADR-0010-java-spring-versions.md) owns the detailed matrix. This documentation update performs no startup, download or deployment.

DB/Flyway/RLS belong to the separately authorized closed L03 technical local integration, now implemented/tested within the [main recovery evidence boundary](../PACK-INVENTORY.md#l03-main-recovery-evidence--2026-11-20); main V1 was already applied and recovery applied no new migration. Audit remains L04, identity and membership validation L05, protected inventory L06 and Angular L07, each subject to its own gates. No business functionality or production readiness is claimed. Full G2 clean-machine provision reproduction is not established; G3–G5 remain open. This documentation task authorizes no database operation. The following topology and operational sections do not broaden the bounded delivery or authorize L02 database actions.

## Proposed integrated local topology

Future Angular development on loopback will proxy API requests to Spring Boot on loopback; candidate ports 4200/8080 must be checked before startup. Neither is started by environment preparation. Never stop unrelated services to free ports or reuse their database, schemas, credentials, tables or Docker resources.

The separate approved local server is `authority-pro-postgres`, PostgreSQL 16.15 pinned by image digest, with `authority_pro` on `127.0.0.1:5434`. It owns its data/secrets volumes and bridge network, with 256 MiB memory, no additional container swap, 0.5 CPU and 20 database connections maximum. Runtime and migrator roles have limits of five and two connections respectively. This bounds the new workload but does not prove host-wide performance isolation.

The old `authority-postgres` server on host port 5433 was inspected only through authorized system metadata; PUBLIC CONNECT/TEMPORARY on its database led to the explicit separate-instance choice. Its ACLs, configuration, data, volumes and lifecycle are unchanged. pgAdmin settings and all concurrent projects remain untouched. Use the guarded [environment commands](../../README.md#dedicated-local-postgresql-environment), not an unreviewed Compose invocation.

## Safe provisioning boundary

1. Positively identify the approved local Docker daemon, checkout, pinned image and loopback port without requesting secrets in chat or putting passwords in command arguments. Existing databases are not provisioning targets.
2. Perform only a separately scoped read-only metadata preflight: server identity/version, candidate database/role collisions and effective privileges. Do not read the other application's tables or export its configuration/data.
3. Confirm database and dedicated project-role names. Abort if a candidate already exists; never silently adopt, drop/recreate or overwrite it. PostgreSQL roles are cluster-wide, so creation must be limited to approved NEW project roles; no existing role alteration, global grants/defaults or service configuration edits.
4. Provision only the new database and agreed roles with narrowly scoped privileges. Use an appropriate maintenance connection, never apply migrations to the old application database. Creating a database cannot be treated as an ordinary rollbackable migration; report partial creation and stop on failure rather than deleting objects automatically.
5. Before migrations, positively verify the connected database, project owner/runtime identities and expected project state. Migrations, tests, cleanup and restoration must target only dedicated project destinations; collision or target mismatch stops execution.

The dedicated-instance approval authorized only first initialization of the new Authority Pro cluster. The later [isolated qualification authorization](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l03-isolated-qualification-authorization--2026-11-19) separately covers the two named technical databases and their test/restore operations, not application integration. No `DROP DATABASE`, recreation, existing data-directory/volume reuse, other-server upgrade/restart, session termination or shared configuration change is authorized. Name or Compose-label collisions stop execution; partial setup is retained, never deleted or silently adopted. If compatibility or isolation requires altering another project, stop rather than changing it.

A separate database shares server resources and cluster administration. It is not proof of resource or privilege isolation. Validate effective project-role access, including inherited and PUBLIC privileges; if restricting cross-application access would require altering existing database/global permissions, stop for approval instead of applying revokes to the other application. Bound new connection pools/jobs to protect the existing workload.

## Credentials and role separation

Use operator-controlled authority only for the approved provisioning step. Dedicated migrator/table-owner and restricted runtime identities serve only Authority Pro; never reuse the other application's application credentials. Runtime must not be a superuser or use provisioning credentials.

Deliver secrets through a protected prompt or approved secret mechanism, not source, chat, command arguments, process output or logs. A blank example password is not a usable account. Do not copy the other application's secret files or edit authentication/service settings to obtain access.

## Delivery and operations

The approved Maven Wrapper now builds the L02 backend on its verified portable toolchain. Angular and its locked frontend build remain future work. Exact verified skeleton startup commands belong in the root README; they do not authorize integrated or hosted deployment. The PostgreSQL client version does not establish the server version or compatibility.

TLS is required outside explicitly local development. Production frontend/API should be same-origin, probes restricted and database exposure controlled. Multiple instances require additional verified resource/session/execution behavior, not an assumption based on a shared database.

## Recovery

The first database-bearing slice requires backup and restoration of Authority Pro data into a NEW isolated destination; never restore over the other application's database. Do not back up or restore the whole shared cluster as a shortcut. RPO/RTO remain proposed workload-dependent targets. Whole-project recovery does not prove selective tenant restoration or reversible application migrations.

## Acceptance

**L02 bounded verification passed:** clean verify on the approved project-portable toolchain completed 65 unit tests and 4 packaged integration tests with no failures or skips. This includes real loopback port-8080 main startup with valid configuration, invalid-config and occupied-port refusal, exact compiled five-root/zero-edge discovery with negative fixtures, only exact GET liveness 200/UP and readiness 503/OUT_OF_SERVICE, and denial elsewhere. CSRF is retained; there is no session/default user, database/driver or external-call path, and test-only OpenAPI is excluded from the production JAR. See the canonical execution evidence above; the offline clean repeat also passed.

**Later database-bearing acceptance:** verify approved target identity, non-colliding names, effective roles, bounded resource use, clean integrated startup, missing-secret/invalid-config refusal, migration target checks and an isolated restore drill. Record no secrets. Full G2 and later integrated/runtime-readiness gates remain open; L02 does not authorize database access, provisioning or deployment. The existing application remains outside the operation's mutation boundary.

## Deployment and distribution vision

**Decision boundary:** The owner requested integration of the reviewed external conversation into the socle vision. The contracts below are **PROPOSED / NOT IMPLEMENTED**, not approval of new infrastructure, executable schemas, remote administration or production deployment. The source uses AUTHORITY ONLINE; these proposals are reconciled for the independent Authority Pro product without importing legacy code or business functionality. Existing L02 and local PostgreSQL evidence remains bounded to those slices.

Keep one Angular/Spring Boot product with separately qualified deployment profiles. Placement (local/private/hosted), topology (single/multiple application nodes, cells/regions), connectivity (connected/disconnected/synchronizing) and resource budgets are independent dimensions, not seven mutually exclusive products. An edge installation is a constrained profile, not a promise that the full platform fits every device. No desktop shell, orchestrator, broker, cache or provider is selected here.

### Deployment capability matrix

This matrix refines existing C01–C58 allocations in [the coverage matrix](../PACK-INVENTORY.md#technical-coverage-matrix), not a new catalogue of engines. Every row below is **PROPOSED / NOT IMPLEMENTED / NOT TESTED** as an end-to-end distribution capability. Owners are responsibility areas, not assigned people. Each future release/profile must attach version-specific evidence or an explicit unsupported status; a successful local database setup does not qualify a distribution.

| Capability / existing rows | Value and proposed owner | Dependencies and bounded contract | Candidate stage / acceptance |
|---|---|---|---|
| One product, local/private/hosted profiles — C07/C43/C50 | Portability; Operations | Same application contracts and PostgreSQL/RLS baseline; isolated configuration, data, secrets and networking | Design now; hosted/private qualification after L1 and selected release controls; clean install and isolated restore |
| Multi-instance application — C01/C29–C33/C43/C49 | Capacity and availability; Runtime/Identity/Operations | Shared session authority, bounded aggregate pools, authorized cache invalidation, durable coordination where needed | Qualify before claiming scale-out; lose one node without leaked scope or duplicate effects |
| Deployment-aware module compatibility — C10–C13/C28/C41 | Reject unsuitable installations; Runtime | Versioned required services, platform/runtime/database versions, connectivity, resources and offline operation declarations | L2/L09–L11 candidate; unsupported/missing requirements fail before activation |
| Windows installer and optional desktop shell — C41/C48/C50/C57 | Accessible independent installation; Operations/Developer platform | Managed Java/backend/Angular/PostgreSQL lifecycle, non-colliding services, signed artifacts, recovery and data-preserving removal | Later selected distribution slice; interruption, collision and failed-update recovery tests |
| Fleet registration, supervision and controlled updates — C23/C33/C46–C48/C50/C57 | Govern independent installations; Operations/Security | Separate installation identities, scoped consent, redacted metadata, revocable actions and local autonomy | L3 candidate; central outage and unauthorized remote action tests |
| Autonomous local operation and edge — C07/C32/C43/C57 | Work without a cloud dependency; Operations | Available local backend/database, bounded hardware profile, declared external dependencies | L3 candidate; disconnect Internet and verify only declared local operations |
| Selective hybrid synchronization — C14/C16/C24/C25/C45/C57 | Controlled sharing without exporting everything; Data/Integration | Per-operation write authority, durable checkpoints, conflict/revocation/deletion and schema policies | L3 candidate after durable execution; interruption/replay/conflict and long-disconnection tests |
| Cells, tenant placement and migration — C03/C34/C35/C43/C45/C50/C57 | Bound failure impact, dedicated capacity and residency; Tenancy/Operations | Explicit routing authority, independent cell data boundaries, one writer during movement, identity/files/jobs/audit consistency | L3 candidate justified by load/residency; stale routing, migration and isolated recovery tests |
| Offline entitlements — C09/C36/C47/C48 | Distribute without permanent license-server access; Licensing/Security | Signed bounded grants, keys, expiration/grace and anti-replay policy distinct from user permissions | L3 candidate; invalid signatures, expiration, clock manipulation and disconnected revocation limits |
| Local and optional central observability — C32/C33/C45/C49/C57 | Diagnose connected or disconnected installations; Operations | Bounded local retention, privacy-filtered export and explicit freshness | Extend L1 baseline only as selected; no secret/customer payload export or false healthy state |
| Portable backup and cross-provider recovery — C34/C35/C43/C47/C50 | Reduce provider dependence; Operations | Consistent database/files/configuration versions, recoverable keys and isolated destination | L1 restore first; later selected portability slice with timed restore, not merely a copied backup |
| Multi-region and advanced replication — C03/C35/C43/C49/C50/C57 | Regional resilience where justified; Operations | Write authority, fencing, replication lag and consistency policy, explicit operating cost | L3 only after measured need; controlled failover and partition tests; no universal multi-primary promise |

Detailed cases are in [deployment acceptance](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md#deployment-and-distribution-acceptance). The stage names do not create new numbered lots or automatically enlarge L1/L2.

### Profile and artifact compatibility contract

A proposed deployment record identifies product/build version, installation ID, environment ID, selected profile and topology, runtime/database versions, service capabilities, network dependencies, resource envelope, secret references, storage, update channel, backup/recovery policy and support limits. Distinguish a persistent installation ID from replaceable application-node IDs, cell IDs and tenant IDs; identifiers alone are not authentication. A managed clone or restored copy must re-enroll/rekey rather than silently reuse a live installation's cryptographic authority. Detect duplicate use only where connected behavior makes it observable; identical offline copies of credentials cannot reliably be distinguished without a separately approved hardware trust mechanism.

A future module contract declares required infrastructure capabilities and versions, supported profiles, persistence and resource needs, and connectivity/offline behavior per operation. Prefer capability requirements over provider names. Unknown or unsatisfied required capabilities block activation; optional degradation must be declared and visible. Configuration cannot override authorization. The existing foundation JSON manifest remains unchanged: approve a versioned extension and positive/negative fixtures before adding fields or claiming an installer enforces compatibility.

Prefer standards and adapters at actual application boundaries (storage, secrets, telemetry), not an application-level abstraction for every compute or DNS operation. Reproducible deployment definitions may use provider-specific implementations. PostgreSQL is not interchangeable with an embedded engine: an alternative requires a separate isolation/migration design and evidence. No provider or Kubernetes dependency is introduced by portability requirements.

### Multi-instance and capacity contract

Externalize authoritative sessions, shared files and durable work; process-local caches must be bounded and disposable, never the authority for grants or job ownership. Keep same-origin browser sessions and CSRF protections. Verify login/logout, session expiry/revocation and membership changes across actual nodes; do not use sticky sessions to conceal a correctness dependency.

The database budget must bound the sum of maximum pools across application nodes, identity/session paths, workers and migration tools, including rollout overlap and operational reserve, below the approved server and role limits. Queue depth, concurrency, timeouts and per-tenant fairness require measured limits; adding application nodes does not increase database capacity. The local environment's 20-connection server and five-connection runtime role limits are not production sizing.

Specify readiness withdrawal, request drain, shutdown deadlines, interrupted work and lease fencing. Migrations have one controlled runner outside normal requests, with explicit concurrency exclusion. Mixed application/schema versions during rollout need a validated compatibility window; stop a rollout when compatibility fails rather than falling back to destructive schema reversal. Retried commands and jobs retain authorized idempotency/deduplication semantics.

Use a workload-based capacity plan: registered users are not concurrent active users or requests per second. Candidate incremental loads such as 100/500/1,000/5,000 active users are experiments to size safely, not promised capacity or mandatory loads on the development machine. Record latency percentiles, throughput, errors, saturation, sustained-load behavior and failure recovery using the existing [measurement protocol](../01-PRODUCT/NON-FUNCTIONAL-REQUIREMENTS.md#1-performance). A proxy/load balancer may be sufficient; an API gateway, distributed cache or broker is not automatically required.

### Installation, update and recovery contract

The first proposed independent distribution keeps Angular, a supported Java runtime, Spring Boot and PostgreSQL accessible through a normal browser. Tauri/Electron remains an optional packaging decision, not a second implementation. Define supported Windows versions/architectures, service identity and minimum measured resources before choosing installer tooling; Docker Desktop is not assumed to be a customer prerequisite.

Preflight ports, service/resource names, permissions, disk and existing data; stop on collisions and leave unrelated installations intact. Local interfaces bind to loopback by default; LAN exposure requires an explicit authenticated/TLS configuration. Validate browser-origin/Host handling and CSRF for local services; localhost is not authentication. Elevation, if necessary, requires explicit operator action, never self-elevation.

Updates verify trusted signatures, provenance, target profile and compatibility before execution. Define signing-key rotation/revocation, release channels, maintenance/drain and approved downgrade policy. Back up recoverable data/configuration first; test interruptions at each stage. Code rollback is permitted only when schema-compatible; otherwise use reviewed forward repair or isolated restoration. Uninstall preserves data unless separately authorized deletion is requested. DPAPI-protected development credentials are machine/user-bound inputs, not a portable backup strategy; define separately protected recovery key access without publishing secrets.

### Fleet control and security contract

An infrastructure control plane owns installation registration, safe version/health metadata and authorized maintenance coordination. It is distinct from the identity control plane described in ADR-0003 and from [Thunder Control](../03-CORE/THUNDER-CONTROL.md), which is an administration surface, not a global database authority. Neither gains tenant-data access by knowing placement or holding a platform-owner label.

Registration requires an authenticated bootstrap and installation-bound cryptographic identity; define key rotation, expiry, revocation and clone/recovery enrollment. Prefer controlled outbound/private connections, not publicly exposed local administration. Remote actions are allowlisted, scoped to an installation and exact version/action, time-bounded, revocable and audited under local customer authorization. Registration or a valid package signature alone does not approve execution. No arbitrary shell, raw SQL or unrestricted file access follows from fleet management.

A control-plane outage must not stop already authorized local operations within their declared validity windows. Cached policy cannot grant indefinite access after expiry; define fail-closed sensitive actions and explicit limited degradation. Suspension/decommissioning must define export/recovery, outstanding jobs and credential revocation without automatic data destruction. Central observability is optional and privacy-filtered; local diagnostics remain bounded and useful while disconnected.

### Offline entitlements and hybrid consistency contract

Distinguish a browser disconnected from its backend, a local installation disconnected from the Internet, and two writable installations synchronizing later. These are different capabilities. Local autonomy requires a functioning local authority and services, not a synchronized database or a desktop shell.

Licensing grants product/module usage, never account membership or tenant-data authority. Define signed grant issuer/audience/installation scope, validity, key rotation, grace and transfer/revocation policy; handle clock rollback and replay without claiming tamper-proof operation on a customer-administered host. Immediate revocation cannot be guaranteed during disconnection. Expired authority follows an explicitly approved restricted/read/export policy, never silent destructive lockout. Commerce, billing and subscription workflows remain excluded.

Before synchronization, classify each technical resource/operation as local-only, centrally authoritative, locally authoritative, read-only replicated or conflict-managed. Specify permitted data fields, tenant/company/site scope, consent/residency, stable IDs, version/causal ordering, bounded durable queues, checkpoints, backoff, idempotency and partial-failure recovery. A transport envelope alone cannot define conflict resolution for future business modules.

Reauthorize replay against current receiving-side permissions. Define offline permission validity and revoked/expired/unsupported commands as rejected or quarantined, not silently accepted. Deletions require tombstones with a retention horizon and mandatory reinitialization for peers older than that horizon; do not resurrect removed data after a long disconnection. Schema incompatibility pauses/quarantines replay. Do not indiscriminately replicate tables, credentials or framework sessions; no default confidential-data export or last-write-wins policy for sensitive state. Critical state changes and their audit retain the applicable atomicity requirements.

### Cells, placement and replication contract

A cell is an independently operated application/data boundary serving an assigned tenant set; it can initially run the same monolith rather than introducing microservices. Shared tenant_id/RLS remains the baseline inside each cell, including a dedicated single-tenant cell. Schema-per-tenant or another database topology requires its own decision; this vision does not reopen the approved strategy by default.

Placement metadata is authenticated control-plane routing information, not a user-supplied database address or authorization grant. Verify tenant membership and the destination's assignment; stale or inconsistent placement fails closed. Decide separately how accounts and sessions are scoped/federated across cells; never copy identity secrets or introduce a universal identity/tenant bypass as a routing shortcut.

Tenant movement must enumerate identities/memberships, tenant records, files, pending jobs/events, audit, keys and version dependencies. Use a reviewed freeze/drain, copy/verify and cutover procedure with one write authority and stale-writer fencing. Define routing epochs, retry behavior, recovery point and rollback limits after new writes. Promise continuity only when that procedure has evidence; full-cell backup is not selective tenant restoration.

Start with independently deployable installations and, when selected, single-region application scale-out. Evaluate active-passive database recovery before multi-region multi-writer operation. Specify promotion authority, fencing, lag/RPO, read-after-write behavior and failback. Stale read replicas cannot authorize sensitive writes. Cross-provider recovery requires an isolated restore exercise, keys and measured RPO/RTO; replication alone is not a backup. Avoid global transactions across cells and unrestricted active-active writes.

## Composition distribution and commercial compatibility

**PROPOSED / NOT IMPLEMENTED — documentation only.** AP-ARCH-EVOL-001 and
AP-MASTER-CONSTITUTION-001 refine the existing deployment contracts under the
[2026-11-20 authorization](../00-GOVERNANCE/DECISION-MANAGEMENT.md#visual-composition-documentation-authorization--2026-11-20),
[coverage refinement](../PACK-INVENTORY.md#visual-composition-coverage-refinement)
and [open decisions O20–O25](../00-GOVERNANCE/DECISION-MANAGEMENT.md#4-open-decisions).
They introduce no infrastructure, installer, local agent or executable manifest
fields. CLOUD, PRIVATE (private cloud/on-premise), STANDALONE, HYBRID and
DISTRIBUTED remain profiles of one product; EDGE resource constraints and OFFLINE
connectivity behavior are independent qualifications. Existing fleet, identity,
backup/recovery, synchronization, cells and operational contracts above remain in
force, as do the deferred GHOST/FOX/PATCH/SHIELD responsibilities; no replacement
or additional service is implied.

### Per-application, module and node declarations

For each proposed application, module, package and node version, declare supported
target profiles, minimum Core/runtime/contract versions, required services and
storage, resource envelope, network destinations/protocols, offline behavior,
synchronization requirements and optional degradation. Device-bound capabilities
also declare device identity/capability requirements, adapter versions, connection
location and any local-agent requirement, including its authentication, update,
revocation and health contract. A cloud backend cannot assume access to a local
printer or browser-host filesystem; missing required adapters/agents block that
capability. This does not select or install an agent or grant arbitrary local access.

Resolve transitive requirements across compositions before activation: an offline
application containing an online-only node is not wholly offline-capable. Test
only supported combinations; unknown or incompatible declarations fail closed.
Use the same approved compatibility contracts for native, visual, SDK and AI
paths, not separate studio-specific deployment semantics. The unchanged foundation
manifest is not evidence that these proposed declarations are executable today.

### Independent lifecycle, entitlement and authorization dimensions

Installed, configured, published, active and entitled are separate dimensions,
not interchangeable labels or a single linear deployment state. A preinstalled
AI Studio may be visible but neither active nor entitled to advanced calls;
a published package need not be installed, and installation cannot activate it
or load every optional component into memory. Activation needs compatible artifacts,
configuration, applicable entitlement and CURRENT scoped permissions. Entitlement
controls permitted product usage/quotas at instance, tenant or user scope; it never
creates roles, memberships or rights to tenant/company/site data. Commercial
changes cannot override the owning authorization or audit contract.

Signed offline entitlements reuse the bounded issuer/audience/installation,
expiry/grace, clock/replay and revocation policy above. A temporary license-server
outage preserves already authorized essential local operations **only within the
approved continuity window**. Specify renewal, finite grace, post-expiry restricted
operation/export and recovery, plus revoked/expired handling on reconnection;
neither permanent access from a cached signature nor immediate disconnected
revocation is promised. Essential local safety/data recovery and restrictions
require explicit policy, not destructive lockout or implicit perpetual grants.

### Retained commercial proposals, not commerce implementation

Preserve catalogue proposals for modules, features/extensions/plugins, custom and
composite nodes, workflows, data/UI/dashboard templates, integration/device
connectors, industry/application packages and developer/premium packages. Proposed
models include free, paid one-time, licensed, trial, subscription and enterprise
packages; publisher verification, purchase history, revenue sharing/commissions
and marketplace fees remain product/legal decisions, with no fee amount or billing
mechanism approved. AI proposals retain restricted free access, Creator,
Professional and Enterprise tiers, usage credits, additional capacity and private
AI deployment. These names are candidate offers, not security roles or implemented
licenses. O4/O7/O15/O18 and O20–O25 must settle scope, legal terms, usage accounting,
compatibility and continuity before a selected slice; no commerce/billing,
marketplace download, subscription or license-server implementation is authorized.

## Related

- [Approved orientation and database constraint](../00-GOVERNANCE/DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations)
- [Database architecture](DATABASE-ARCHITECTURE.md)
- [Non-functional requirements](../01-PRODUCT/NON-FUNCTIONAL-REQUIREMENTS.md)
- [Validation gates](../13-IMPLEMENTATION/VALIDATION-GATES.md)
