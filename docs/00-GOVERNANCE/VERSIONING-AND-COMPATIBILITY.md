# Versioning and Compatibility

**Decision status:** PROPOSED policy and version matrix. Public contract
versioning, compatibility governance and safe migrations remain source requirements.
**Delivery status:** PLANNED / NOT IMPLEMENTED; no tested compatibility matrix
or released platform exists. The rules below describe proposed future behavior.

## 1. Platform versioning

The proposed policy uses Semantic Versioning: `MAJOR.MINOR.PATCH`.
This specifies future release behavior, not an existing version history.

- **MAJOR** — incompatible platform change, removal of a deprecated contract.
- **MINOR** — backward-compatible capability.
- **PATCH** — fixes without contract change.

Every future release would publish: platform version, validated
Java/Angular/PostgreSQL versions, API version and module manifest schema version.
No support or release status advances without evidence links.

## 2. API versioning

- PROPOSED URI strategy (ADR-0011): public HTTP APIs would use `/api/v1`,
  `/api/v2`, and so on; no endpoint is implemented.
- Within a major version, changes must be additive: new fields, new endpoints,
  new optional parameters.
- Removing or renaming a field requires a new major version.
- Deprecation uses the `Deprecation` and `Sunset` response headers and a
  changelog entry. A deprecated contract remains available for at least two
  minor releases.
- Error shapes remain stable within a major version.

## 3. Event versioning

Events use an envelope with `eventId`, `eventType`, `schemaVersion`,
`occurredAt`, `tenantId`, `correlationId` and `payload`.

- Schema changes are backward compatible: new optional fields only.
- Breaking changes create a new event type or a new major `schemaVersion`.
- Consumers must ignore unknown fields.

## 4. Module versioning

- Each module has its own semantic version.
- A manifest declares `platformVersion` (compatibility range) and
  `sdkVersion` (minimum).
- Proposed installation validation would refuse an unsatisfied range; no
  installer or compatibility test is implemented.
- Module upgrades follow expand-and-contract for their own data.

## 5. Database compatibility

- Flyway migrations are immutable once released. Corrections use new versions.
- The naming convention is `V<number>__<description>.sql`; repeatable scripts
  use `R__<description>.sql`.
- Destructive operations follow expand-and-contract:
  1. Expand: add the new structure, keep both paths working.
  2. Migrate: backfill and verify.
  3. Contract: remove the old structure after the compatibility window.
- Every migration that changes data has a rollback or restore procedure.
- Future migrations must be tested against a clean database and, once one
  exists, the previous release schema. No migration tests have run yet.

## 6. Frontend compatibility

PROPOSED: serve Angular with its compatible backend from the same origin and
validate API compatibility between the two. Any non-secret persisted preference
schema would need versioning/migration. Do not store session credentials in
localStorage, sessionStorage or IndexedDB: the proposed authentication uses
Spring Security + Spring Session JDBC and HttpOnly cookies with CSRF protection.

## 7. Candidate compatibility matrix (PROPOSED, unverified)

| Component | Candidate only; not supported/tested status |
|---|---|
| Java | 21; validate against the selected Boot/toolchain versions |
| Spring Boot | 4.1.x candidate; verify availability, support and dependency compatibility before selection |
| PostgreSQL | 16 or 17 candidates; select and test an exact version |
| Angular | Exact supported release to select; do not use a floating "current LTS" build baseline |
| Node.js | 24 candidate for builds only; verify against the selected Angular release |

No numeric version is owner-approved here. The approved Angular + Spring Boot
stack does not approve this matrix. Before declaring support, pin exact versions,
record authoritative compatibility references and link reproducible backend,
frontend, database/migration and integration test results. A runnable build is
not yet present.

## 8. Deprecation policy

Deprecation requires: an ADR or design note, a changelog entry, a migration
guide, telemetry where available, and a removal date no earlier than two minor
releases after deprecation.

## Related documents

- [Platform Constitution](PLATFORM-CONSTITUTION.md)
- [API Architecture](../02-ARCHITECTURE/API-ARCHITECTURE.md)
- [Database Architecture](../02-ARCHITECTURE/DATABASE-ARCHITECTURE.md)
