# Flashcard Refactor Status

## Purpose

This document tracks the operational status of the flashcard refactor.

It is not the source of truth for design decisions. The design source of truth remains the phase documents.

This file exists to answer:

- where the refactor is right now
- what is already implemented
- what is still pending
- what the next step should be
- what risks exist if work continues without closing a gap

## Current Summary

The refactor has completed the contract-definition stage and has started the migration stage.

Current reality:

- Phase 0 is defined and closed at the contract level
- Phase 1 is defined and implemented in `domain`
- Phase 2 is defined and implemented in `domain`
- `data` has a working generation path, parser, and rich local persistence for `GeneratedLearningNote`
- the app generation path now goes through `GeneratedLearningNote`
- local persistence now stores note-rich fields and derived cards inside `Flashcard`
- study mode and remote sync still behave as old flat flashcard consumers

## High-Level Progress

Estimated qualitative progress:

- Product and learning contract definition: `done`
- Input contract: `done and integrated in app`
- Output contract: `done and integrated in data/app`
- Prompt migration: `done for the new generation path`
- Repository integration: `done for generation and save`
- Preview migration: `done`
- Persistence migration: `started and locally integrated`
- Study-mode migration: `not started`
- Quality gate before save: `defined and partially enforced through validated generation`

## Phase Status

## Phase 0

Status:

- `closed`

Result:

- learning-note model defined conceptually
- derived-card model defined conceptually
- saveable-quality rules defined
- forbidden patterns defined

Reference:

- [FLASHCARD_PHASE0_LEARNING_CONTRACT.md](/Users/emm/AndroidStudioProjects/Hello/docs/flashcard-refactor/FLASHCARD_PHASE0_LEARNING_CONTRACT.md)

## Phase 1

Status:

- `closed at domain level`

Implemented:

- new input enums
- `FlashcardGenerationInput`
- communicative intents
- ambiguity validation
- unit tests

Not integrated yet:

- `NewCardUiState`
- Compose input flow
- error/warning rendering in UI
- use of validated input in generation pipeline

Reference:

- [FLASHCARD_PHASE1_INPUT_CONTRACT.md](/Users/emm/AndroidStudioProjects/Hello/docs/flashcard-refactor/FLASHCARD_PHASE1_INPUT_CONTRACT.md)

## Phase 2

Status:

- `closed at domain level`
- `started in data`

Implemented:

- `GeneratedLearningNote`
- `GeneratedStudyCard`
- output enums
- output validation use case
- domain tests
- parser DTOs in `data`
- `GeneratedLearningNoteResponseParser`
- parser tests in `data`

Integrated:

- prompt output schema
- repository generation path
- preview path
- save path

Reference:

- [FLASHCARD_PHASE2_OUTPUT_CONTRACT.md](/Users/emm/AndroidStudioProjects/Hello/docs/flashcard-refactor/FLASHCARD_PHASE2_OUTPUT_CONTRACT.md)

## Phase 3

Status:

- `started`

Planned focus:

- rewrite prompts to generate `GeneratedLearningNote`
- remove artificial randomness constraints
- align prompt output exactly with DTOs and parser expectations

## Phase 4

Status:

- `not started`

Planned focus:

- quality gate before save
- fail and warning rules in app flow

## Phase 5

Status:

- `not started`

Planned focus:

- editable preview
- per-card preview
- regenerate-one-field flows

## Phase 6

Status:

- `not started`

Planned focus:

- new study card types
- cloze and production review

## Phase 7

Status:

- `not started`

Planned focus:

- persistence migration
- local DB changes
- sync payload changes
- Supabase alignment

## Phase 8

Status:

- `not started`

Planned focus:

- benchmark set
- rubric scoring
- iteration loop on quality

## Status By Layer

## Domain

Status:

- `strong progress`

Completed:

- input contract
- output contract
- validation for input
- validation for generated note
- unit tests for both validators

Pending:

- use cases that bridge validated input to generated output
- retirement or conversion strategy for `FlashcardGenerated`

## Data

Status:

- `started and connected for parallel generation path`

Completed:

- DTOs for generated learning note response
- parser for generated learning note response
- parser tests
- repository method for generated learning note
- prompt builder for generated learning note

Pending:

- retire old generation methods once no longer referenced
- add repository integration tests for rich persistence
- decide whether to keep JSON-embedded derived cards or extract them later

## App

Status:

- `started`

Current situation:

- UI already captures optional disambiguation fields in the word or phrase flow
- preview can render `GeneratedLearningNote` and derived cards
- word or phrase save now persists directly from `GeneratedLearningNote`
- category flow also uses the new generation contract through mapped communicative intent

Pending:

- render validation errors and warnings more explicitly
- support editing the learning note preview before save
- adapt study mode to use derived cards instead of only flat front/back

## Local Database

Status:

- `started`

Pending:

- local schema is already expanded to store rich note fields and derived cards
- current persistence stores derived cards and quality checks as embedded JSON on `Flashcard`
- examples remain in `FlashcardExample`
- remote sync and Supabase still need the same field expansion

## Supabase

Status:

- `not started for schema alignment`

Pending:

- define remote representation for learning notes and cards
- align sync payloads
- align RPC and push/pull assumptions

## Tests

Status:

- `good for domain contracts`
- `started for data parser`

Completed:

- input validation tests
- generated note validation tests
- generated note parser tests
- local persistence compile coverage
- app creation flow tests still passing

Pending:

- repository integration tests
- prompt/parser compatibility tests
- UI tests for new creation flow

## Compatibility State

The project currently still contains two generation models in code:

- old model: `FlashcardGenerated`
- new model: `GeneratedLearningNote`

But the `NewCard` flow already runs on the new model end to end.

It becomes dangerous if:

- both paths remain active too long
- prompt output starts drifting between schemas
- sync or read paths keep flattening rich data silently

## Known Risks

- Study mode still assumes a flat front/back card.
- Remote sync and Supabase do not yet persist the expanded flashcard fields.
- `FlashcardGenerated` still exists and can become dead-weight if not retired soon.
- Rich fields are embedded as JSON in local persistence; that is pragmatic now, but may become limiting later.

## Recommended Next Step

The next step should be:

1. finish Phase 7 for sync and remote schema
2. start Phase 6 for actual study-card-based review
3. remove dead compatibility paths when the new persistence is stable

More concretely:

- expand Supabase `flashcard` schema with the same rich fields
- update sync payload handling for rich flashcard data
- adapt `StudyScreen` and session loading to prefer derived study cards
- decide the retirement path for `FlashcardGenerated`

## Recent Commits

- `b1b791e` `feat(flashcards): define refactor input contract`
- `2f7917c` `feat(flashcards): add generated learning note contract`
- `430132d` `build(detekt): relax domain complexity thresholds`
- `a816a21` `feat(data): add generated learning note parser`
- `9c60683` `feat(flashcards): wire parallel learning note generation`
- `c18ad76` `feat(app): preview generated learning notes`
- `077bd61` `refactor(flashcards): unify new card generation flow`

## Current Next Action

The most valuable next implementation step is:

- align local sync and Supabase with the expanded flashcard schema
- then migrate study/review to consume `studyCards`

## Update Rule

Update this file whenever one of these changes happens:

- a phase is closed
- a new layer starts migration
- a compatibility path is introduced or removed
- a major blocker appears
- a commit materially changes the refactor state
