# Current Study

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Study` flow |
| Source of Truth | No |
| Read this when | You need to understand how the study session works today |

## Summary

The study session works over a queue of `StudySessionItem`s derived from the deck's flashcards.

Each flashcard can expand into multiple study items. The review is persisted once per flashcard, when its pending items are done.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyTop.kt` (private chrome — Ember Phase 2.2 sub-1)
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyUiEffect.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudySessionItem.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyAnswerPolicy.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/CardFace.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/FlippableCard.kt`

## Current state

`StudyUiState` holds:

- `currentItem`
- `reviewedCount`
- `totalCount`
- `sessionFinished`
- `intervalPreviews: Map<ReviewGrade, Long>` — interval previews shown under each grade chip

The heavy logic lives in `StudyScreen` and `StudyViewModel`.

## Session load

`StudyViewModel`:

- fetches flashcards via `GetStudySessionUseCase(deckId)`
- expands each flashcard into `StudySessionItem`s
- stores a local `ArrayDeque` queue
- initializes `totalCount`
- shows the first available item

## Progress model

For each flashcard, the viewmodel keeps:

- pending items by `flashcardId`
- the most conservative aggregated grade per `flashcardId`
- a reference to the original flashcard

When the last item of a flashcard is answered:

- computes the most conservative final grade
- schedules a new review with `ScheduleFlashcardReviewUseCase`
- persists it with `UpdateFlashcardReviewUseCase`

## Visual stages

`StudyScreen` uses these local stages:

- `Start`
- `Empty`
- `Recall`
- `Check`
- `Grade`

The stage depends on:

- whether the session has started
- whether there's a current item
- whether the card needs a typed answer
- whether the typed answer has been checked

## Current interaction flow

### Start

Shows a start card with:

- total item count
- estimated time
- CTA to begin

### Recall

Shows the front of the card and a CTA to reveal or jump to answering.

### Check

If the card requires a typed answer:

- shows input
- lets you check the answer
- lets you reveal anyway without answering

### Grade

Shows grading buttons per the policy allowed for that card and the typed-answer result.

## Typed answer

Local screen state holds:

- `typedAnswer`
- `typedAnswerChecked`
- `typedAnswerCorrect`

Matching is done against:

- `expectedAnswer`
- `acceptedAnswers`
- `evaluationMode`

## Navigation and exit

Current behaviors:

- if there's already progress, back shows an exit confirmation
- when the session ends, `SessionFinished` is emitted
- closing the final dialog or back emits `NavigateBack`

### Exit-confirmation dialog (Phase 4 microcopy)

Rendered via `HAlertDialog` (`isDangerous = true`) with:

- title: "¿Salir de la sesión?"
- description: "perderás el ritmo actual."
- confirm: `study_exit_confirm_leave`
- cancel: `study_keep_going`

### Session-finished dialog

A custom `SessionFinishedDialog` (private composable in `StudyScreen.kt`) replaces the previous `HAlertDialog`. It renders inside a `Dialog` with `emberElev` surface and shows:

- mono eyebrow `session_completed_eyebrow`
- serif headline `session_completed_title` ("Listo.")
- italic serif subtitle `session_completed_desc` ("Repasaste N tarjetas.")
- a stats row on `emberSurface` with the total count in `emberAccent` plus a mono label
- a full-width `HButton` (`Accent` variant) as the "Volver" CTA, which calls `onDismiss`

### Back content sub-composables (Phase 5b split)

`FlashcardBackContent` now only picks one of three sub-composables based on whether the answer should be revealed and whether the typed answer matched:

- `FlashcardBackPrompt` — front prompt re-shown when the answer must stay hidden
- `FlashcardBackMismatch` — mismatch cards plus result message when the typed answer is wrong
- `FlashcardBackReveal` — answer label + primary text + optional phonetic, supporting text, success message and `CardTypeAnswerSupport`

## Empty stage CTA

When `StudyStage.Empty`, `StudyActionDock` renders a full-width `HButton` (Secondary / Lg) with label `study_empty_create_card_cta` ("Crear una tarjeta"). Tapping it fires `StudyUiIntent.CreateCardClicked`.

## Current intents

`StudyUiIntent`:

- `BackClicked` — emits `NavigateBack`
- `FinishDialogDismissed` — emits `NavigateBack`
- `CreateCardClicked` — emits `NavigateToNewCard`
- `ReviewAnswered(item, reviewGrade)`
- `TypedAnswerChanged(answer)`

## Current effects

`StudyUiEffect` currently exposes:

- `NavigateBack`
- `SessionFinished`
- `NavigateToNewCard` — collected in `StudyDestination`; navigates to `NewCardRoute`
