# Current Study

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Study` flow |
| Source of Truth | No |
| Read this when | You need to understand how the study session works today |

## Summary

The study session shows every due flashcard exactly once. Each `StudyFlashcard` maps to one `StudySessionItem`, the user reveals the back and grades it, and the review is scheduled with FSRS-6 and persisted on the spot. There is no start interstitial, no typed answer and no exit confirmation.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyTop.kt` (private chrome)
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyUiEffect.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudySessionItem.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/CardFace.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/FlippableCard.kt`

## Session item

`StudySessionItem` is a 1:1 projection of `StudyFlashcard` onto the fields the card faces render:

- `flashcardId`, `review: FsrsCard`
- `word`, `phonetic`, `meaning`, `translation`
- `usagePattern`, `irregularForms`

`StudyFlashcard.toStudySessionItem()` builds it. The flashcard's generated study cards (`studyCards`) are not part of the session: a card with several generated study cards and a card with none are both one item and one grade.

## Current state

`StudyUiState` holds:

- `isLoading: Boolean = true` — session load in flight
- `loadError: StudyLoadError?` — `SessionLoadFailed`; distinguishes a failed read from a genuinely empty due queue
- `currentItem: StudySessionItem?`
- `reviewedCount` — cards graded so far
- `totalCount` — cards in the session (equals the due count the Dashboard shows)
- `sessionFinished`
- `intervalPreviews: Map<ReviewGrade, Long>` — interval previews shown under each grade chip
- `isGradeHintVisible: Boolean` — first-session grade hint card

## Session load

`StudyViewModel` is constructed with a `deckId: String`. `StudyRoute.ALL_DUE_DECKS` (`"__all_due_decks__"`) is the sentinel Koin receives for the all-decks session; the viewmodel normalizes it back to `null`.

`loadSession()` (called from `init`, and again by `RetryLoad`):

- clears the queue and sets `isLoading = true`, `loadError = null`, `reviewedCount = 0`
- fetches via `studySessionRepository.sessionTodayAllDecks()` when the target is `null`, otherwise `studySessionRepository.sessionToday(deckId.toDeckId())`
- maps each `StudyFlashcard` to one `StudySessionItem` and queues them in an `ArrayDeque`
- sets `totalCount` to the number of cards and computes `isGradeHintVisible` from `onboardingState.hasSeenGradeHint()`
- shows the first card
- on any throwable (except `CancellationException`) logs and sets `loadError = SessionLoadFailed`

## Grading and persistence

`ReviewAnswered(item, grade)`:

- schedules a new `FsrsCard` with `ScheduleFlashcardReviewUseCase(item.review, grade, item.flashcardId)`
- persists it with `flashcardReviewRepository.update(newCard, grade)` — immediately, with the grade as given
- increments `reviewedCount` and shows the next card

`SessionFinished` is emitted once there is no next card to show and the session had at least one card. An empty session never finishes; it renders the empty state instead.

## Visual stages

`StudyScreen` uses a private `StudyStage` enum, resolved in this order:

1. `Loading` — `state.isLoading`
2. `Error` — `state.loadError != null`
3. `Empty` — no `currentItem`
4. `Recall` — card face is Front
5. `Grade` — card face is Back

`Loading` renders `StudyLoadingState()`, `Error` renders `StudyErrorState()` with a retry CTA wired to `RetryLoad`, `Empty` renders `StudyEmptyState()` (mascot + "Hoy no toca repasar.").

The `StudyTop` counter reads `position/total` for the card on screen ("3/10") and is hidden outside `Recall`/`Grade`. State labels: `RECORDAR` (Recall) and `RESPUESTA` (Grade).

## Card faces

- **Front**: the word in Instrument Serif plus the IPA in Geist Mono. Tapping the card or the "Ver respuesta" CTA flips it.
- **Back**: mono `SIGNIFICADO` eyebrow, the translation (serif, primary), the IPA, the English meaning (italic serif, secondary), irregular forms inline when present ("Formas relacionadas: went, gone") and `usagePattern` as a supporting line when present.

A static mono `INGLÉS → ESPAÑOL` overlay sits top-left of the card; the TTS button top-right speaks the word.

## Grade dock

In `Grade`, `StudyActionDock` renders `AnswerButtons`: four `GradeChip`s (`AGAIN` / `HARD` / `GOOD` / `EASY`) with their interval preview from `intervalPreviews`, plus a mono "Desliza para calificar rápido" hint. `FlippableCard` also grades by horizontal swipe on the back face. The first-session `GradeHintCard` explains the four buttons and is dismissed by `GradeHintDismissed` or by the first answer.

## Navigation and exit

- the X button and system back both fire `ExitClicked`, which always emits `NavigateBack`. Every grade is already persisted, so leaving mid-session loses nothing and asks for no confirmation.
- when the last card is graded, `SessionFinished` is emitted and the route shows the finish dialog
- closing the final dialog fires `FinishDialogDismissed`, which emits `NavigateBack`

### Session-finished dialog

`SessionFinishedDialog` (private composable in `StudyScreen.kt`) renders inside a `Dialog` with `emberElev` surface and shows:

- the celebrating mascot
- mono eyebrow `session_completed_eyebrow`
- serif headline `session_completed_title` ("Listo.")
- italic serif subtitle `session_completed_desc` ("Repasaste N tarjetas.")
- a stats row on `emberSurface` with the total count in `emberAccent` plus a mono label
- a full-width `HButton` (`Accent` variant) as the "Volver" CTA, which calls `onDismiss`

## Empty stage CTA

When `StudyStage.Empty`, `StudyActionDock` renders a full-width `HButton` (Secondary / Lg) with label `study_empty_create_card_cta` ("Crear una tarjeta"). Tapping it fires `StudyUiIntent.CreateCardClicked`.

## Current intents

`StudyUiIntent`:

- `FinishDialogDismissed` — emits `NavigateBack`
- `CreateCardClicked` — emits `NavigateToNewCard`
- `GradeHintDismissed` — marks the hint seen and hides it
- `RetryLoad` — re-runs `loadSession()`
- `ExitClicked` — emits `NavigateBack`
- `ReviewAnswered(item, reviewGrade)`

The card face (`Front`/`Back`) is local `StudyScreen` state, reset whenever `currentItem` changes.

## Current effects

`StudyUiEffect` currently exposes:

- `NavigateBack`
- `SessionFinished`
- `NavigateToNewCard` — collected in `StudyDestination`; navigates to `NewCardRoute`
