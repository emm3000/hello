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

1. **Notifications Sprint 2** (settings toggle + time picker) — finishes the already-scaffolded feature.
2. **Global flashcard search** — absorbed by restructure Phase 4 (Biblioteca); the task breakdown below still applies.

Deferred by the restructure (see `RESTRUCTURE_PLAN.md` → Deferred): **Stats history / heatmap**, **Flashcard-level tags**, **Cram mode**. Their sections below are kept for sizing only.

`F2` (device validation) and `S1-T7` (privacy URL) are on the user, not me.

**Already shipped** (kept out of the list above): Undo soft-delete (`RestoreFlashcardUseCase`, `RestoreDeckUseCase`, `UndoEventHolder` + snackbars) and First-run onboarding (`app/.../newfeatures/onboarding/`, `hasSeenWelcome` gate — see `docs/ONBOARDING_CURRENT.md`).

---

## 1. Notifications Sprint 2

| Field | Value |
|---|---|
| Size | Medium (~1.5 h) |
| Blocks | Nothing |
| Blocked by | Notifications Sprint 1 (already done, `29d11c1`) |
| Full plan | `docs/NOTIFICATIONS_PLAN.md` Sprint 2 + Follow-ups |

**Tasks:**

- **N2-T6**: Settings toggle on/off persisted in `DataStore`. Switching OFF → `StudyReminderScheduler.cancel(context)`. ON → `scheduleDaily(context)`.
- **N2-T7**: i18n strings + Material guideline-compliant icon (white 24×24 vector, no background).
- **F-Time-Picker**: TimePicker in Settings to pick the hour. Persist as `LocalTime`. `StudyReminderScheduler.scheduleDaily(context, time)` recomputes `initialDelay`.
- **F-Deep-Link**: tap notification → `Study` for the deck with the most due cards. Requires extending `MainActivity` with intent extras.

**Definition of done:** user can disable the reminder, change the time, and tapping the notification jumps straight into studying the busiest deck.

---

## 2. Global flashcard search (Feature #8)

| Field | Value |
|---|---|
| Size | Medium (~2 h) |
| Blocks | Nothing |
| Blocked by | Nothing |

**Atomic tasks:**

- **S-T1**: New SQL query in `Flashcard.sq`:
    ```sql
    searchFlashcards:
    SELECT f.*, d.name AS deckName
    FROM Flashcard f
    INNER JOIN Deck d ON f.deckId = d.id
    WHERE f.deletedAt IS NULL
      AND d.deletedAt IS NULL
      AND (LOWER(f.word) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(f.meaning) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(f.translation) LIKE '%' || LOWER(:query) || '%')
    ORDER BY f.createdAt DESC
    LIMIT 50;
    ```
- **S-T2**: `FlashcardRepository.search(query: String): Flow<List<FlashcardSearchResult>>` (includes deck name).
- **S-T3**: Extend `DashboardUiState`:
  - `flashcardResults: List<FlashcardSearchResult>`
  - When typing in `HSearchBar`, in addition to filtering decks, call `search(query)` debounced (~300 ms).
- **S-T4**: UI in Dashboard: if query is non-empty, show two sections — "Decks" (current) and "Flashcards" (matched cards with their deck name and a highlight of the matched term).
- **S-T5**: Tap a search-result card → navigate to `CardDetailRoute(flashcardId)`.

**Definition of done:** with 500 cards, searching "phrasal" shows all cards with that word/meaning in under 200 ms.

---

## 3. Stats history / heatmap (Feature #7, partially shipped)

| Field | Value |
|---|---|
| Size | Medium (~2-3 h) |
| Blocks | Nothing |
| Blocked by | Nothing |

**Already shipped:** `DashboardStats(cardsStudiedToday, cardsDueToday, currentStreak, cardsDueThisWeek)`, `StudyStatsRepository` / `DefaultStudyStatsRepository`, `GetDashboardStatsUseCase`, and the counters rendered by `DashboardStatsSection`. They are computed from `ReviewEvent`/`ReviewProjection`, with no dedicated log table.

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
- **St-T6**: Extend `DashboardStatsSection` with a simple 30-day heatmap chart.

**Definition of done:** on top of today's counters, the Dashboard shows a 30-day visual heatmap backed by a real review history.

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
- **T-T4**: Tag filter in `DeckDetail` and `Dashboard` flashcard search.
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
- **C-T2**: UI: "Cram" button in `DeckDetail` next to the current "Study".
- **C-T3**: In the cram session, grade buttons exist but do NOT write to `FlashcardReview` — they only advance to the next card. A subtle banner says "Quick review mode (does not affect your SRS progress)".
- **C-T4**: Same Study UI; only the item source and grade callback change.

**Definition of done:** "Cram" starts with all cards shuffled. Tapping "Good" advances without writing SRS. Leaving cram leaves the deck's intervals intact.

---

## Closing decisions

- **Privacy URL (audit S1-T7)**: blocked on the user until `docs/privacy-policy.md` is published at a public URL. Once you give me the URL, I'll add the meta-data to the manifest in 5 minutes.
- **F2 device validation**: I need your hardware. Pass me whatever you find and I'll refine.
- **F3 HARD color**: already closed (tinted overlay during swipe-to-grade and warning color for HARD).
