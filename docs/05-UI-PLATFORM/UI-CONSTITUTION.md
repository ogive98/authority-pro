# UI Constitution

**Decision status:** PROPOSED detailed rules; premium restrained Soft Glass D161 direction and four languages are blueprint requirements.
**Implementation status:** NOT IMPLEMENTED; no screens have been browser-tested.

## Purpose, scope and ownership

A coherent Angular platform shell, reusable controls and registered contributions. The UI owner maintains tokens, navigation contracts and accessibility behavior. Feature owners compose these controls without changing global navigation or shared styling privately. No legacy assets or unrelated conversation preferences are imported.

## Visual rules

Readable, dense workspaces; restrained transparency; limited rounding; a single primary action per context; no excessive colored frames, duplicated icons or empty space. Glass effects never sit beneath dense table text unless contrast is demonstrably sufficient. Both light and dark themes are required. Reduced motion/transparency must keep the interface fully usable.

## Navigation and interaction

Every implemented destination has a visible entry from its parent. Represent tenant/company scope clearly and persist no unauthorized selection. Route guards improve navigation only; the backend remains authoritative. Keyboard focus is visible, follows dialog semantics and returns to the launching control. Escape closes dismissible overlays without discarding data silently.

Forms have associated labels, field-level errors and accessible summaries. Submitting disables duplicate submission while preserving input. Tables have captions, column headings, stable sorting, bounded pagination and a dedicated horizontal scroll area rather than page overflow. Loading, empty, error, denied and unavailable are distinct states; never show invented numbers to fill a dashboard.

## Localization and accessibility

French, English, Italian and Arabic/RTL are required. Strings, dates, numbers and direction are locale-aware. Directional icons mirror only when their meaning is directional. Use logical spacing properties; keep identifiers and code in suitable text direction. A language rollout reduction requires explicit approval.

Target WCAG 2.2 AA as a proposal: text contrast at least 4.5:1 for ordinary text, 3:1 for large text and meaningful non-text indicators; keyboard operation, zoom/reflow, non-color status cues and sufficiently sized targets. This is a target, not a compliance certificate.

## Security and failure behavior

No raw HTML injection from manifests, translations or server messages. Render escaped text; use reviewed URL allowlists for links. Never persist session credentials in JavaScript storage. Clear old tenant data before switching context and ignore stale responses. A denied API request must not reveal the previous tenant's cached view.

## Acceptance

Browser evidence must cover login, logout, scope switching, unauthorized audit reads, error/empty states, all locales/RTL, both themes, keyboard/dialog focus, zoom and narrow viewports. A build and unit tests alone cannot mark the UI tested. Token values and spacing remain proposals until reviewed visually.

## Related

- [Design system](DESIGN-SYSTEM.md)
- [Frontend architecture](../02-ARCHITECTURE/FRONTEND-ARCHITECTURE.md)
- [Test strategy](../11-QUALITY-AND-TESTING/TEST-STRATEGY.md)
