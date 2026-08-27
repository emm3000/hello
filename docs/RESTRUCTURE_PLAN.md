# Restructure Plan — Session First

| Field | Value |
|---|---|
| Status | Active |
| Role | Plan and delivery history of the session-first product restructure |
| Source of Truth | Yes for phase scope and order; `*_CURRENT.md` and the code win for behavior |
| Read this when | You're picking up the next restructure phase or auditing a shipped one |
| Last verified | 2026-08-27 |

## Why

The app was organized around managing a collection (decks, tags, filters, a seven-step card wizard) while the daily job is running a study session. The audit on 2026-08-26 found the friction was structural, not visual:

- Card creation asked for a mode before the user typed anything, then gated saving behind an 8–12 s AI wait and a power-user editor.
- The Study screen showed a `Start` interstitial after the user had already tapped "Estudiar ahora".
- One flashcard expanded into N study items, but a single review was persisted with the most conservative grade, silently. The progress counter counted items, not cards.
- Search matched deck names, not card content.
- Grading needed a hint card to explain four buttons plus interval previews.

The foundation is sound and stays: `:domain` (FSRS-6, value objects, use cases), `:data` (schema, backup, AI), `core/ui/H*` + the Instrument theme, notifications.

## Target

Three surfaces instead of seven loosely related features:

| Surface | Job | Replaces |
|---|---|---|
| **Hoy** | Open the app, see what is due, one tap into the session. Zero due: what comes next + capture CTA. | Dashboard as home |
| **Capturar** | One field. Type or dictate a word, save. The card is studiable immediately; AI enrichment runs in the background. | Mode → Input → Review wizard |
| **Biblioteca** | Every card, searchable by content. Decks become an optional grouping. Card detail, edit and the advanced AI editor live here, one tap deep. | Dashboard deck list + Deck Detail + tags |

Study session: 1 flashcard = 1 visible review = 1 grade. Two grade buttons (`AGAIN` / `GOOD`), `EASY` behind long-press. Typed-answer and cloze items leave the SRS loop (candidate for a later optional practice mode).

## Progress

| Phase | Scope | Status | Commits |
|---|---|---|---|
| 0 — Plan | This document, roadmap re-pointed | ✅ Done | — |
| 1 — Study session | Single review per flashcard, no `Start` stage, binary grading with long-press easy, example + part of speech on the card back | ✅ Done | `64cfe69`, `d93a1dc`, `9be2a1b`, `d3adb9c`, `b94bee8` |
| 2 — Capture | Single-field capture, background AI enrichment worker, mode selector removed | ⏳ Pending | — |
| 3 — Hoy | Session-first home replacing Dashboard | ⏳ Pending | — |
| 4 — Biblioteca | All-cards list with content search; absorbs Deck Detail; decks as optional filter | ⏳ Pending | — |
| 5 — Cleanup | Dead code (mode screen, deck tags), docs resync, roadmap close | ⏳ Pending | — |

## Phase 1 — Study session

Work units, each green on `./gradlew detekt testDebugUnitTest :domain:test` before commit:

1. `refactor(study)` — `StudyFlashcard` maps to exactly one `StudySessionItem`; the review is persisted immediately with the grade as given. `Start` and `Check` stages, `StudyAnswerPolicy`, typed-answer UI and the exit confirmation are removed (every grade is already persisted, so leaving loses nothing).
2. `feat(study)` — Grade dock becomes two buttons, "No la sabía" / "La sabía", long-press on the second fires `EASY`. Interval previews, the grade hint card and the `hasSeenGradeHint` flag are removed end-to-end. Swipe-to-grade keeps two zones.
3. `feat(study)` — `StudyFlashcard` gains `partOfSpeech`, `example`, `exampleTranslation` (additive read model, one new query, no migration). The card back shows translation, meaning, `IPA · POS`, the example and irregular forms.

Deliberately unchanged: `ReviewGrade` keeps four values (FSRS rating is ordinal-based); `GeneratedStudyCard`s keep being generated and stored, the session ignores them until Phase 2 decides.

Shipped: unit 1 as `64cfe69`, unit 2 as `d93a1dc` + `9be2a1b`, unit 3 as `d3adb9c` + `b94bee8`. `HARD` is now unreachable from the dock — it survives only as an FSRS ordinal and in backup import. `PreviewNextInterval` went with unit 2 and no longer exists.

## Schema cleanup (out of phase)

Run on 2026-08-27, between Phase 2 units 2 and 3, after an audit of the schema.

