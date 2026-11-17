# Authority Pro

Independent Angular + Spring Boot platform foundation. Business-neutral Thunder Core,
modular monolith, DDD, hexagonal architecture and Plugin-Based Components.

**Current state: Phase 0 documentation and validation tooling. No backend, frontend,
business module, application test or release exists yet.**

## Decisions

Approved: Authority Pro name, this independent monorepo, Angular/Spring Boot,
shared PostgreSQL with `tenant_id` and RLS, documentation plus platform socle only.
Detailed authentication, provisioning, database-role/audit mechanisms and exact
technology versions remain proposals requiring validation and N0 approval.

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

## Current files

- [Documentation index](docs/README.md), [coverage inventory](docs/PACK-INVENTORY.md)
- [Decision log](docs/00-GOVERNANCE/DECISION-MANAGEMENT.md)
- [Foundation boundaries](docs/13-IMPLEMENTATION/MVP-BOUNDARIES.md)
- [Validation gates](docs/13-IMPLEMENTATION/VALIDATION-GATES.md)
- [Local validator](tools/validate-docs.mjs) and [tests](tools/validate-docs.test.mjs)
- [CI definition](.github/workflows/foundation.yml), not yet run remotely
- [Compose draft](compose.yaml): loopback-only development database, no committed password

Compose provisions only a database bootstrap identity. It does not create the
restricted application role or an application. Do not connect a future backend
using that elevated bootstrap credential. No database startup or migration is
performed by the documentation checks.

## Name and source

The [source blueprint](docs/MASTER-BLUEPRINT.md) retains its original AUTHORITY ONLINE
title unchanged. The owner renamed this independent project **Authority Pro** on
2026-11-17. New identifiers use `authority-pro`; the local Git repository is
initialized on `main`, with no remote, commit, push or publication performed.
