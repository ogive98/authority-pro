# ADR-0004 — Monorepo for documentation and foundation

- **Status:** APPROVED — one independent Authority Pro monorepo; the bounded L02 backend build is approved under ADR-0010. Other build aggregation and CI mechanisms remain PROPOSED.
- **Approval basis:** Explicit user direction for `C:/Users/admin/Documents/authority-pro`, separate from other projects.

- **Record reviewed:** 2026-11-17; this is not an approval date for proposed details.
- **Accountable owner / approval authority:** Project owner.
- **Delivery:** Local monorepo with the L02 no-database backend skeleton IMPLEMENTED / TESTED; see [execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18). The private repository already contains the initial published baseline. The latest owner authorization lifts the subsequent review hold for verified L02 publication, but does not authorize automatic L03 or deployment; publication outcomes require separate remote evidence.

## Context

Authority Pro began as a greenfield documentation and technical-foundation
effort with no application code at the initial decision. The L02 backend skeleton
now exists; documentation, backend contracts and future frontend consumers should
evolve together. Business modules are outside the authorized
scope. This decision does not authorize changes to another project or location.

## Options considered

1. One monorepo for documentation, backend and frontend — selected.
2. Separate repositories for each — independent access and release management,
   but coordination is needed for contract changes.
3. A code monorepo with separate documentation — easier documentation-only
   access, but greater drift risk.

## Decision and approval boundary

Keep the independent Authority Pro project in one monorepo rooted at
`C:/Users/admin/Documents/authority-pro`. Documentation, the L02 backend skeleton
and future frontend foundation belong together.

`docs/`, `backend/` and `frontend/` are the proposed top-level organization.
Build aggregation, CI provider, path filters, release automation and branch
policies are not approved by this ADR. A monorepo does not require lockstep
frontend/backend releases or allow imports across component boundaries.

This documentation revision neither creates code nor initializes Git, changes
configuration or permissions. The L02 Maven Wrapper/build and local structural
checks now exist; frontend build aggregation and remote CI execution are not
established by that evidence.

## Consequences

- Contract, consumer and documentation updates can be reviewed as one change.
- Shared changes need cross-project validation; path-only CI filters must not
  skip checks affected by common contracts or build configuration.
- Independent deployment remains possible with compatible APIs.
- Repository growth and access needs may later justify a separately reviewed
  split, rather than premature multi-repository coordination.

## Security considerations

Keep credentials, session data, database exports and operator provisioning
secrets out of source control and build artifacts. Proposed CI should use
least-privilege credentials and distinguish runtime from migration privileges.
A shared repository does not grant its components unrestricted data access.

## Validation and tests

The initial decision called for layout, documentation and reproducible-build
verification. L02 now has a clean backend build and component-boundary evidence
on its approved tools, linked above. Future shared-contract frontend/backend CI
triggers, broader secret scanning and release reproducibility still need their
own evidence; no remote workflow execution is inferred from local checks.

## Migration implications

No existing application repositories or code need consolidation. Do not copy
unrelated projects into this foundation. If a later split is approved, preserve
history and define contract publication, version compatibility and release
coordination first.

## Related

- [ADR-0001 — Stack and scope](ADR-0001-stack.md)
- [ADR-0002 — Component boundaries](ADR-0002-modular-monolith.md)
- [ADR-0007 — Monorepo migration ordering](ADR-0007-flyway.md)
- [ADR-0010 — Reproducible version baseline](ADR-0010-java-spring-versions.md)
- [ADR-0011 — API compatibility](ADR-0011-api-versioning.md)
