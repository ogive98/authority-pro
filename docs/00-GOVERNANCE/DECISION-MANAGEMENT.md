# Decision Management

**Decision status:** PROPOSED process details; locked blueprint principles and
explicit approvals are recorded separately below.
**Delivery status:** L02 no-database backend skeleton IMPLEMENTED / TESTED; local
PostgreSQL environment preparation is separately authorized below. Functional data
integration, identity, audit, frontend and releases remain undelivered.

## 1. Decision types

| Type | Use | Approval |
|---|---|---|
| **ADR** | Architecture or technology choice with lasting impact | Explicit owner decision after appropriate technical review |
| **RFC** | N0 change proposal with impact analysis | Explicit owner decision after review |
| **Design note** | N1 implementation approach | Technical review; approval process proposed |
| **Ticket** | N2 work item within authorized scope | Proposed team review; no independent scope authorization |

Product ownership does not establish technical expertise or technical review.
Record the author, reviewer and approver separately, only when evidenced.

## 2. ADR lifecycle and delivery evidence

Decision lifecycle:

```text
PROPOSED -> APPROVED -> (SUPERSEDED | DEPRECATED)
    |
    +-> REJECTED
```

- One ADR per decision, numbered sequentially, never silently replaced.
- Decision approval does not imply implementation, testing, release or permission
  to execute actions outside the approved scope.
- Record status changes with the actual date, decision maker, exact approved
  scope and evidence link. Do not invent an approval date or reviewer.
- Superseding an ADR requires a new ADR referencing the old one.
- ADRs live in [`docs/adr/`](../adr/).

Required sections follow the [ADR template](../14-TEMPLATES/ADR-TEMPLATE.md):
context, problem, options, recommendation/decision, reasons, consequences,
risks, migration implications, decision status, delivery status and evidence.

