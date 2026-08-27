---
paths:
  - "**/*.kt"
---

# Naming rules

Three combined sources: Uncle Bob (Clean Code), official Kotlin, and professional Android practice. On conflict, that is the priority order.

## Uncle Bob, applied to Kotlin/Android

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

A name that needs a comment to be understood is the wrong name. Rename it instead of explaining it — see `kotlin-style.md`, comments.

## Official Kotlin

- **Classes / objects / interfaces**: `PascalCase` — `DashboardViewModel`, `MviState`
- **Functions / properties**: `camelCase` — `loadDeck()`, `isLoading`
- **Constants** (`const val`, companion, top-level): `SCREAMING_SNAKE_CASE` — `SEARCH_DEBOUNCE_MS`
- **Packages**: `lowercase.nounderscores`
- **Backing properties**: `_` prefix + same name — `_state` / `state`
- **Lambdas**: use `it` only if the context is obvious in 2 lines or fewer; otherwise name the parameter
- Prefer `val` over `var`; prefer extension functions over utility classes

## Patterns by layer

| Type | Pattern | Examples |
|---|---|---|
| ViewModel | `<Feature>ViewModel` | `DashboardViewModel` |
| UseCase | `<Verb><Subject>UseCase` | `GetDecksUseCase`, `ScheduleFlashcardReviewUseCase` |
| Repository (interface) | `<Entity>Repository` | `DeckRepository`, `FlashcardReviewRepository` |
| Repository (impl) | `<Entity>RepositoryImpl` or `<Source><Entity>Repository` | `SqlDelightDeckRepository` |
| UiState | `<Feature>UiState` — data class, all fields `val` | `DashboardUiState` |
| UiIntent | `<Feature>UiIntent` — sealed interface, past tense or noun-verb | `QueryChanged`, `TagToggled`, `SaveClicked` |
| UiEffect | `<Feature>UiEffect` — sealed interface, describes the effect | `NavigateBack`, `ShowMessage` |
| Exposed Flow/StateFlow | name without the `Flow` suffix | `val decks: Flow<List<Deck>>`, not `decksFlow` |
| Booleans | `is`, `has`, `can`, `should` prefix | `isLoading`, `hasSession`, `canSave` |
| Callbacks in a Composable | `on` prefix | `onIntent`, `onClick`, `onDismiss` |
| Suspend fun | name it as if it were synchronous | `fetchById()`, not `fetchByIdSuspend()` |

## Additional rules

- `UiIntent` names describe **what the user did**, not what the ViewModel should do: `DeleteClicked`, not `TriggerDelete`.
- `UiEffect` describes **the resulting effect**, not the action: `NavigateBack`, not `GoBack`.
- A private ViewModel function takes the `handle` prefix only when it groups several sub-cases. If it does one thing, name it directly: `loadDeck()`, not `handleLoadDeck()`.
- Avoid redundant prefixes inside a scope: inside `DeckDetailViewModel`, `loadDeck()`, not `loadDeckDetail()`.
