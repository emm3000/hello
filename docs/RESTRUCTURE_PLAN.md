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
| 3 — Hoy | Session-first home replacing Dashboard; unify the three duplicated due-count queries | ⏳ Pending | — |
| 4 — Biblioteca | All-cards list with content search; absorbs Deck Detail; decks as optional filter | ⏳ Pending | — |
| 5 — Cleanup | Dead code (mode screen, deck tags), docs resync, roadmap close | ⏳ Pending | — |

## Phase 1 — Study session

Work units, each green on `./gradlew detekt testDebugUnitTest :domain:test` before commit:

1. `refactor(study)` — `StudyFlashcard` maps to exactly one `StudySessionItem`; the review is persisted immediately with the grade as given. `Start` and `Check` stages, `StudyAnswerPolicy`, typed-answer UI and the exit confirmation are removed (every grade is already persisted, so leaving loses nothing).
2. `feat(study)` — Grade dock becomes two buttons, "No la sabía" / "La sabía", long-press on the second fires `EASY`. Interval previews, the grade hint card and the `hasSeenGradeHint` flag are removed end-to-end. Swipe-to-grade keeps two zones.
3. `feat(study)` — `StudyFlashcard` gains `partOfSpeech`, `example`, `exampleTranslation` (additive read model, one new query, no migration). The card back shows translation, meaning, `IPA · POS`, the example and irregular forms.

Deliberately unchanged: `ReviewGrade` keeps four values (FSRS rating is ordinal-based); `GeneratedStudyCard`s keep being generated and stored, the session ignores them until Phase 2 decides.

Shipped: unit 1 as `64cfe69`, unit 2 as `d93a1dc` + `9be2a1b`, unit 3 as `d3adb9c` + `b94bee8`. `HARD` is now unreachable from the dock — it survives only as an FSRS ordinal and in backup import. `PreviewNextInterval` went with unit 2 and no longer exists.

## Deferred (was in `FEATURE_ROADMAP.md`)

- Stats history / heatmap — vanity metric for a single-user app; revisit after Phase 3.
- Flashcard-level tags — doubles down on the organizing axis the restructure removes.
- Cram mode — after the session model settles.

Still valid: Notifications Sprint 2 (`docs/NOTIFICATIONS_PLAN.md`) and global flashcard search (now Phase 4).
