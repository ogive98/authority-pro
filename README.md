# Authority Pro

Independent Angular + Spring Boot platform foundation. Business-neutral Thunder Core,
modular monolith, DDD, hexagonal architecture and Plugin-Based Components.

**Current state: L02 backend skeleton implemented and locally tested, plus documentation
and validation tooling. No database integration, identity, audit service, frontend,
business module, release or deployment exists yet.**

## Decisions

Approved: Authority Pro name, this independent monorepo, Angular/Spring Boot,
shared PostgreSQL with `tenant_id` and RLS, documentation plus platform socle only.
The owner subsequently approved cookie/JDBC sessions with CSRF, local operator
provisioning, restricted database runtime with verified transaction-local context,
critical-change/audit atomicity, compiled first-party contracts and `/api/v1` with
controlled tenant selection and standardized errors. The bounded L02 toolchain and
closed HTTP boundary are approved and tested; detailed database/identity policies,
SQL/grants, integration and frontend versions remain separately gated.

PostgreSQL already serves another application. Authority Pro must use a NEW dedicated
database on that server, with separate project roles; never modify or overwrite the
other application's database, credentials or service configuration. Endpoint/version
and name collisions must be verified before provisioning. No restart or database
operation has occurred as part of recording this decision.

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
decisions stay open. PostgreSQL is untouched and frontend findings require review before L07.

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

The next lot is L03 database design/provisioning on a **new dedicated database**,
only after endpoint/version, unused names, roles and safe operator authority are
established. Completing L02 does not authorize connecting to the existing server.

## Current files

- [Documentation index](docs/README.md), [coverage inventory](docs/PACK-INVENTORY.md)
- [Decision log](docs/00-GOVERNANCE/DECISION-MANAGEMENT.md)
- [Foundation boundaries](docs/13-IMPLEMENTATION/MVP-BOUNDARIES.md)
- [Validation gates](docs/13-IMPLEMENTATION/VALIDATION-GATES.md)
- [Local validator](tools/validate-docs.mjs) and [tests](tools/validate-docs.test.mjs)
- [CI definition](.github/workflows/foundation.yml); remote results require separate verification
- [Unused Compose alternative](compose.yaml): draft separate server, not the selected existing-server topology

Do not start Compose for the selected topology. Its image/port/database defaults
are not the existing server's verified settings. Follow the [safe provisioning boundary](docs/02-ARCHITECTURE/DEPLOYMENT-ARCHITECTURE.md#safe-provisioning-boundary)
for the new dedicated database; no backend may use the operator's elevated
credential. Documentation checks do not start a database or run migrations.

## Name and source

The [source blueprint](docs/MASTER-BLUEPRINT.md) retains its original AUTHORITY ONLINE
title unchanged. The owner renamed this independent project **Authority Pro** on
2026-11-17. New identifiers use `authority-pro`. The initial documentation commit
`f5d2914` was pushed to the private repository `ogive98/authority-pro` on `main`.
The owner's subsequent L02 instruction authorizes committing and pushing the verified
skeleton together with pending documentation and the L01 checkpoint. This lifts the
intermediate review hold, not the restrictions on future lots, releases, deployments
or database operations; no application release or deployment is claimed.
