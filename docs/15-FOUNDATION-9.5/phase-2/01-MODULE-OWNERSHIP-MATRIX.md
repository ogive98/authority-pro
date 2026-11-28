# Phase 2 — Module Ownership Matrix

**Decision status:** PROPOSED ownership and dependency rules; existing boundaries are OBSERVED.
**Delivery status:** DESIGN ONLY. No module or export is added by this document.
**Document validation status:** DRAFT.

## 1. Observed state

Five Modulith roots are compiled as `CLOSED` with an explicitly empty
`allowedDependencies` array, and no named interfaces exist. The bytecode test rejects
any cross-root edge and any implicit root-package API. Support code lives outside the
roots in `runtime`, `persistence`, `openapi` (test scope) and `launch`.

## 2. Logical contexts and ownership

| Context | Owns | Does not own | Current code |
|---|---|---|---|
| Identity | principals, credentials, factors, sessions, recovery | tenant permissions, business data | `identity` root, V3/V4 |
| Tenancy | tenant, company, site hierarchy; membership lifecycle | authorization decisions | `tenancy` root, V1 |
| Access | roles, permissions, scoped grants, decisions | identity of the caller, tenant data | `access` root, V3/V5 |
| Audit | audit records, attempts, integrity, retention | business payloads | `audit` root, V2 |
| Inventory | compiled descriptor catalogue and protected read | executable installation | `inventory` root + runtime catalogue |
| Contracts | versioned schemas, error model, envelopes | storage | documents + test OpenAPI |
| Runtime | dispatch, execution context, configuration guard | business rules | `runtime` |
| Durable work | outbox, inbox, jobs, retries | business effects | DEFERRED |
| Configuration | typed scoped configuration and entitlements | secrets storage | PARTIAL (environment only) |
| Operations | health, logs, metrics, diagnostics, recovery | application data | PARTIAL (health, logs) |

## 3. Proposed dependency rules

1. Default remains zero edges between roots; any new edge needs an approved ADR, an
   explicit named export and a positive/negative ArchUnit fixture.
2. Allowed direction of a future edge: Access and Audit may read ownership data only
   through owner-published read contracts; Identity may ask Access for a decision but
   not for tenant data.
3. Tenancy never imports Access; Access consumes tenancy selectors as opaque,
   validated identifiers.
4. Inventory may depend on Runtime metadata, never on business contexts.
5. Audit is write-oriented for other contexts; no context reads audit to make a
   business decision.
6. Shared code must be a published contract or a runtime primitive, never a hidden
   utility package.

## 4. Candidate named exports (PROPOSED, not implemented)

| Owner | Export | Consumer | Rationale | Gate |
|---|---|---|---|---|
| Tenancy | `MembershipLookup` (user → memberships with labels) | Access, Identity bootstrap | Avoid duplicating membership SQL | Phase 3 |
| Access | `AuthorizationDecision` (user + scope + permission → allow/deny) | Inventory, Audit, future modules | Single decision point | Phase 3 |
| Audit | `AuditAppend` (command with envelope) | Access, Identity | Atomic critical action plus audit | Phase 3 |
| Identity | `SessionTermination` (user → terminate sessions) | Access (membership revocation) | Revocation needs deterministic session behaviour | Phase 3 |

Each export must carry a version, an owning contract test and a documented failure mode.
No export may expose a table or a raw `DataSource`.

## 5. Frontend ownership

| Area | Owns | Backend counterpart |
|---|---|---|
| `core/session` | session state, CSRF handling | Identity auth endpoints |
| `core/scope` | selected tenant/company, stale-response protection | Access bootstrap |
| `core/inventory` | inventory read model | Inventory endpoint |
| `core/audit` | audit page state and cursors | Audit endpoint |
| `features/*` | route-level composition and states | same endpoints |

The frontend never computes permissions from roles; it renders what the bootstrap
returns and still relies on backend denial.

## 6. Ownership tests to add

- A machine-readable table-ownership map checked against migration `GRANT` statements.
- An ArchUnit rule that a named export is only consumable by declared consumers.
- A test that a permission code used by a route exists in the access catalogue.

## Related documents

- [Target blueprint](00-TARGET-BLUEPRINT.md)
- [Contract catalogue](02-CONTRACT-CATALOGUE.md)
- [Database ownership map](03-DB-OWNERSHIP-AND-MIGRATION-MAP.md)
