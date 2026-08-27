---
paths:
  - "app/src/main/kotlin/com/emm/hello/**/*.kt"
---

# Shared UI rules

Every shared component lives in `app/src/main/kotlin/com/emm/hello/core/ui/`. The components are inspired by shadcn/ui and define the app's theme.

## The iron rule

Feature screens call **only** `H*` components. **Never** raw Material3 — no `Button`, `OutlinedTextField`, `TextField`, `Card`, `IconButton`.

A custom component written inside a screen never replaces a `core/ui` component that exists for that purpose. If the `core/ui` one does not fit, extend or modify it first.

## Naming

- **Public composables**: `H` prefix — `HInput`, `HButton`, `HBadge`, `HCard`, `HIconButton`. Hard rule.
- **Files**: mixed today. Of the files in `core/ui/`, some use the `H` prefix (`HTopBar.kt`, `HSearchBar.kt`, `HEmptyState.kt`) and some do not (`Button.kt`, `Input.kt`, `Card.kt`). When touching an existing component, follow the pattern already established for that component family. A new standalone shared component gets an `H`-prefixed file.
- **Exception without an `H*` composable**: `FieldShell` — an internal building block and the template for inputs.

## Before creating a component

1. **Check `core/ui/` first.** If it exists, use it. No exceptions.
2. **Decide the scope.** Used by a single screen, it belongs to the feature package (`newfeatures/<Feature>/`). Used app-wide, it belongs in `core/ui/`.
3. **If it must be created**, template on `Input.kt` + `FieldShell.kt` — animated border, transparent background, 48dp minimum touch target — and include a `@PreviewLightDark` preview.

## Theme tokens

Colors come from `core/theme/Color.kt`. Never hardcode a `Color(0x...)` in a feature screen; add or reuse a token.

Spacing comes from `HelloSpacing`. The screen gutter is `HelloSpacing.screenGutter` — do not re-declare a local padding constant for it.

Design direction, and the rationale behind the color rules, live in `docs/DESIGN_BRIEF.md`.
