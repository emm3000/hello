---
paths:
  - "**/*.kt"
---

# Architecture rules

Clean Architecture in three modules. `ARCHITECTURE.md` describes what the structure *is* today; this file states what new code *must* obey.

## Modules and dependency direction

| Module | Contains |
|---|---|
| `:domain` | Pure Kotlin. Models, value objects, use cases, and the **interfaces** the outer layers implement. |
| `:data` | Implementations of the domain interfaces: SQLDelight, Firebase AI, local identity, backup, mappers. |
| `:app` | Presentation. MVI features, Compose UI, navigation, Koin wiring, startup. |

Allowed dependencies, and nothing else:

```
app -> data
app -> domain
data -> domain
```

- `:domain` is **JVM-only**. No Android imports, no SQLDelight, no Firebase, no network, no `Context`. If a use case needs the current time, it takes a `Clock`; it does not call `System.currentTimeMillis()`.
- `:data` never depends on `:app`.
- `HelloDb` is the source of truth for reads and writes.

## Dependency inversion is the seam

The domain declares the contract; the infrastructure obeys it. The domain never imports an implementation.

Reference implementation already in the repo: `GenerationQuota` is an interface in `domain/generation/`, and `DailyGenerationQuota` implements it in `data/flashcard/` on top of `SharedPreferences`. The domain knows there is a quota; it does not know it is stored in preferences.

Repository interfaces live in `:domain`. Implementations live in `:data`.

## Each layer owns its own model

A database entity, a network or AI DTO, a domain model and a `UiState` are four different things even when their fields match. Mappers convert between them — see `FlashcardMappers.kt`, `DeckMappers.kt`.

- A SQLDelight row never reaches a `UiState`.
- An AI DTO never reaches `:domain`.
- A domain model never carries presentation concerns (formatted strings, resource ids, colors).

This is not duplication to be removed. See `principles.md`, DRY.

## MVI contract

Naming lives in `naming.md`. This is the flow.

- **One state object per feature.** `<Feature>UiState` is a `data class` with every field `val`. There is no second source of screen state.
- **One public entry point.** The ViewModel exposes `onIntent(intent: <Feature>UiIntent)`. Nothing else is public except the state and effect streams.
- **State is a `StateFlow`, effects are one-shot.** Effects (navigation, toasts, dialogs) are emitted once and consumed once. An effect is never stored in `UiState`, because state replays on recomposition and configuration change and would fire the effect twice.
- **The screen is stateless.** `<Feature>Screen` receives the state and a lambda that dispatches intents. It never holds business state, never touches a repository, never touches a use case, never injects a ViewModel.
- **The route wires everything.** `<Feature>Route` obtains the ViewModel through Koin, collects state, passes the dispatch lambda down, and consumes effects.
- **Intents describe what the user did**, not what the ViewModel should do. Local UI state that has no business meaning — a card's flipped face, an expanded section — may stay as `remember` inside the composable.

## When a new dependency crosses a layer

Before adding a dependency to any module, check the direction above. If the change needs `:domain` to reach outward, the design is wrong: invert it with an interface in `:domain`.
