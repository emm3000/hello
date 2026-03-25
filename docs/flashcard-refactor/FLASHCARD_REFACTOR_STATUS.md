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

The refactor has completed the contract-definition stage and is now in the replacement stage.

Current reality:

- Phase 0 is defined and closed at the contract level
- Phase 1 is defined and implemented in `domain`
- Phase 2 is defined and implemented in `domain`
- `data` has a working generation path, parser, and rich local persistence for `GeneratedLearningNote`
- the app generation path now goes through `GeneratedLearningNote`
- local persistence now stores note-rich fields and derived cards inside `Flashcard`
- local storage is now aligned with the account-scoped sync model
- remote sync is aligned with the rich flashcard payload
- local schema cleanup removed unused legacy tables
- study mode now expands one learning note into multiple derived review items
- study mode now supports typed answers for exact and flexible-text cards
- study mode now renders better guidance and support text by derived card type
- study mode now restricts grading options after typed-answer evaluation
- cloze and form cards now have dedicated support blocks aligned with core UI components
- the old `FlashcardGenerated` generation path is being retired

## Pre-Development Operating Rule

This refactor is happening before production users and before any real backward-compatibility promise exists.

For this stage:

- compatibility is not a deliverable
- legacy support is not a deliverable
- preserving old local persistence is not a deliverable
- preserving old remote schema is not a deliverable
- destructive schema changes are allowed if they improve the flashcard core

The main goal is to make the flashcard creation core correct and strong. If old structures interfere with that goal, they should be removed or replaced, not maintained.

## High-Level Progress

Estimated qualitative progress:

- Product and learning contract definition: `done`
- Input contract: `done and integrated in app`
- Output contract: `done and integrated in data/app`
- Prompt migration: `done for the new generation path`
- Repository integration: `done for generation and save`
- Preview migration: `done`
- Persistence migration: `substantially complete locally`
- Study-mode migration: `started and materially advanced around derived cards`
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

- `substantially complete`

Completed focus:

- prompts now generate `GeneratedLearningNote`
- artificial randomness constraints were removed from the main generation path
- prompt output is aligned with parser and DTO expectations

## Phase 4

Status:

- `partially integrated`

Planned focus:

- quality gate before save
- fail and warning rules in app flow

## Phase 5

Status:

- `started`

Implemented:

- learning-note preview now supports inline editing of key note fields before save
- derived card prompt and expected answer can now be edited from the preview
- preview validation is recomputed immediately after inline edits
- example, cloze, and individual derived cards now support targeted regeneration from the preview
- rich note fields such as `whyUseful`, `usagePattern`, and `commonMistake` now support targeted regeneration from the preview
- quality warnings are now surfaced closer to the affected meaning, example, or card sections in the preview instead of only as a global summary
- derived cards now expose finer preview controls, including inline hint editing and active/inactive toggling before save

Pending focus:

- decide whether some quality warnings should trigger direct regenerate actions from the exact affected section
- decide whether Phase 5 needs more regenerate shortcuts or if the next highest-value work is now real-device sync validation

## Phase 6

Status:

- `substantially complete`

Implemented:

- session queue now expands a flashcard into multiple `StudySessionItem`s
- review scheduling runs once per note after its derived cards are answered
- study UI renders the active derived card instead of flattening to a single front/back pair
- typed-answer flow exists for `Exact` and `FlexibleText` cards
- answer reveal is now gated until typed-answer checking completes
- front and back study UI now adapt support copy based on `StudyCardType`
- typed-answer grading is now constrained to avoid obviously incoherent outcomes
- cloze and form cards now render dedicated support blocks using shared UI components

Pending:

- focused UI tests for derived-card study flows
- decide whether sibling derived cards should stay adjacent or be buried/interleaved later

## Phase 7

Status:

- `started and structurally advanced`

Implemented:

- rich flashcard persistence in local storage
- local schema cleanup of unused tables
- local storage alignment with account-scoped sync model
- account-scoped keys in local SQLDelight entities
- stronger local foreign keys for review projection and event linkage
- remote hot-path indexes added for account/deck/sync reads

Pending:

- validate end-to-end sync on multiple physical devices after the local schema reshape
- decide whether some rich JSON fields remain embedded or get extracted later

## Recommended Next Step

The highest-value next step is still Phase 5, but now it is about tightening preview UX rather than unlocking basic editability.

Why:

- Phase 5 already supports editing, inline validation, and targeted regeneration for cards and rich note fields
- Phase 6 is already strong enough to support the new retrieval model
- Phase 7 is mostly blocked on real-device validation rather than more code churn
- the biggest remaining product gap is helping the user understand and fix generation quality issues more locally before saving

Recommended task sequence:

1. make warnings and quality issues more actionable at the exact field or card they affect
2. add richer per-card controls where the preview still feels too generic
3. only after that, run physical-device sync validation for the richer payload end-to-end

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

- stronger app-level handling of validation warnings before save

## Data

Status:

- `migrated to the new generation path`

Completed:

- DTOs for generated learning note response
- parser for generated learning note response
- parser tests
- repository method for generated learning note
- prompt builder for generated learning note
- old `FlashcardGenerated` generation path removed from repository/domain
- parser now rejects learning notes that fail domain validation
- `cards` and `quality_checks` are required in the AI response DTO contract
- local SQLDelight schema is now account-scoped where data belongs to an app account
- local-first review state now has tighter relational guarantees

Pending:

- add repository integration tests for rich persistence
- decide whether to keep JSON-embedded derived cards or extract them later
- run multi-device sync validation after the schema reshape

## App

Status:

- `started`

Current situation:

- UI already captures optional disambiguation fields in the word or phrase flow
- preview can render `GeneratedLearningNote` and derived cards
- word or phrase save now persists directly from `GeneratedLearningNote`
- category flow also uses the new generation contract through mapped communicative intent
- UI now distinguishes invalid input, invalid AI output, and save failures
- save is now gated by explicit `canSavePreview` state instead of implicit assumptions
- preview shows quality checks and warnings before save

Pending:

- support editing the learning note preview before save
- typed answer / richer study interactions for production cards

## Validation Boundary

Current hard validation layers:

- `data`: the AI response parser rejects malformed or pedagogically invalid learning notes
- `domain`: the generated note validator rejects missing active cards and failed quality checks
- `save`: `CreateFlashcardUseCase` refuses to persist invalid generated notes
- `study`: session generation no longer invents fallback study cards

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

The desired end state is no compatibility layer.

Current reality:

- old model remnants may still exist in code
- the active `NewCard` flow already runs on `GeneratedLearningNote`
- any remaining legacy path should be treated as temporary debt to delete

It becomes dangerous if:

- both paths remain active too long
- prompt output starts drifting between schemas
- sync or read paths keep flattening rich data silently
- legacy code is preserved only to avoid changing local or remote schema

## Known Risks

- legacy structures survive longer than necessary and dilute the new core
- study mode still assumes a flat front/back card in some places
- schema rewrites may be frequent while the product loop is still being discovered
- rich fields are embedded as JSON in local persistence; that is pragmatic now, but may become limiting later

## Recommended Next Step

The next step should be:

1. finish Phase 7 for sync and remote schema
2. start Phase 6 for actual study-card-based review
3. remove dead compatibility paths aggressively instead of maintaining them

More concretely:

- expand Supabase `flashcard` schema with the same rich fields
- update sync payload handling for rich flashcard data
- adapt `StudyScreen` and session loading to prefer derived study cards
- delete the remaining `FlashcardGenerated` path instead of supporting coexistence

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
