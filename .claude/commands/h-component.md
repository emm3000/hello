---
description: Scaffold a shared H* component in core/ui following Input.kt/FieldShell.kt patterns
argument-hint: <ComponentName>
---

Create a shared component named **H$ARGUMENTS** in `app/src/main/kotlin/com/emm/hello/core/ui/`.

## Before creating

1. Read `app/src/main/kotlin/com/emm/hello/core/ui/Input.kt` and `FieldShell.kt` — they are the canonical templates.
2. Verify `H$ARGUMENTS` does not already exist in `core/ui/`.
3. Confirm with me the expected usage (is it app-wide or better kept inside a feature?).

## Requirements

- Name with `H` prefix (`HSearchBar`, `HChip`, `HTagInput`, etc.).
- shadcn-style pattern: animated border, transparent background, minimum height 48dp.
- Minimal public API — no escape hatches to raw Material3.
- Pure composable: no ViewModel, no internal side effects.
- Include `@PreviewLightDark` with at least one base state.

## After creating

- Run `./gradlew :app:compileDebugKotlin` to verify.
- If it makes sense, leave a note in `core/ui/` on when to use it vs. existing components.

Do not modify Material3 imports in existing features in this turn — that is a separate scope.
