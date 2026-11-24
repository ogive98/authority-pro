# Decision Management

**Decision status:** PROPOSED process details; locked blueprint principles and
explicit approvals are recorded separately below.
**Delivery status:** L02 and the bounded local closed L03 technical persistence slice
are IMPLEMENTED / TESTED; see [main recovery evidence](../PACK-INVENTORY.md#l03-main-recovery-evidence--2026-11-20).
Four technical tenancy tables remain empty. Clean-machine qualification, authenticated
isolation, identity, audit, frontend, full G2–G5 and production releases remain open.

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
| 0003 | Shared PostgreSQL, `tenant_id`, RLS | APPROVED strategy and bounded closed L03 technical integration; authenticated authorization and broader policies remain separately gated; dedicated instance, other applications untouched | Bounded local technical integration QUALIFIED: V1 applied, four empty technical tables, packaged no-op and data-local launches; user-authenticated isolation remains open |
| 0004 | One monorepo | APPROVED organization | Existing private repository and locally qualified L02 backend |
| 0005 | Spring Security + Spring Session JDBC; browser cookie sessions and CSRF; local operator bootstrap | APPROVED orientations; detailed policies/integration PROPOSED | L02 deny-all/CSRF boundary tested; identity/JDBC sessions/provisioning NOT IMPLEMENTED |
| 0006 | One durable publication mechanism, selected by spike: Modulith registry or explicit outbox | PROPOSED | PLANNED / NOT IMPLEMENTED |
| 0007 | Flyway migrations | APPROVED bounded closed L03 subset under the 2026-11-19 authorization; broader upgrade/deployment workflow PROPOSED; excluded from historical L02 | Bounded local migration qualified through the [2026-11-20 recovery](../PACK-INVENTORY.md#l03-main-recovery-evidence--2026-11-20); clean-machine and general upgrade workflow remain open |
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

### L03 main recovery and publication authorization — 2026-11-20

After the disclosed [post-migration stop](../PACK-INVENTORY.md#l03-main-migration-stop--2026-11-20), the owner requested "continue", then "puis enchaine et commit et push". This authorizes the bounded continuation and publication of verified changes, not a replay of the failed first-migration command. Bind recovery to the retained failure/qualification IDs and immutable journal hashes; keep the applied V1, existing privileges and all original journals unchanged. Verify the exact initialized default ACLs, type and column privileges, complete compared catalog, empty main tables, executable evidence and protected-resource identities before running only the outstanding packaged no-op and positive data-local checks. Record results in separate append-only recovery journals and stop on any new claimed failure.

No destructive cleanup, privilege weakening, other-project changes, persistent service, production deployment or automatic implementation of audit/identity/frontend is authorized. Those lots retain their own contracts and gates. Commit/push permission applies to the verified L03 continuation and its evidence; the earlier failed result must remain visible.

### Catalogue and layer integration authorization — 2026-11-21

The owner first required all capabilities in the supplied catalogue ("il faut absolement mettre tte ces fonctionnalites"), requested analysis, then explicitly authorized integration ("integre aussi tt ca") while reaffirming Modular Monolith, DDD, Hexagonal Architecture and PBC. The subsequent twelve-layer attachment supplements the same request. This authorizes updating existing architecture, requirements, terminology and acceptance documents, not implementing every source imperative. The [required target catalogue](../PACK-INVENTORY.md#required-target-catalogue--2026-11-21) records source identities, individual capability names and their mapping; the [architecture clarification](../02-ARCHITECTURE/TARGET-ARCHITECTURE.md#architectural-foundations-and-logical-layers) owns the twelve logical responsibility layers.

All supplied capability families are required coverage of the long-term product target, rather than disposable optional ideas. Optional installation, edition and deployment-profile support are separate from this requirement; scheduling, detailed contracts and implementation acceptance remain open. Retain each capability or an explicitly traced equivalent, accountable area, dependencies, gaps and acceptance criteria; omission or removal needs an explicit owner decision. This does not make every capability mandatory in every installation, expand L1 acceptance, approve commercial operation or authorize business modules. Current technical implementation remains limited to the previously approved slices.

The owner's **Packaged Business Capabilities** describes the product packaging and governance of capabilities; the blueprint's **Plugin-Based Components** remains the historical technical formulation. [ADR-0002](../adr/ADR-0002-modular-monolith.md#product-and-logical-layer-clarification--2026-11-21) and the [glossary](../GLOSSARY.md) make this correspondence explicit without changing first-party compilation, trust, module boundaries or executable manifests. The twelve numbered layers are a responsibility map, not a Java dependency chain, twelve deployment units or replacements for DDD/ports and adapters. Universal and Unified Application Model refer to the same proposed UAM, not two models; Application Studio complements the retained studios.

The supplied folder tree, P0 priority labels, business examples and suggested technologies remain explanatory inputs. No directory move, Maven restructuring, new module root, runtime loading, schema/permission change, database operation, broker/cache/provider installation, service restart, commit, push or deployment is authorized by this documentation instruction. Source business nodes call separately authorized provider contracts; no Orders, Inventory, invoicing or printing demonstrator is introduced. Unknown physical outcomes require reconciliation, not a promise of exactly-once printing. The source's later audit/reliability phase cannot defer protections required before sensitive mutations.

This is an assistant-authored record of actual conversational authorization, not an independently reviewed complete architecture or an N0 mechanism amendment. Preserve the immutable blueprint, existing C/VC identifiers, lot IDs, previous evidence and uncommitted L03 continuation. Detailed UAM, runtime, trust and deployment choices remain O12–O25 gates; the detailed L04 contract is now drafted with owner decisions pending, not completion of every advanced engine before coding.

### L04 detailed contract drafting — 2026-11-21

The owner instructed "prochain travail" after the catalogue and layer integration. The documented next nucleus gate was the detailed L04 atomic-audit contract, so this instruction authorizes drafting [Audit Engine §9](../04-PLATFORM-ENGINES/AUDIT-ENGINE.md#9-l04-detailed-contract-proposal--2026-11-21) only. The proposal records concrete taxonomy, logical schemas, append/read rules, failure semantics, retention proposals and acceptance cases.

After reviewing the drafted contract, the owner answered grounded questions on 2026-11-21 and selected every recommended option:
**D1** approve taxonomy v1 as written; **D2** critical audit 24 months, failed attempts 30 days raw and 90 days aggregated; **D3** successful login/logout/session creation are not critical in L04, while session revocation is; **D4** tenant-wide readers see all companies, company-restricted readers only their own; **D5** 20 records/minute per source class, 600/minute global and a 2,000-record queue, with aggregation and visible suppression counters; **D6** 4 KiB and 20 metadata entries, 100-row pages and the three named read permissions.

This approves D1–D6 of the contract. L04 remains the existing **audit** platform/security root, not a business module: no Orders, Inventory, invoicing or other domain function is introduced. The proposal remains assistant-authored and requires observed acceptance evidence.

### L04 isolated implementation authorization — 2026-11-21

After that clarification, the owner selected **“Oui, isolée (Recommandé)”** in response to authorization for L04 implementation. This authorizes Java code, one Flyway migration and tests for the existing `audit` root, executed only against a **new L04 test database** inside the dedicated `authority-pro-postgres` instance. Verify the new database name is unused and bind the run to the expected environment; stop on collisions or wrong targets.

The main `authority_pro` database, its V1 history and four empty tables, all L03 journals, the serving modes, other projects, containers/volumes and pgAdmin state remain untouched. No L04 main migration, service restart, permanent HTTP exposure, production deployment, commit or push is authorized. Runtime/migrator credentials follow the existing DPAPI/stdin pattern; no secret enters source, arguments or logs. Partial or failed resources are retained for inspection, never silently deleted.

Capability status remains **NOT IMPLEMENTED** until the isolated migration, transactional append/rollback fault cases, scoped reads, bounded failed-attempt path and negative tests actually pass and are recorded. Approval of the contract and of this bounded environment is not acceptance of G3/G4 or production readiness.

### L04 isolated implementation verified — 2026-11-21

The bounded implementation authorized above was executed. One Flyway V2 migration, the `audit` domain/application/adapter code, pure tests, an isolated executable qualifier and the `tools/l04-audit.mjs` guard were added. The backend suite passed 89 tests, and the qualifier ran only against the new `authority_pro_l04_audit_test` database, reporting `AUDIT_QUALIFICATION_OK checks=36` with evidence under `tmp/l04-audit/`.

The runner recorded the main `authority_pro` database (V1 history, four empty tenancy tables), every other database and the protected container snapshot as unchanged before and after. The main-database V2 migration, L05 wiring, HTTP/UI exposure, retention execution, commit/push and any G3/G4 claim remain unauthorized. The earlier "NOT IMPLEMENTED" paragraph is preserved as the state before this evidence; the isolated test-database implementation is now verified, not the production capability.

### L05 detailed contract drafting — 2026-11-21

After the L04 evidence was recorded, the owner instructed **“enchaine prochaine etape”**. The documented next nucleus gate is L05 identity/access, whose high-level orientations were already approved but whose session, provisioning, policy and permission details were still PROPOSED. This instruction is recorded as authorizing drafting of [Identity and Access §10](../04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md#10-l05-detailed-contract-proposal--2026-11-21) only: logical schema, session policy, local provisioning, HTTP contracts, own-membership label policies, permission catalogue, failure semantics, acceptance matrix and decisions D1–D6.

No identity, session, provisioning, database-role, dependency, migration or HTTP change is authorized by this drafting step. D1–D6 require explicit owner answers, and implementation will then need a separate isolated authorization, as L04 did. The constraint that the socle contains no business module still applies: no Orders, Inventory, invoicing or other domain capability is introduced.

### L05 decisions D1–D6 approved — 2026-11-21

The owner answered the two decision rounds for [Identity and Access §10.10](../04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md#1010-owner-decisions-before-implementation): BCrypt strength 12; 30-minute idle / 8-hour absolute / 3 concurrent sessions with rotation; SELECT-only membership-gated label policies on tenants and companies; non-critical logout with atomic administrative revocation audit; `provision-local` mode with a dedicated `authority_pro_provisioner` role, single-use guard and atomic control-plane audit; five named permissions and three named roles without wildcard or inheritance.

This approves the contract only. Implementation, dependency changes (Spring Session JDBC and the packaged-artifact baseline amendment), the new database-role provisioning step, any migration and any HTTP exposure require a separate isolated authorization and remain **NOT AUTHORIZED** until then.

### L05 isolated implementation authorization — 2026-11-21

The owner then selected **“Oui, isolée (Recommandé)”** in response to authorization for L05 implementation against a new `authority_pro_l05_identity_test` database inside the dedicated `authority-pro-postgres` instance. This authorizes Java code for the existing technical `identity`, `access` and `tenancy` roots (no business module), one Flyway migration, the Spring Session JDBC dependency with the corresponding L02 packaged-artifact baseline amendment, one dedicated non-owner PostgreSQL role `authority_pro_provisioner` (connection limit 1), tests, an isolated executable qualifier and its guard tool.

The main `authority_pro` database (V1 history and four empty tenancy tables), every other database and project, L03 journals, the serving modes, containers/volumes and pgAdmin state remain untouched. No main-database migration, permanent HTTP exposure, production deployment, commit or push is authorized. Runtime, migrator and provisioner credentials follow the existing DPAPI/stdin pattern; no secret enters source, arguments, logs or evidence. Partial or failed resources are retained for inspection, never silently deleted.

### L05 isolated schema verified — 2026-11-21

Within that authorization, `V3__identity_access_foundation.sql` (identity/access technical schemas, role catalogue, membership authorization revision and disable marker, membership-gated label policies) was applied to the new `authority_pro_l05_identity_test` database and verified with `IDENTITY_QUALIFICATION_OK checks=29`; evidence is `tmp/l05-identity/run-81e8bba0-aace-4ac1-9bca-e41268c98af9.json` and the details are recorded in [Identity and Access §10.12](../04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md#1012-isolated-schema-evidence--2026-11-21). The pure domain (login normalization, session policy, permission catalogue/evaluator, membership-selection decision) and the read-only JDBC identity/authorization foundations are implemented and covered by the qualifier; the backend suite holds 109 tests. The main `authority_pro` database, all other databases and the protected containers were fingerprinted before and after and are unchanged.

Two authorized items are not yet started because they require an external change: Spring Session JDBC is absent from the offline Maven repository, and the `authority_pro_provisioner` role requires provisioning a new database role and its DPAPI secret plus matching `tools/local-environment.mjs` assertions. Identity/access Java services, session store, provisioning mode, HTTP contracts and their qualifier checks remain unimplemented pending those two environment decisions; no capability is claimed.

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
| O8 | L02 JDK/build and boundary-tool subset approved and historically qualified; closed L03 candidate dependencies approved and locally qualified. Frontend selection remains gated | Compatibility and maintenance; bounded earlier evidence cannot qualify changed inputs | Fresh backend/dependency checks before any new migration or release; never replay the claimed commands; frontend review before L07 |
| O9 | Sessions/CSRF and local operator CLI approved as orientations; exact cookie/expiry/recovery/MFA policies, guard, role model and grants remain open | Security, authorization | Before corresponding implementation or production readiness |
| O10 | Dedicated environment and closed L03 technical integration are locally qualified, including the bounded main migration/restore; authenticated membership context belongs to L05; a fresh clean-machine qualification remains open; all other projects untouched | Data isolation and coexistence | No scratch-to-application acceptance inference; do not repeat claimed commands; authenticated isolation in L05 and clean-machine gates separately |
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
| O23 | How do Application Studio and the retained composition/development workspaces map to Studio lifecycle areas, analytics and AI; which required target capabilities are supported, installed, enabled, configured, published or entitled in each profile? | Product clarity, governance and independent operation; catalogue coverage is required by the 2026-11-21 integration, not a complete edition contract | Before product contracts; retain analytics/decisions and common AI assistance, no discarded studios; PBC product/technical terminology is reconciled in ADR-0002 without changing execution rights |
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

### L05 implemented and qualified — 2026-11-23

The two former external blockers were resolved inside the same isolated authorization: Spring Session JDBC 4.1.1 was already present in the offline repository, so it is wired explicitly (Spring Boot 4 does not auto-configure it) with the L02 packaged-artifact baseline amended accordingly, and the dedicated `authority_pro_provisioner` role was provisioned with a DPAPI secret and matching environment assertions. `V4__session_store_and_provisioning.sql` adds the session table and provisioning guard; the identity/access application and adapter layers plus the §10.5 HTTP surface are implemented and only exist in `data-local` mode.

Implementation decisions D7–D12 were decided and recorded in [Identity and Access §10.10](../04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md#1010-owner-decisions-before-implementation): SPA-compatible CSRF cookie and header handling, own-profile content without language/theme or permission enumeration, per-source throttling instead of a durable account lock, absolute 8-hour lifetime from session creation, 3-session oldest-eviction, and local cookies without `Secure` pending TLS deployment.

Verification: offline `mvn clean verify` passed 128 unit/HTTP/architecture tests and seven packaged-JAR integration tests with zero failures or skips; `npm run identity:l05` reported `IDENTITY_QUALIFICATION_OK checks=36` against `authority_pro_l05_identity_test` (V1→V4), evidence `tmp/l05-identity/run-8bf7c9af-8b5b-46d2-9137-61febcddf21e.json`; details in [Identity and Access §10.13](../04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md#1013-isolated-implementation-evidence--2026-11-23). Main database, other databases, containers and earlier evidence were fingerprinted unchanged. Commit and push to origin `main` were authorized by the owner and are the remaining delivery step; main-database migration, production deployment and real-browser acceptance stay separate.

### L06 protected inventory decisions approved — 2026-11-24

The owner approved the three implementation points for L06 after reviewing the
PROPOSED contracts in [Module Runtime §4–§8](../03-CORE/MODULE-RUNTIME.md) and
[Identity and Access §10.7](../04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md#107-permission-evaluation-and-administrative-changes):

1. **Manifest subset frozen.** The foundation manifest stays exactly `0.1.0`
   with its eleven existing fields; unknown fields are rejected; only compiled
   first-party resources are discovered; the descriptor digest is SHA-256 over
   the exact compiled bytes; the build revision is injected by the build and is
   not a manifest field; the release record stays a separate reviewed artifact;
   only exact versions are accepted.
2. **Named exports stay empty.** No dependency or capability export is declared
   for the five roots in L06. The inventory permission catalogue adds exactly
   one code, `platform.inventory.read`, with control-plane scope and a
   `TENANT_OWNER`-only grant, alongside the five existing codes and ten existing
   grants; the ownership map is derived from V1–V4 migrations only.
3. **Read-only protected surface.** `GET /api/v1/platform/inventory` requires an
   authenticated, authorized session, runs only in `data-local` mode and serves
   data-local facts only: module id, version, digest, lifecycle, availability
   reason, dependencies, capabilities, permissions and data ownership. It
   exposes no filesystem paths, secrets, tenant data or stack traces, fails
   closed with 503 when descriptors cannot be trusted, audits administrative
   transitions, does not write critical audit for reads, and records
   authorization refusals in `audit.security_attempts`.

Execution constraints recorded with this approval: no main/production
migration and no replay of `migrate-main`/`recover-main`; L06 schema changes
(V5) are exercised only on a dedicated isolated test database; the main
`authority_pro` database remains at V1; other projects' containers and
databases are untouched; all L04/L05 `run-*.json` evidence is preserved.

Implementation followed those constraints on 2026-11-24. Offline
`mvn clean verify` passed 151 unit/HTTP/architecture tests and seven
packaged-JAR integration tests; `npm run inventory:l06` passed 12 isolated
checks on V1→V5 and `npm run identity:l05` still passed 36 checks against the
retained V4 database. Descriptors live at classpath `authority-pro/modules/`
because Spring Boot 4 does not relocate arbitrary `META-INF` resources into
`BOOT-INF/classes/`. Evidence is recorded in
[L06 protected inventory evidence](../PACK-INVENTORY.md#l06-protected-inventory-evidence--2026-11-24).
Lifecycle, enablement, real-browser acceptance and any main-database migration
were not performed.
