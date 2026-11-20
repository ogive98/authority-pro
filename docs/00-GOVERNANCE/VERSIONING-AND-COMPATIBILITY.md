# Versioning and Compatibility

**Decision status:** PROPOSED policy except the bounded L02 pins approved in
[ADR-0010](../adr/ADR-0010-java-spring-versions.md) and PostgreSQL 16.15 for the
[authorized L03 local environment](DECISION-MANAGEMENT.md#l03-local-environment-authorization--2026-11-18).
Public contract versioning, compatibility governance and safe migrations remain source requirements.
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

- Each module has its own semantic version, distinct from capability contract
  and manifest dialect versions.
- The [foundation manifest](../03-CORE/MODULE-RUNTIME.md#4-foundation-manifest-contract)
  remains `0.1.0`, with its existing eleven fields, exact module/capability versions
  and rejection of unknown fields. It has no `platformVersion` or `sdkVersion`
  field, compatibility ranges, node definitions or UAM metadata.
- Platform/SDK range negotiation is a future proposal requiring separate contract
  design and schema-version approval, not an accepted extension to `0.1.0`.
  A reviewed release compatibility record must supply the tested combination;
  absent fields never imply universal compatibility.
- No live installer or automatic compatibility solver is implemented. Compiled
  first-party updates remain controlled builds and restarts.
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
  exists, the previous release schema. [Isolated scratch migration/restore checks](../PACK-INVENTORY.md#l03-isolated-qualification-evidence--2026-11-19)
  provide bounded technical evidence, not a main-application migration baseline
  or released-schema upgrade acceptance.

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
| PostgreSQL | 16.15 approved for the bounded L03 dedicated local instance, replacing the earlier existing-server topology; [provisioning checks passed](../PACK-INVENTORY.md#l03-local-environment-evidence--2026-11-18), while application compatibility remains unverified. No PostgreSQL operation occurred in L01/L02 |
| Angular | Exact supported release to select; do not use a floating "current LTS" build baseline |
| Node.js | 24 candidate for frontend builds only; verify against the selected Angular release |

[ADR-0010](../adr/ADR-0010-java-spring-versions.md) owns the version approval and
compatibility matrix; the L03 authorization above bounds the local PostgreSQL pin.
The runnable L02 build and clean verification do not prove frontend,
database/migration, session-store integration or production support. Those slices
still require remaining version selections and reproducible compatibility evidence;
offline L02 clean verification also passed as recorded in the execution evidence.
The dedicated-instance approval is environment initialization only, not general
Flyway adoption or implemented tenant logic. Subsequent isolated scratch qualification
provides bounded JDBC/RLS, migration and same-cluster restore evidence; it is not a
delivered application capability or production backup/recovery proof. L03 application
JDBC/RLS integration is IN PROGRESS, not delivered, and no main-application migration
baseline is established. Functional identity/audit, Angular and full application
compatibility acceptance are not established by L02 or that scratch evidence; G2 is open.

## 8. Deprecation policy

Deprecation requires: an ADR or design note, a changelog entry, a migration
guide, telemetry where available, and a removal date no earlier than two minor
releases after deprecation.

## 9. Future application revisions and immutable releases

**PROPOSED / NOT IMPLEMENTED.** This consolidation of AP-ARCH-EVOL-001 and
AP-MASTER-CONSTITUTION-001 follows the [documentation-only authorization](DECISION-MANAGEMENT.md#visual-composition-documentation-authorization--2026-11-20),
decisions O20–O25 and [coverage refinement](../PACK-INVENTORY.md#visual-composition-coverage-refinement).
It changes no approved permission, foundation manifest field or implementation gate.

A UAM application revision identifies a coherent semantic composition, independently
of platform release, module version, node/composite version, capability/API contract
version, data schema version and editor-layout version. A layout-only change cannot
silently alter application semantics. Draft revisions support conflict detection;
a release records an immutable snapshot and exact approved bundle digest.

The release compatibility record pins exact provider/module/node/composite dependencies,
data-model/schema references, UI bindings/form schemas, process definitions and required
capability versions. Validation covers the complete combination and resource bindings,
not just each fragment in isolation. Approval binds the exact digest; edits require
new validation and approval. Publication must not expose mixed old/new fragments or
rewrite an already published snapshot. These are future contracts, not new fields
implicitly accepted by the current manifest.

New releases select versions for new work only. Running instances retain their pinned
definitions, capability/form versions and execution revisions. Preserve compatible
providers until approved drain or explicit instance migration; migration needs reviewed
state mapping, impact and recovery evidence. Replacing a version pointer is not migration.
Deprecation must account for still-running instances, not only current editor clients.

Rollback selects a previous compatible application release; it does not undo database
migrations, delete retained history, reverse committed domain writes or cancel external
effects already performed. Database restore/roll-forward and provider-owned compensation
remain separate authorized procedures. Reject rollback when schema, bindings or required
providers are incompatible, rather than report a false restoration.

## 10. Future import, export and clone compatibility

**PROPOSED / NOT IMPLEMENTED.** Application portability concerns versioned definitions
and approved non-secret metadata, not a copy of operational business or execution state.
Exports carry dialect/revision/digest, exact dependency references, provenance and an
explicit inventory of logical resource bindings. Provenance records source identity and
lineage; a signature or trusted source does not itself authorize use in a destination.

Secrets, credentials, tokens and live sessions are excluded. Sensitive infrastructure
references must be omitted or replaced by safe logical binding names. Import/clone never
copies memberships, permission grants, approvals, licenses or historical initiating-user
authority. Required-permission declarations may describe requirements but confer no rights.
Business-data export, if separately approved, remains a different scoped contract.

At the destination, validate provenance/integrity, dialect and exact dependency
compatibility, provider availability, tenant-safe resource references and budgets before
acceptance. Resource binding remaps are explicit, reviewed and reauthorized for the target
tenant/company/site; unresolved or foreign bindings block publication. A clone receives
new identity/revision and retained source lineage as a draft, not the source's activation
or approval. Changed bindings change the bundle digest and require fresh scoped preview,
review and publication approval. Import cannot install missing code, create database
models or provision secrets automatically.

Future evidence follows [visual-composition acceptance](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md#visual-composition-acceptance)
and the [runtime composition contracts](../03-CORE/MODULE-RUNTIME.md#10-future-visual-composition-contracts).
No import/export, clone, compatibility or rollback test is claimed as executed here.

## Related documents

- [Platform Constitution](PLATFORM-CONSTITUTION.md)
- [API Architecture](../02-ARCHITECTURE/API-ARCHITECTURE.md)
- [Database Architecture](../02-ARCHITECTURE/DATABASE-ARCHITECTURE.md)
