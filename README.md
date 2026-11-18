# Authority Pro

Independent Angular + Spring Boot platform foundation. Business-neutral Thunder Core,
modular monolith, DDD, hexagonal architecture and Plugin-Based Components.

**Current state: Phase 0 documentation and validation tooling. No backend, frontend,
business module, application test or release exists yet.**

## Decisions

Approved: Authority Pro name, this independent monorepo, Angular/Spring Boot,
shared PostgreSQL with `tenant_id` and RLS, documentation plus platform socle only.
The owner subsequently approved cookie/JDBC sessions with CSRF, local operator
provisioning, restricted database runtime with verified transaction-local context,
critical-change/audit atomicity, compiled first-party contracts and `/api/v1` with
controlled tenant selection and standardized errors. Detailed policies, SQL/grants,
framework integration and numeric versions still require validation and scoped approval.

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
approve application pins or prove backend/database/browser acceptance. The L01
compatibility investigation is recorded; the full G1 decision gate remains open.
The next implementation decision is the bounded L02 backend skeleton after its
applicable approvals, not an automatic start. PostgreSQL remains untouched and the
frontend toolchain must be reviewed again before L07.

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
The owner separately authorized committing and pushing the verified L01 documentation
checkpoint. That authorization does not extend to future lots, releases, deployments
or database operations; no application release or deployment is claimed.
