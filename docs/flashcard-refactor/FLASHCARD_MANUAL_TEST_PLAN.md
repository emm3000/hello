# Flashcard Refactor Manual Test Plan

## Purpose

This document defines the manual test plan to decide whether the flashcard refactor is operationally done.

The goal is to validate the full flow:

- generation
- editable preview
- save
- study
- local-first behavior
- sync between devices

## Test Setup

Before running the tests:

1. Prepare two devices or two clean app installations.
   - `Device A`
   - `Device B`
2. Make sure both point to the same backend environment.
3. Start with clean local state on both devices if possible.
4. Make sure at least one deck exists on `Device A`.
5. If pairing is part of the test, `Device A` must be able to generate a pairing code and `Device B` must be able to redeem it.

## Pass Criteria

The refactor can be considered operationally validated if:

- the app does not crash during bootstrap, generation, save, study, or sync
- preview editing and targeted regeneration work without corrupting the note
- invalid notes cannot be saved
- saved flashcards preserve rich note fields and derived cards
- study mode behaves consistently with the new derived-card model
- sync preserves flashcards, rich payloads, and review progress across devices
- basic offline then reconnect flows work without duplication or loss

## Critical Failures

Any of these should block closure:

- crash with `Current app account is not available`
- preview allows saving an invalid note
- sync drops rich fields or `studyCards`
- review state duplicates or disappears between devices
- pairing links devices to different logical accounts by mistake

## Manual Test Cases

### 1. Cold Start Without Existing Account

Steps:

1. Install and open the app on `Device A`.
2. Wait for the dashboard to load.
3. Do not perform any action yet.

Expected:

- the app does not crash
- the dashboard opens even before account bootstrap finishes
- there is no Koin error
- there is no `Current app account is not available` error

### 2. Bootstrap First Logical Account

Steps:

1. On `Device A`, complete the initial flow that triggers anonymous bootstrap.
2. Wait a few seconds.
3. Fully close and reopen the app.

Expected:

- the app opens normally after restart
- local state is preserved
- there is no startup crash from sync scheduling

### 3. Pair Second Device

Steps:

1. On `Device A`, generate a pairing code.
2. Open the clean app installation on `Device B`.
3. Redeem the pairing code on `Device B`.
4. Wait for initial sync.

Expected:

- `Device B` joins the same logical account as `Device A`
- `Device B` does not create a parallel account
- pairing completes without crash

### 4. Generate Base Learning Note

Suggested input:

- word: `hello`

Steps:

1. On `Device A`, open the new card screen.
2. Select a deck.
3. Enter `hello`.
4. Generate the preview.

Expected:

- a learning note preview appears
- derived cards are visible
- loading finishes normally
- if the note is valid, save is enabled

### 5. Inline Validation For Required Field

Steps:

1. In the preview, clear `exampleSentence`.
2. Observe UI state.
3. Re-enter a valid example sentence.

Expected:

- an inline error appears on the affected field
- save becomes disabled when the field is invalid
- the error disappears after correction
- save becomes enabled again when the note is valid

### 6. Edit Derived Card Fields

Steps:

1. In the preview, edit the `prompt` of one derived card.
2. Edit its `expectedAnswer`.
3. Edit its `hint`.

Expected:

- changes appear immediately in the preview
- the rest of the note remains intact
- validation recalculates without crash

### 7. Toggle Derived Card Active State

Steps:

1. In the preview, deactivate one derived card.
2. Observe warnings and save state.
3. Reactivate it.

Expected:

- the active state changes immediately
- if valid active cards remain, the note can still be saved
- if the note becomes invalid because of inactive cards, save is blocked consistently

### 8. Regenerate Example Only

Steps:

1. Tap `Regenerar ejemplo`.

Expected:

- only the example sentence and translation change
- the rest of the note remains intact
- loading finishes normally

### 9. Regenerate Cloze Only

Steps:

1. If the note has a cloze sentence, tap `Regenerar cloze`.

Expected:

- only the cloze sentence changes
- validation remains coherent

### 10. Regenerate Rich Note Fields

Steps:

1. Tap `Regenerar why useful`.
2. If present, tap `Regenerar usage pattern`.
3. If present, tap `Regenerar common mistake`.

Expected:

- only the requested field changes each time
- the rest of the note remains intact
- the preview remains editable after regeneration

### 11. Regenerate Single Derived Card

Steps:

1. Tap `Regenerar card` on one derived card.

Expected:

- only that card changes
- the other derived cards remain unchanged
- preview state remains stable

