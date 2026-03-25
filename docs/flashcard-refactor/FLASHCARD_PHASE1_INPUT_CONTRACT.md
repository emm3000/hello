# Flashcard Phase 1 Input Contract

## Purpose

Phase 1 redesigns the input layer so the app stops accepting weak requests that usually produce weak flashcards.

The focus of this phase is:

- define the new input contract
- define source-of-truth enums
- define ambiguity rules
- define validation behavior before generation

This phase does not yet migrate the current Compose screen end-to-end. It creates the domain contract and validation base that the UI and data layers must adopt next.

## Implemented In Code

The following domain artifacts were added:

- `FlashcardInputType`
- `LearningGoal`
- `LevelBand`
- `RegisterPreference`
- `LearningDomain`
- `CommunicativeIntent`
- `FlashcardGenerationInput`
- `FlashcardGenerationInputValidation`
- `ValidateFlashcardGenerationInputUseCase`

## New Input Contract

## Required Fields

- `inputType`
- `userText`
- `learningGoal`
- `levelBand`

## Optional But Important Fields

- `intendedMeaningEs`
- `contextSentence`
- `register`
- `domain`
- `communicativeIntentId`

## Input Types

### `Word`

Use when the learner wants to study a single word.

Example:

- `awkward`
- `borrow`

### `Phrase`

Use when the learner wants to study a multiword chunk.

Example:

- `by the way`
- `in charge of`

### `Sentence`

Use when the learner wants to study a reusable sentence pattern or a full expression in context.

Example:

- `I was about to leave`
- `Would you mind helping me`

### `CommunicativeGoal`

Use when the learner wants language for a real situation rather than a predefined word.

Example:

- ask for directions politely
- complain about a delayed order

## Source-Of-Truth Enums

## Learning Goals

- `RecognitionOnly`
- `ProductionOnly`
- `Both`

## Level Bands

- `A1_A2`
- `B1_B2`
- `C1_PLUS`

## Register

- `Casual`
- `Neutral`
- `Formal`

## Domain

- `DailyLife`
- `Travel`
- `Social`
- `Work`
- `Study`
- `Media`
- `Mixed`

## Communicative Intents

The new contract introduces communicative intent options instead of relying only on broad categories.

Examples included:

- introduce yourself
- talk about plans
- describe past events
- make polite requests
- order food
- ask for directions
- solve daily problems
- express preferences
- express emotions
- social small talk
- handle complaints
- phone and messages

## Validation Rules

## Hard Errors

Generation should be blocked when:

- `userText` is blank
- a `Sentence` input is too short to provide enough context
- a `CommunicativeGoal` input has no `communicativeIntentId`
- an ambiguous target has neither `intendedMeaningEs` nor `contextSentence`

## Warnings

Warnings should be shown when:

- a `Word` input appears to contain multiple tokens
- a `Phrase` input looks too short and may really be a word
- a communicative goal description is too short
- a context sentence is too short to truly disambiguate use

## Ambiguity Handling

Some targets should not be generated without disambiguation.

Examples:

- `get`
- `set`
- `run`
- `take`
- `make`
- `do`
- `pick up`
- `take off`
- `look up`
- `run out`

For these cases, the user must provide at least one of:

- `intendedMeaningEs`
- `contextSentence`

## Why This Matters

The old flow accepts minimal inputs and pushes too much responsibility onto the prompt. That causes:

- mixed meanings
- low-value examples
- weak production prompts
- generic cards

The new contract moves some responsibility earlier:

- the user clarifies the target
- the app validates ambiguity
- generation starts from better input

## Current Integration State

Already implemented:

- typed domain contract
- typed communicative intents
- normalization of whitespace
- validation use case
- unit tests for core cases

Still pending:

- replace current `NewCardUiState` input model
- replace current category-first generation UX
- route validation errors and warnings into UI
- connect validated input to the next prompt contract

## Recommended Next Step

Phase 2 should now define the new AI output contract so the input model and output model can be connected cleanly.

Before UI migration, the next implementation step should be:

1. map current `NewCardUiState` fields to `FlashcardGenerationInput`
2. identify fields to remove from the current flow
3. add validation feedback to the screen
4. adapt generation use cases to consume the new validated input