Track delivery separately: **PLANNED / NOT IMPLEMENTED**, **IMPLEMENTED**,
**TESTED**, **RELEASED**, or **DEPRECATED**. Before advancement, link respectively
to actual code/build artifacts, reproducible test commands and results for the
identified revision/environment, and release approval plus the published artifact.
A diagram, generated specification, unchecked checklist or future path is not
implementation evidence. Test execution and release must never be inferred from
an approved ADR. See [status rules](DOCUMENTATION-STANDARDS.md#3-status-labels).

## 3. Current decision log

### Approval basis

The owner explicitly approved, in the instruction commissioning these corrections:
**Authority Pro**, **C:/Users/admin/Documents/authority-pro**, **one monorepo**,
**Angular + Spring Boot**, **modular monolith**, **shared PostgreSQL with tenant_id
and RLS**, and **normative documentation plus a runnable foundation, with no
business modules**. These approvals are limited to that scope. The instruction
supplies no stable conversation URL or separate technical review; this paragraph
records the available evidence without fabricating either. Preserve these known
approvals; future decision-status advances require a durable approval reference.

[MASTER-BLUEPRINT](../MASTER-BLUEPRINT.md) sections 2 and 5 are the locked source
for DDD, hexagonal architecture, Plugin-Based Components, business-neutral core,
strict ownership, explicit contracts and secure-by-default principles. Section 15
requires FR, EN, IT and AR/RTL. The source's AUTHORITY ONLINE title does not
connect this independent project to legacy AUTHORITY.

### Owner confirmation of foundation orientations

The owner's subsequent conversational response explicitly validated the six presented orientations: "validée sauf que le postgres tourne deja pour une autre application ... creer une nouvelle base de donnes sans toucher a l'ancienne". This is bounded approval of the following principles, not of every implementation detail in the linked ADRs. The approval is recorded from this conversation; no separate technical reviewer, stable conversation URL or executed verification is invented.

1. Spring Security with JDBC-backed browser cookie sessions and CSRF protection.
2. Initial administrator creation through a local operator CLI, never a public setup page.
3. Restricted PostgreSQL runtime privileges, RLS and server-verified transaction-local tenant context.
4. Critical sensitive state changes and their audit commit together or roll back together.
5. Compiled first-party components with explicit contracts; no arbitrary hot-loaded plugins.
6. Application APIs under `/api/v1`, controlled tenant selection and standardized errors.

**Historical database constraint:** PostgreSQL already serves another application, according to the owner. Authority Pro must use a newly created dedicated database, not that application's database or schemas. The original same-server direction and candidate name `authority_pro` were subsequently reviewed; the [dedicated local instance decision](#l03-local-environment-authorization--2026-11-18) now governs environment preparation. No existing database, role, service configuration, data directory or volume may be overwritten or repurposed. No restart, server upgrade, drop/recreate, restore over existing data or global privilege change is authorized. Dedicated project roles need their own approved names and narrowly scoped creation plan because PostgreSQL roles are cluster-wide.

This authorizes the new-database direction, not execution against an unidentified endpoint or elevation of privileges. Host/port, actual server version, unused database/role names, operator authority and safe secret handling must be established before provisioning. A collision or need to alter the other application's security/configuration stops the operation for owner review. No database connection, creation or migration is claimed by this record.

Those six orientations alone did not approve numeric versions, SQL/grants, identity policies, bootstrap guards, session/audit integration, module tooling, tenant headers or exact HTTP/probe contracts. The subsequent L02 decision below approves only its bounded subset. Other G1 implementation/readiness conditions remain open; the existing database is not assumed compatible merely because a client or draft Compose image exists.

| ADR | Decision | Decision status | Delivery status |
|---|---|---|---|
| 0001 | Angular + Spring Boot | APPROVED stack; L02 backend subset approved through ADR-0010, other numeric versions PROPOSED | L02 backend qualified; Angular NOT IMPLEMENTED |
| 0002 | Modular monolith | APPROVED style and bounded L02 package graph/Modulith/ArchUnit selection; future functional dependencies separately gated | Five closed roots/zero edges qualified; functional services NOT IMPLEMENTED |
| 0003 | Shared PostgreSQL, `tenant_id`, RLS | APPROVED strategy and bounded closed L03 technical integration; authenticated authorization and broader policies remain separately gated; dedicated instance, other applications untouched | Technical application integration IN PROGRESS; isolated scratch evidence does not qualify the main database or user authorization |
| 0004 | One monorepo | APPROVED organization | Existing private repository and locally qualified L02 backend |
| 0005 | Spring Security + Spring Session JDBC; browser cookie sessions and CSRF; local operator bootstrap | APPROVED orientations; detailed policies/integration PROPOSED | L02 deny-all/CSRF boundary tested; identity/JDBC sessions/provisioning NOT IMPLEMENTED |
| 0006 | One durable publication mechanism, selected by spike: Modulith registry or explicit outbox | PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0007 | Flyway migrations | APPROVED bounded closed L03 subset under the 2026-11-19 authorization; broader upgrade/deployment workflow PROPOSED; excluded from historical L02 | Application integration IN PROGRESS; scratch qualification is separate; main migration and current application qualification remain gated |
| 0008 | Plugin execution model | APPROVED compiled first-party orientation and L02 single-artifact boundary; descriptor lifecycle/distribution details remain PROPOSED; third-party execution deferred | L02 artifact boundary qualified; plugin lifecycle NOT IMPLEMENTED |
| 0009 | Script engine language and sandbox | PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0010 | Numeric Java / Spring Boot version baseline | APPROVED L02 subset; database/frontend matrix remains gated | Portable target JDK/wrapper and online/offline clean L02 verification passed |
| 0011 | URI API versioning and tenant selection/header contract | APPROVED orientations plus bounded L02 local probes/error envelope; tenant headers, identity integration and hosted exposure remain PROPOSED | L02 probes/errors qualified; functional API/tenant selection NOT IMPLEMENTED |

### L02 skeleton decisions — 2026-11-18

**Decision status: APPROVED for the bounded L02 design below. Delivery: L02 implemented and locally qualified; see [execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18).** After requesting finalization of L02 decisions, the owner explicitly selected all three recommended answers in the decision questionnaire. The available approval evidence is transcribed here; no stable conversation URL or separate human technical reviewer was supplied. Assistant research and metadata checks are not independent security certification.

| Approval question | Exact selected answer | Approved scope |
|---|---|---|
| Quelle chaîne de compilation approuvez-vous pour le squelette L02 ? | Cible isolée (Recommandé) | Java 21 Temurin 21.0.13+9 portable for this project, Spring Boot 4.1.2 BOM, Maven 3.9.16 and Wrapper 3.3.4 with integrity checking; no global Java replacement |
| Approuvez-vous les cinq composants techniques et leur contrôle automatique pour L02 ? | Modulith et ArchUnit (Recommandé) | One backend artifact, root `com.authoritypro.platform.AuthorityProApplication`, exactly `identity`, `tenancy`, `access`, `audit`, `runtime`; Modulith 2.1.2 and ArchUnit 1.4.2; no intercomponent calls in L02, no event engine or dynamic loading |
| Approuvez-vous le démarrage local fermé et sans base de données décrit ci-dessous ? | Local fermé (Recommandé) | Explicit `skeleton-local` mode on `127.0.0.1`, port 8080 only if free; only GET liveness/readiness, readiness 503 while incomplete; deny other access, retain CSRF, validated configuration and RFC 9457 with `code`/`traceId`; springdoc 3.2.0 for tests/build only |

The selected alternatives were presented against retaining the installed Microsoft JDK, using ArchUnit alone, omitting the HTTP listener, or deferring approval. The isolated target keeps this project reproducible without altering other applications, at the cost of a new local JDK download and verification. Modulith/ArchUnit reduce bespoke boundary tooling but are not a hostile-code sandbox. Minimal local probes permit diagnosis without claiming an authenticated application is ready.

The [version subset](../adr/ADR-0010-java-spring-versions.md#l02-baseline-approval--2026-11-18), [package boundary](../adr/ADR-0002-modular-monolith.md#l02-boundary-approval--2026-11-18) and [local HTTP contract](../adr/ADR-0011-api-versioning.md#l02-local-http-approval--2026-11-18) carry the detailed acceptance constraints. Later exports/consumers need their own declared dependency approval; no placeholder identity, audit or registry service may pretend those capabilities exist.

No JDBC/JPA/driver, embedded database, Flyway migration, session store, default account, login or protected mutation belongs to L02. Database/RLS design and provisioning remain L03, audit L04, identity L05, protected inventory L06 and Angular L07. No change to the existing PostgreSQL application, role or service, not even a connection preflight, is authorized by this decision.

The questionnaire step recorded design decisions only. The portable JDK, wrapper and real skeleton must be verified during separately started L02 implementation; a failed integrity, compatibility or security check stops acceptance rather than authorizing an unsupported override. No download, application code, database access, new commit, push or deployment was performed by that questionnaire record. At that point the previous push remained on hold. This resolved the stated L02 design choices, not all of G1, the database-bearing G2 gate or production readiness.

### L02 implementation and publication authorization — 2026-11-18

The owner subsequently instructed: “continue seule , enchaine avec le lot prochain  commit et pousse , prends les bon choix pour moi”. This authorizes autonomous implementation and qualification of the next lot, L02, followed by a new verified commit and push to the existing repository. It lifts the prior publication hold for the pending documentation and this verified skeleton; it is not evidence that a push or any application test has succeeded.

The bounded work includes project-local tools/build configuration, startup/security adapters, architecture and artifact tests, repeatable qualification commands and the corresponding CI checks. Only the project's own loopback test processes may be started and stopped; an occupied port must not be freed by terminating another service. No business module, PostgreSQL connection/provisioning/migration, other-application change, deployment or automatic L03 implementation follows from this instruction.

### L03 local environment authorization — 2026-11-18

**Decision status: APPROVED for local environment preparation only.** The owner
requested: “prepare tout l'envirenemment authority pro sans toucher a authority”.
The authorized read-only preflight observed PostgreSQL 16.15 in `authority-postgres`,
Windows port 5433, and PUBLIC CONNECT/TEMPORARY privileges on the existing
`authority` database. These privileges do not prove access to application tables,
but cannot be denied to one new role without changing existing permissions.
No application data or password was read, and no existing privileges were changed.

After this finding, the owner explicitly selected **“Instance dédiée (Recommandé)”**
in answer to permission to create a separate PostgreSQL alongside the existing
container. This replaces the earlier same-server local topology, not the shared
`tenant_id`/RLS strategy inside Authority Pro. The owner also reaffirmed that all
other concurrent projects, Docker resources and pgAdmin connections stay untouched.
No separate human technical reviewer or conversation URL is invented.

The bounded preparation uses a new `authority-pro-postgres` container, separate
`authority-pro-pgdata` and `authority-pro-pgsecrets` volumes, and
`authority-pro-network`; only loopback port 5434 is published. PostgreSQL 16.15
uses the already available digest pinned in [Compose](../../compose.yaml), with
256 MiB memory and 0.5 CPU limits. A temporary project-owned, networkless secret
writer is removed only after success; failed resources are retained for inspection.
No existing container is restarted, renamed, attached, removed or reconfigured.

New identities are `authority_pro_operator` (local setup only),
`authority_pro_owner` (NOLOGIN), `authority_pro_migrator` (explicit SET ROLE to
owner) and `authority_pro_runtime` (restricted LOGIN without owner membership).
Only this new cluster's database/schema ACLs are changed. Generated passwords
never enter source, chat, command arguments or logs: Windows CurrentUser DPAPI
protects the project-local credential file, and a dedicated read-only Docker
secret volume supplies bootstrap material. Application migration credentials
must remain separate from runtime credentials.

The [execution evidence](../PACK-INVENTORY.md#l03-local-environment-evidence--2026-11-18)
records what was actually verified. This is not approval or delivery of Flyway,
JDBC application integration, tenant schemas/RLS, identity, audit, Angular,
backup/restore, G2 completion, deployment or publication of this new work.

### Deployment vision integration — 2026-11-19

After reviewing the supplied AI conversation on cloud, private, Windows, hybrid and distributed installations, the owner accepted the recommendation to retain these capabilities in Authority Pro's vision and requested continuation: "ok je te suis , enchaine". This authorizes the present documentation enrichment, not all imperative instructions inside the external conversation. The source's AUTHORITY ONLINE branding does not replace Authority Pro or authorize reuse of the other application.

The [deployment capability matrix and contracts](../02-ARCHITECTURE/DEPLOYMENT-ARCHITECTURE.md#deployment-capability-matrix) reconcile the proposals with existing C01–C58 owners. One product, distribution compatibility, installation lifecycle, controlled fleet operations, local autonomy, selective synchronization, cells/placement, offline entitlements, observability and portable recovery are retained as proposed capabilities rather than silently discarded. Detailed mechanisms, support promises and sequencing remain PROPOSED / NOT IMPLEMENTED.

No new backend component, executable manifest revision, Windows installer, database topology, cloud provider, container, service, CI workflow or remote-management permission is approved by this step. No commit, push, release or deployment follows from it. L03 application isolation/migration decisions remain the next nucleus gate; advanced distribution does not enlarge L01–L08. The immutable blueprint/archive is unchanged. Future delivery requires the scoped decisions below and [actual acceptance evidence](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md#deployment-and-distribution-acceptance).

### L03 isolated qualification authorization — 2026-11-19

After the backend/role audit, the owner selected "Qualification isolée (Recommandé)": qualify Flyway and JDBC without JPA, transaction-local context and RLS using technical fixtures in two NEW test/restore databases inside the dedicated `authority-pro-postgres` instance. The bounded destinations are `authority_pro_l03_probe` and `authority_pro_l03_restore` at `127.0.0.1:5434`; verify environment/cluster identity, role grants, available RAM and both unused names before creation. Stop on collisions, target mismatch or inadequate resources; retain partial resources without automatic deletion.

This allows test-only migrations/DML, an isolated backup/restore and project-local compatibility tooling. It does not authorize migrating `authority_pro`, changing the L02 serving artifact, touching other projects, creating another server, publishing code or claiming G2 completion. Existing project operator/owner/migrator/runtime identities may receive only the necessary privileges in the two new databases; operator credentials stay out of the Java probe. Generated secrets stay protected and are passed only in process memory/stdin, never source, arguments or reports.

Candidate dependencies come from the existing Boot 4.1.2 BOM: Flyway core/PostgreSQL adapter 12.4.0, pgjdbc 42.7.14, HikariCP 7.0.2 and Spring JDBC/transaction support 7.0.10 on the portable Temurin 21.0.13+9. They are authorized for qualification, not approved application pins or proven supported combinations. Record resolved versions, failures, security/license findings, tested boundaries and recovery limitations before recommending application adoption. No identity/session or production tenant-authorization guarantee follows from trusted technical test contexts.

The [executed qualification record](../PACK-INVENTORY.md#l03-isolated-qualification-evidence--2026-11-19) now records 71 probe checks, 50 restored-target checks and the subsequent strengthened verification. This establishes bounded compatibility evidence only. Main-database migration, application dependency adoption and changes to the closed L02 contract still require a separate decision; supply-chain and integrated authorization/recovery acceptance remain open.

### L03 closed application integration authorization — 2026-11-19

After the isolated qualification, the owner selected **"L03 fermé (Recommandé)"** in response to authorization for backend integration and the first main-database migration after passing tests. The approved boundary is JDBC without JPA, separately executed Flyway migrations and technical tenant/organization, company, site and membership structures with forced RLS. The selected candidate versions are the Boot-managed subset already qualified, subject to renewed dependency checks and actual application tests before migration.

Preserve the five closed modules and expose no user API, account, login, management screen, business module or demonstration data. Explicit `skeleton-local` remains database-free in behavior; `data-local` uses only the restricted runtime credential and never migrates; `migrate-local` uses only the migrator credential and never starts HTTP. All modes stay local and fail closed; successful database checks do not make the incomplete nucleus ready. Transaction contexts in this slice are internal technical boundaries, not authenticated membership authorization or a substitute for L04 audit/L05 identity.

The only main mutation target is `authority_pro` in the previously verified dedicated `authority-pro-postgres` at `127.0.0.1:5434`. Production migration SQL contains no fixture data. First qualify tracked migrations/transactions on new collision-checked `authority_pro_l03_app_test` and `authority_pro_l03_app_restore`, preserving the earlier scratch databases and all other projects. No automatic deletion, resource adoption, role-wide privilege changes, other-server access, service restart, commit, push or deployment is authorized. Verify actual test results, source/artifact identity and exact target before applying the first migration; retain partial resources and stop on failures.

### Visual composition documentation authorization — 2026-11-20

After reviewing AP-ARCH-EVOL-001 and AP-MASTER-CONSTITUTION-001, the owner requested: "ok continue sans rien oublier des document fonctionnalites et modifications que je t'ai donner". The assistant presented an additive consolidation plan covering the existing capability inventory, UAM, studios, extension points, runtime, decisions and acceptance scenarios. The subsequent instruction "enchaine" authorizes this documentation consolidation. It does not approve every imperative in the supplied documents, rewrite the constitution, or authorize implementing their complete product vision. This record transcribes conversational evidence; no stable conversation URL or independent human review is claimed.

Preserve the immutable blueprint/archive, C01–C58, existing approved architecture and the socle-only implementation boundary. The [source and requirement map](../PACK-INVENTORY.md#visual-composition-coverage-refinement) retains both complements, including commercial, business and advanced operational candidates without adding them to the current delivery scope. Amend existing specifications rather than creating a parallel constitution or reorganizing the repository. New semantic contracts and allocations remain **PROPOSED / NOT IMPLEMENTED**, with O20–O25 below recording unresolved decisions.

No code, executable manifest schema, database, service, container, CI configuration, permission setting, commit, push or deployment is authorized by this documentation step. The previously authorized closed L03 work remains in progress and separately gated; historical L02 and isolated scratch evidence is not acceptance of the current application integration. Any N0 amendment still requires the constitution's RFC, ADR, impact/security review, migration plan and explicit owner approval.

### Qualification checkpoint publication authorization — 2026-11-20

The owner subsequently requested "prochaine etape puis commit et push", authorizing continued bounded L03 work and publication of verified changes, including the approved documentary consolidation. This supersedes the earlier commit/push hold for this checkpoint only, not the prohibitions on other projects, destructive cleanup, service restarts or deployment. The [application qualification record](../PACK-INVENTORY.md#l03-application-qualification-checkpoint--2026-11-20) distinguishes successful backend/persistence/restore checks from the still-unexecuted main migration and valid packaged database launches. Publishing that checkpoint does not close L03 or G2.

## 4. Open decisions

These questions are explicitly open. Recommendations must not be implemented
silently as though approved. Phase references are planning suggestions only.

| # | Question | Impact | Target |
|---|---|---|---|
| O1 | Compiled first-party execution and bounded L02 single-artifact packaging/boundary tools approved; later descriptor lifecycle/distribution and any declarative or isolated third-party execution remain open. A classloader is not an untrusted-code sandbox | Security, contracts, operations | Before later lifecycle/distribution implementation or any third-party execution authorization |
| O2 | Script engine: GraalJS, typed DSL, or no scripts in the foundation? | Security, UX, performance | Before any Phase 5 authorization |
| O3 | Search: PostgreSQL full-text vs OpenSearch | Infrastructure, cost, relevance | Before search implementation |
| O4 | Marketplace and licensing operating model | Product, legal | Future vision |
| O5 | Which operations are local-only, online-only or synchronized; what write authority, validity window, conflict/tombstone/checkpoint policy and schema/revocation behavior apply? | Data integrity, privacy, disconnection and recovery | Before a selected hybrid slice; see deployment consistency contract, not general table replication |
| O6 | Is any staged language rollout acceptable? FR/EN/IT/AR with RTL remain required; FR/EN-only is not approved | Accessibility, acceptance | Before language scope changes |
| O7 | AI providers and data-sharing policy | Compliance, cost | Future vision |
| O8 | L02 JDK/build and boundary-tool subset approved and historically qualified; closed L03 candidate dependencies approved subject to renewed dependency/application qualification. Frontend selection remains gated | Compatibility and maintenance; bounded earlier evidence cannot qualify changed inputs | Fresh backend/dependency and application checks before main migration; frontend review before L07 |
| O9 | Sessions/CSRF and local operator CLI approved as orientations; exact cookie/expiry/recovery/MFA policies, guard, role model and grants remain open | Security, authorization | Before corresponding implementation or production readiness |
| O10 | Dedicated environment and closed L03 technical integration have bounded approval; current application qualification, restoration and first main migration remain gates, while authenticated membership context belongs to L05; all other projects untouched | Data isolation and coexistence | Complete the approved L03 evidence before main migration; no scratch-to-application acceptance inference |
| O11 | Critical sensitive-state/audit atomicity approved; action taxonomy, framework integration, retention and measurable budgets including password-hash cost remain open | Integrity, capacity | Before corresponding implementation and acceptance |
| O12 | Which bounded workflow dialect/engine, definition/instance ownership, task/deadline rules and running-version/external-effect protocol satisfy the deferred execution contracts? | Durability, tenant isolation, operating and migration cost | Before any selected L3 workflow/automation slice; not an L02 blocker |
| O13 | What agent/service identity, attenuated delegation, exact-action approval, memory/provenance and execution-budget policies apply, alongside O7 provider/export decisions? | Authority, privacy, audit and human control | Before selected L3 agent tools or memory; no approval through model confidence |
| O14 | Which governed UI schema dialect, compiled widgets, API bindings and publication/version rules are supported? Runtime entity/table/code generation remains outside this proposal | Contract evolution, accessibility and data ownership | Before C12/C38/C39 schema support in L2/L11; visual/AI builders remain L3 |
| O15 | Which profile/capability vocabulary, versioned module declarations, resource/connectivity requirements and release compatibility matrix are supported? | Installation correctness and contract evolution; standards/adapters versus unnecessary abstractions | Before deployment-aware manifest/runtime changes in a selected L2/L09–L11 slice |
| O16 | Which hosted/private topology, global pool budget, workload/SLO, session revocation, coordination, rolling-version window and migration runner are qualified? | Capacity, isolation and operating cost; simplest measured topology first | Before hosted or multi-instance support claims; no mandatory gateway/cache/broker/Kubernetes |
| O17 | Which Windows/architecture baseline, packaging/runtime/service strategy, signing trust, upgrade interruption and data-preserving removal policy apply? | Installation privileges, dependency redistribution, support and recovery | Before a Windows distribution slice; normal browser first, desktop shell optional; PostgreSQL baseline retained |
| O18 | Which installation bootstrap/rotation/clone rules, local customer permissions, remote-action limits, central-outage behavior and offline entitlement validity apply? | Fleet compromise, privacy, availability and offline revocation limits | Before remote fleet/entitlement actions; coordinate with O4/O5/O9/O11; no universal tenant access |
| O19 | Which cell/region placement authority, identity/session boundary, write fencing, tenant-movement scope, replication consistency and failback/RPO/RTO are justified? | Residency, integrity, recovery and operational cost | Before cell/multi-region implementation; evaluate independent installations and active-passive recovery first |
| O20 | What UAM ownership, versioned semantic dialect, references, import/export boundaries and coherent publication record join data/UI/process/capability definitions without duplicating domain or execution state? | Cross-studio compatibility, security, migration and model portability | Before a UAM schema or runtime; recommend a semantic model separate from canvas layout, domain values and provider state |
| O21 | Which typed node/port contracts, composite encapsulation, parameter binding, nesting/cycle rules and resource limits are supported, and which existing runtime owns execution? | Determinism, reuse, recovery and denial of service | Before composition execution; coordinate with O12 and preserve one authority for each execution fact |
| O22 | Which versioned screen extension points and permission-filtered visualizer projections may expose real installed capabilities, configuration and development navigation? | Metadata confidentiality, UI compatibility and accessibility | Before visualizer/injection implementation; recommend declared contributions, never arbitrary source rewriting or automatic Java-to-graph conversion |
| O23 | How do the six composition studios map to existing Studio areas, analytics and AI; which capabilities are mandatory, optional, installed, enabled, configured, published or entitled? | Product clarity, governance and independent operation | Before product contracts; recommend Intelligence for analytics/decisions and shared AI assistance without losing either complement's functions; PBC retains Plugin-Based Components |
| O24 | What reviewed development/build/migration/trust pipeline supports new data models, custom code, device adapters and reusable packages? | Ownership, executable supply chain, local-device authority and physical effects | Before generators, code publication or device execution; UI settings never authorize DDL, signatures never prove sandboxing and unknown print results require reconciliation |
| O25 | Which critical UAM/registry/extension contracts require N0 protection, and which dependent tooling can follow N1 without weakening approved boundaries? | Constitution compliance and durable compatibility | Before corresponding contract changes; keep existing N0 requirements, classify impact rather than accepting a blanket N1 label from a complement |

Open decisions need realistic options, trade-offs and a PROPOSED recommendation,
followed by explicit approval evidence. No universal RLS bypass is an acceptable
substitute for separately authorized control-plane and tenant-plane operations.

## 5. Emergency changes

PROPOSED procedure: an emergency may shorten design review to restore service,
with an ADR and incident record within 48 hours. It does not waive explicit
approval, security boundaries or evidence requirements. No service is released today.

## 6. Traceability

Every future approved N0 change must link its approval, RFC and ADR to the
implementation review and changelog. Implementation, tests and releases retain
separate evidence links; documentation approval alone cannot close a phase gate.

## Related documents

- [Platform Constitution](PLATFORM-CONSTITUTION.md)
- [Versioning and Compatibility](VERSIONING-AND-COMPATIBILITY.md)
- [Risk Register](../13-IMPLEMENTATION/RISK-REGISTER.md)
