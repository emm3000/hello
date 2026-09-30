---
name: feature
description: Scaffold a new MVI feature under app/newfeatures following the repo rules
argument-hint: <FeatureName>
allowed-tools: Read Edit Write Grep Glob Bash(ls:*) Bash(./gradlew:*)
disable-model-invocation: true
---

Scaffold a new feature called **$ARGUMENTS** under `app/src/main/kotlin/com/emm/hello/newfeatures/<lowercase-name>/`. Use the lowercase name for both directory and package, and PascalCase for Kotlin types.

## Before creating anything

1. Confirm the feature name is PascalCase and not already used (`ls app/src/main/kotlin/com/emm/hello/newfeatures/`).
2. Read 1-2 existing features to copy idiomatic patterns (state shape, intent grouping, route DI).
3. State which existing feature guides the implementation and proceed. Ask only if missing product behavior materially changes the result. Read the applicable repo rules before adapting the templates.

## Files to create

Copy each template from `${CLAUDE_SKILL_DIR}/templates/<file>` into the new feature directory, drop the `.template` suffix and substitute every placeholder:

| Placeholder | Replace with |
|---|---|
| `__Feature__` | the PascalCase name **$ARGUMENTS** |
| `__feature__` | the same name lowercased, used as the package segment |

The templates are the minimal shape: one state field, one intent, one effect. Grow them to fit the feature — extra `val`s on the state, extra intents in the `sealed interface`, extra effects — without changing the idioms they encode (`MviViewModel` base, `state`/`effect` exposure, `onIntent` as the single entry point, `koinViewModel()` in the route, `@PreviewLightDark` in the screen).

Placeholder copy in the screen is a literal string. Move it to `values/strings.xml` and `values-es/strings.xml` and read it with `stringResource` before the feature ships.

## Templates

- `templates/__Feature__UiState.kt.template` — `data class` with initial values, no logic.
- `templates/__Feature__UiIntent.kt.template` — `sealed interface` covering user actions.
- `templates/__Feature__UiEffect.kt.template` — `sealed interface` for one-shot effects (navigation, toasts).
- `templates/__Feature__ViewModel.kt.template` — exposes `state: StateFlow<UiState>`, `effect: Flow<UiEffect>`, public entry point `onIntent(intent: UiIntent)`. Injectable via Koin.
- `templates/__Feature__Route.kt.template` — Compose entry that wires the ViewModel, observes state, dispatches intents, consumes effects.
- `templates/__Feature__Screen.kt.template` — stateless `@Composable` that receives state + lambda for intent dispatch.

## Wiring

- Register the ViewModel in `app/src/main/kotlin/com/emm/hello/di/NewModule.kt`: `viewModel { $ARGUMENTSViewModel() }`, one `get()` per constructor dependency.
- Register the destination in `app/src/main/kotlin/com/emm/hello/newfeatures/NewRoot.kt`: `entry<$ARGUMENTSRoute> { $ARGUMENTSDestination(navigator) }`.

## Hard rules (from `CLAUDE.md` and `.claude/rules/`)

- Use `core/ui/H*` controls; layout primitives, theme access and non-interactive `Surface` containers are allowed. Follow `ui-components.md`.
- `domain` stays JVM-only — this feature lives in `:app`.
- Nesting ≤ 3, no nested `also/apply/run/let`, ≤ 5 returns per function.
- Include `@PreviewLightDark` in `Screen.kt`.

After wiring the files, run `./gradlew :app:compileDebugKotlin` and the applicable checks in `.claude/rules/verification.md`. Report commands and results; do not commit automatically.
