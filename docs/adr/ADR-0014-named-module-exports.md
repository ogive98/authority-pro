# ADR-0014 — Named exports and module edge rules

- **Status:** PROPOSED — convention not enforced; no lint configuration exists (X-06).
- **Scope:** TypeScript/Angular source layout and import direction. Java module
  boundaries remain governed by Spring Modulith and ArchUnit.
- **Risk addressed:** undocumented module edges; absent frontend linting.

## Context

The Angular application groups code by feature. Nothing currently prevents a feature
from importing another feature's internal files, and nothing standardizes default
versus named exports. Without a written rule, boundaries erode quietly and the first
enforcement attempt finds hundreds of violations.

## Options considered

1. **No rule; rely on review.** Zero setup, no durable guarantee.
2. **Named exports only, plus one-directional feature edges.** Enforces discoverability
   and makes imports greppable. Requires a lint rule to be effective.
3. **Path aliases plus a barrel per feature.** Cleaner imports, but barrels can hide
   cycles and inflate bundles if misused.
4. **Full workspace split (Nx-style projects).** Strongest isolation, disproportionate
   for the current size.

## Proposed decision

Adopt option 2 now, option 3 only where a feature genuinely exports a public surface.

- All TypeScript symbols are exported by name; no default exports. Angular standalone
  components, services, guards and models follow this rule.
- A feature may import from `shared/`, `core/` and its own directory. Importing
  another feature's internals is forbidden; any cross-feature need goes through that
  feature's `public-api.ts`, which lists only intended symbols.
- No circular feature dependencies; if two features need each other, the shared part
  moves to `shared/`.
- Backend side: Spring Modulith roots keep `allowedDependencies` empty; internal
  packages stay package-private unless promoted to a named exported type. New public
  types require the owning module's agreement and a test.
- Enforcement: add `eslint` with `import/no-default-export`,
  `import/no-restricted-paths` (or an equivalent boundary plugin) and a cycle check
  in package S-03/C-03. Until lint exists, the rule is reviewed manually and tracked
  as a known gap.

## Consequences

Imports become predictable and refactors safer. The first lint run will surface
existing violations; they must be fixed rather than suppressed, or the rule is
downgraded to a warning with a dated exit condition. Public API files add one
indirection per feature.

## Security considerations

Boundary rules reduce accidental coupling that could let UI code bypass services and
call APIs directly. They do not replace authorization checks, which remain server-side.

## Validation and tests

1. Lint fails on a default export and on a cross-feature internal import (fixtures).
2. No import cycles reported across features.
3. Existing 100 frontend tests pass after normalization.
4. Backend ArchUnit/Modulith tests unchanged and green.

## Migration implications

Mechanical import edits across the frontend; no runtime behaviour change. Bundle
budgets must be re-checked after any barrel introduction.

## Related

- [Target blueprint](../15-FOUNDATION-9.5/phase-2/00-TARGET-BLUEPRINT.md)
- [Module ownership matrix](../15-FOUNDATION-9.5/phase-2/01-MODULE-OWNERSHIP-MATRIX.md)
- [Frontend / API map](../15-FOUNDATION-9.5/phase-2/06-FRONTEND-API-MAP.md)
