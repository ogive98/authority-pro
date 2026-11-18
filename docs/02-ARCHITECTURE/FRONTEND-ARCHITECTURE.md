# Frontend Architecture

**Decision status:** Angular APPROVED; structure and libraries PROPOSED.
**Implementation status:** NOT IMPLEMENTED. No browser verification has occurred.

## Purpose and boundaries

A platform shell for identity, tenant selection, diagnostics and authorized audit access. No business modules, fake operational metrics or pretend module installation screens. Permission-based hiding improves usability but never replaces backend authorization.

## Component organization

Standalone routed features: authentication, shell, technical overview, audit, user preferences. Core services own session discovery, selected scope and locale. Shared components implement accessible controls and design tokens; add abstractions only when actually reused.

Use signals for synchronous UI state and RxJS where asynchronous cancellation/streams justify it. Lazy-load routed features. Compile reviewed first-party UI contributions into the Angular build; downloading arbitrary JavaScript from a manifest is not part of the foundation.

## Routes and navigation (proposal)

| Route | Experience |
|---|---|
| `/login` | Sign in; provisioning instructions if operator setup is incomplete |
| `/app/tenants` | Only the authenticated user's available scopes |
| `/app/overview` | Actual platform state or a clearly labelled unavailable state |
| `/app/audit` | Tenant audit table; unauthorized users get no hidden-data fetch |
| `/app/preferences` | Language, theme and density preferences |

No public setup form can claim the first administrator account. Sidebar and account navigation expose every implemented child page. Unknown paths show an accessible not-found state with a safe route back.

## Session and tenant state

Session credential remains in an HttpOnly cookie. Angular reads a separate CSRF token through the framework-supported mechanism and sends it on unsafe requests. Never store credentials in localStorage/sessionStorage. `/auth/me` restores authenticated state on page load.

Keep selected tenant in memory; an optional user-specific persisted preference is revalidated against current memberships before use. On tenant switch, cancel requests and clear tenant caches/views before loading the new scope. Late responses from the previous scope must not repopulate the screen. Logout clears all sensitive client state; 401 redirects safely, 403 does not masquerade as logout.

## Languages, design and accessibility

French, English, Italian and Arabic plus RTL are source requirements. No reduced rollout is approved. Externalize strings, use logical CSS properties, locale-aware formatting and language/direction attributes. The translation library choice remains proposed; do not build a custom engine without need.

Soft Glass D161 is a visual direction, not an existing imported design system. Specify both light and dark tokens, table density, focus indicators and reduced-transparency behavior. No old-project assets or preferences are imported.

## Deferred schema-driven interfaces

**PROPOSED / NOT IMPLEMENTED.** C38/C39 schemas remain L2/L11; visual and AI
builders remain L3. The static L1 frontend scope is unchanged.

Every schema must declare its owner, stable id, version and target contract
revision/digest. Separate the data schema, UI layout and instance values: none
confers authority on the others. Allow only bounded declared fields and a registry
of reviewed, precompiled Angular widgets. Labels and validation errors must support
French, English, Italian and Arabic with RTL. Backend input/output validation
remains authoritative; client validation is only a usability aid.

References must be bounded, local and preapproved, with no automatic network
schema-reference resolution. Schemas must not supply remote executable JavaScript,
templates, SQL or class names; no eval or generated-template compilation is allowed.
Servers must omit inaccessible fields and data, not merely hide them in the UI,
and reject crafted attempts to mutate read-only fields.

Versioned drafts must pass validation, preview and review before publication.
Approval must bind the exact revision; conflicts, stale revisions and target
contract revision/digest mismatches must be rejected before effects. Current server
permissions remain necessary at publication and use. Schema changes cannot create
database models or tables; preserve the [configuration boundary](../03-CORE/CONFIGURATION-ENGINE.md).
AI proposals must never publish automatically and remain subject to the
[deferred agent authority contracts](../00-GOVERNANCE/SECURITY-GOVERNANCE.md#10-deferred-agent-authority-contracts).

Future workflow task forms must send guarded commands with expected revisions,
not direct state edits. Reject stale ownership or revoked authority; for an unknown
result, reconcile the same operation identity rather than blindly repeat effects.
Discard responses from a previous tenant context, even if the schema is shared.

Schema-driven forms must preserve keyboard access, focus, labels and accessible
errors in all four locales, including RTL. Distinguish unavailable, denied, stale,
loading, empty and error states honestly; schemas do not justify fake operational
screens or claims that deferred capabilities are implemented.

## Failure, performance and tests

Every data view distinguishes loading, empty, error and denied states. Abort stale requests, bound table pagination and avoid unsafe HTML rendering. Propose bundle budgets after measuring an actual Angular production build.

Browser acceptance: login/logout, expired session, scope switching with slow responses, reload, audit permission denial, empty/offline/error states, keyboard focus, four locales including RTL, both themes, zoom and responsive table scrolling. Build/test success alone is not UI acceptance.

## Related

- [API](API-ARCHITECTURE.md)
- [UI constitution](../05-UI-PLATFORM/UI-CONSTITUTION.md)
- [Design system](../05-UI-PLATFORM/DESIGN-SYSTEM.md)
- [Validation gates](../13-IMPLEMENTATION/VALIDATION-GATES.md)
