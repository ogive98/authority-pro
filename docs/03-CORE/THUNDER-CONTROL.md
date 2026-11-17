# Thunder Control

**Document status: PROPOSED**  
**Capability status: NOT IMPLEMENTED — no administration UI or API exists yet.**  
**Governance: N1 for presentation; N0 for access, audit and protected operations.**  
**Proposed owners: Angular shell maintainer and platform operations lead; approval authority: project owner.**

## 1. Purpose, source and scope

Thunder Control is the authorized management surface for Authority Pro. It presents the state of platform capabilities and invokes their published application contracts. It is not Thunder Core and owns neither identity, tenant data, configuration persistence nor module internals.

The locked source principles are [MASTER-BLUEPRINT](../MASTER-BLUEPRINT.md), sections 5, 6.2, 8, 15, 20–23 and 30. Mechanisms below are proposed under the [Core status convention](THUNDER-CORE.md), pending owner approval. The approved Angular/Spring Boot stack and documentation-then-socle scope apply.

Initial proposed scope: protected platform overview, own session and membership context, static module/contract inventory, safe runtime health, authorized configuration reads, and an audit reader. A minimal tenant-settings change may be selected as a technical acceptance flow only after its mutation contract is approved.

Non-scope: business screens, marketplace, general-purpose owner console, user impersonation, public setup wizard, SQL console, server shell, remote package upload, untrusted UI extensions, automatic repairs, restore execution, license enforcement, or invented queue/worker data.

## 2. Ownership and dependencies

| Surface | Source of truth / contract owner | Control responsibility |
|---|---|---|
| Current identity and session | `identity` | Display safe account/session state; invoke guarded logout |
| Tenant/company selector | `tenancy` membership API | Show only the caller's authorized memberships; clear stale view state on change |
| Permissions | `access` | Use safe capability hints for presentation; never replace server checks |
| Module and contract inventory | `runtime` | Read-only versioned catalogue and availability reasons |
| Tenant/company settings | `tenancy` | Render allowed typed values with effective scope and revision |
| Deployment configuration status | `runtime` | Display redacted validation/restart-required state; no secret values |
| Audit | `audit` through a guarded read API | Bounded, scoped and permission-checked browsing |

Thunder Control is an Angular feature boundary, not an additional backend module with universal persistence privileges. Angular adapters consume versioned REST DTOs; backend adapters delegate to named APIs. No UI declaration may insert arbitrary scripts, URLs or HTML into global navigation. Static route registration is governed by [Module Runtime](MODULE-RUNTIME.md).

## 3. Proposed interfaces and contracts

The following names describe application contracts, **not existing endpoints**. Exact OpenAPI routes and DTOs require approval before coding.

| Contract | Input | Output / authorization boundary |
|---|---|---|
| `GetOwnSession` | Authenticated session | Safe principal summary, expiry metadata; no session credential |
| `ListOwnMemberships` | Authenticated principal, bounded cursor | Own tenant/company memberships only; no user-supplied subject identity |
| `GetPlatformOverview` | Control-plane permission | Build version, safe health summary and installed compiled module inventory; no tenant counts or tenant data by default |
| `GetModuleContract` | Approved module/capability ID | Exported version/schema summary only; no internal class or filesystem details |
| `GetEffectiveTenantSettings` | Validated tenant/company context and read permission | Typed non-secret settings, source scope, revision |
| `UpdateTenantSettings` | Validated context, allowed changes, expected revision and approved idempotency metadata if supported | Committed revision or safe conflict; critical audit is atomic |
| `ListAuditRecords` | Tenant or control-plane permission, cursor and bounded filters | Records only from that scope, with allowlisted metadata |

Selection of a tenant or company is a **request for scope**, not proof of authorization. Each backend request revalidates session, membership, company ancestry and permission. The client MUST clear previous tenant data, cancel in-flight view requests where possible, and reject stale responses tagged with an earlier selection. Switching scope does not create a new tenant membership or convey the platform-owner role into a tenant.

Mutations require CSRF protection and an explicit confirmation when the action is security-sensitive. The UI MUST show server-confirmed results; optimistic display cannot claim a protected write succeeded before its transaction commits. A timeout is an unknown result until resolved, not an instruction to generate a different idempotency key.

## 4. Surface availability and honest administration

