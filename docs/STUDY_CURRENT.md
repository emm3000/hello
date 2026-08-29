# Current Study

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Study` flow |
| Source of Truth | No |
| Read this when | You need to understand how the study session works today |
| Last verified | 2026-08-28 |

## Summary

The study session shows every due flashcard exactly once. Each `StudyFlashcard` maps to one `StudySessionItem`, the user reveals the back and grades it, and the review is scheduled with FSRS-6 and persisted on the spot. A card is asked in one of two directions: recognition (word → meaning) until it graduates, production (meaning → word) forever after. There is no start interstitial, no typed answer and no exit confirmation.

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
- `domain/src/main/kotlin/com/emm/domain/study/ScheduleFlashcardReviewUseCase.kt` (graduation rule)

## Session item

`StudySessionItem` is a 1:1 projection of `StudyFlashcard` onto the fields the card faces render:

- `flashcardId`, `review: FsrsCard`
- `word`, `phonetic`, `meaning`, `translation`
- `direction: StudyDirection` — `RECOGNITION` or `PRODUCTION`
- `usagePattern`, `irregularForms`
- `partOfSpeech`, `example`, `exampleTranslation`

`StudyFlashcard.toStudySessionItem()` builds it. `direction` is derived from the review: `PRODUCTION` when `review.productionSince != null`, otherwise `RECOGNITION`.

`StudySessionItem.cue` is a computed property: `translation`, or `meaning` when `translation` is blank. It is the Spanish-side text the faces render (the production prompt and the recognition answer), so a card without a translation still has something to ask and answer with.

`StudySessionItem.revealsWordOn(face: CardFace)` is true when the English word is visible on that face: always for `RECOGNITION`, only on `Back` for `PRODUCTION`.

The flashcard's generated study cards (`studyCards`) are not part of the session: a card with several generated study cards and a card with none are both one item and one grade.

## Graduation

`ScheduleFlashcardReviewUseCase` wraps `FsrsScheduler.schedule` and decides `productionSince`:

- if the card already has `productionSince`, it is kept as is
- otherwise, when the scheduled card is in `FsrsState.REVIEW` with `stability >= GRADUATION_STABILITY_DAYS` (`21.0`), `productionSince` becomes `scheduled.lastReviewedAt`
- otherwise it stays `null`

Once set, `productionSince` is never cleared: a graduated card is asked in production direction from the next session on, regardless of later grades.

## Current state

`StudyUiState` holds:

- `isLoading: Boolean = true` — session load in flight
- `loadError: StudyLoadError?` — `SessionLoadFailed`; distinguishes a failed read from a genuinely empty due queue
- `currentItem: StudySessionItem?`
- `reviewedCount` — cards graded so far
- `knewCount` — cards graded `HARD`, `GOOD` or `EASY`
- `forgotCount` — cards graded `AGAIN`
- `totalCount` — cards in the session (equals the due count Today shows)
- `sessionFinished` — true once the last card is graded and the session had at least one card

## Session load

`StudyViewModel` is constructed with a `deckId: String`. `StudyRoute.ALL_DUE_DECKS` (`"__all_due_decks__"`) is the sentinel Koin receives for the all-decks session; the viewmodel normalizes it back to `null`.

`loadSession()` (called from `init`, and again by `RetryLoad`):

- clears the queue and sets `isLoading = true`, `loadError = null`, `reviewedCount = 0`, `knewCount = 0`, `forgotCount = 0`, `sessionFinished = false`
- fetches via `studySessionRepository.sessionTodayAllDecks()` when the target is `null`, otherwise `studySessionRepository.sessionToday(deckId.toDeckId())`
- maps each `StudyFlashcard` to one `StudySessionItem` and queues them in an `ArrayDeque`
- sets `totalCount` to the number of cards
- shows the first card
- on any throwable (except `CancellationException`) logs and sets `loadError = SessionLoadFailed`

## Grading and persistence

`ReviewAnswered(item, grade)`:

- schedules a new `FsrsCard` with `ScheduleFlashcardReviewUseCase(item.review, grade, item.flashcardId)` (see Graduation)
- persists it with `flashcardReviewRepository.update(newCard, grade)` — immediately, with the grade as given
- increments `reviewedCount`, plus `forgotCount` for `AGAIN` or `knewCount` for any other grade
- shows the next card

When there is no next card and the session had at least one card, `sessionFinished` is set to `true` in state. An empty session never finishes; it renders the empty state instead.

## Visual stages

`StudyScreen` uses a private `StudyStage` enum, resolved in this order:

1. `Loading` — `state.isLoading`
2. `Error` — `state.loadError != null`
3. `Done` — `state.sessionFinished`
4. `Empty` — no `currentItem`
5. `Recall` — card face is Front
6. `Grade` — card face is Back

`Loading` renders `StudyLoadingState()` (`HLoadingSpinner`), `Error` renders `StudyErrorState()` (`HEmptyState` with `study_error_headline` / `study_error_body`), `Empty` renders `StudyEmptyState()` (`HEmptyState` with `study_empty_headline` "Nothing to review today." / `study_empty_body`), `Done` renders `StudyDoneState()`.

While a card is on screen (`Recall` / `Grade`) the page background is one of `cardHues`, indexed by `reviewedCount` and animated between cards; every other stage uses `pageBackground`.

`StudyTop` shows the position (`study_position`, "3 / 10") and an `HProgressBar` only in `Recall` / `Grade`; both are hidden in the other stages. The X button (`exit_session_desc`) is always present.

## Card faces

The face is local `StudyScreen` state, reset to `Front` whenever `currentItem` changes. Tapping anywhere on the card toggles the face; the `Recall` dock also offers a "Show answer" button.

- **Front** (`FlashcardFront`): an uppercase prompt label, then the dominant text.
  - `RECOGNITION`: label `study_prompt_label` ("What does it mean?"), the English `word`, then `phonetic` if not blank.
  - `PRODUCTION`: label `study_prompt_production` ("How do you say it?"), the `cue`. No phonetic.
- **Back** (`FlashcardBack`, scrollable): a muted top line, the dominant answer, then reference.
  - `RECOGNITION`: top line is `word`, answer is `cue`.
  - `PRODUCTION`: top line is `cue`, answer is `word`, followed by `phonetic` if not blank.
  - Then, when `example` is not blank: the `example` with the first occurrence of `word` underlined (`underlineFirstMatch`), over a softer `exampleTranslation` if present.
  - Then a muted reference line joining `partOfSpeech`, `phonetic` (recognition only; production already showed it) and `meaning` with ` · `, skipping blanks. When `translation` is blank, `meaning` appears twice on the back: as the `cue` and in this line.
  - `usagePattern` and `irregularForms` are carried by the item but not rendered on either face.

## TTS

`StudyTop` renders `TtsFloatingButton` next to the X only when the word is revealed: `sessionStage` is `Recall` or `Grade` and `currentItem.revealsWordOn(cardFace)` is true. In practice: any face for `RECOGNITION`, only `Back` for `PRODUCTION`. It speaks `currentItem.word` through `TextToSpeechManager`, toggles to a stop icon while speaking, and is disabled until TTS is ready.

A `LaunchedEffect(wordRevealed)` calls `tts.stop()` whenever the word stops being revealed: the face flips back to `Front` on a `PRODUCTION` card, the item changes, or the session leaves the card (`Done`, `Empty`, `Error`, `Loading`). Speech never outlives the word it belongs to.

## Action dock

`StudyActionDock` renders per stage, animated with `AnimatedContent`:

- `Loading` — nothing
- `Error` — full-width `HButton` Primary `study_error_retry` ("Retry") → `RetryLoad`
- `Empty` — full-width `HButton` Secondary `study_empty_create_card_cta` ("Add a word") → `CreateCardClicked`
- `Recall` — full-width `HButton` Primary `study_recall_cta_reveal` ("Show answer") flips to `Back`, plus the muted hint `study_recall_hint` ("Try to recall it first.")
- `Grade` — `AnswerButtons`
- `Done` — three full-width `HButton`s stacked: Primary `study_done_get_new_words` ("Get new words") → `GetNewWordsClicked`; Secondary `study_done_add_word` ("Add a word") → `CreateCardClicked`; Text `study_done_back` ("Back to Today") → `ExitClicked`

### Grade buttons

`AnswerButtons` renders two buttons of equal width. Their anatomy is fixed by `.claude/rules/ui-components.md`, not chosen per screen.

- `GradeForgotButton` (`study_grade_forgot`, "Forgot") is transparent with a hairline `outline` border and fires `AGAIN`.
- `GradeKnewButton` (`study_grade_knew`, "Knew it") is filled with `ink` and fires `GOOD`; a long press fires `EASY` with `HapticFeedbackType.LongPress`.

Neither is red, green or accent. `HARD` is unreachable from the dock, and `EASY` has no button of its own. There is no swipe-to-grade.

## Done stage

`StudyDoneState` renders inline (no dialog): a full `HRing` with a check icon, the headline `study_done_title` ("Done for today.") and `study_done_stats` ("%1$d reviewed · %2$d knew it · %3$d to see again") filled with `reviewedCount`, `knewCount`, `forgotCount`. The dock above lists its three CTAs.

## Navigation and exit

- the X button and system back both fire `ExitClicked`, which always emits `NavigateBack`. Every grade is already persisted, so leaving mid-session loses nothing and asks for no confirmation.
- `CreateCardClicked` emits `NavigateToCapture`; `StudyDestination` navigates to `CaptureRoute`
- `GetNewWordsClicked` emits `NavigateToSuggest`; `StudyDestination` navigates to `SuggestRoute`

## Current intents

`StudyUiIntent`:

- `CreateCardClicked` — emits `NavigateToCapture`
- `GetNewWordsClicked` — emits `NavigateToSuggest`
- `RetryLoad` — re-runs `loadSession()`
- `ExitClicked` — emits `NavigateBack`
- `ReviewAnswered(item, reviewGrade)` — schedules, persists, tallies, advances

## Current effects

`StudyUiEffect`:

- `NavigateBack`
- `NavigateToCapture`
- `NavigateToSuggest`

Session completion is state (`sessionFinished`), not an effect.