- `a673aad` `refactor(data)` — `LocalAccountState`, `OperationLog`, `SyncCheckpoint` and
  `AppliedRemoteOperation` dropped in migration `3.sqm`. Four tables, five indexes and
  eighteen queries backing a remote-sync path that was never activated; every one had zero
  Kotlin references. `LocalDeviceIdentity` stays: it backs the install identity and its
  `lamportCounter` is serialized into the backup envelope.
- `391fc07` `refactor(data)` — the `StudiableFlashcard` view (`4.sqm`) owns the two
  predicates that decide whether a card may be scheduled, alive and enriched. The five due
  queries select from it instead of repeating them. This is the structural fix for the
  class of bug `56a9491` had to correct by hand. No Kotlin file changed: SQLDelight
  generates the same result types.
- `c308af8` `fix(export)` — backup import wrote `enrichmentStatus` straight from the JSON.
  The read path maps an unknown value to `ENRICHED` while the SQL compares the raw string,
  so a typo'd backup produced a card that looked complete in the library and never appeared
  in a session. Import now writes the decoded enum name.

Deliberately not done: `CHECK` constraints on the enum-shaped `TEXT` columns. SQLite has no
`ALTER TABLE ADD CONSTRAINT`, so each one costs a full table rebuild, and the only untrusted
writer was the import path that `c308af8` already closed. The dead SM-2 columns on
`ReviewProjection` and the duplicated `grade` / `rating` pair on `ReviewEvent` are the same
trade and wait for FSRS to settle. Renaming `LocalFirst.sq`, which now holds only device
identity and review scheduling, would rename the generated `localFirstQueries` across
thirteen files; it waits for Phase 5.

## Phase 2 — Capture

Today `CreateFlashcardUseCase` is the only creation path in `:domain`, and it opens with
`validateGeneratedLearningNoteUseCase(learningNote).requireValid()`. There is no way to
create a flashcard from a bare word — `DefaultSeedDataInitializer` only manages it by
calling `FlashcardRepository.create` directly and skipping the use case. So "save now,
enrich later" is a second creation path in the domain before it is a screen.

Decision taken up front: **a card that has not been enriched is not due.** All three due
queries filter it out, so FSRS never receives a grade for a card whose back face is empty.
The cost is that a card whose enrichment failed would silently vanish from the product, so
Capturar must surface the pending and failed count with a retry. That is part of unit 3,
not a follow-up.

Work units, each green on `./gradlew detekt testDebugUnitTest :domain:test` before commit:

1. `feat(domain)` — `EnrichmentStatus` (`PENDING` / `ENRICHED` / `FAILED`) joins `Flashcard`.
   `CaptureFlashcardUseCase(deckId, word)` creates a minimal card as `PENDING`, enforcing
   the same in-deck uniqueness the generated path enforces. `CreateFlashcardUseCase` keeps
   its contract and produces `ENRICHED`. Tests first, and each one must fail before the
   change.
2. `feat(data)` — the `enrichmentStatus` column and its migration. `countDueFlashcards`,
   `flashcardsToReviewByDeck` and `flashcardsToReviewAllDecks` all gain the filter.
   `FlashcardEnrichmentWorker` takes a `flashcardId`, calls
   `FlashcardGenerationRepository.generateLearningNote`, applies the note and flips the
   status; a quota `Exceeded` or a throw leaves `FAILED` and schedules a retry.
3. `feat(capture)` — the Capturar screen. One field, type or dictate, save. It becomes the
   `HFab` destination and shows the pending / failed count with a retry affordance.
4. `refactor(card)` — the wizard stops being the capture path. `NewCardModeScreen` and
   `TypeView` are deleted. The input, review and preview editors survive untouched: they
   are the advanced AI editor that Phase 4 moves behind Biblioteca, reachable from a card
   rather than from the FAB.

Deliberately unchanged: `GeneratedLearningNote` and its validation policies, the preview
and regeneration components, and the 50/day quota. Phase 2 adds a second entrance; it does
not rewrite the generated path.

## Deferred (was in `FEATURE_ROADMAP.md`)

- Stats history / heatmap — vanity metric for a single-user app; revisit after Phase 3.
- Flashcard-level tags — doubles down on the organizing axis the restructure removes.
- Cram mode — after the session model settles.

Still valid: Notifications Sprint 2 (`docs/NOTIFICATIONS_PLAN.md`) and global flashcard search (now Phase 4).
