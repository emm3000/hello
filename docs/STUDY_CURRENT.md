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
- `partOfSpeech`, `example`, `exampleTranslation`

`StudyFlashcard.toStudySessionItem()` builds it. The flashcard's generated study cards (`studyCards`) are not part of the session: a card with several generated study cards and a card with none are both one item and one grade.

## Current state

`StudyUiState` holds:

- `isLoading: Boolean = true` — session load in flight
- `loadError: StudyLoadError?` — `SessionLoadFailed`; distinguishes a failed read from a genuinely empty due queue
- `currentItem: StudySessionItem?`
- `reviewedCount` — cards graded so far
- `totalCount` — cards in the session (equals the due count the Dashboard shows)
- `sessionFinished`

## Session load

`StudyViewModel` is constructed with a `deckId: String`. `StudyRoute.ALL_DUE_DECKS` (`"__all_due_decks__"`) is the sentinel Koin receives for the all-decks session; the viewmodel normalizes it back to `null`.

`loadSession()` (called from `init`, and again by `RetryLoad`):

- clears the queue and sets `isLoading = true`, `loadError = null`, `reviewedCount = 0`, `sessionFinished = false`
- fetches via `studySessionRepository.sessionTodayAllDecks()` when the target is `null`, otherwise `studySessionRepository.sessionToday(deckId.toDeckId())`
- maps each `StudyFlashcard` to one `StudySessionItem` and queues them in an `ArrayDeque`
- sets `totalCount` to the number of cards
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

- **Front**: the word, then the IPA in mono. Tapping the card or the "Ver respuesta" CTA flips it.
- **Back**: two beats first, then reference. The `translation` alone, then `example` with the target word emphasised by weight (`highlightWordInExample`) over a muted `exampleTranslation`. Below that, everything optional and muted: a mono `IPA · partOfSpeech` line, `meaning`, irregular forms as a mono line ("Formas relacionadas: went, gone") and `usagePattern`. No accent color appears on this face, and nothing on it is emphasised by color.

A static mono `INGLÉS → ESPAÑOL` overlay sits top-left of the card; the TTS button top-right speaks the word.

## Grade dock

In `Grade`, `StudyActionDock` renders `AnswerButtons`: two neutral buttons of equal width, plus a mono "Desliza para calificar rápido" hint. Their anatomy is fixed by `.claude/rules/ui-components.md`, not chosen per screen.

- `GradeForgotButton` ("No la sabía") sits on the page background with a hairline border and fires `AGAIN`.
- `GradeKnewButton` ("La sabía") sits on the raised surface and fires `GOOD`; a long press fires `EASY` with `HapticFeedbackType.LongPress`.

Neither is red, green or accent — they differ by fill weight and position only. `HARD` is unreachable from the dock, and `EASY` has no button of its own. `FlippableCard` also grades by horizontal swipe on the back face, with two zones matching the two buttons.

## Navigation and exit

- the X button and system back both fire `ExitClicked`, which always emits `NavigateBack`. Every grade is already persisted, so leaving mid-session loses nothing and asks for no confirmation.
- when the last card is graded, `SessionFinished` is emitted and the route shows the finish dialog
- closing the final dialog fires `FinishDialogDismissed`, which emits `NavigateBack`

### Session-finished dialog

`SessionFinishedDialog` (private composable in `StudyScreen.kt`) renders inside a `Dialog` with `instrumentElev` surface and shows:

- the celebrating mascot
- mono eyebrow `session_completed_eyebrow`
- headline `session_completed_title` ("Listo.")
- subtitle `session_completed_desc` ("Repasaste N tarjetas.")
- a stats row on `instrumentSurface` with the total count in `instrumentAccent` plus a mono label
- a full-width `HButton` (`Accent` variant) as the "Volver" CTA, which calls `onDismiss`

## Empty stage CTA

When `StudyStage.Empty`, `StudyActionDock` renders a full-width `HButton` (Secondary / Lg) with label `study_empty_create_card_cta` ("Crear una tarjeta"). Tapping it fires `StudyUiIntent.CreateCardClicked`.

## Current intents

`StudyUiIntent`:

- `FinishDialogDismissed` — emits `NavigateBack`
- `CreateCardClicked` — emits `NavigateToNewCard`
- `RetryLoad` — re-runs `loadSession()`
- `ExitClicked` — emits `NavigateBack`
- `ReviewAnswered(item, reviewGrade)`

The card face (`Front`/`Back`) is local `StudyScreen` state, reset whenever `currentItem` changes.

## Current effects

`StudyUiEffect` currently exposes:

- `NavigateBack`
- `SessionFinished`
- `NavigateToNewCard` — collected in `StudyDestination`; navigates to `NewCardRoute`
