# Current Store

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Store` flow (install curated decks) |
| Source of Truth | No |
| Read this when | You need to understand how curated decks are listed and installed |
| Last verified | 2026-09-29 |

## Summary

The Store lists hand-made curated decks compiled into `:data` and lets the user install them into `HelloDb`. There is no remote fetch. Installing creates a local deck and adds every note through `CreateFlashcardUseCase`, so curated cards pass the same validation as any other card. The installed deck id is deterministic (`curated-<id>`), which makes installing idempotent: a second install, or one that resumes a partial install, adds only the missing cards.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/store/StoreRoute.kt` (route + `StoreDestination`)
- `app/src/main/kotlin/com/emm/hello/newfeatures/store/StoreScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/store/StoreDeckCard.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/store/StoreDeckItem.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/store/StoreViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/store/StoreUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/store/StoreUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/store/StoreUiEffect.kt`
- `data/src/main/kotlin/com/emm/data/curated/BundledCuratedDeckCatalog.kt` (the catalog)

## :domain / :data dependencies

- `com.emm.domain.curated.CuratedDeckCatalog` → `com.emm.data.curated.BundledCuratedDeckCatalog`
- `com.emm.domain.curated.GetCuratedDecksUseCase`
- `com.emm.domain.curated.InstallCuratedDeckUseCase`
- `com.emm.domain.deck.DeckRepository` and `com.emm.domain.authoring.CreateFlashcardUseCase`, used by both use cases

## Catalog

`BundledCuratedDeckCatalog` returns five decks: `FirstCallsDeck`, `SpanishTrapsDeck`, `JobInterviewDeck`, `TechInterviewDeck` and `DailyStandupDeck`. Each `CuratedDeck` carries `id`, `name`, `description`, `tags`, `levelBand` and its `notes`.

## Loading

`GetCuratedDecksUseCase()` maps `deckWithFlashcardCount()` to one `CuratedDeckListing` per catalog deck. A deck counts as installed when its `curated-<id>` id is among the live decks, so deleting the installed deck makes it installable again. Each emission clears `isLoading`.

## State

`StoreUiState`:

- `isLoading: Boolean` (initially `true`)
- `decks: List<StoreDeckItem>`
- `installingDeckId: String?`, the curated deck being installed

`StoreDeckItem` holds `id`, `name`, `description`, `tags`, `levelBand`, `cardsCount` (the number of notes) and `installedDeckId`, with a computed `isInstalled`.

## Intents

- `BackRequested` → emits `NavigateBack`
- `OpenDeckRequested(deckId)` → emits `OpenDeck(deckId)`
- `InstallRequested(curatedDeckId)` → ignored if that deck is already installing; otherwise sets `installingDeckId` and calls `InstallCuratedDeckUseCase`. Success clears `installingDeckId`. Failure logs, clears `installingDeckId` and emits `ShowMessage(store_install_failed)`.

`installingDeckId` tracks one deck: starting a second install while the first runs moves the loading state to the second deck.

## Install

`InstallCuratedDeckUseCase(curatedDeckId)`:

1. Looks the deck up in the catalog; an unknown id throws `UnknownCuratedDeckException`.
2. Creates the deck with id `curated-<id>` unless `fetchById` already finds it.
3. Calls `CreateFlashcardUseCase` for each note. A `DomainValidationException` with `DuplicateExactCardInDeck` is skipped; any other failure propagates.

## Effects

`StoreUiEffect`, consumed in `StoreDestination`:

- `NavigateBack` → `navigator.goBack()`
- `OpenDeck(deckId)` → `LibraryRoute(initialDeckId = deckId)`, Library filtered to that deck
- `ShowMessage(messageRes)` → a long Toast

## Layout

`HTopBar` titled "Store" with a back button. While loading, a centered `HLoadingSpinner`. Otherwise a list with a header (`store_headline` "Curated decks" and `store_intro`) and one `StoreDeckCard` per deck. There is no empty or error state; install errors surface only as the Toast.

`StoreDeckCard` is an `HCard` with the name, the description when not blank, a footer row (an "Installed" marker when installed, the card count and the level band), the tags line (hidden once installed) and a full-width secondary `HButton`: "Install" with a download icon, disabled and loading while installing, or "Open deck" once installed.

## Navigation

The only entry is the storefront action in the `Decks` top bar (`DecksUiIntent.StoreRequested` → `DecksUiEffect.OpenStore`, see `DECK_CURRENT.md`). Exits are back and "Open deck".
