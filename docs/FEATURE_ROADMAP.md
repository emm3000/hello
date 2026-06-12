# Feature Roadmap

| Field | Value |
|---|---|
| Status | Active |
| Role | Short plan per pending feature. Each entry expands into an atomic doc like `STUDY_UX_ITERATION.md` when prioritized. |
| Source of Truth | Yes for priority and size of each item |
| Read this when | You're picking which feature to implement next |
| Last verified against code | 2026-05-16 |

## Recommended order

1. **Notifications Sprint 2** (settings toggle + time picker) — finishes the already-scaffolded feature.
2. **Undo soft-delete** — small, UX polish.
3. **Global flashcard search** — high utility, medium.
4. **Stats / streak** — engagement, medium-large.
5. **Flashcard-level tags** — schema migration, large.
6. **First-run onboarding** — optional, medium.
7. **Cram mode (no-SRS)** — optional, medium.

`F2` (device validation) and `S1-T7` (privacy URL) are on the user, not me.

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

## 2. Undo soft-delete (Feature #12)

| Field | Value |
|---|---|
| Size | Small (~1 h) |
| Blocks | Nothing |
| Blocked by | Nothing |

**Atomic tasks:**

- **U-T1**: Add `restoreByTimestamp` queries in `Flashcard.sq` and `FlashcardExample.sq`:
    ```sql
    restoreByTimestamp:
    UPDATE Flashcard SET deletedAt = NULL
    WHERE id = :id AND deletedAt = :timestamp;
    ```
    The timestamp filter prevents restoring examples soft-deleted earlier (independent of the current cascade).
- **U-T2**: `FlashcardRepository.restoreFlashcard(flashcardId, deletedAtTimestamp)` + impl in `DefaultFlashcardRepository` with a transaction that also restores cascaded examples.
- **U-T3**: `RestoreFlashcardUseCase`.
- **U-T4**: Modify `FlashcardDetailViewModel.deleteFlashcard()`:
  1. Capture `deletedAt` timestamp.
  2. Set state `pendingDeletion = PendingDeletion(timestamp, dismissJob)`.
  3. Launch coroutine with `delay(5_000)` → emits `FlashcardDeleted` (navigates back).
  4. If it receives `UndoDeletion` intent → cancel job + `RestoreFlashcardUseCase(flashcardId, timestamp)`.
- **U-T5**: UI: `Snackbar` with "Undo" action in `FlashcardDetailScreen` while `pendingDeletion != null`. Auto-dismiss after 5s.

**Caveat:** during those 5s, the card is soft-deleted in DB but UI still shows it (data is in state memory). User flow is coherent because the screen does not reload.

---

## 3. Global flashcard search (Feature #8)

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

## 4. Stats / streak (Feature #7)

| Field | Value |
|---|---|
| Size | Large (~4-5 h) |
| Blocks | Onboarding pre-ranking |
| Blocked by | Nothing |

**Atomic tasks:**

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
- **St-T2**: Migration `2.sqm` to create the table. Verify with `verifySqlDelightMigration`.
- **St-T3**: Insert log entry in `ScheduleFlashcardReviewUseCase` (or in the VM before updating review). Decision: do it in the use case to keep a single source of truth.
- **St-T4**: Queries in `Stats.sq`:
  - `reviewsByDay`: COUNT(*) GROUP BY date(reviewedAt/1000, 'unixepoch')
  - `currentStreak`: computed in Kotlin over consecutive days with ≥ 1 review.
- **St-T5**: Domain: `GetStudyStatsUseCase` returns `StudyStats(streak, reviewsToday, reviewsThisWeek, accuracy30d, heatmap30d)`.
- **St-T6**: Extend `DashboardStatsSection` to show streak and a simple 30-day heatmap chart.

**Definition of done:** Dashboard shows "Streak: 5 days", "Today: 12 cards", a 30-day visual heatmap.

---

## 5. Flashcard-level tags (Feature #9)

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

## 6. First-run onboarding (Feature #10)

| Field | Value |
|---|---|
| Size | Medium (~2-3 h) |
| Blocks | Notifications opt-in dialog |
| Blocked by | Nothing |

**Atomic tasks:**

- **O-T1**: Detect first run in `AppStartupCoordinator` (flag persisted in DataStore).
- **O-T2**: New feature `app/.../onboarding/` with 3 screens: welcome, "create your first deck" (generates demo deck), notifications opt-in (request POST_NOTIFICATIONS on Android 13+).
- **O-T3**: Skippable but with a prominent CTA to create the first deck.
- **O-T4**: Mark `firstRunCompleted = true` at the end.

**Definition of done:** fresh install → onboarding appears once. Closing or completing sets the flag and it doesn't appear again.

---

## 7. Cram / no-SRS mode (Feature #11)

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
