# Test Strategy

**Decision status:** PROPOSED acceptance policy derived from blueprint security/quality requirements.
**Application test status:** NOT RUN; no application exists yet.

## Levels and evidence

| Level | Required proof |
|---|---|
| Documentation | Links and anchors resolve, JSON schemas compile, positive/negative fixtures behave as intended, Mermaid parses |
| Unit | Domain invariants, authorization decisions, value validation and concurrency rules |
| Architecture | Expected module names discovered; illegal import and dependency cycle rejected; domain has no persistence dependencies |
| Integration | Real PostgreSQL migrations and actual runtime role; Spring Security/Session behavior; same-transaction audit |
| Contract | OpenAPI examples, error shapes/statuses, event compatibility and manifest semantics |
| Browser | Golden path plus expired session, scope switching/races, denied/empty/error states, locales/RTL, themes, accessibility |
| Operations | Clean upgrade, failed migration, backup restore, restart, bounded resource use |

Each result records command, pinned tool versions, outcome and scope. A specification, self-declared status or mocked RLS test is not runtime evidence. Do not require a global coverage percentage without measuring what it protects; critical branches need explicit positive and negative cases.

## Tenant isolation matrix

Use two tenants, multiple companies/sites, single/multi-membership users and a platform operator with no tenant grant. Assert runtime NOSUPERUSER/NOBYPASSRLS/no ownership first. Test every DML operation, absent/malformed scope, foreign identifier references, own-membership discovery, role revocation, pooled-connection reuse, nested/new transactions, async jobs and rollback. Repeat API denials separately from direct SQL policy tests.

## Identity, provisioning and audit

Verify secure/HttpOnly/SameSite cookie configuration, CSRF on login/logout/mutations, fixation protection, idle/absolute expiry, revocation and unavailable session store. Operator provisioning must be closed by default and atomic under two concurrent attempts. No secret may appear in process arguments or outputs.

Inject an audit insertion failure and confirm protected state did not commit; inject a protected-write failure and confirm no success audit exists. Separately test bounded failed-login recording and suppression metrics under saturation. Framework session/audit atomicity requires a spike rather than assumptions.

## Manifest and documentation checks

The local validator must run from a clean dependency installation and accept the valid manifest while rejecting the intentionally invalid fixture. Negative tests include unknown properties, malformed version, unsupported execution type and duplicate identifiers; graph/dependency/runtime authorization checks are a separate gate, not inferred from JSON Schema success.

Mermaid parsing verifies syntax only, not layout or architectural correctness. Manual diagram review and later rendering remain distinct. The immutable source blueprint is exempt from current-pack link/status checks because its tree describes the entire future vision.

## CI and release boundaries

Start with document/schema checks; add backend build, database integration and frontend/browser jobs only when the code exists. Never configure a passing placeholder job as proof of a missing feature. Remote execution, secrets, releases and production deployment require separate authorization.

Any tenant leakage, credential disclosure, invalid permissions, missing critical audit or migration data loss blocks release. Restore and browser evidence cannot be replaced by unit tests. Runtime tests must use disposable isolated data, never an unrelated project database.

## Related

- [Validation gates](../13-IMPLEMENTATION/VALIDATION-GATES.md)
- [Multi-tenancy](../02-ARCHITECTURE/MULTI-TENANCY-ARCHITECTURE.md)
- [UI constitution](../05-UI-PLATFORM/UI-CONSTITUTION.md)
