# Authority Pro

Independent Angular + Spring Boot platform foundation. Business-neutral Thunder Core,
modular monolith, DDD, hexagonal architecture and Plugin-Based Components.

**Current state: L02, bounded closed L03, the L04 audit engine and the L05
identity/access surface are locally implemented and tested.** Main V1 has four empty
technical tables; L04 and L05 run only against their isolated test databases (V1→V4
for L05, `checks=36`) and the closed data-local HTTP launch passed.
See the [recovery evidence and retained failure](docs/PACK-INVENTORY.md#l03-main-recovery-evidence--2026-11-20)
and [L05 implementation evidence](docs/04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md#1013-isolated-implementation-evidence--2026-11-23).
No frontend, business module, main-database V2–V4 migration, release or hosted
deployment exists.

## Decisions

Approved: Authority Pro name, this independent monorepo, Angular/Spring Boot,
shared PostgreSQL with `tenant_id` and RLS, documentation plus platform socle only.
The owner subsequently approved cookie/JDBC sessions with CSRF, local operator
provisioning, restricted database runtime with verified transaction-local context,
critical-change/audit atomicity, compiled first-party contracts and `/api/v1` with
controlled tenant selection and standardized errors. The bounded L02 toolchain and
closed HTTP boundary are approved and tested; detailed database/identity policies,
SQL/grants, integration and frontend versions remain separately gated.

PostgreSQL already serves another application. After a read-only privilege preflight,
the owner explicitly approved a **separate PostgreSQL instance** for Authority Pro,
replacing the earlier same-server local topology. The dedicated database, accounts,
network and volumes share no application objects with other projects. Their existing
containers, databases, credentials, pgAdmin registrations and settings remain untouched.
The shared-tenancy design still applies within Authority Pro's own database.

The project does not import legacy AUTHORITY code, assets or assumptions.

## Local documentation checks

Requirements: Node.js 24 and npm. Run from this repository root:

```bash
npm ci --ignore-scripts
npm test
npm run check:docs
```

These validate document links, index coverage, Mermaid syntax and proposed JSON
manifest fixtures. They do not test an application, prove runtime security or
render/visually review diagrams.

## L01 compatibility checkpoint — 2026-11-17

The [candidate matrix and probe evidence](docs/adr/ADR-0010-java-spring-versions.md)
records the observed tools, supported release constraints and completed disposable
checks. Java 21 / Spring Boot 4.1.2 passed five narrow tests, including an offline
rerun. Angular 22 strict dependency resolution failed; an Angular 21 LTS compiler
probe passed but development-tool advisories remain open. These findings do not
by themselves approve application pins or prove backend/database/browser acceptance.
The subsequent L02 approval and implementation are recorded below; the remaining G1
decisions stay open. PostgreSQL was untouched during L01/L02; the later environment
preparation below is separate evidence. Frontend findings require review before L07.

## L02 backend skeleton — 2026-11-18

[Execution evidence](docs/PACK-INVENTORY.md#l02-execution-evidence--2026-11-18) records
65 unit/HTTP/architecture/contract tests and four packaged-JAR integration tests,
all passing both a clean build and an offline clean repeat with the same JAR hash.
This is a closed technical skeleton, not a usable tenant application.

Requirements: Node.js 24 and publisher-verified **Temurin 21.0.13+9** (release 21).
Use a portable JDK and a process-local `JAVA_HOME`, not a system-wide Java change.
The wrapper verifies Maven 3.9.16 against its pinned SHA-256. Maven settings,
repository and wrapper cache are project-local; the first run downloads dependencies.
Run from the repository root in PowerShell after obtaining the approved portable JDK:

```powershell
$env:JAVA_HOME = 'C:\Users\admin\Documents\authority-pro\tmp\l02\tooling\jdk-21.0.13+9'
npm run check:backend
npm run check:backend -- --offline
```

Adjust the JDK path for another checkout. The offline repeat requires a populated
wrapper/dependency cache. Port 8080 must be free for packaged tests; do not stop an
existing service to satisfy this requirement. Test servers are stopped by the tests.

For an explicit manual local run, in a clean shell without another project's Spring
configuration or JVM options, set the portable `JAVA_HOME` and run:

```powershell
$env:AUTHORITY_PRO_MODE = 'skeleton-local'
& "$env:JAVA_HOME\bin\java.exe" -Xmx256m -jar backend/target/authority-pro-backend-0.0.1-SNAPSHOT.jar
```

Only exact GET `/actuator/health/liveness` (200/UP) and
`/actuator/health/readiness` (503/OUT_OF_SERVICE) are permitted on `127.0.0.1:8080`.
Readiness deliberately stays unavailable until later capabilities exist. Other
requests are denied, CSRF stays enabled, and no account or HTTP session is created.
There is no login, Swagger UI, datasource or migration. Stop this manual foreground
process normally when finished; do not expose it beyond loopback.

## Dedicated local PostgreSQL environment

The [separate environment authorization](docs/00-GOVERNANCE/DECISION-MANAGEMENT.md#l03-local-environment-authorization--2026-11-18)
and [executed checks](docs/PACK-INVENTORY.md#l03-local-environment-evidence--2026-11-18)
record the original empty environment. The main database now contains V1's empty
technical tables; its [bounded local recovery passed](docs/PACK-INVENTORY.md#l03-main-recovery-evidence--2026-11-20).
Do not rerun either claimed main command or delete the original or recovery journals.

| Item | Dedicated Authority Pro setting |
|---|---|
| Container / server | `authority-pro-postgres`, PostgreSQL 16.15 pinned by image digest |
| Windows endpoint | `127.0.0.1:5434`, database `authority_pro` |
| Runtime identity | `authority_pro_runtime`, no DDL, ownership, elevated role or maintenance database access |
| Migration identity | `authority_pro_migrator`, explicit `SET ROLE authority_pro_owner`; never a runtime credential |
| Storage / network | `authority-pro-pgdata`, `authority-pro-pgsecrets`, `authority-pro-network` |
| Budgets | 256 MiB, 0.5 CPU, 20 server connections; runtime 5, migrator 2 |

From this checkout, with Windows Docker Desktop running and Node.js 24:

```powershell
npm run test:environment
npm run environment:prepare
npm run environment:verify
```

Preparation requires the pinned image already present locally, available port 5434
and unused dedicated names. It rejects name/Compose-label collisions and remote Docker
daemons; it never adopts or deletes another project's resources. Repeating preparation
on the verified environment runs checks without reinitialization or password rotation.
A partial setup stops for inspection; do not delete state files or run `down -v` to
bypass the guard. Do not restart Docker or another project's service to free resources.

Generated passwords are stored only in a Windows CurrentUser DPAPI-encrypted file at
`tmp/l03/credentials.dpapi`; they are not printed or committed. The bootstrap secret
volume is mounted read-only by PostgreSQL, with restricted Linux permissions; Docker
administrators can still access it. The current Windows user can decrypt the DPAPI
file, which contains setup and migration credentials and must never be provided to
an application runtime. Loss of that Windows profile/file requires a separately
reviewed credential recovery, not automatic re-creation. No production secret vault,
rotation or backup/restore guarantee is claimed.

Verification authenticates through the actual Windows endpoint, checks the database
cluster identity and role grants, rejects bad credentials and forbidden operations,
and rolls back its technical DDL probe. It leaves no application tables. The ignored
`tmp/l03/verification.json` contains non-secret results. The pinned `pg` Node client
is local development tooling, not a backend JDBC dependency.

pgAdmin and its existing server registrations remain unchanged. The environment
commands do not start Java/Angular or change L02 readiness; migrations, transaction
context, RLS and recovery qualification are the next separately scoped L03 work.

## Current files

- [Documentation index](docs/README.md), [coverage inventory](docs/PACK-INVENTORY.md)
- [Decision log](docs/00-GOVERNANCE/DECISION-MANAGEMENT.md)
- [Foundation boundaries](docs/13-IMPLEMENTATION/MVP-BOUNDARIES.md)
- [Validation gates](docs/13-IMPLEMENTATION/VALIDATION-GATES.md)
- [Local validator](tools/validate-docs.mjs) and [tests](tools/validate-docs.test.mjs)
- [CI definition](.github/workflows/foundation.yml); remote results require separate verification
- [Dedicated Compose environment](compose.yaml), guarded by [local preparation](tools/local-environment.mjs)
- [Local bootstrap SQL](tools/local-postgres-init.sql) and [environment guard tests](tools/local-environment.test.mjs)

Follow the [safe provisioning boundary](docs/02-ARCHITECTURE/DEPLOYMENT-ARCHITECTURE.md#safe-provisioning-boundary);
no backend may use the operator's elevated credential. `npm test` and documentation
checks do not start a database or run migrations.

## Name and source

The [source blueprint](docs/MASTER-BLUEPRINT.md) retains its original AUTHORITY ONLINE
title unchanged. The owner renamed this independent project **Authority Pro** on
2026-11-17. New identifiers use `authority-pro`. The initial documentation commit
`f5d2914` was pushed to the private repository `ogive98/authority-pro` on `main`.
The owner's subsequent L02 instruction authorizes committing and pushing the verified
skeleton together with pending documentation and the L01 checkpoint. This lifts the
intermediate review hold, not the restrictions on future lots, releases, deployments
or database operations; no application release or deployment is claimed.
