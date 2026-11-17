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

## Failure, performance and tests

Every data view distinguishes loading, empty, error and denied states. Abort stale requests, bound table pagination and avoid unsafe HTML rendering. Propose bundle budgets after measuring an actual Angular production build.

Browser acceptance: login/logout, expired session, scope switching with slow responses, reload, audit permission denial, empty/offline/error states, keyboard focus, four locales including RTL, both themes, zoom and responsive table scrolling. Build/test success alone is not UI acceptance.

## Related

- [API](API-ARCHITECTURE.md)
- [UI constitution](../05-UI-PLATFORM/UI-CONSTITUTION.md)
- [Design system](../05-UI-PLATFORM/DESIGN-SYSTEM.md)
- [Validation gates](../13-IMPLEMENTATION/VALIDATION-GATES.md)
