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
- `data` has started the migration path with new DTOs and a parser for `GeneratedLearningNote`
- the old generation flow is still active in the app
- persistence and UI are still based on the old flashcard model

## High-Level Progress

Estimated qualitative progress:

- Product and learning contract definition: `done`
- Input contract: `done in domain`, `not yet integrated in app`
- Output contract: `done in domain`, `partially started in data`
- Prompt migration: `not started`
- Repository integration: `not started`
- Preview migration: `not started`
- Persistence migration: `not started`
- Study-mode migration: `not started`
- Quality gate before save: `partially defined`, `not integrated`

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

Not integrated yet:

- prompt output schema
- repository generation path
- preview path
- save path

Reference:

- [FLASHCARD_PHASE2_OUTPUT_CONTRACT.md](/Users/emm/AndroidStudioProjects/Hello/docs/flashcard-refactor/FLASHCARD_PHASE2_OUTPUT_CONTRACT.md)

## Phase 3

Status:

- `not started`

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

- full prompt migration replacing old schema
- parser selection strategy
- migration of generation repository methods in UI-facing flow
- compatibility strategy between old and new generation responses

## App

Status:

- `started`

Current situation:

- UI already captures optional disambiguation fields in the word or phrase flow
- preview can render `GeneratedLearningNote` and derived cards
- save still uses a temporary adapter back to the old flashcard model
- category flow still uses the old generation path

Pending:

- render validation errors and warnings more explicitly
- stop relying on temporary mapping to `FlashcardGenerated` for save
- decide whether category mode will be migrated or replaced
- support editing the learning note preview before save

## Local Database

Status:

- `not started`

Pending:

- define storage model for learning notes and derived cards
- decide coexistence or replacement strategy for old flashcard tables
- update SQLDelight queries and entities

## Supabase

Status:

- `not started`

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

Pending:

- repository integration tests
- prompt parser compatibility tests
- UI tests for new creation flow

## Compatibility State

The project currently has two parallel models:

- old path: `FlashcardGenerated`
- new path: `GeneratedLearningNote`

This is acceptable temporarily, but it is a controlled transition risk.

It becomes dangerous if:

- both paths remain active too long
- prompt output starts drifting between schemas
- preview or save uses mismatched contracts
- a temporary UI adapter hides missing persistence work

## Known Risks

- The old prompt still targets the old schema.
- The repository still returns the old generated flashcard model.
- The UI still assumes one flashcard, not one note plus cards.
- The database still stores the old card structure only.
- The longer the coexistence lasts, the more mapping glue will be needed.

## Recommended Next Step

The next step should be:

1. start Phase 3 at the integration edge
2. rewrite prompt output for one safe generation path
3. adapt repository parsing to produce `GeneratedLearningNote`
4. keep old flow in place only as temporary compatibility

More concretely:

- add a new prompt builder for `GeneratedLearningNote`
- connect `DefaultFlashcardRepository` to the new parser in a parallel method
- validate the generated note with `ValidateGeneratedLearningNoteUseCase`
- only after that, migrate preview UI

## Recent Commits

- `b1b791e` `feat(flashcards): define refactor input contract`
- `2f7917c` `feat(flashcards): add generated learning note contract`
- `430132d` `build(detekt): relax domain complexity thresholds`
- `a816a21` `feat(data): add generated learning note parser`

## Current Next Action

The most valuable next implementation step is:

- integrate `GenerateLearningNotePreviewUseCase` into a safe preview path
- decide whether the current `NewCardScreen` gets a parallel developer-only flow or a full replacement
- begin replacing `FlashcardGenerated` in the preview stage before touching persistence

## Update Rule

Update this file whenever one of these changes happens:

- a phase is closed
- a new layer starts migration
- a compatibility path is introduced or removed
- a major blocker appears
- a commit materially changes the refactor state
