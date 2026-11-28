# Authority Pro — Documentation

English documentation; French conversation. This foundation design pack accompanies
the delivered and tested L02–L07 slices, not a complete platform.

Decision approval and delivery status are separate. Only explicitly bounded owner
choices are approved; other technical recommendations remain PROPOSED. See the
[current state](15-FOUNDATION-9.5/CURRENT-STATE.md) and the
[L02 execution evidence](PACK-INVENTORY.md#l02-execution-evidence--2026-11-18)
for the historical clean verification of 65 unit tests and 4 packaged integration
tests. The current revision verifies 180 backend tests and seven packaged integration
tests, plus 100 Angular unit tests. No PostgreSQL was touched by those test runs; no
business feature is implemented. The main database remains at V1, while identity,
audit and protected inventory run against isolated databases. Local document checks
are not runtime tests; the integrated foundation and deployment gates remain open.

## Start here

- [Coverage inventory and technical capability matrix](PACK-INVENTORY.md)
- [Three delivery levels and proposed implementation lots](13-IMPLEMENTATION/IMPLEMENTATION-PHASES.md)
- [Decision management and approval basis](00-GOVERNANCE/DECISION-MANAGEMENT.md)
- [Foundation scope](13-IMPLEMENTATION/MVP-BOUNDARIES.md)
- [Validation gates](13-IMPLEMENTATION/VALIDATION-GATES.md)
- [Risk register](13-IMPLEMENTATION/RISK-REGISTER.md)

## Governance

- [Platform constitution](00-GOVERNANCE/PLATFORM-CONSTITUTION.md)
- [Architecture governance](00-GOVERNANCE/ARCHITECTURE-GOVERNANCE.md)
- [Versioning and compatibility](00-GOVERNANCE/VERSIONING-AND-COMPATIBILITY.md)
- [Security governance](00-GOVERNANCE/SECURITY-GOVERNANCE.md)
- [Documentation standards](00-GOVERNANCE/DOCUMENTATION-STANDARDS.md)

## Product

- [Vision](01-PRODUCT/PRODUCT-VISION.md)
- [Objectives](01-PRODUCT/PRODUCT-OBJECTIVES.md)
- [Socle requirements](01-PRODUCT/REQUIREMENTS-MVP.md)
- [Non-functional targets](01-PRODUCT/NON-FUNCTIONAL-REQUIREMENTS.md)
- [Product roadmap](01-PRODUCT/PRODUCT-ROADMAP.md)

## Architecture

- [Target](02-ARCHITECTURE/TARGET-ARCHITECTURE.md)
- [System context](02-ARCHITECTURE/SYSTEM-CONTEXT.md)
- [Backend](02-ARCHITECTURE/BACKEND-ARCHITECTURE.md)
- [Frontend](02-ARCHITECTURE/FRONTEND-ARCHITECTURE.md)
- [Database](02-ARCHITECTURE/DATABASE-ARCHITECTURE.md)
- [Multi-tenancy](02-ARCHITECTURE/MULTI-TENANCY-ARCHITECTURE.md)
- [API](02-ARCHITECTURE/API-ARCHITECTURE.md)
- [Deployment](02-ARCHITECTURE/DEPLOYMENT-ARCHITECTURE.md)

## Core and platform contracts

- [Thunder Core](03-CORE/THUNDER-CORE.md)
- [Thunder Control](03-CORE/THUNDER-CONTROL.md)
- [Module runtime](03-CORE/MODULE-RUNTIME.md)
- [Command/query/event contracts](03-CORE/COMMAND-QUERY-EVENT-ENGINE.md)
- [Outbox/inbox/idempotency](03-CORE/OUTBOX-INBOX-IDEMPOTENCY.md)
- [Configuration](03-CORE/CONFIGURATION-ENGINE.md)
- [Identity and access](04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md)
- [Audit](04-PLATFORM-ENGINES/AUDIT-ENGINE.md)

## UI and quality

- [UI constitution](05-UI-PLATFORM/UI-CONSTITUTION.md)
- [Design system](05-UI-PLATFORM/DESIGN-SYSTEM.md)
- [Test strategy](11-QUALITY-AND-TESTING/TEST-STRATEGY.md)
- [Implementation phases](13-IMPLEMENTATION/IMPLEMENTATION-PHASES.md)

## Foundation 9.5 programme

- [Current state — authoritative status](15-FOUNDATION-9.5/CURRENT-STATE.md)
- [Baseline file inventory](15-FOUNDATION-9.5/BASELINE-FILE-INVENTORY.md)
- [Implementation evidence matrix](15-FOUNDATION-9.5/IMPLEMENTATION-EVIDENCE-MATRIX.md)
- [Documentation drift report](15-FOUNDATION-9.5/DOCUMENTATION-DRIFT-REPORT.md)
- [Dependency and boundary map](15-FOUNDATION-9.5/DEPENDENCY-AND-BOUNDARY-MAP.md)
- [Open risks and unknowns](15-FOUNDATION-9.5/OPEN-RISKS-AND-UNKNOWNs.md)
- [Adapted Foundation 9.5 plan](15-FOUNDATION-9.5/FOUNDATION-9.5-ADAPTED-PLAN.md)

These documents record the read-only Phase 0 audit at revision `27e9b14`. The drift
report is preserved as written; Phase 1 corrections were applied additively with
dated notes, without rewriting history. Any code, migration, CI or runtime change
still requires separate owner approval.

### Phase 2 — blueprint and contracts (design only)

- [Target blueprint](15-FOUNDATION-9.5/phase-2/00-TARGET-BLUEPRINT.md)
- [Module ownership matrix](15-FOUNDATION-9.5/phase-2/01-MODULE-OWNERSHIP-MATRIX.md)
- [Contract catalogue](15-FOUNDATION-9.5/phase-2/02-CONTRACT-CATALOGUE.md)
- [Database ownership and migration map](15-FOUNDATION-9.5/phase-2/03-DB-OWNERSHIP-AND-MIGRATION-MAP.md)
- [Event catalogue](15-FOUNDATION-9.5/phase-2/04-EVENT-CATALOGUE.md)
- [Security threat model](15-FOUNDATION-9.5/phase-2/05-SECURITY-THREAT-MODEL.md)
- [Frontend / API integration map](15-FOUNDATION-9.5/phase-2/06-FRONTEND-API-MAP.md)
- [Acceptance criteria and packages](15-FOUNDATION-9.5/phase-2/07-ACCEPTANCE-AND-PACKAGES.md)
- [Observed OpenAPI contract](15-FOUNDATION-9.5/phase-2/openapi/authority-pro-v1.yaml)

Nothing in Phase 2 is implemented or approved: it describes the observed contract at
`27e9b14`, proposed target rules and dated ADRs.

## Templates and fixtures

- [ADR template](14-TEMPLATES/ADR-TEMPLATE.md)
- [Draft-2020-12 foundation manifest schema](14-TEMPLATES/MODULE-MANIFEST.schema.json)
- [Valid manifest fixture](14-TEMPLATES/MODULE-MANIFEST-TEMPLATE.json)
- [Intentionally invalid fixture](14-TEMPLATES/MODULE-MANIFEST-INVALID.json)

These fixtures describe a restricted static first-party descriptor, not a complete
marketplace installer. JSON validity does not establish trust or grant permissions.

## Architecture decision records

- [0001 — Approved Angular/Spring Boot; versions proposed](adr/ADR-0001-stack.md)
- [0002 — Approved modular monolith; tooling proposed](adr/ADR-0002-modular-monolith.md)
- [0003 — Approved shared RLS strategy; policies proposed](adr/ADR-0003-multi-tenancy.md)
- [0004 — Approved monorepo](adr/ADR-0004-monorepo.md)
- [0005 — Approved session/provisioning orientations; details proposed](adr/ADR-0005-sessions.md)
- [0006 — Proposed durable-publication selection](adr/ADR-0006-outbox.md)
- [0007 — Proposed Flyway migrations](adr/ADR-0007-flyway.md)
- [0008 — Approved first-party execution; tooling proposed](adr/ADR-0008-plugin-runtime.md)
- [0009 — Proposed scripting boundary](adr/ADR-0009-script-engine.md)
- [0010 — Candidate versions and bounded compatibility evidence](adr/ADR-0010-java-spring-versions.md)
- [0011 — Approved API orientations; detailed contracts proposed](adr/ADR-0011-api-versioning.md)
- [0012 — Proposed atomic session cap](adr/ADR-0012-atomic-session-cap.md)
- [0013 — Proposed production HTTP profile](adr/ADR-0013-production-http-profile.md)
- [0014 — Proposed named exports and module edges](adr/ADR-0014-named-module-exports.md)

## Reference

- [Glossary](GLOSSARY.md)
- [Immutable founding blueprint](MASTER-BLUEPRINT.md)

Run `npm test` and `npm run check:docs` from the repository root. The archive is
preserved, not rewritten to match the renamed product or the staged delivery scope.
