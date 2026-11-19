# Deployment Architecture

**Decision status:** APPROVED bounded L02 local/no-database design and subsequently a [dedicated local PostgreSQL environment](../00-GOVERNANCE/DECISION-MANAGEMENT.md#l03-local-environment-authorization--2026-11-18), replacing the earlier same-server local topology. Other projects remain outside the mutation boundary.
**Implementation status:** L02 skeleton and the separate empty local PostgreSQL environment are IMPLEMENTED / TESTED within their [respective evidence boundaries](../PACK-INVENTORY.md#l03-local-environment-evidence--2026-11-18). Application database integration, migrations, RLS and hosted deployment remain unimplemented.

## Modes and current scope

Lite (local), Standard (hosted) and Hybrid are blueprint targets. LITE/STANDARD/POWER/ADAPTIVE are separate resource profiles. The foundation first validates a local development topology; no hosted environment or production release is created automatically.

## Approved bounded L02 mode (implemented and tested locally)

Explicit `skeleton-local` mode binds only `127.0.0.1`, using port `8080` only if free; otherwise stop the attempted startup, never another service. Typed, validated configuration must reject invalid values and omit secrets from diagnostics. The only allowed HTTP requests are the two status-only GET probes in [API architecture](API-ARCHITECTURE.md#bounded-l02-http-design): live process `UP`, readiness `503 OUT_OF_SERVICE` while the nucleus is incomplete; every other request is denied, with CSRF retained and no sessions provisioned.

No datasource, JDBC, JPA, Flyway, database/driver (including embedded DB), session store, credentials, account/login, broker, containers or external calls belong to the L02 application. Project-portable Temurin `21.0.13+9`, Boot `4.1.2` BOM and Maven `3.9.16` / Wrapper `3.3.4` are the installed and tested bounded L02 baseline; Microsoft `21.0.12.1` was only the historical L01 probe runtime. [ADR-0010](../adr/ADR-0010-java-spring-versions.md) owns the detailed matrix. This documentation update performs no startup, download or deployment.

DB/Flyway/RLS belong to proposed L03, audit to L04, identity to L05, protected inventory to L06 and Angular to L07, each subject to its own gates. Next is qualified L03 facts/design and approval, not automatic database operations. The following database-bearing topology and operational sections are later integration targets, not L02 actions.

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

Only first initialization of the new Authority Pro cluster is authorized by the dedicated-instance approval. No `DROP DATABASE`, recreation, existing data-directory/volume reuse, other-server upgrade/restart, session termination or shared configuration change is authorized. Name or Compose-label collisions stop execution; partial setup is retained, never deleted or silently adopted. If compatibility or isolation requires altering another project, stop rather than changing it.

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

## Related

- [Approved orientation and database constraint](../00-GOVERNANCE/DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations)
- [Database architecture](DATABASE-ARCHITECTURE.md)
- [Non-functional requirements](../01-PRODUCT/NON-FUNCTIONAL-REQUIREMENTS.md)
- [Validation gates](../13-IMPLEMENTATION/VALIDATION-GATES.md)
