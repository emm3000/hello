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

- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyUiEffect.kt`

## Current state

`StudyUiState` holds:

- `currentItem`
- `reviewedCount`
- `totalCount`
- `sessionFinished`

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

## Current effects

`StudyUiEffect` currently exposes:

- `NavigateBack`
- `SessionFinished`
