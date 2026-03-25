# Flashcard Phase 0 Learning Contract

## Status

Approved working contract for the next implementation stages.

This document is the source of truth for:

- what a generated learning note is
- what a derived study card is
- which fields are mandatory
- what quality rules block save
- which note patterns are forbidden

This contract is intentionally strict. The purpose of Phase 0 is to reduce ambiguity before touching prompts, UI, or persistence.

## Phase 0 Goal

Lock the learning contract so the app stops thinking in terms of:

- one input
- one front
- one back

And starts thinking in terms of:

- one learning note
- multiple retrieval cards

## Core Definitions

## Learning Note

A learning note is the canonical unit of knowledge stored by the app.

It contains:

- the target expression
- the intended meaning
- the linguistic context
- the usage pattern
- the teaching metadata
- the derived study cards

The learning note is not what the user reviews directly. The user reviews cards derived from the note.

## Derived Study Card

A derived study card is a single retrieval task created from one learning note.

A derived study card must test one clear thing only.

Examples:

- recall the Spanish meaning of an English expression
- produce the English expression from a Spanish cue
- complete a cloze sentence
- recall a verb form or required preposition

## Learning Outcomes Supported

The system should support these outcomes:

- recognition
- production
- contextual recall
- form recall

The system should not pretend one card can train all of them at the same time.

## Canonical Enums

## NoteType

- `word`
- `phrase`
- `phrasal_verb`
- `idiom`
- `sentence_pattern`

## CardType

- `recognition`
- `production`
- `cloze`
- `form`

## PartOfSpeech

- `noun`
- `verb`
- `adjective`
- `adverb`
- `preposition`
- `conjunction`
- `interjection`
- `phrasal_verb`
- `idiom`
- `chunk`
- `other`

## LearningGoal

- `recognition_only`
- `production_only`
- `both`

## Register

- `casual`
- `neutral`
- `formal`

## LevelBand

- `A1_A2`
- `B1_B2`
- `C1_PLUS`

## Domain

- `daily_life`
- `travel`
- `social`
- `work`
- `study`
- `media`
- `mixed`

## GeneratedLearningNote Contract

## Mandatory Fields For All Notes

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

## Optional Fields Depending On Note Type

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

## Field Semantics

### `expression`

The exact target expression to learn.

Rules:

- must be a single teachable target
- must be the canonical target for the note
- must not combine multiple unrelated targets

### `intendedMeaningEs`

The precise Spanish meaning for the target expression in this note.

Rules:

- must represent one sense only
- must be natural Spanish
- must not be a bag of synonyms

### `simpleDefinitionEn`

A simple English definition that helps connect the learner to the target expression.

Rules:

- must be short
- must avoid dictionary overload
- must match the chosen intended meaning

### `whyUseful`

A short explanation of why the expression is worth learning.

Rules:

- must be practical
- must refer to actual communicative use
- must not be motivational filler

### `exampleSentence`

One high-value example sentence.

Rules:

- must sound natural
- must reflect real use
- must fit the learner level
- must not overload the learner with many unknown words

### `clozeSentence`

A sentence used for cloze recall.

Rules:

- should contain only one tested omission
- should be answerable without ambiguity
- may be omitted if the note type does not benefit from cloze

### `usagePattern`

The grammar or lexical pattern attached to the note.

Examples:

- `interested in`
- `depend on`
- `ask someone to do something`
- `pick someone up`

### `irregularForms`

Only for notes where form recall matters.

Examples:

- verb forms
- plural irregularity
- comparative irregularity

### `collocations`

A short list of high-value natural combinations.

Rules:

- must be few
- must be relevant
- must not turn into a long vocabulary list

### `commonMistake`

One high-value learner warning.

Examples:

- false friend
- wrong preposition
- literal translation trap
- formality misuse

## Derived Card Contract

Every card must include:

- `cardId`
- `cardType`
- `prompt`
- `expectedAnswer`
- `evaluationMode`
- `isActive`

Optional:

- `acceptedAnswers`
- `hint`
- `explanation`
- `sourceField`

## Evaluation Modes

- `exact`
- `flexible_text`
- `manual_self_check`

## Card Rules By Type

### Recognition Card

Purpose:

- fast meaning recall from English to Spanish

Required:

- prompt based on `expression`
- expected answer based on `intendedMeaningEs`

Rules:

- must not ask for multiple meanings
- must not require essay-length answers

### Production Card

Purpose:

- recall the English target from Spanish or context

Required:

- prompt based on Spanish cue, context, or both
- expected answer based on `expression`

Rules:

- must be answerable with one main target
- must not accept several unrelated correct answers unless explicitly modeled

### Cloze Card

Purpose:

- recall the target in context

Required:

- prompt based on `clozeSentence`
- expected answer based on `expression`

Rules:

- only one omission should be tested
- the surrounding sentence must strongly constrain the answer

