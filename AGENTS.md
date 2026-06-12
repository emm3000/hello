# AGENTS.md

| Field | Value |
|---|---|
| Status | Active |
| Role | Operating guide for agents |
| Source of Truth | Yes |
| Read this when | You're about to edit code or documentation in the repo |

## Mandatory state

The product runs in local-first single-device mode.

Don't assume:

- active remote sync
- active pairing
- active remote bootstrap

## Sources to read

1. `README.md`
2. `ARCHITECTURE.md`
3. `LOCAL_FIRST.md`
4. `docs/README.md`

## Repo rules

- modules: `:app`, `:data`, `:domain`
- dependencies: `app -> data`, `app -> domain`, `data -> domain`
- `domain` stays JVM-only
- `HelloDb` is the source of truth

## Current startup

- `App -> Koin -> AppStartupCoordinator.start()`
- startup only initializes local install identity

## Feature conventions

- MVI per feature with `UiState`, `UiIntent`, `UiEffect`
- public entry point: `onIntent(intent)`
- naming in `app/src/main/kotlin/com/emm/hello/newfeatures/`: `*ViewModel`, `*Route`, `*UiState`, `*UiIntent`, `*UiEffect`

## Shared UI components (core/ui)

### Naming convention

- **Public composables**: `H` prefix (`HInput`, `HButton`, `HBadge`, `HCard`, etc.). This is the hard rule. Never call raw Material3 components from feature screens.
- **Files**: mixed — 12 files use the `H` prefix (`HTopBar.kt`, `HSearchBar.kt`, `HEmptyState.kt`, etc.) and 15 do not (`Button.kt`, `Input.kt`, `Card.kt`, etc.). When touching an existing component, follow the file-naming pattern already established for that component family. New standalone shared components use `H`-prefixed files (e.g. `HFoo.kt`).
- **Exception without `H*` composable**: `FieldShell` — internal building block, template for inputs.

### Hierarchy for new designs

1. **First**: check `app/src/main/kotlin/com/emm/hello/core/ui/`. If the component you need exists, USE IT no exceptions. The components are inspired by shadcn/ui and define the app's theme.

2. **Local vs shared**:
   - scope = single screen → create in the feature (`newfeatures/X/`)
   - scope = app-wide → create in `core/ui/`

3. **If it doesn't exist**:
   - Create an `H*` composable in `core/ui/` (`HSearchBar`, `HChip`, `HTagInput`, etc.).
   - **Do NOT use** raw Material3 components (`OutlinedTextField`, `Button`, `TextField`, etc.).
   - Template: `Input.kt` + `FieldShell.kt` (animated border, transparent background, 48dp minimum height).
   - Include `PreviewLightDark` preview.

**Iron rule**: a custom component in a screen NEVER replaces a `core/ui` component that exists for that purpose. If the `core/ui` one doesn't fit, extend or modify it first.

## Naming conventions

Three combined sources: Uncle Bob (Clean Code), official Kotlin, and professional Android practice (GDE tier). In case of conflict, this order is the priority.

### Uncle Bob principles — applied to Kotlin/Android

