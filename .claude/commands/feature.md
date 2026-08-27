---
description: Scaffold a new MVI feature under app/newfeatures following the repo rules
argument-hint: <FeatureName>
---

Scaffold a new feature called **$ARGUMENTS** under `app/src/main/kotlin/com/emm/hello/newfeatures/$ARGUMENTS/`.

## Before creating anything

1. Confirm the feature name is PascalCase and not already used (`ls app/src/main/kotlin/com/emm/hello/newfeatures/`).
2. Read 1-2 existing features to copy idiomatic patterns (state shape, intent grouping, route DI).
3. Confirm with me which existing feature you used as the template.

## Files to create

- `${ARGUMENTS}UiState.kt` — `data class` with initial values, no logic.
- `${ARGUMENTS}UiIntent.kt` — `sealed interface` covering user actions.
- `${ARGUMENTS}UiEffect.kt` — `sealed interface` for one-shot effects (navigation, toasts).
- `${ARGUMENTS}ViewModel.kt` — exposes `state: StateFlow<UiState>`, `effects: Flow<UiEffect>`, public entry point `onIntent(intent: UiIntent)`. Injectable via Koin.
- `${ARGUMENTS}Route.kt` — Compose entry that wires the ViewModel, observes state, dispatches intents, consumes effects.
- `${ARGUMENTS}Screen.kt` — stateless `@Composable` that receives state + lambda for intent dispatch.

## Hard rules (from `CLAUDE.md` and `.claude/rules/`)

- UI uses **only** `core/ui/H*` components (`HInput`, `HButton`, etc.). **Never** raw Material3.
- `domain` stays JVM-only — this feature lives in `:app`.
- Nesting ≤ 3, no nested `also/apply/run/let`, ≤ 5 returns per function.
- Include `@PreviewLightDark` in `Screen.kt`.

After creating the files, run `./gradlew :app:compileDebugKotlin` to verify it compiles.
