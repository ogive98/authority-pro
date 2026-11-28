# Baseline File Inventory

**Decision status:** N/A — read-only audit output.
**Delivery status:** PHASE 0 OUTPUT (Foundation 9.5), observation only. No application file, database, container, migration, branch or workflow was changed.
**Document validation status:** DRAFT — awaiting Gate G0 owner approval.
**Revision observed:** `27e9b14663363d45024e68897fd31ad784df215f` (main), 2026-11-28.
**Pack baseline:** `131823a`. Delta: one owner-authorized commit, `27e9b14` ("feat: gate a disposable evidence database for local data modes"), additive and tested.

## 1. Purpose and method

This document records what exists in the repository at the observed revision, so that
later phases cannot claim assets, tests or services that were never inspected. Every
statement below was obtained by reading files, Git metadata and already-approved local
test commands. No database was opened, no container was queried and no migration was
run.

## 2. Git state

| Item | Observed value | Evidence |
|---|---|---|
| Branch | `main`, tracking `origin/main` | `git status`, `git branch -vv` |
| HEAD | `27e9b14` | `git rev-parse HEAD` |
| Working tree | clean | `git status --short` (empty) |
| Untracked files | 0 | `git ls-files --others --exclude-standard` |
| Tracked files | 345 | `git ls-files` |
| Commit before HEAD | `131823a` (pack baseline) | `git log` |
| Remote | `ogive98/authority-pro` | Git remotes |

Ignored working material (not part of the inventory, not committed): `tmp/` (portable
JDK, DPAPI credential file, qualification evidence), `backend/target/`,
`frontend/node_modules/`, `node_modules/`. These are rebuildable or secret local state;
none is evidence by itself.

## 3. File distribution

| Area | Tracked files | Notes |
|---|---:|---|
| `backend/` | 196 | 105 main Java files, 69 test-tree files (29 `*Test.java`, 1 `*IT.java`, remainder fixtures/helpers) |
| `frontend/` | 77 | 22 feature/core TypeScript areas, 16 spec files |
| `docs/` | 53 | 50 Markdown files plus 3 JSON schema/fixture files; blueprint excluded from link indexing |
| `tools/` | 11 | Node qualification and documentation tooling |
| `.github/workflows/` | 1 | `foundation.yml`, two jobs |
| root | 8 | `package.json`, `package-lock.json`, `compose.yaml`, `README.md`, `.env.example`, ignore/config files |

## 4. Backend map

Main packages under `backend/src/main/java/com/authoritypro/platform/`:

| Root | Responsibility (observed code) | Persistence / migration |
|---|---|---|
| `tenancy` | Tenant, company, site, membership model | V1 `tenancy_foundation.sql` |
| `audit` | Tenant records and control-plane audit, security attempts | V2 `audit_foundation.sql` |
| `identity` | Users, credentials, sessions, provisioning, authentication HTTP surface | V3 `identity_access_foundation.sql`, V4 `session_store_and_provisioning.sql` |
| `access` | Roles, scoped grants, authorization decisions, bootstrap read model | V5 `platform_inventory_permission.sql` |
| `inventory` | Protected first-party component inventory | Module descriptors in `resources` |
| `runtime` | Application shell, configuration guard, datasource bootstrap, HTTP problem advice | `authority-skeleton.properties` |
| `openapi` | Test-only contract probe, absent from the production JAR | none |
| `persistence` | Shared persistence support | none |
| `launch` | Packaged launch boundary and startup refusal rules | none |

Architecture evidence: `backend/src/test/java/com/authoritypro/architecture/PlatformArchitectureTest.java`
asserts exactly five closed roots (`access`, `audit`, `identity`, `inventory`,
`tenancy`), zero allowed inter-module dependencies and inward-pointing layers. The
`runtime`, `openapi`, `persistence` and `launch` trees sit outside the Modulith roots
and are covered by bytecode boundary rules.

Resources: migrations V1–V5, `authority-skeleton.properties`, module descriptor JSON
files and `META-INF/services` registrations. Toolchain: Maven wrapper 3.9.16 pinned by
SHA-256, portable Temurin 21.0.13+9, Spring Boot 4.1.2, spring-modulith, ArchUnit,
Flyway core plus PostgreSQL support, spring-session-jdbc, springdoc (test scope only).

## 5. Frontend map

Angular 22.3.0, zoneless signals, standalone components, Vitest 5.1.2 with jsdom,
`@ngx-translate/core` 18.1.0 with FR/EN/IT/AR catalogues, TypeScript ~6.0.2.
Features: `shell`, `login`, `tenants`, `overview`, `inventory`, `audit`,
`preferences`, `not-found`. Core services: session, scope, authorization guards,
inventory, audit, i18n, preferences. Build budgets: initial 500 kB warning / 1 MB
error; component styles 4 kB / 8 kB. No lint script and no coverage threshold are
configured; the production build and unit tests are the only frontend gates.

## 6. Documentation and tooling

Governance, product, architecture, core, platform engines, UI, quality, implementation
and templates are present. `docs/MASTER-BLUEPRINT.md` is preserved as the immutable
founding document. Tooling: `tools/validate-docs.mjs` (links, index coverage, Mermaid,
manifest fixtures), `tools/check-backend.mjs` (clean Maven verify with portable JDK),
`tools/local-environment.mjs` (dedicated PostgreSQL lifecycle guard),
`tools/local-data.mjs`, `tools/l04-audit.mjs`, `tools/l05-identity.mjs`,
`tools/l06-inventory.mjs`, plus three Node test suites.

CI (`foundation.yml`) has a documentation job (Node 24, `npm ci`, `npm test`,
`npm run check:docs`) and a backend job (JDK 21.0.13+9 pinned by commit SHA,
`npm run check:backend`). No frontend, security, SBOM, license or migration job
exists. `actions/checkout@v4` and `actions/setup-node@v4` are not pinned to immutable
SHAs.

## 7. Local services and data that must not be touched

| Resource | Rule |
|---|---|
| `authority-pro-postgres` (127.0.0.1:5434) | Dedicated instance; lifecycle only through guarded tooling, never deleted or recreated to clear an error |
| Main database `authority_pro` | At V1; no migration, reset or recovery command may be replayed |
| Disposable database `authority_pro_l07_ui_test` | Whitelisted in code but not created; creation requires a separate authorized package |
| `tmp/l03/credentials.dpapi` | Secret material; never printed, copied or committed |
| Other projects' containers (e.g. ports 5433, 6379, 9000) | Out of scope; never restarted, adopted or inspected for this platform |

## 8. Commands executed for this inventory

Read-only inspection (`git`, file reads) plus already-approved local checks:

- `npm run check:docs` — 0 errors, 53 documents, 798 links, 9 diagrams;
- `npm test` — 67/67 tool tests;
- `frontend`: `npm test` — 100/100 tests across 16 files;
- backend clean verify at this revision — BUILD SUCCESS, 180 unit tests and 7 packaged
  integration tests (executed earlier in the same session, same commit).

No test wrote to a database. No container or migration was touched.

## Related documents

- [Implementation evidence matrix](IMPLEMENTATION-EVIDENCE-MATRIX.md)
- [Documentation drift report](DOCUMENTATION-DRIFT-REPORT.md)
- [Dependency and boundary map](DEPENDENCY-AND-BOUNDARY-MAP.md)
- [Open risks and unknowns](OPEN-RISKS-AND-UNKNOWNs.md)
- [Current state](CURRENT-STATE.md)
