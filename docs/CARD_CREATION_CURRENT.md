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
- optional quota warning text above the Generate CTA — rendered when `state.showQuotaWarning` is true (i.e. `quotaRemaining` is in 1..10); copy comes from `new_card_quota_remaining_warning` plurals
- `Generate` CTA

Current gating:

- `WordOrPhase`: requires deck and `word`
- `WithCategories`: requires deck
- `WithAiHelp`: requires deck and `aiRequest`

**Zero-deck inline CTA**: when the deck list is empty, `DeckPickerRow` renders a tappable row ("Crear un mazo" — `R.string.new_card_input_create_deck_cta`) plus a hint text (`R.string.new_card_input_no_decks_hint`). Tapping navigates to `NewDeckRoute()` via `onCreateDeck` propagated from `NewCardDestination → NewCardInputStepScreen → DeckPickerSection → DeckPickerRow`.

Current support:

- microphone in word inputs. While `sttManager.isListening == true`, the word input grows a pulsing accent ring and renders an uppercase "ESCUCHANDO…" label (`R.string.listening_placeholder`) below the text; the mic FAB swaps `MicNone` → `Mic`, fills with the accent, and pulses (added in the redesign's Phase 3, commit `29c4443`).
- static categories via bottom sheet (`BottomSheetDialogForPickCategory`). Full-bleed `ModalBottomSheet` with a mono uppercase title and one row per category, and the selected row marks itself with an accent `Outlined.Check` icon. No chips (added in the redesign's Phase 3, commit `29c4443`).
- simple difficulty mapped to `LevelBand`; difficulty chip labels go through `difficultyDisplayLabel()` (display layer, not stored): `"basico"` → `"Básico"`, `"intermedio"` → `"Intermedio"`, `"avanzado"` → `"Avanzado"`

## UiState quota fields

`NewCardUiState` gained two quota-awareness members:

- `quotaRemaining: Int` — defaults to `Int.MAX_VALUE`; updated at init and after each successful preview via `GenerationQuota.remainingToday()` (non-consuming read)
- `showQuotaWarning: Boolean` (computed) — `true` when `quotaRemaining in 1..10`

`NewCardViewModel` receives `GenerationQuota` via constructor injection.

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
- loading (`LoadingPreviewSkeleton` — 3 accent pulse dots + `"Pensando en cuándo se suele usar {word}…"` + mono `SUELE TARDAR 8–12 S` + shimmer skeleton lines)
- quota error (`QuotaExceededState` — `!` glyph in an `instrumentBadSoft` circle, a headline, a `TU PALABRA` surface preserving the user's word, `Volver a editar` / `Avisarme mañana` buttons, mono reset hint); discriminated by `NewCardErrorUi.quotaResetAt != null`
- generic error (`HAlert` Destructive variant) + `HButton` "Reintentar" (`R.string.retry_action`, Secondary/Md/full) that refires `GenerateClicked`
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

**In-flight guard**: `GenerateClicked` and all `runPreviewWorkflowUpdate` regen actions are no-ops when `currentState.isLoading == true`, preventing duplicate in-flight requests.

**Error copy** (as of this change): invalid-AI-response title is `"No se pudo procesar la respuesta de IA"`; generation failure fallback is `"No se pudo generar la tarjeta. Inténtalo de nuevo."`; regen failure messages follow the pattern `"No se pudo regenerar [X]. Inténtalo de nuevo."`.

## Relevant model

The preview revolves around `GeneratedLearningNote`:

- base note
- example
- linguistic metadata
- `cards`
- `qualityChecks`
- `warnings`

Exact-duplicate detection exists via `FlashcardDuplicateRepository`.