| Source ambition | Socle treatment | Status |
|---|---|---|
| Overview, runtime health, static module/capability/contract explorer | Minimal protected reads proposed | NOT IMPLEMENTED |
| Configuration and audit | Limited scope-aware reads; mutations individually gated | NOT IMPLEMENTED |
| Dependency manager / module manager | Read-only compiled inventory and impact information; no runtime install button | NOT IMPLEMENTED |
| Event, queue, worker and scheduler explorers | Deferred until corresponding engines and permissions exist | NOT IMPLEMENTED |
| Resource monitoring | Minimal health metrics first; adaptive governor deferred | NOT IMPLEMENTED |
| Feature flags, licenses and entitlements | Deferred; no fake active entitlement status | NOT IMPLEMENTED |
| Backup, restore, repair, update and maintenance execution | Separate operational contracts and approval gates required | NOT IMPLEMENTED |
| Logs and security overview | Only curated redacted data, not raw server logs | NOT IMPLEMENTED |

Unsupported functions should be absent or explicitly marked NOT IMPLEMENTED in a documentation/prototype context, never represented by successful dummy results. Lack of an engine must not be rendered as a healthy empty queue. A platform operator's visibility is limited to approved control-plane metadata; access to customer data requires explicit, scoped, time-limited, revocable and audited authorization, with the mechanism deferred until approved.

## 5. Security and data handling

Use the proposed same-origin Spring Security / Spring Session JDBC browser contract in [Identity and Access](../04-PLATFORM-ENGINES/IDENTITY-AND-ACCESS.md). The session cookie is HttpOnly, Secure and SameSite; the Angular client sends the approved CSRF header on unsafe requests. No custom bearer token, localStorage/sessionStorage credential, query-string token, or public unauthenticated bootstrap route is allowed.

Backend authorization applies to every read and write, including catalogue/health details. Hiding a menu is not authorization. Tenant settings and audit reads require tenant context; control-plane overview never falls back to a privileged tenant query. Responses use allowlisted DTOs. Secrets are omitted rather than merely visually hidden, and personal information is minimized. Shared caches, downloads and browser persistence must not mix scopes; sensitive administration responses are proposed to use `Cache-Control: no-store`.

Angular output encoding, a restrictive content security policy and controlled links are required before release. Accessibility, keyboard focus, loading/error states and scope labels must be tested. Visual design does not weaken security confirmations or hide which tenant/company is affected.

## 6. Failure and observability

| Failure | Required behavior |
|---|---|
| Session expired/revoked | Clear protected client state and require sign-in; do not repeatedly retry |
| Membership removed or scope denied | Clear scoped data and show a non-enumerating denial; no platform-scope retry |
| Invalid or missing CSRF token | Reject mutation; re-establish valid CSRF state through the approved same-origin flow |
| Revision conflict | Preserve the user's proposed change locally, fetch authorized current state and require review |
| Network timeout after mutation | Display unknown outcome and resolve through the approved command contract |
| Audit/database failure | Show operation failure; never announce success or silently bypass audit |
| Registry/health unavailable | Display unavailable/stale with timestamp, not healthy |
| Unsupported engine | Hide execution controls and state capability is NOT IMPLEMENTED |

Client telemetry includes route category, duration, coarse error code and correlation ID, not session/CSRF values, input contents, settings values or tenant data. Backend metrics measure authorized request latency, denial rates and dependency availability with bounded labels. Critical changes are recorded by their backend owners using [Audit Engine](../04-PLATFORM-ENGINES/AUDIT-ENGINE.md); clicking a button is not proof that the protected write committed.

## 7. Tests and acceptance

Required future tests:

- A platform-only operator sees safe platform metadata but cannot list tenant settings/audit without membership and permission.
- Two tenants and several companies prove selectors expose only own memberships and stale concurrent responses never populate the wrong scope.
- Direct HTTP access to hidden routes remains denied; tampering with IDs, CSRF headers or frontend permission hints grants nothing.
- A configuration revision conflict, unknown timeout, dependency outage and audit failure each show accurate failure states.
- Empty inventory and unavailable registry are distinguishable. Deferred engines have no working-looking mutation controls.
- Logout/revocation clears sensitive client state; browser storage contains no credentials. Keyboard/screen-reader operation identifies scope, confirmation and result states.

Acceptance requires approved API/permission contracts, real backend enforcement, a passing scope-switch/security test suite and demonstrable absence of business screens. Mock data and screenshots do not establish a runnable administration capability.

## 8. Remaining owner gates

Approve the exact minimal Control surfaces, control-plane versus tenant permission names, safe health disclosure, API schemas, configuration mutation slice and audit read scope. Restore/repair/update/maintenance execution requires a separate reviewed operational design with backup and recovery evidence; this document does not authorize those actions.

## Related contracts

- [Thunder Core](THUNDER-CORE.md)
- [Module Runtime](MODULE-RUNTIME.md)
- [Configuration Engine](CONFIGURATION-ENGINE.md)
- [Command, Query and Event Engine](COMMAND-QUERY-EVENT-ENGINE.md)
- [ADR Template](../14-TEMPLATES/ADR-TEMPLATE.md)
