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

## Theme tokens are the style guide

`core/theme/Color.kt`, `Type.kt` and `Foundation.kt` are the single source of truth for every visual value. There is no separate style-guide document holding hex codes, and none should be created — a second copy drifts.

- Never hardcode a `Color(0x...)`, a `.sp` size or a `.dp` padding in a feature screen. Add or reuse a token.
- Spacing comes from `HelloSpacing`; the screen gutter is `HelloSpacing.screenGutter`. Do not re-declare a local constant for it.
- Shapes come from `HelloShapes`: `control` for buttons, inputs and chips, `container` for cards and sheets, `pill` where a full round is meant.

What tokens cannot express are the usage rules below. Those bind too.

## Color usage

Rationale for all three lives in `docs/DESIGN_BRIEF.md`; do not re-derive it here.

1. **One accent job per screen.** The accent marks the primary action — the Hoy CTA, "Ver respuesta", "Guardar", the active tab, a selected chip. If it appears in several unrelated places on one screen it marks nothing.
2. **The study back face carries no accent at all.** At the moment of self-grading nothing may stand out. The session progress bar is a faint neutral, never the accent, so it cannot compete with the primary action.
3. **Semantic colors are for system states, not for the user's recall.** Success, warning and destructive belong to load errors, destructive actions and warnings. They never score an answer.

## Grade buttons

The two grade buttons are a fixed anatomy, not a styling choice:

- Neither is red nor green, and neither is the accent.
- They differ by fill weight and position only: the left one ("No la sabía") sits on the page background with a hairline border, the right one ("La sabía") on the raised surface with a medium-weight label.
- Both are full-height touch targets of at least 56dp.
- `EASY` is a long-press on the right button. It gets no third button and no separate color.

## Never

- Raw Material3 in a feature screen.
- A literal color, size or radius outside `core/theme/`.
- Red or green as the carrier of right/wrong.
- An illustrated mascot. The redesign removed it; it does not come back through an empty state.
- Emoji as an icon. Icons are vector assets or drawn paths.