### 12. Localized Quality Guidance

Steps:

1. Generate a note with warnings, or manually create a situation likely to trigger warnings.
2. Inspect the meaning, example, and derived-card sections.

Expected:

- relevant warnings appear near the affected section
- warnings are not only shown as a single global summary
- the guidance helps identify what should be fixed

### 13. Save Valid Flashcard

Steps:

1. From a valid preview, save the flashcard.

Expected:

- a success message appears
- the form is cleared
- the flashcard becomes visible in list and detail flows

### 14. Persist Rich Fields

Steps:

1. Open the detail screen for the saved flashcard.

Expected:

- `whyUseful` is present when applicable
- `usagePattern` is present when applicable
- `commonMistake` is present when applicable
- `clozeSentence` is present when applicable
- `studyCards` are present
- `qualityChecks` are present
- `warnings` are present if they were saved

### 15. Study Derived Cards

Steps:

1. Start a study session using the saved flashcard.
2. Go through multiple derived cards from the same note.

Expected:

- study does not behave like a single generic front/back card
- each derived card type shows distinct support and presentation
- the experience differs appropriately for recognition, production, cloze, and form

### 16. Typed Answer Enforcement

Steps:

1. Find an `Exact` or `FlexibleText` card.
2. Try to proceed without entering an answer.
3. Submit an incorrect answer.
4. On another card, submit a correct answer.

Expected:

- grading is not available before answer checking
- after an incorrect answer, grading options are restricted consistently
- after a correct answer, incoherent grading options remain blocked

### 17. Review Scheduling Consistency

Steps:

1. Complete a short study session.
2. Leave and re-enter study.
3. Inspect next review behavior.

Expected:

- review progression is consolidated at the note level
- there are no obvious duplicated review states across derived cards
- next review behavior remains coherent

### 18. Create On A, Receive On B

Steps:

1. On `Device A`, create and save a new flashcard.
2. Wait for push sync.
3. On `Device B`, wait for pull sync or trigger refresh.

Expected:

- the flashcard appears on `Device B`
- rich fields arrive, not only basic word/translation data
- derived cards arrive correctly
- the payload is not truncated

### 19. Preview Edits Do Not Sync Before Save

Steps:

1. On `Device A`, generate a preview.
2. Edit several preview fields.
3. Do not save.
4. Inspect `Device B`.

Expected:

- `Device B` does not receive preview-only changes
- only persisted data syncs after save

### 20. Study On B, Reflect On A

Steps:

1. On `Device B`, study the shared flashcard.
2. Wait for sync.
3. Refresh or inspect the same content on `Device A`.

Expected:

- `Device A` receives the updated review state
- the flashcard remains intact
- review events are not duplicated visibly

### 21. Create Offline On A, Sync Later

Steps:

1. Disconnect `Device A` from the network.
2. Create and save two or three flashcards.
3. Confirm they are visible locally.
4. Reconnect `Device A`.
5. Wait for sync.
6. Inspect `Device B`.

Expected:

- `Device A` works offline
- pending operations sync after reconnection
- `Device B` eventually receives all new flashcards
- there are no duplicates or missing records

### 22. Review Offline On B, Sync Later

Steps:

1. Disconnect `Device B` from the network.
2. Complete a short study session.
3. Reconnect `Device B`.
4. Inspect review state on `Device A`.

Expected:

- offline review events are uploaded after reconnect
- `Device A` receives the updated review state
- there is no crash from outbox or sync scheduling

### 23. Restart During Normal Usage

Steps:

1. Open the app on `Device A`.
2. Generate and save a flashcard.
3. Fully close the app.
4. Reopen it.

Expected:

- the saved flashcard remains available
- bootstrap remains stable
- there is no null-account startup crash

### 24. Restart With Pending Sync

Steps:

1. Create or modify data on `Device A`.
2. Close the app before sync fully completes.
3. Reopen the app with network available.

Expected:

- sync resumes correctly
- the change is not lost
- the change is not duplicated

## Suggested Execution Record

Use this simple format while running the plan:

- `PASS` `TC-01` Cold Start Without Existing Account
- `PASS` `TC-02` Bootstrap First Logical Account
- `FAIL` `TC-18` Create On A, Receive On B
  - observed:
  - expected:
  - notes:

## Final Closure Rule

The flashcard refactor can be considered done when:

1. all critical flows above pass
2. no critical failure is observed
3. sync between paired devices is validated with the new rich payload
4. the full flow `generate -> preview/edit -> save -> study -> sync` is stable
