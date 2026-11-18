# Versioning and Compatibility

**Decision status:** PROPOSED policy except the bounded L02 pins approved in
[ADR-0010](../adr/ADR-0010-java-spring-versions.md). Public contract versioning,
compatibility governance and safe migrations remain source requirements.
**Delivery status:** L02 no-database backend skeleton IMPLEMENTED / TESTED;
[execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18)
is not a complete backend/frontend/database compatibility matrix or a release.
The rules below describe proposed future behavior unless explicitly approved.

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

- APPROVED `/api/v1` orientation (ADR-0011); later major-version/deprecation
  details remain PROPOSED. No functional application API is implemented; L02
  exposes only the two local GET health probes, outside `/api/v1`.
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

## 7. Bounded L02 baseline and remaining candidates

| Component | Approval / verification boundary |
|---|---|
| Java | Project-portable Temurin 21.0.13+9; approved and tested for L02 only |
| Spring Boot / build | Boot 4.1.2, Maven 3.9.16 and Wrapper 3.3.4; approved and tested for L02 only |
| PostgreSQL | Exact existing-server version and compatibility remain unverified; no PostgreSQL operation occurred in L02 |
| Angular | Exact supported release to select; do not use a floating "current LTS" build baseline |
| Node.js | 24 candidate for frontend builds only; verify against the selected Angular release |

[ADR-0010](../adr/ADR-0010-java-spring-versions.md) owns the exact approval and
compatibility matrix. The runnable L02 build and clean verification do not prove
frontend, database/migration, session-store integration or production support.
Those slices still require approved pins and reproducible evidence; offline
clean verification also passed as recorded in the execution evidence.

## 8. Deprecation policy

Deprecation requires: an ADR or design note, a changelog entry, a migration
guide, telemetry where available, and a removal date no earlier than two minor
releases after deprecation.

## Related documents

- [Platform Constitution](PLATFORM-CONSTITUTION.md)
- [API Architecture](../02-ARCHITECTURE/API-ARCHITECTURE.md)
- [Database Architecture](../02-ARCHITECTURE/DATABASE-ARCHITECTURE.md)
