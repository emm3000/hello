# Flashcard Phase 2 Output Contract

## Purpose

Phase 2 defines the new AI output contract so generation stops returning a single loose flashcard blob and starts returning a structured learning note with derived study cards.

This phase establishes the domain output model and the minimum validation rules for generated notes.

## Implemented In Code

The following domain artifacts were added:

- `LearningNoteType`
- `StudyCardType`
- `EvaluationMode`
- `PartOfSpeechTag`
- `GeneratedStudyCard`
- `GeneratedNoteQualityCheck`
- `GeneratedLearningNote`
- `GeneratedLearningNoteValidation`
- `ValidateGeneratedLearningNoteUseCase`

## Why A New Output Contract Is Needed

The current `FlashcardGenerated` model is too small for the new architecture because it assumes:

- one flashcard
- one front/back style result
- one flat payload

The refactor needs:

- one canonical learning note
- multiple derived retrieval cards
- explicit quality checks
- typed note and card categories

## Coexistence Strategy

For now, the new Phase 2 contract coexists with the old `FlashcardGenerated` model.

This is intentional.

The migration path is:

1. define the new output contract in `domain`
2. adapt prompt output in `data`
3. adapt parsers and repository mapping
4. adapt preview UI
5. adapt persistence
6. retire or convert `FlashcardGenerated`

## New Output Model

## `GeneratedLearningNote`

This is the canonical generated object.

Core required fields:

- `noteId`
- `noteType`
- `expression`
- `intendedMeaningEs`
- `simpleDefinitionEn`
- `partOfSpeech`
- `register`
- `levelBand`
- `domain`
- `whyUseful`
- `exampleSentence`
- `exampleTranslation`
- `cards`
- `qualityChecks`

Optional fields:

- `lemma`
- `ipa`
- `usagePattern`
- `irregularForms`
- `collocations`
- `commonMistake`
- `confusableWith`
- `clozeSentence`
- `sourceContext`
- `warnings`

## `GeneratedStudyCard`

Each note contains derived cards.

Required fields:

- `cardId`
- `cardType`
- `prompt`
- `expectedAnswer`
- `evaluationMode`
- `isActive`

Optional fields:

- `acceptedAnswers`
- `hint`
- `explanation`
- `sourceField`

## Output Enums

## Learning Note Type

- `Word`
- `Phrase`
- `PhrasalVerb`
- `Idiom`
- `SentencePattern`

## Study Card Type

- `Recognition`
- `Production`
- `Cloze`
- `Form`

## Evaluation Mode

- `Exact`
- `FlexibleText`
- `ManualSelfCheck`

## Part Of Speech Tag

- `Noun`
- `Verb`
- `Adjective`
- `Adverb`
- `Preposition`
- `Conjunction`
- `Interjection`
- `PhrasalVerb`
- `Idiom`
- `Chunk`
- `Other`

## Quality Check Model

The generated note now carries explicit quality checks through:

- `GeneratedNoteQualityCheck`
- `GeneratedNoteQualityCode`

Included quality codes:

- `SingleMeaning`
- `NaturalExample`
- `ExampleSupportsMeaning`
- `NonAmbiguousAnswers`
- `RequiredFieldsPresent`
- `ClearCardFocus`
- `NoteCardAlignment`

This matters because quality is no longer implicit. The generator must declare whether key pedagogical expectations were met.

## Validation Strategy

Phase 2 adds `ValidateGeneratedLearningNoteUseCase`.

The validator currently checks:

- mandatory note fields
- mandatory cards by note type
- note-type-specific required fields
- missing prompt or answer in derived cards
- presence of a `SingleMeaning` quality check

## Current Hard Validation Rules

Generation is invalid when:

- the note has no id
- the expression is blank
- the intended meaning is blank
- the simple definition is blank
- `whyUseful` is blank
- the example sentence is blank
- the example translation is blank
- there are no cards
- there are no quality checks
- a phrase or phrasal verb has no `usagePattern`
- a sentence pattern has no `usagePattern`
- a sentence pattern has no `clozeSentence`
- an expected card type is missing for the note type
- a card prompt is blank
- a card expected answer is blank
- the note has no `SingleMeaning` quality check

## Warning Rules

The validator currently emits a warning when:

- a note contains an inactive derived card

This is intentionally minimal for now. Stronger output validation belongs to the next quality-gate-focused phase.

## Card Expectations By Note Type

## `Word`

Default expected cards:

- `Recognition`
- `Production`

## `Phrase`

Default expected cards:

- `Recognition`
- `Production`
- `Cloze`

## `PhrasalVerb`

Default expected cards:

- `Recognition`
- `Production`
- `Cloze`

## `Idiom`

Default expected cards:

- `Recognition`
- `Production`

## `SentencePattern`

Default expected cards:

- `Production`
- `Cloze`

## Tests Added

Unit tests were added to verify:

- valid word note passes validation
- phrase note without `usagePattern` fails
- missing `SingleMeaning` quality check fails
- sentence pattern without cloze fails
- derived cards with blank prompt fail

## What This Phase Does Not Yet Change

Phase 2 does not yet:

- rewrite the prompt
- update Gemini DTOs
- update repository parsing
- update local DB schema
- update Supabase schema
- replace the current preview screen
- replace the current study screen

That is intentional. Phase 2 establishes the output contract first so downstream layers can be migrated cleanly.

## Recommended Next Step

The next implementation step after this phase should be:

1. redesign the prompt output schema to match `GeneratedLearningNote`
2. create DTOs and parsers in `data` for the new output contract
3. map old preview flow to the new note and card structure
4. decide how long `FlashcardGenerated` remains as a compatibility type
