# ADR-0004 — Monorepo for documentation and foundation

- **Status:** APPROVED — one independent Authority Pro monorepo; concrete build and CI mechanisms are PROPOSED.
- **Approval basis:** Explicit user direction for `C:/Users/admin/Documents/authority-pro`, separate from other projects.

- **Record reviewed:** 2026-11-17; this is not an approval date for proposed details.
- **Accountable owner / approval authority:** Project owner.
- **Delivery:** Local Git initialized on main; no commit or remote. Application NOT IMPLEMENTED.

## Context

Authority Pro is a greenfield documentation and technical-foundation effort.
There is no application code. Documentation, backend contracts and frontend
consumers should evolve together. Business modules are outside the authorized
scope. This decision does not authorize changes to another project or location.

## Options considered

1. One monorepo for documentation, backend and frontend — selected.
2. Separate repositories for each — independent access and release management,
   but coordination is needed for contract changes.
3. A code monorepo with separate documentation — easier documentation-only
   access, but greater drift risk.

## Decision and approval boundary

Keep the independent Authority Pro project in one monorepo rooted at
`C:/Users/admin/Documents/authority-pro`. Documentation and future backend and
frontend foundation belong together.

`docs/`, `backend/` and `frontend/` are the proposed top-level organization.
Build aggregation, CI provider, path filters, release automation and branch
policies are not approved by this ADR. A monorepo does not require lockstep
frontend/backend releases or allow imports across component boundaries.

This documentation revision neither creates code nor initializes Git, changes
configuration, permissions or other files. Repository tooling is future work,
not an existing capability asserted by this record.

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

Planned, not executed: verify repository layout and documentation links; ensure
shared-contract changes trigger both frontend and backend checks; confirm
component-boundary checks and secret scanning cover the relevant paths. Test
reproducible builds only after versions and build tools have been verified.

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
