# Configuration Engine

**Document status: PROPOSED**  
**Capability status: NOT IMPLEMENTED — no typed configuration registry or settings store exists yet.**  
**Governance: N0 for security/startup settings, N1 for ordinary presentation defaults.**  
**Proposed owners: runtime maintainer for deployment configuration, tenancy maintainer for scoped settings; approval authority: project owner.**

## 1. Purpose, source, scope and non-scope

Provide typed, owned and explainable configuration without an unrestricted global key/value service. Locked principles derive from [MASTER-BLUEPRINT](../MASTER-BLUEPRINT.md), sections 5–8, 22–26 and 30. All detailed mechanisms below are proposals awaiting owner approval under the [Core status convention](THUNDER-CORE.md).

Scope: validated Spring Boot deployment settings, secret references, explicitly declared tenant/company presentation settings, deterministic precedence, revisioned writes and critical audit. The approved stack remains Angular/Spring Boot in the monorepo; no configuration code is delivered here.

Non-scope: arbitrary EAV data models, business settings, executable expressions, remote configuration infrastructure, secrets returned to the browser, per-user preferences, runtime package loading, license calculation, a generic feature-flag service or configuration that disables authentication, RLS or audit.

### Settings, interface schemas and generated models

The existing typed-settings scope changes only declared, bounded values; it does
not define business models or provide a universal EAV store. New setting keys
cannot grant permissions.
Future schema-driven forms must bind existing named contracts and precompiled
widgets under the [frontend schema boundary](../02-ARCHITECTURE/FRONTEND-ARCHITECTURE.md#deferred-schema-driven-interfaces).
Generating runtime entities, tables, APIs or code is a separate product and N0
decision requiring owner-managed migrations, compatibility, isolation, data
recovery and review. Configuration or UI support does not authorize generation.
AI output changes none of these trust boundaries. These distinctions are proposed
requirements, not claims of implemented settings, schemas or generated models.

## 2. Ownership and dependencies

`runtime` owns technical environment/bootstrap configuration and safe effective-status reporting. `tenancy` owns persisted tenant/company settings and their authorization scope. Each module owns its typed configuration definitions; it must not read another owner's tables or undocumented environment keys. `access` supplies explicit permissions, and `audit` appends critical change evidence in the same transaction.

A `config` god-module is not required. Small pure validation helpers can remain internal utilities. Shared configuration types must not become a dependency shortcut around named module APIs. UI clients use [Thunder Control](THUNDER-CONTROL.md) contracts and receive only approved non-secret settings.

## 3. Typed definition contract

Every supported key requires a build-reviewed definition:

| Attribute | Required meaning |
|---|---|
| Key and owner | Stable namespaced key and exactly one owning module |
| Type and validation | Boolean/integer/string/enum or explicit small schema; range, length and allowed values |
| Allowed scopes | Deployment, tenant, company; no implicit scope fallback |
| Default | Safe documented value or explicit required/no-default status |
| Override policy | Which lower scope may override; security keys are not tenant-overridable |
| Sensitivity | Public-safe, internal, sensitive or secret-reference; controls every read/log path |
| Mutability | Restart-required or approved transactional hot-change |
| Authorization | Explicit read/write permissions and resource scope |
| Revision/compatibility | Definition version, migration/deprecation and effective-value revision semantics |

Unknown keys, wrong types, duplicate definitions and invalid scopes must be rejected, not stored for speculative future use. Values cannot name classes, execute scripts, inject SQL, change database roles or override tenant context. A definition is not a user grant.

Proposed examples, not implementation claims:

| Key | Owner / scope | Contract |
|---|---|---|
| Session idle timeout | `identity` / deployment only | Positive bounded duration, reviewed security minimum/maximum, restart policy; no tenant override |
| Runtime diagnostic verbosity | `runtime` / deployment only | Allowlisted levels; secrets/payload logging forbidden at every level |
| `platform.ui.defaultLocale` | `tenancy` / tenant, optional company override | Enum `en`, `fr`, `it`, `ar`; default `en`; selects a preference, not proof that translations already exist |

The source requires those four eventual UI languages; a stored locale value does not claim localized frontend capability. More settings require their own definitions rather than arbitrary key creation by an administrator.

## 4. Resolution and write contracts

### Two distinct configuration channels

**Deployment settings** are resolved from an explicitly documented, controlled Spring Boot property-source set. Proposed order is safe packaged non-secret defaults, then approved environment/deployment overrides. Secret values come only from an approved secret source through references. Arbitrary runtime command-line or remote property overrides must not silently alter security behavior; the final property-source policy must be verified against the chosen Spring Boot version. Production runtime credentials must not include migration-role secrets.

**Scoped settings** resolve from the owning definition's safe default, then the authenticated tenant override, then an allowed company override belonging to that tenant. Site/user overrides are not supported in the initial subset. No tenant may read another tenant's default/override record, and no scoped key can shadow a security/deployment-only key.

For each scoped read, return the effective value, source scope, definition version and revision information used to compute it. A request without a company resolves only default/tenant values; it must not select an arbitrary company. A missing optional override uses the permitted parent/default. A malformed stored override is a configuration error, **not** permission to silently ignore it and report healthy defaults.

### Proposed application APIs

| API | Inputs | Output / invariants |
|---|---|---|
| Describe allowed settings | Verified scope and read permission | Definitions safe for this caller; secret values and internal deployment details omitted |
| Resolve effective setting | Key and verified tenant/company scope | Typed value, provenance and revision, or safe error |
| Update tenant/company settings | Allowed changes/removals, expected scope revision, verified write permission | New revision after validation and atomic audit |
| Report deployment configuration status | Control-plane permission | Validation state and restart-required indicators; no secret material |

Writes use optimistic concurrency on a scope-level revision. Validate the whole change set before commit; partial application is forbidden. Removing an override is explicit and must explain the resulting inherited value; `null` is not a generic delete/reset command. Tenant/company ancestry and permissions are rechecked inside the guarded write boundary. Critical audit records changed key names and revision transition, not values.

The first socle should avoid a distributed settings cache. Reads resolve from authoritative storage so no unavailable event bus is required for correctness. If caching is later approved, cache keys include tenant, company, definition version and relevant revision; invalidation must preserve revocation and scope boundaries. A global cache indexed only by setting name is forbidden.

## 5. Change lifecycle and compatibility

1. The owning maintainer proposes a typed definition and its scope/security classification.
2. Review checks defaults, override rules, validation, read/write permissions, retention and migration needs. N0 keys require explicit owner approval.
3. Deployment validates definitions and environment values before readiness. Secrets are resolved without disclosure.
4. A permitted hot-change validates expected revision, writes within the tenant transaction and appends critical audit atomically. A restart-required change is staged by the deployment procedure, not falsely reported as active.
5. Reads expose effective provenance and revision so administrators can explain inherited values.
6. Definition changes use compatible rollout/migration planning. Removing/renaming a key does not silently discard stored values or reset security policy. Rollback checks old-binary compatibility first.

Feature flags and company module enablement remain separate policies. Configuration does not install a module, grant a permission, establish an entitlement or bypass a mandatory security check. Deferred entitlement engines must fail closed at their future boundary; this engine must not return a permissive fabricated license state.

## 6. Security and failure behavior

Tenant/company settings use the [Identity and Access](../04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md) transaction-local context and runtime DB role, with forced RLS and tenant-qualified constraints. Control-plane configuration readers have no implied tenant access. Application permissions complement RLS, especially for company restrictions.

Secrets are referenced, never checked into documentation/configuration/source, returned by APIs or copied to audit/logs. Masking in the browser is insufficient: the server must omit values. Secret rotation and secret-provider failure require an explicit operational policy; security-critical missing values block startup or dependent operations, not an insecure default.

| Failure | Required behavior |
|---|---|
| Missing required deployment value or unsafe runtime DB role | Fail startup/readiness with key category and safe reason |
| Unknown key/type/scope | Reject request without persistence |
| Secret unavailable | Block dependent capability; no fallback credential or token disclosure |
| Stale expected revision | Conflict; no partial settings or audit write |
| Tenant/company mismatch or missing membership | Deny before data access; no global read fallback |
| Audit failure | Roll back settings change |
| Invalid persisted value | Mark affected capability unavailable and require reviewed repair; do not silently rewrite |
| Restart-required change pending | Report pending versus effective state distinctly |

## 7. Observability and tests

Metrics: validation failures by bounded key category, resolution latency, revision conflicts, write/audit failure counts and restart-required state. Logs contain safe key names, scope class, definition revision and correlation ID, not values, secret references that reveal infrastructure, credentials or request payloads. Tenant-specific diagnostics require authorized access; metric labels must not include tenant/user IDs.

Required future tests:

- Typed definition validation rejects unknown keys, wrong types/ranges and illegal override scopes.
- Resolution distinguishes tenant/default/company precedence and never reads a foreign company; missing tenant fails closed.
- A control-plane-only operator cannot read tenant settings and a tenant administrator cannot change session/RLS/audit deployment policy.
- Concurrent updates conflict on revision; removing an override has the documented inherited result.
- Audit failure and invalid mixed change sets leave no partial writes.
- Startup rejects missing secrets, invalid security values and migration credentials in serving configuration; responses/logs contain no secret values.
- Definition evolution and restart staging report actual effective state; no event cache/invalidation capability is fabricated.

## 8. Acceptance and owner gates

Acceptance requires an approved small definition catalogue, property-source precedence, secret source/rotation policy, scope/permission mapping, complete persistence/RLS design and passing validation/concurrency/isolation/audit tests. A settings JSON shape alone does not establish this capability.

Owner decisions remain: exact security bounds/defaults, which settings are hot-changeable, whether company overrides belong in the first socle, safe read exposure, recovery procedure for invalid stored values and later cache/flag/entitlement boundaries. Proposed defaults must not be promoted to approved production settings without review.

## Related contracts

- [Thunder Core](THUNDER-CORE.md)
- [Thunder Control](THUNDER-CONTROL.md)
- [Module Runtime](MODULE-RUNTIME.md)
- [Command, Query and Event Engine](COMMAND-QUERY-EVENT-ENGINE.md)
- [Identity and Access](../04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md)
- [Audit Engine](../04-PLATFORM-ENGINES/AUDIT-ENGINE.md)
- [ADR Template](../14-TEMPLATES/ADR-TEMPLATE.md)
