# Current Card Creation

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `New Card` flow |
| Source of Truth | No |
| Read this when | You need to understand how card creation works today |

## Summary

The current creation flow has 3 steps handled in `NewCardRoute`:

1. `Mode`
2. `Input`
3. `Review`

Navigation is local to the route and uses a single shared `NewCardViewModel`.

## Key files

These are the **main entry points and artifacts** of the creation flow. The rest of `newfeatures/card/` contains internal components (preview UI, validation, drafts) and the sibling detail/edit flows documented separately.

- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardModeScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardInputStepScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardReviewScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardGenerationMappings.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardPreviewWorkflow.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardDraftEditor.kt`

## Related flows

- View existing card: `docs/CARD_DETAIL_CURRENT.md`
- Edit existing card: `docs/EDIT_FLASHCARD_CURRENT.md`

## Step 1. Mode

`NewCardModeScreen` shows a mode selector and a CTA to continue.

Current `TypeView` modes:

- `WordOrPhase`
- `WithCategories`
- `WithAiHelp`

## Step 2. Input

`NewCardInputStepScreen` shows:

- inputs based on `TypeView`
- deck selection
- default-deck checkbox
- `Generate` CTA

Current gating:

- `WordOrPhase`: requires deck and `word`
- `WithCategories`: requires deck
- `WithAiHelp`: requires deck and `aiRequest`

Current support:

- microphone in word inputs
- static categories via bottom sheet
- simple difficulty mapped to `LevelBand`

## Domain input

`NewCardUiState` is translated to `FlashcardGenerationInput` in `NewCardGenerationMappings.kt`.

Current mapping:

- `WordOrPhase` infers `Word`, `Phrase` or `Sentence` from `word`
- `WithCategories` uses `CommunicativeGoal` from a static category
- `WithAiHelp` uses `CommunicativeGoal` from free text

Input is always validated before generating a preview.

## Step 3. Review

`NewCardReviewScreen` renders one of these states:

- preview available
- loading (`LoadingPreviewSkeleton` — 3 accent pulse dots + italic-serif `"Pensando en cuándo se suele usar {word}…"` + mono `SUELE TARDAR 8–12 S` + shimmer skeleton lines)
- quota error (`QuotaExceededState` — `!` glyph in `emberBadSoft` circle, serif headline, `TU PALABRA` surface preserving the user's word, `Crear a mano` / `Avisarme mañana` buttons, mono reset hint); discriminated by `NewCardErrorUi.quotaResetAt != null`
- generic error (`HAlert` Destructive variant)
- empty state

The current review allows:

- editing `GeneratedLearningNote` fields
- editing prompt, expected answer and hint for each `GeneratedStudyCard`
- enabling or disabling individual cards
- regenerating example
- regenerating cloze
- regenerating specific fields (`WhyUseful`, `UsagePattern`, `CommonMistake`)
- regenerating an individual card

Validation is recomputed after each edit or regeneration.

## Saving

`NewCardViewModel.saveFlashcard()`:

- requires `deckSelected`
- requires `learningNotePreview`
- re-validates preview before saving
- uses `CreateFlashcardUseCase`
- on success it resets state, shows a message and closes the flow

## Flow effects

`NewCardUiEffect` today exposes:

- `ShowMessage`
- `OpenReview`
- `CloseFlow`

`GenerateClicked` fires `OpenReview` before resolving the result, so the review step also contains loading and errors.

## Relevant model

The preview revolves around `GeneratedLearningNote`:

- base note
- example
- linguistic metadata
- `cards`
- `qualityChecks`
- `warnings`

Exact-duplicate detection exists via `FlashcardDuplicateRepository`.
