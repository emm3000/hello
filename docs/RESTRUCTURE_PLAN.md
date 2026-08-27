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
| **Biblioteca** | Every card, searchable by content. Decks become an optional grouping, and deck management moves to Settings. Card detail, edit and the advanced AI editor live here, one tap deep. | Dashboard deck list + Deck Detail + tags |

Study session: 1 flashcard = 1 visible review = 1 grade. Two grade buttons (`AGAIN` / `GOOD`), `EASY` behind long-press. Typed-answer and cloze items leave the SRS loop (candidate for a later optional practice mode).

## Progress

| Phase | Scope | Status | Commits |
|---|---|---|---|
| 0 — Plan | This document, roadmap re-pointed | ✅ Done | — |
| 1 — Study session | Single review per flashcard, no `Start` stage, binary grading with long-press easy, example + part of speech on the card back | ✅ Done | `64cfe69`, `d93a1dc`, `9be2a1b`, `d3adb9c`, `b94bee8` |
| 2 — Capture | Single-field capture, background AI enrichment worker, mode selector removed | ✅ Done | `ef990ba`, `5670093`, `84e93f7`, `56a9491`, `ea2313b`, `4c0bed8`, `71c923e`, `24ab589` |
| 3 — Hoy | Session-first home replacing Dashboard | ◐ Partly done | `34ee414`, `b7524b2`, `c1c698b` |
| 4 — Biblioteca | All-cards list with content search; absorbs Deck Detail; decks as optional filter | ✅ Done | `d5ec1c4`, `06ea470`, `22f1f4d`, `8a17035`, `7b41631`, `bf1becd`, `970166e`, `039dd1a`, `67674f9` |
| 5 — Cleanup | Dead code, package rename, docs resync, roadmap close | ✅ Done | `ea4d2fe`, `569b716` |

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

Unit 4 as executed went further than "untouched", and deliberately. `TypeView` drove three
input modes, so deleting it forced a choice of which one the wizard keeps. `WordOrPhase`
won, because it is the only mode whose input is a word the user already has — the shape
Phase 4 needs when the editor is reached *from a card*. `WithCategories` and `WithAiHelp`
generated a word the user never chose, and both went with the enum: the category bottom
sheet, `StaticCategories`, `aiRequest`, and the keyword-inference block that mapped free
text onto a `communicativeIntentId` and a `LearningDomain`. The wizard is now two steps.

Deliberately unchanged: `GeneratedLearningNote` and its validation policies, the preview
and regeneration components, and the 50/day quota. Phase 2 adds a second entrance; it does
not rewrite the generated path.

## Phase 3 — Hoy

Work units:

1. `feat(domain)` — `NextDueBatch` (`at` / `cardCount` / `daysFromToday`).
   `DashboardStats` gains a nullable `nextDue`, computed only when nothing is
   due today. The new `nextReviewAtAfter` query uses an INNER join on purpose:
   a NULL `nextReviewAt` means never-reviewed, which is due now, not future.
2. `refactor(hoy)` — the hero becomes the action. One full-width `Xl` accent
   pill with the count inside it as mono supporting context. `HButtonSize.Xl`
   (88 dp) was added because `Lg` at 56 dp cannot hold two lines. Zero-due
   reads "Nada que repasar hoy" plus the next batch plus a capture CTA, and
   renders no CTA at all rather than a disabled slab. `DashboardStatsSection`
   was dead code and got wired under "Tu progreso", below the fold.
3. `refactor(hoy)` — rename the `dashboard` package to `hoy`.

Shipped: unit 1 as `34ee414`, unit 2 as `b7524b2`, plus `c1c698b` for a bug the
unit-2 device run exposed — the study streak read zero in any zone behind UTC,
because `LocalFirst.sq` bucketed by UTC day while `computeStreak` compared that
bucket as a local date. `GetDashboardStatsUseCase` now takes a `ZoneId`. Twelve
green tests had covered a broken implementation because the fake emitted local
midnight while production emitted UTC midnight: when a fake stands in for a SQL
query, it must emit the query's actual output shape.

Unit 3 shipped later, as `569b716`, once Phase 4 had reduced the screen — a
rename over a file that was about to lose 500 lines is a diff nobody can review.

**Still open.** `DESIGN_BRIEF.md` (125–134) specifies an AI flow for the
zero-due state: a situation in natural language plus ~6 candidate words, the
user picks, and the prompt is seeded with the last 15–20 captured words so the
model calibrates level. The plan table only ever asked for "what comes next +
capture CTA"; the brief is later and more specific. Its AI half cannot be
verified on device while Firebase AI Logic is deactivated in the project, so
the non-AI half shipped alone and the suggestion flow waits on App Check.

