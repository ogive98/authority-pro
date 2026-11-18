# Test Strategy

**Decision status:** PROPOSED acceptance policy derived from blueprint security/quality requirements.
**Application test status:** L02 passed 65 unit/HTTP/architecture/contract tests and four packaged-JAR integration tests, including an offline clean repeat. Database, identity, audit, browser and recovery acceptance remains NOT RUN; see [execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18).

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

## Deferred capability acceptance

**PROPOSED / NOT RUN.** Apply these cases only when the relevant L2/L3 slice is explicitly selected. They refine [workflow and automation](../03-CORE/MODULE-RUNTIME.md#9-deferred-workflow-and-automation-contracts), [agent authority](../00-GOVERNANCE/SECURITY-GOVERNANCE.md#10-deferred-agent-authority-contracts) and [schema-driven interfaces](../02-ARCHITECTURE/FRONTEND-ARCHITECTURE.md#deferred-schema-driven-interfaces); they neither enlarge L1 nor prove an implementation exists.

| Area | Required positive and negative evidence |
|---|---|
| Definition versions | Valid reviewed draft publishes; unsupported nodes, unbounded cycles and stale approval fail. New starts use the selected version while existing instances stay pinned; direct pointer replacement cannot migrate an instance |
| Human task ownership | An authorized candidate claims/completes; two concurrent claimants cannot both win. Reassignment, membership revocation, stale forms and completion-versus-expiration races reject the losing transition without duplicate effects |
| Timers and subscriptions | Restart preserves required wake-ups; duplicate, late, out-of-order and stale-lease callbacks cannot reopen a terminal task. Timezone/calendar and missed-deadline cases follow the approved policy |
| Tenant and provider boundaries | Two tenants with multiple companies prove instance/task/variable/timer references and callbacks cannot cross scope. Control-plane operators gain no implicit access; workflow actions cannot read another module's tables |
| Atomic local transitions | Inject audit/publication failure during a protected task transition and prove rollback. Equal command retries return the authorized committed outcome; a changed fingerprint conflicts; expired/revoked access denies replay |
| External execution and compensation | Crash before/after dispatch and lose acknowledgements; reconcile under stable identities without duplicate starts/effects. Stale projections cannot authorize writes; unavailable engine/provider is pending or blocked, never false success. Compensation failure remains visible; cancellation cannot claim to undo an irreversible effect |
| Module/definition evolution | A capability/form update cannot strand pinned instances unnoticed. Drain or approved migration preserves history, pending work and tenant scope; rejected upgrades/removals do not delete data |
| Agent delegation and approval | A valid scoped approved action succeeds; expired/revoked or widened delegation, wrong tenant/actor/tool version, changed arguments/resource revision and reuse beyond the approved use limit fail. A child agent cannot exceed its parent's current authority |
| Agent injection and privacy | Malicious instructions in retrieved documents/tool output cannot grant tools or trigger execution. Cross-tenant/revoked memory is not retrieved; provider export restrictions, secret redaction and provenance controls are enforced |
| Agent execution limits | Exhaust time/cost/step/concurrency budgets and trigger cancellation; new work stops without fictitious rollback of external effects. Simulations cannot call effectful production tools; a failed approval/audit write never yields a successful protected action |
| Interface schemas | Approved bounded schemas render with compiled Angular controls; executable templates/scripts, remote references, undeclared fields and unauthorized data bindings fail. Crafted requests cannot update read-only/hidden fields; incompatible schema/contract versions fail before mutation |
| Dynamic-interface browser paths | Verify task forms, draft/diff/approval, revision conflicts and unknown outcomes; slow old-tenant responses are discarded. Exercise keyboard/screen-reader paths, FR/EN/IT/AR with RTL, both themes and loading/empty/denied/error states |

Use inert technical definitions, synthetic scoped identities and test-only providers, not accounting, HR, sales or other business demonstrations. Future persistence/engine tests must positively verify dedicated project test destinations; never use the other application's PostgreSQL database or modify its roles/services. Record actual engine/tool versions, commands, fault windows and outcomes. Documentation validation cannot close any row of this matrix.

## CI and release boundaries

The CI definition now includes document/schema checks and real L02 backend qualification. Add database integration and frontend/browser jobs only when those implementations exist. Never configure a passing placeholder as proof of a missing feature. The owner's L02 publication instruction covers the existing repository's push-triggered CI; its outcome must be observed separately. Secrets, releases and production deployment still require separate authorization.

Any tenant leakage, credential disclosure, invalid permissions, missing critical audit or migration data loss blocks release. Restore and browser evidence cannot be replaced by unit tests. Runtime tests must use disposable isolated data, never an unrelated project database.

## Related

- [Validation gates](../13-IMPLEMENTATION/VALIDATION-GATES.md)
- [Multi-tenancy](../02-ARCHITECTURE/MULTI-TENANCY-ARCHITECTURE.md)
- [UI constitution](../05-UI-PLATFORM/UI-CONSTITUTION.md)
