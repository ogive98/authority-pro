# Design System

**Decision status:** PROPOSED token and component specification.
**Implementation status:** NOT IMPLEMENTED; contrast, rendering and interaction targets unverified.

## Token ownership

Use three layers: primitives (raw palette, spacing and typography), semantic aliases (surface, text, action, error, focus), and component tokens (button, input, table, navigation). Components reference semantic/component tokens, never arbitrary raw colors. Theme switching overrides semantic values; it does not duplicate component CSS.

## Candidate values

| Primitive | Proposed value | Intended use |
|---|---|---|
| neutral-950 / neutral-900 | #101318 / #191E26 | Dark application/surface |
| neutral-50 / neutral-0 | #F6F7F9 / #FFFFFF | Light application/surface |
| neutral-200 / neutral-700 | #D8DEE8 / #384252 | Text or border by theme |
| blue-300 / blue-700 | #91B9FF / #1D4ED8 | Focus/accent per theme |
| space-1 / 2 / 3 / 4 / 6 | 4 / 8 / 12 / 16 / 24 px | Layout rhythm |
| radius-control / panel | 6 / 10 px | Restrained rounding |
| font-body / compact / title | 14 / 12 / 22 px | Workspaces and headings |
| duration-fast | 120 ms | Optional transition; zero under reduced motion |

These values are candidates, not measured accessible combinations. Define explicit foreground/background pairs and test the rendered output, including glass opacity and disabled states. Prefer system fonts with Arabic coverage and no external font request dependency.

## Semantic and component mapping

`surface.app`, `surface.panel`, `text.primary`, `text.secondary`, `border.control`, `action.primary`, `status.error`, `focus.ring` map to theme-appropriate primitives. `button.primary.background` references `action.primary`; `input.focus.border` references `focus.ring`; `table.row.hover` references a semantic interaction surface.

Aliases must form an acyclic graph and resolve in both themes. Add tokens only when a real component needs them; no speculative hundreds-token catalogue.

## Component contracts

| Component | Required states and behavior |
|---|---|
| Button | Default/hover/active/focus/disabled/busy; accessible name; no width jump while busy |
| Input | Empty/filled/focus/error/disabled; visible label; error association; password manager support |
| Table | Loading/empty/error/data; stable row identity; headers and caption; bounded pagination; scroll container |
| Tenant selector | Only allowed scopes; current scope clear; disabled during invalid transition; canceled stale requests |
| Navigation | Active location, keyboard focus, accessible collapsed labels, parent links |
| Dialog | Named modal, focus trap/restore, Escape policy, explicit destructive confirmation |
| Status | Text and icon, not color alone; unavailable is not healthy |

Density is semantic: propose 36px compact table rows and 44px comfortable controls, subject to target size and zoom checks. Responsive behavior may scroll tables but must not hide permission/error actions or trap keyboard users.

## Ownership, versioning and tests

UI contributions register known route/component keys and declared permissions, not executable strings. A breaking shared component contract needs N1 review. Token changes require before/after screenshots in light/dark and Arabic RTL plus contrast and focus checks. No screenshot or component implementation exists yet.

## Related

- [UI constitution](UI-CONSTITUTION.md)
- [Frontend architecture](../02-ARCHITECTURE/FRONTEND-ARCHITECTURE.md)
