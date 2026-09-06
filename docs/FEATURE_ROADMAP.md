# Feature Roadmap

| Field | Value |
|---|---|
| Status | Active |
| Role | Short plan per pending feature. Each entry expands into an atomic doc like `STUDY_CURRENT.md` when prioritized. |
| Source of Truth | Yes for the size and task breakdown of each item; ordering lives in `RESTRUCTURE_PLAN.md` |
| Read this when | You're picking which feature to implement next |
| Last verified against code | 2026-08-26 |

## Recommended order

Since 2026-08-26 the product is being restructured session-first — see `docs/RESTRUCTURE_PLAN.md`. Its phases take priority over this list.

Still valid, in this order once the restructure allows:

1. **Notifications Sprint 2** (settings toggle + time picker + deep link) — shipped; see below.
2. **Global flashcard search** — absorbed by restructure Phase 4 (Biblioteca); the task breakdown below still applies.

Deferred by the restructure (see `RESTRUCTURE_PLAN.md` → Deferred): **Stats history / heatmap**, **Flashcard-level tags**, **Cram mode**. Their sections below are kept for sizing only.

`F2` (device validation) and `S1-T7` (privacy URL) are on the user, not me.

**Already shipped** (kept out of the list above): Undo soft-delete (`RestoreFlashcardUseCase`, `RestoreDeckUseCase`, `UndoEventHolder` + snackbars) and First-run onboarding (`app/.../newfeatures/onboarding/`, `hasSeenWelcome` gate — see `docs/ONBOARDING_CURRENT.md`).

---

## 1. Notifications Sprint 2 — SHIPPED

Delivered as three commits (`1988e5c`, `8779630`, `6a2c6ad`), in a different shape than planned here: the deep link opens `Study` for all due cards, not the single busiest deck, and there was no separate icon/i18n task since `N2-T7` had already shipped in Sprint 1 (`29d11c1`).

- **N2-T6**: Settings toggle on/off, persisted through `DataStore` / `StudyReminderSettingsRepository`.
- **N2-T8** (was `F-Time-Picker`): reminder time picker in Settings, persisted as hour/minute.
- **N2-T9** (was `F-Deep-Link`): tapping the notification opens `Study` for all due cards.

See `docs/NOTIFICATIONS_PLAN.md` for the full breakdown, including the open follow-up (`F-First-Launch-Prompt`: a fresh install never sees the `POST_NOTIFICATIONS` prompt until the user opens Settings).

---

## 2. Global flashcard search (Feature #8) — SHIPPED

Delivered as Phase 4 of `RESTRUCTURE_PLAN.md`, in a different shape than
planned here. The search does not sit on the deck list next to a "Decks"
section; it is the whole of `Biblioteca`, which lists every card and matches
`word`, `translation` and `meaning`.

Two deliberate departures from the sketch above:

- Matching runs in `:domain` (`SearchLibraryUseCase`), not in SQL. `LIKE` in
  SQLite is only case-insensitive for ASCII, so a `LIKE` query could not match
  `cafe` against `café` without a normalized column and a migration. The domain
  filter normalizes both sides — NFD, drop combining marks, `Locale.ROOT`
  lowercase — and needs no schema change.
- No `LIMIT 50`. The list is what the user is browsing, so truncating it would
  hide cards rather than protect a budget.

See `docs/LIBRARY_CURRENT.md`.

---

## 3. Stats history / heatmap (Feature #7, partially shipped)

| Field | Value |
|---|---|
| Size | Medium (~2-3 h) |
| Blocks | Nothing |
| Blocked by | Nothing |

**Already shipped:** `DashboardStats(cardsStudiedToday, cardsDueToday, currentStreak, cardsDueThisWeek)`, `StudyStatsRepository` / `DefaultStudyStatsRepository`, `GetDashboardStatsUseCase`, and the counters rendered by `TodayStatsSection`. They are computed from `ReviewEvent`/`ReviewProjection`, with no dedicated log table.

**Still pending** — the history/heatmap half:

- **St-T1**: New `ReviewLog` table:
    ```sql
    CREATE TABLE ReviewLog (
      id TEXT NOT NULL PRIMARY KEY,
      flashcardId TEXT NOT NULL,
      reviewedAt INTEGER NOT NULL,
      grade INTEGER NOT NULL
    );
    CREATE INDEX idx_ReviewLog_reviewedAt ON ReviewLog(reviewedAt);
    ```
- **St-T2**: Migration `2.sqm` (v2 -> v3) to create the table — `1.sqm` is already taken by the FSRS-6 migration. Verify with `verifySqlDelightMigration`.
- **St-T3**: Insert log entry in `ScheduleFlashcardReviewUseCase` (or in the VM before updating review). Decision: do it in the use case to keep a single source of truth.
- **St-T4**: Queries in `Stats.sq`:
  - `reviewsByDay`: COUNT(*) GROUP BY date(reviewedAt/1000, 'unixepoch')
  - `currentStreak`: computed in Kotlin over consecutive days with ≥ 1 review.
- **St-T5**: Domain: extend the stats surface with `accuracy30d` and `heatmap30d` (the streak/today/week counters already exist in `DashboardStats`).
- **St-T6**: Extend `TodayStatsSection` with a simple 30-day heatmap chart.

**Definition of done:** on top of today's counters, Hoy shows a 30-day visual heatmap backed by a real review history.

---

## 4. Flashcard-level tags (Feature #9)

| Field | Value |
|---|---|
| Size | Large (~5-6 h) |
| Blocks | Nothing |
| Blocked by | Nothing |

**Atomic tasks:**

- **T-T1**: `FlashcardTag(flashcardId, tagId)` table with FKs ON DELETE CASCADE. Migration `3.sqm`.
- **T-T2**: Repo methods: `addTag`, `removeTag`, `flashcardTags(flashcardId)`, `flashcardsByTag(tagId)`.
- **T-T3**: UI in `EditFlashcardScreen` to assign tags via `HTagInput`.
- **T-T4**: Tag filter in `Biblioteca`, alongside the deck chips.
- **T-T5**: Backup export/import includes `FlashcardTag` with soft-delete filter (same pattern as `DeckTag`).

**Definition of done:** creating/editing a card allows assigning tags. Filtering a deck by tag shows only tagged cards.

---

## 5. Cram / no-SRS mode (Feature #11)

| Field | Value |
|---|---|
| Size | Medium (~2-3 h) |
| Blocks | Nothing |
| Blocked by | Nothing |

**Atomic tasks:**

- **C-T1**: New `StartCramSession` intent in `StudyViewModel` that loads ALL deck cards (not only due) in random order, without updating the schedule.
- **C-T2**: UI: "Cram" entry point in `Biblioteca`, on the selected deck chip.
- **C-T3**: In the cram session, grade buttons exist but do NOT write to `FlashcardReview` — they only advance to the next card. A subtle banner says "Quick review mode (does not affect your SRS progress)".
- **C-T4**: Same Study UI; only the item source and grade callback change.

**Definition of done:** "Cram" starts with all cards shuffled. Tapping "Good" advances without writing SRS. Leaving cram leaves the deck's intervals intact.

---

## Closing decisions

- **Privacy URL (audit S1-T7)**: blocked on the user until `docs/privacy-policy.md` is published at a public URL. Once you give me the URL, I'll add the meta-data to the manifest in 5 minutes.
- **F2 device validation**: I need your hardware. Pass me whatever you find and I'll refine.
- **F3 HARD color**: already closed (tinted overlay during swipe-to-grade and warning color for HARD).