## Phase 4 — Biblioteca

The plan's one line — "all-cards list with content search; absorbs Deck Detail;
decks as optional filter" — left one thing unanswered: Deck Detail was also
where a deck was renamed and deleted. Deleting the screen without a home for
those two actions would have dropped them from the product.

Decision taken up front: **deck management goes to Settings**, under a new
Organización section. A deck is now an optional grouping, so it belongs with
the things you set up once, not on the daily path — and it keeps Biblioteca
about cards.

Work units, each green on `./gradlew detekt testDebugUnitTest :domain:test`
before commit:

1. `fix(ui)` — the disabled button. `instrumentPrimary` #F2F5F7 at 38 % over
   `instrumentBg` #08090A composites to #616364, a bright mid grey that reads
   as enabled. Alpha over a near-white container lightens the page instead of
   dimming the control. Disabled now resolves to tokens: filled variants recess
   to `instrumentSurface2`, transparent ones stay transparent.
2. `feat(domain)` — `LibraryFlashcard`, `LibraryRepository`,
   `SearchLibraryUseCase` and `searchNormalized`. Matching is accent and case
   insensitive on both sides. Nine tests, all nine red first.
3. `feat(data)` — `libraryFlashcards` joins Deck for the name and left-joins
   `ReviewProjection` for `nextReviewAt`.
4. `feat(library)` — the screen. Dense rows, persistent search, deck chips as
   an optional narrowing. Empty-library and no-results are told apart from the
   filter alone, with no second count.
5. `feat(deck)` — Mazos under Settings, and deck deletion moved into the deck
   form in edit mode, mirroring what Phase 2.7 did for cards.
6. `refactor(hoy)` — Hoy sheds search, tags, the deck list and both empty
   states. State drops from ten fields to two, the ViewModel from five
   dependencies to one, the screen from 810 lines to 300.
7. `refactor(deck)` — Deck Detail deleted.
8. `fix(library)` — a regression from unit 7: removing Deck Detail removed the
   only listener for `UndoEvent.CardDeleted`, so a deleted card vanished with
   no undo. Biblioteca is where the back navigation lands, so it took over the
   listener, the snackbar and the restore.

Deliberately not done: per-deck study. `StudyRoute` still accepts a single deck
id and nothing passes one any more. Adding a "study this deck" action to the
chip row is a feature, not a Phase 4 obligation.

The device run on `medium_phone` verified all eight units and found one defect
no gate could: Biblioteca's header read "1 TARJETAS" once a search narrowed the
list to one card. `library_card_counter` was a plain string with a `%1$d`, which
cannot agree in number. Fixed as `67674f9`, together with `cards_count` in
`DeckRow`, which carried the same defect from before this phase and which the
new Mazos screen had started to surface. detekt, 113 unit tests and the Compose
previews all passed over both.

## Phase 5 — Cleanup

- `ea4d2fe` — `GetFilteredDecksUseCase`, `DeckSearchCriteria`,
  `DeckRepository.observeFiltered` and its six-table SQL, `GetDeckDetailUseCase`
  and `DeckRepository.fetchTagsForDeck` all lost their last caller with the deck
  list and Deck Detail. 157 of 434 string resources had no reference left in any
  Kotlin or XML file, and the two mascot drawables went with the empty state
  that was their only caller. One locale, so nothing fell out of translation.
- `569b716` — the `dashboard` package, its seven types and the fifteen surviving
  `dashboard_*` string keys become `hoy`. `DashboardStats` and
  `GetDashboardStatsUseCase` keep their names: in `:domain` the word describes a
  set of study metrics, not the screen that renders them.

Deliberately not done: `ObserveFlashcardsWithReviewUseCase` and
`StudySessionRepository.flashcardWithReview` are unreachable, but
`FlashcardWithReviewMappingTest` is the only coverage of the row-to-
`StudyFlashcard` mapping that the live session queries share. Re-pointing that
coverage at a live query is a work unit with its own falsifier, not part of a
sweep.

## Deferred (was in `FEATURE_ROADMAP.md`)

- Stats history / heatmap — vanity metric for a single-user app; revisit once Phase 3 closes.
- Flashcard-level tags — doubles down on the organizing axis the restructure removes.
- Cram mode — after the session model settles.

Global flashcard search shipped as Phase 4. Still open: Notifications Sprint 2
(`docs/NOTIFICATIONS_PLAN.md`), the settings time picker deferred out of the
redesign, and the Phase 3 zero-due AI suggestion, which is blocked on Firebase
App Check rather than on engineering.