### Form Card

Purpose:

- recall form-specific knowledge

Required:

- prompt based on a specific form point
- expected answer based on the form

Rules:

- only create when the form point is genuinely useful
- never create by default for every note

## Mandatory Fields By NoteType

## `word`

Required:

- `expression`
- `intendedMeaningEs`
- `simpleDefinitionEn`
- `partOfSpeech`
- `exampleSentence`
- `exampleTranslation`
- `cards`
- `qualityChecks`

Conditionally required:

- `ipa` if pronunciation is useful
- `commonMistake` if a false friend or trap exists
- `collocations` if they strongly improve usage

## `phrase`

Required:

- `expression`
- `intendedMeaningEs`
- `simpleDefinitionEn`
- `exampleSentence`
- `exampleTranslation`
- `usagePattern`
- `cards`
- `qualityChecks`

Conditionally required:

- `clozeSentence`
- `commonMistake`

## `phrasal_verb`

Required:

- `expression`
- `intendedMeaningEs`
- `simpleDefinitionEn`
- `usagePattern`
- `exampleSentence`
- `exampleTranslation`
- `cards`
- `qualityChecks`

Conditionally required:

- separability note inside `usagePattern`
- `commonMistake`
- `clozeSentence`

## `idiom`

Required:

- `expression`
- `intendedMeaningEs`
- `simpleDefinitionEn`
- `whyUseful`
- `exampleSentence`
- `exampleTranslation`
- `cards`
- `qualityChecks`

Conditionally required:

- `register`
- `domain`

## `sentence_pattern`

Required:

- `expression`
- `intendedMeaningEs`
- `simpleDefinitionEn`
- `usagePattern`
- `exampleSentence`
- `exampleTranslation`
- `clozeSentence`
- `cards`
- `qualityChecks`

## Card Generation Matrix

## `word`

Default cards:

- `recognition`
- `production`

Optional:

- `cloze`
- `form`

## `phrase`

Default cards:

- `recognition`
- `production`
- `cloze`

Optional:

- `form`

## `phrasal_verb`

Default cards:

- `recognition`
- `production`
- `cloze`

Optional:

- `form`

## `idiom`

Default cards:

- `recognition`
- `production`

Optional:

- `cloze`

## `sentence_pattern`

Default cards:

- `production`
- `cloze`

Optional:

- `recognition`

## Saveable Quality Standard

A note is saveable only if all of the following are true:

- it represents one intended meaning only
- it has at least one natural example
- the example supports the intended meaning
- the main card answers are not ambiguous
- the note type has all mandatory fields
- each derived card tests one clear thing
- there is no obvious mismatch between note content and card content

## Hard Fail Rules

Block save when any of these happen:

- multiple senses in one note
- vague or generic intended meaning
- unnatural example sentence
- production card with multiple likely answers
- cloze card with weak context
- missing mandatory fields
- form card generated without real pedagogical need
- target expression too broad to study as one note

## Soft Warning Rules

Allow save with warning only when:

- collocations are empty but not critical
- IPA is missing for a familiar high-frequency note
- common mistake is absent but not essential
- cloze card is omitted for a note type where it is optional

## Forbidden Patterns

These patterns are forbidden in the new system:

- one note with several translations for different senses
- one card testing meaning, grammar, and spelling at the same time
- examples that sound translated instead of natural
- too much metadata with no review use
- generated fields that are never persisted
- generated fields that are persisted but never surfaced
- prompts that invent richness without a product use
- cards that require subjective self-interpretation when an objective answer is possible

## Explicit Non-Goals For Phase 0

Phase 0 does not define:

- final UI layout
- final database schema names
- final Supabase table names
- final animation or interaction design

Phase 0 only defines the contract that those layers must implement.

## Phase 0 Deliverables

Phase 0 is complete when the team has:

- one source of truth for note types
- one source of truth for card types
- one source of truth for mandatory fields
- one source of truth for saveable quality
- one source of truth for forbidden note patterns

## Recommended Immediate Follow-Up

After this document, the next work should be:

1. map current app fields against this contract
2. identify obsolete current fields
3. identify missing fields required by the new contract
4. redesign user input for ambiguity handling
5. redesign prompt output to match this contract exactly

## Implementation Note For Trial-And-Error Development

Because schema changes are allowed without formal migrations during this stage, every future change must still be synchronized across all affected layers:

- local DB
- domain models
- repository mapping
- preview state
- study state
- sync payloads
- Supabase structure

No field should exist in only one layer for long. Temporary drift should be treated as a bug, not as an acceptable state.

This also means Phase 0 does not impose any backward-compatibility obligation.

During this pre-development stage:

- local schema can be rewritten
- remote Supabase schema can be rewritten
- old structures can be removed without preservation
- legacy support should not be carried forward just because it already exists

The contract is more important than preserving any earlier implementation.