| Rule | Bad | Good |
|---|---|---|
| Name reveals intent | `d`, `data`, `tmp` | `deckId`, `filteredDecks`, `elapsedMs` |
| No disinformation | `deckList` (it's a `List`) | `decks` |
| Meaningful distinction | `getDeck` vs `fetchDeck` vs `retrieveDeck` | one verb per concept |
| Pronounceable | `genDtTmStmp` | `generatedAt` |
| Searchable (no magic literals) | `if (type == 2)` | `if (type == CardType.CLOZE)` |
| Classes: nouns | `DataProcessor`, `Manager` | `FlashcardRepository`, `DeckDetailViewModel` |
| Functions: verbs | `card()`, `data()` | `loadCard()`, `buildState()` |
| One word per concept | `fetch` in one place, `get` in another | pick one and use it across the codebase |
| No humor or jargon | `whack()`, `eatMyShorts()` | `delete()`, `clear()` |

### Official Kotlin

- **Classes / objects / interfaces**: `PascalCase` — `DashboardViewModel`, `MviState`
- **Functions / properties**: `camelCase` — `loadDeck()`, `isLoading`
- **Constants** (`const val`, companion, top-level): `SCREAMING_SNAKE_CASE` — `SEARCH_DEBOUNCE_MS`
- **Packages**: `lowercase.nounderscores`
- **Backing properties**: `_` prefix + same name — `_state` / `state`
- **Lambdas**: use `it` only if context is obvious in ≤ 2 lines; otherwise explicit name
- Prefer `val` over `var`; prefer extension functions over utility classes

### Professional Android / Kotlin

**Naming patterns by layer:**

| Type | Pattern | Examples |
|---|---|---|
| ViewModel | `<Feature>ViewModel` | `DashboardViewModel` |
| UseCase | `<Verb><Subject>UseCase` | `GetDecksUseCase`, `ScheduleFlashcardReviewUseCase` |
| Repository (interface) | `<Entity>Repository` | `DeckRepository`, `FlashcardReviewRepository` |
| Repository (impl) | `<Entity>RepositoryImpl` or `<Source><Entity>Repository` | `SqlDelightDeckRepository` |
| UiState | `<Feature>UiState` — data class, all fields `val` | `DashboardUiState` |
| UiIntent | `<Feature>UiIntent` — sealed interface, names in **past tense or noun-verb** | `QueryChanged`, `TagToggled`, `SaveClicked` |
| UiEffect | `<Feature>UiEffect` — sealed interface, names describing the effect | `NavigateBack`, `ShowMessage` |
| Exposed Flow/StateFlow | name without `Flow` suffix | `val decks: Flow<List<Deck>>` not `val decksFlow` |
| Booleans | `is`, `has`, `can`, `should` prefix | `isLoading`, `hasSession`, `canSave` |
| Callbacks / lambdas in Composable | `on` prefix | `onIntent`, `onClick`, `onDismiss` |
| Suspend fun | name as if synchronous | `fetchById()` not `fetchByIdSuspend()` |

**Additional rules:**

- `UiIntent` names describe **what the user did**, not what the ViewModel should do: `DeleteClicked`, not `TriggerDelete`.
- `UiEffect` describes **the resulting effect**, not the action: `NavigateBack`, not `GoBack`.
- Private functions in a ViewModel that handle an intent take the `handle` prefix only if they group logic for several sub-cases; if they do one thing, use a direct name: `loadDeck()`, not `handleLoadDeck()`.
- Avoid redundant prefixes within a scope: inside `DeckDetailViewModel`, `loadDeck()` not `loadDeckDetail()`.

## Linting and code style (detekt)

Active rules in `config/detekt/detekt.yml`:

### Complexity — avoid callback hell and deep nesting

```yaml
CyclomaticComplexMethod:
  active: true
  threshold: 10
  ignoreSingleWhenExpression: true
  ignoreSimpleWhenEntries: true
  nestingFunctions:
    - 'also'
    - 'apply'
    - 'run'
    - 'let'
    - 'use'
    - 'with'
```

**Why**: These functions are the ones that generate callback hell. If you see methods with many `also { apply { run { ... } } }`, refactor with intermediate functions or early return.

### Style — early return and moderate returns

```yaml
ReturnCount:
  active: true
  max: 5
  excludeLabeled: true       # labeled returns no cuentan (return@mapNotNull)
  excludedFunctions:
    - 'equals'
  ignoreAnnotated:
    - 'Composable'
```

**Why**: More than 5 returns is confusing. Use:
- Early returns in guard clauses (validation, null-checks)
- Labeled returns in lambdas (`return@mapNotNull null`) for short-circuit
- Extract logic into private functions if a method has many branches

### Iron rule for new code

Before committing, check:
1. More than 3 levels of nesting? → extract function
2. Many chained `else if`? → use `when` or extract functions
3. Function doing many things? → split into smaller functions
4. Lambdas with nested `also/apply/run/let`? → refactor with intermediate functions

## Current toolchain

- Java 17
- AGP `9.2.1`
- Kotlin `2.3.21`
- compileSdk `36`

## Commits

- **Do not add `Co-Authored-By` from Claude, Anthropic or any AI assistant** in commit messages. Commits are signed only by the human author. Applies to `git commit`, `git commit --amend`, rebases and any auto-generated message flow.

## Final rule

If a doc contradicts the current code, the code wins and the doc gets updated afterwards.
