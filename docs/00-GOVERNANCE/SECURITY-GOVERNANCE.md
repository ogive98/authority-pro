# Security Governance

**Decision status:** Source security principles and the
[owner-confirmed foundation orientations](DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations)
are binding. Detailed controls and operating policies remain PROPOSED unless
explicitly approved; this is not blanket approval of this document.
**Delivery status:** L02 no-database backend skeleton IMPLEMENTED / TESTED only;
see [execution evidence](../PACK-INVENTORY.md#l02-execution-evidence--2026-11-18).
Its local HTTP boundary allows only exact GET liveness (200/UP) and readiness
(503/OUT_OF_SERVICE), denies other requests, retains CSRF, creates no session or
default user and returns sanitized RFC 9457 errors. This is not implemented
identity, tenant isolation or audit, nor production-readiness certification.
No PostgreSQL was touched by L02 and no frontend is implemented. Current L03
closed application integration is IN PROGRESS / NOT QUALIFIED; the main
`authority_pro` database is not migrated. Earlier isolated L03 qualification is
not evidence for current application changes. This consolidation runs no backend
test and changes no database, permission or security configuration.

## 1. Principles

1. Deny by default: endpoints must require authentication unless explicitly
   public after security review. No public setup endpoint is proposed.
2. Authorization must be enforced server-side. Frontend checks are user
   experience, not security.
3. Tenant isolation must be enforced in PostgreSQL and verified by tests.
4. Least privilege applies to users, services and agents.
5. Secrets must not be committed, logged or shipped to browsers.
6. Security-relevant actions require audit; implementation must not silently
   swallow audit failures.

## 2. Integrated socle controls (future scope)

Approved orientations are identified below; remaining details are PROPOSED.
These integrated controls remain NOT IMPLEMENTED beyond the bounded L02
HTTP/CSRF/error behavior noted above; that skeleton provides no authentication,
session store, permission model, database isolation or durable audit.

| Control | Decision boundary / design and validation needed |
|---|---|
| Password storage | Spring Security BCrypt; cost 12 is a candidate only, pending reproducible CPU/concurrency benchmarks |
| Browser sessions | APPROVED orientation: Spring Security/JDBC-backed cookie sessions with CSRF. PROPOSED details: Spring Session JDBC integration, same-origin deployment, HttpOnly/Secure/SameSite configuration and login/logout CSRF flow |
| Session lifecycle | PROPOSED: session fixation protection, expiry, logout and administrative revocation; timeout and concurrency policies remain open |
| Bootstrap | APPROVED orientation: first administrator through a local operator CLI, not a public setup route or browser form. PROPOSED details: closed-default enablement, atomic single-use guard and provisioning/audit integration |
| Transport | HTTPS outside localhost; exact deployment/termination details proposed |
| Authorization | Explicit memberships and permissions; OWNER/ADMIN/MEMBER are candidate names, not approved roles or delivered behavior |
| Tenant model | Business-neutral tenant = organization; companies belong to an organization and sites to a company; company/site authorization must not be flattened into a single tenant role |
| Tenant isolation | APPROVED orientation: restricted PostgreSQL runtime + `tenant_id` + RLS. PROPOSED details: exact schema/grants, FORCE RLS/policy mechanics and non-superuser, non-owner, NOBYPASSRLS runtime/migration role separation |
| Context | APPROVED orientation: verified transaction-local tenant context. PROPOSED details: context settings/integration and pooled-connection cleanup checks; missing/invalid context must deny |
| Audit | APPROVED orientation: critical sensitive changes and durable audit both commit or both roll back. PROPOSED details: transaction/session integration and bounded separate transactions for anonymous failures, without secrets |
| Headers | Spring Security defaults, HSTS outside development and sanitized errors; exact header policy to review |
| Rate limiting | Authentication throttling and abuse controls must be designed and tested before exposure beyond controlled development; no production deferral implied |
| Secrets | Runtime environment or secret store; development examples must contain only labelled non-production values |

Do not issue custom bearer session tokens for browser storage. The session
credential must not be exposed in JavaScript, localStorage or sessionStorage.
An Angular-readable CSRF token is distinct from the HttpOnly session cookie;
its exact delivery/integration contract remains PROPOSED. SameSite is not a
replacement for CSRF protection.

## 3. N0 security surface

Changes to the following require security review, an ADR and explicit approval:

- authentication flow, bootstrap and session storage;
- role and permission model, including company/site scoping;
- tenant resolution, runtime database privileges and RLS policies;
- audit durability, access, pipeline and retention;
- secret storage and key management;
- package signing and plugin trust (future vision).

Control-plane operations administer identity and platform metadata through
narrowly authorized contracts. They do not grant tenant business-data access.
Tenant-plane operations require verified tenant context and tenant authorization.
There is no global `app.platform_scope` bypass. Migration credentials must not
be available to web requests or ordinary jobs. Support access, if later approved,
must remain explicit, scoped, time-limited, audited and revocable under RLS.

## 4. Tenant isolation verification (required, not run)

- **Database** — test cross-tenant SELECT/INSERT/UPDATE/DELETE, missing/invalid
  context and attempts to change tenant ownership using the actual restricted
  runtime role. Assert it cannot own protected tables, become a migration role,
  bypass RLS or disable the protection.
- **Application** — repositories and services must reject absent or mismatched
  context. Approved context orientation is transaction-local and based on verified
  identity, not a client-supplied user id or unchecked tenant header; exact
  context-setting and integration mechanisms remain PROPOSED.
- **API** — validate tenant selection against current authorized memberships;
  proposed header/route details are not an approved contract.
- **Cache/search/files** — prove tenant isolation when these capabilities are
  introduced; tenant-prefixed keys/paths alone do not prove authorization.
- **Jobs** — explicitly restore and verify tenant/user authorization for each
  unit of work; no default privileged context.
- **Pooling** — prove tenant/user context cannot leak between transactions,
  including exceptions, rollback and connection reuse.

Any demonstrated cross-tenant leakage blocks acceptance and release. No such
test suite currently exists.

## 5. Audit and secrets

APPROVED invariant: a critical sensitive state change and its durable security
audit both commit or both roll back. The same-transaction design and exact
session/audit integration remain PROPOSED and require validation. Optional
external audit export must not replace this durable record. Test audit storage
failure, rollback and retry; no blanket catch-and-continue policy is acceptable.

PROPOSED failed-attempt handling: anonymous login failures and rejected
unauthenticated requests have no successful state-change transaction. Record
them in a separate bounded transaction with
sanitized, minimal data; define rate limits and observable storage-failure handling.
Never upgrade a denial to success if that audit fails. Do not record passwords,
session credentials, CSRF secrets or raw sensitive request bodies. Retention,
redaction and exact event taxonomy remain open decisions.

Production secrets must come from the deployment environment or a secret manager.
Any future development examples must be clearly non-production. A documented,
validated rotation procedure is required before production; none is claimed here.

## 6. Vulnerability management (proposed)

- Plan CI dependency scanning; no pipeline is asserted to run today.
- Block releases with critical vulnerabilities under a reviewed exception policy.
- Produce an SBOM before public distribution.
- Deliver security fixes through the approved release/versioning process.

## 7. Threat modeling

A STRIDE review of the foundation is planned, with updates before any new trust
boundary (plugins, scripts, integrations, AI, offline synchronization). A linked
threat-model document is a specification, not evidence of completed security
review. Security acceptance requires an identified reviewer and findings record.

## 8. Incident response (proposed)

1. Detect and classify (confidentiality, integrity, availability, tenancy).
2. Contain and preserve evidence.
3. Notify the owner and affected parties according to applicable obligations.
4. Remediate, then document root cause and preventive actions.
5. Add regression tests for the failure mode.

## 9. Prohibited patterns

- Reversible/plaintext password storage or logging secrets.
- Authorization implemented only in Angular.
- Arbitrary scripts with server privileges.
- Universal RLS bypass flags or ordinary application access to migration roles.
- Treating platform ownership as permission to read tenant business records.
- Public first-user setup, custom browser bearer session storage, or disabling
  CSRF merely because the application uses cookies or is same-origin.
- Silently allowing sensitive writes after durable audit failure.
- Sharing production credentials through chat or tickets.

## 10. Deferred agent authority contracts

**PROPOSED / NOT IMPLEMENTED — L3, C55–C57.** These requirements add no L1
scope and select no agent framework or provider. Contract names below are
conceptual, not current endpoints; existing approved principles still apply.

Tools must have typed, versioned, allowlisted contracts with bounded inputs and
outputs and declared effects: read-only, protected mutation or external effect,
with sensitivity classified separately. Each invocation must identify the invoking
actor, agent and full delegator chain. Effective authority is the intersection of
CURRENT human/service grants throughout that chain, explicit delegated scope and
tool policy; no agent may self-grant. Subdelegation must be narrower, expiring and
revocable. Revalidate authority at execution and replay, including tenant,
company/site ancestry and scope; queued work must not preserve revoked grants.

Human approval for sensitive actions must bind the canonical exact action, tool
version, parameters digest, tenant, actor/delegation chain, resource revision,
expiry and finite-use decision. Changed arguments, version, scope or revision
invalidate approval. Approval never bypasses current authorization, durable audit
or owning transition invariants. Model confidence must never waive human approval
for sensitive publication, security, code or data actions.

Approval consumption must be durably traceable and coordinated with the owning
API's protected mutation and audit, preserving their commit/rollback invariant
and preventing concurrent over-consumption. Unknown outcomes must be resolved
using the same idempotency/operation identity, not by repeating the effect under
a new identity. External effects require a durable intent, dispatch and outcome
reconciliation protocol; SQL atomicity must not be claimed across an external
system. Apply the [outbox/inbox and idempotency contract](../03-CORE/OUTBOX-INBOX-IDEMPOTENCY.md).

Progression must be gated: read-only proposals, then validated drafts, then
human-approved bounded execution. Production assistants must call the same
owning APIs as other callers, with no raw database or shell access. Generated
code belongs only in an isolated, reviewed build/release process, never runtime
hot-loading; preserve [module runtime boundaries](../03-CORE/MODULE-RUNTIME.md)
and [frontend boundaries](../02-ARCHITECTURE/FRONTEND-ARCHITECTURE.md).

Prompts, documents and tool results are untrusted data, not authority. Retrieved
instructions cannot expand tool policy or delegation. Tenant memory and retrieval
must be isolated, support revocation and recheck current permissions before use;
revoked or cross-tenant material must not be reused as retained context. Provider
export requires explicit approval for the data and destination; access to a tool
or document is not permission to export it. Record provenance references and
minimal decision evidence, not full secret-bearing prompts or sensitive payloads.
Set enforceable cost, time, concurrency and delegation-depth limits.

Dry-run must mean isolated execution with no effectful tools; its success is not
a production security guarantee. An emergency stop must prevent new work and
further dispatch, but cannot magically undo already committed or external effects;
resolve in-flight outcomes through the durable operation protocol.

## 11. Shared composition and development security boundary

**PROPOSED / NOT IMPLEMENTED — gated by applicable N0 approval and slice evidence.**
This consolidates AP-ARCH-EVOL-001 and AP-MASTER-CONSTITUTION-001 under the
[documentation-only authorization](DECISION-MANAGEMENT.md#visual-composition-documentation-authorization--2026-11-20).
See the [coverage refinement](../PACK-INVENTORY.md#visual-composition-coverage-refinement)
and [open decisions O20–O25](DECISION-MANAGEMENT.md#4-open-decisions). It changes
neither the Constitution nor existing roles, grants or executable manifests.
The UAM, registries and owning APIs remain the shared contract authority; visual,
SDK/API, imported, AI-generated and Development Studio contributions get no
alternative permission system or direct database access.

### Common conditions across creation and execution paths

- Check CURRENT actor/delegation permissions and the operation's declared scope
  on discovery, read, draft edit, validation, simulation, build, import/export,
  approval, publication, activation, execution and recovery. A control-plane
  contract requires explicit control-plane authority, not a fabricated tenant;
  it grants no access to tenant data. A tenant-plane contract requires verified
  tenant/organization context and company/site scope where its resource contract
  requires them. Missing required or mismatched scope denies the operation.
  A saved draft, cached catalogue, queued action or purchased capability cannot
  preserve a revoked grant or convert control-plane authority into tenant access.
- Filter visualizer **metadata as well as data** server-side: entity/schema names,
  relationships, dependencies, devices, configuration, extension points, execution
  traces and implementation links can disclose protected structure. Counts, search,
  errors, previews, caches and exports must not reveal hidden objects indirectly.
  Revalidate on tenant switch and revocation; hiding canvas elements is insufficient.
- Separate permission to develop, test, approve, publish and activate from permission
  to execute a capability or read its data. A developer can declare required
  permissions, never self-assign grants or elevate users through generated roles,
  manifests or imports. Licensing and UI mode are not authorization.
- Record sanitized provenance from source/import identity and content digest through
  exact draft revision, dependency lock, builder/tool versions, test results and
  artifact digest to publication. Retain human/AI authorship and change lineage;
  provenance is evidence, not execution authority. No secrets in exported UAM,
  packages, prompts, logs, traces or build artifacts; use scoped secret references
  rebound through approved destination configuration, never copied credentials.
- Apply the exact-action approval contract in section 10 to the **exact draft
  revision and resulting artifact**, including reviewed dependency/schema changes
  and target environment. Human approval requires current authority; any change
  invalidates it and requires renewed validation/tests and approval. Publication
  cannot silently approve migrations, privileged actions or changed build output.

### Untrusted input, builds and device boundaries

Treat all imported archives, templates, graphs, schemas, references and generated
code as untrusted. Before accepting an import, bound compressed and expanded size,
entry count, compression ratio, nesting, parser depth/tokens, graph expansion and
reference count/depth. Reject path traversal, absolute paths, escaping links,
duplicate/conflicting entries, unsupported formats/versions and cyclic or missing
references; parsing must not instantiate executable types or trigger side effects.
Resolve only approved package-local or registry references with pinned identity,
version and digest. Do not fetch arbitrary URLs, external schema/entity references
or dependencies during parse/validation. Any separately approved acquisition path
needs allowlisted origins, redirect/DNS/address checks against SSRF (including
loopback, private and metadata endpoints), bounded downloads and provenance checks;
no implicit network access or dependency installation follows from an import.

Trusted reviewed first-party artifacts, organization-controlled extensions and
untrusted third-party/generated code are distinct trust classes. Untrusted code
requires a **separately approved isolated trust/execution model** with resource,
network, filesystem, credential and tenant boundaries and escape tests before any
execution. A signature proves artifact provenance/integrity, not safety or a sandbox;
a classloader, SDK or successful build supplies no isolation guarantee. No runtime
hot-loading, arbitrary shell, generated DDL or migration credential follows from
Development Studio. Generated schema changes remain reviewed proposals for the
owning migration process. Device/local-agent access needs explicit scoped contracts,
authorized destinations and effect declarations; simulation uses inert adapters,
never a physical device. Unknown physical outcomes require reconciliation, not a
blind retry or a promise that cancellation undoes the effect.

### Reuse existing AI governance

Extend section 10 and existing C55–C57 responsibilities, not a second AI authority
or provider subsystem. O7/O13 remain applicable alongside O20–O25. Provider/model
configuration, tenant memory/retrieval revocation, prompt and model versioning,
evaluation datasets/results and output validation must be linked to the same draft
provenance and approval path. Evaluation data needs its own authorized scope and
retention policy. Attribute usage, cost and quotas to the invoking scope/capability;
record provider/model/prompt/evaluation versions without retaining secret-bearing
payloads. Approved data classification, destination, residency, retention and
provider-training policy apply to every call and fallback. A fallback provider or
model cannot silently widen data export, authority or cost limits; unsupported or
unavailable providers block the optional AI action, not unrelated Core operations.
No provider, model, memory store or evaluation service is selected by this proposal.

## Related documents

- [Platform Constitution](PLATFORM-CONSTITUTION.md)
- [Identity and Access](../04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md)
- [Multi-Tenancy Architecture](../02-ARCHITECTURE/MULTI-TENANCY-ARCHITECTURE.md)
- [Audit Engine](../04-PLATFORM-ENGINES/AUDIT-ENGINE.md)
