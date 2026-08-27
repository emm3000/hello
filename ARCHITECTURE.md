# Current Architecture

| Field | Value |
|---|---|
| Status | Active |
| Role | Project technical structure |
| Source of Truth | Yes |
| Read this when | You need to understand modules, boundaries or current wiring |

## Stack

- Kotlin
- Jetpack Compose
- Koin
- SQLDelight (`HelloDb`)
- Firebase AI with `gemini-2.5-flash-lite`

## Toolchain

- Java 17
- AGP `9.2.1`
- Kotlin `2.4.0`
- compileSdk `37` / targetSdk `36` / minSdk `26`

## Modules

### `app`

- screens and navigation
- MVI viewmodels
- Koin wiring
- startup

### `data`

- concrete repositories
- local persistence with SQLDelight
- local install identity
- content generation with Firebase AI

### `domain`

- models and use cases
- no Android
- no DB
- no network

## Dependencies

- `app -> data`
- `app -> domain`
- `data -> domain`

## Navigation

- Entry composable: `NewRoot` in `app/src/main/kotlin/com/emm/hello/newfeatures/NewRoot.kt`.
- Stack: Jetpack Navigation 3 (`NavDisplay`, `rememberNavBackStack`).
- Backstack wrapper: `Navigator` class (`app/.../navigation/Navigator.kt`).
- Transitions: horizontal slide (350ms) for push/pop/predictive-pop.
- Active routes: `OnboardingRoute`, `DashboardRoute`, `StudyRoute(deckId: String? = null)` (`null` = study every due card across all decks), `NewCardRoute`, `NewDeckRoute(deckId)`, `DeckDetailRoute(deckId)`, `CardDetailRoute(cardId, deckId)`, `EditFlashcardRoute(cardId, deckId)`, `SettingsRoute`.
- Decorators: `rememberSaveableStateHolderNavEntryDecorator` + `rememberViewModelStoreNavEntryDecorator`.
- Startup gate: `NewRoot` observes `AppStartupViewModel` and shows loading/error before `AppNavigation`.

## Current startup

Current flow:

`App -> Koin -> AppStartupCoordinator.start() -> LocalIdentityInitializer.ensureReady() -> SeedDataInitializer.ensureSeeded()`

On success the coordinator emits `AppStartupState.Ready(hasSeenWelcome)`, reading the flag from `OnboardingStateRepository.hasSeenWelcome()`; that flag decides whether `NewRoot` starts on `OnboardingRoute` or `DashboardRoute`. On failure it emits `AppStartupState.Error`.

There are no other mandatory product stages in startup, and none of them requires the network.

## Current persistence

- `HelloDb` is the source of truth
- decks, flashcards and reviews are read from local state
- `DefaultFlashcardReviewRepository` persists `ReviewEvent` and `ReviewProjection`

### SQLDelight migrations

- Current schema version is **2**. Snapshots live in `data/src/main/sqldelight/databases/`: `1.db` (the original baseline) and `2.db` (the current schema). Both must stay committed.
- Migrations live next to the `.sq` files in `data/src/main/sqldelight/com/emm/data/`. Today there is one: `1.sqm` (v1 -> v2), which adds the FSRS-6 columns (`state`, `stability`, `difficulty`, …) to `ReviewProjection`/`ReviewEvent` additively and seeds them from the legacy SM-2 columns without changing any `nextReviewAt`.
- `verifyMigrations = true` in `data/build.gradle.kts`: every PR that modifies `.sq` must regenerate the corresponding `.db` and add an `N.sqm` with the required `ALTER`/`CREATE`.
- Schema change policy:
  1. Edit the `.sq` with the change.
  2. Create `data/src/main/sqldelight/com/emm/data/N.sqm` (where `N` is the current version before the bump) with idempotent SQL that migrates `v(N)` -> `v(N+1)`.
  3. Run `./gradlew :data:generateDebugHelloDbSchema` to produce `(N+1).db`.
  4. Validate with `./gradlew :data:verifySqlDelightMigration`.
- Never delete previous `.db` files: they are the source for `verifyMigrations`.

## Features relevant today

- card creation with editable preview and partial regenerations
- study shows each due flashcard once: `StudySessionItem` is a 1:1 projection of `StudyFlashcard`
- one review per flashcard, scheduled with FSRS-6 and persisted the moment the card is graded

## Preserved seam

The only explicit seam for a possible return of the remote is local identity:

- `LocalIdentityInitializer`
- `LocalIdentityState`
- `deviceId`

## See also

- `LOCAL_FIRST.md`
- `docs/CARD_CREATION_CURRENT.md`
- `docs/STUDY_CURRENT.md`
