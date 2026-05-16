# Daily Notification Plan

| Field | Value |
|---|---|
| Status | Active |
| Role | Atomic plan to implement the daily due-cards notification |
| Source of Truth | Yes (until all tasks close) |
| Read this when | You're going to touch `WorkManager`, `NotificationChannel`, or `Settings` for opt-in |
| Last verified against code | 2026-05-16 |

## TL;DR

`POST_NOTIFICATIONS` is declared in the original plan but **not in the current manifest** (verified). There is no notification infra. This iteration adds: permission, channel, periodic worker that counts globally due cards, scheduler at startup. **No time picker or deep link in v1** — fixed trigger at 19:00 local, tap opens the app on the main screen.

## Explicit decisions

- **Fixed trigger at 19:00 local** in v1. Time picker is a follow-up.
- **No deep link to Study** in v1 — the `PendingIntent` opens `MainActivity`. Follow-up: deep link to deck/study.
- **Default ON** — users get the reminder on install. Toggle off lives in Settings. Follow-up: onboarding consent dialog.
- **No exponential backoff** — if network/disk fails, next run is in 24h.
- **Global due-cards count**, not per deck. The notification is generic ("You have N cards to review today").
- **Skip if due_count == 0** — don't bother when there's nothing.

## Sprint 1 — Base infra (goal: 1.5 h)

### N1-T1: Manifest + permissions + icon

- **Files:** `app/src/main/AndroidManifest.xml`, `app/src/main/res/drawable/ic_notification.xml` (new).
- **What to do:**
    1. Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`.
    2. (Optional v2) `RECEIVE_BOOT_COMPLETED` to revive the schedule after reboot — WorkManager handles it alone if you use `PeriodicWorkRequest`, so SKIP.
    3. Create a simple white vector `ic_notification.xml` (24×24). Otherwise, use `R.mipmap.ic_launcher` as fallback (not recommended per guidelines).
- **Criterion:** build passes. The app requests notification permission on first use (automatically from Android 13+ when posting is attempted).
- **Estimate:** 15 min.
- **Status:** [ ]

### N1-T2: NotificationChannel registered in App.onCreate

- **Files:** `app/src/main/kotlin/com/emm/hello/notifications/StudyReminderChannel.kt` (new), `App.kt`.
- **What to do:**
    1. Constant `STUDY_REMINDER_CHANNEL_ID = "study_reminders"`.
    2. Function `ensureStudyReminderChannel(context: Context)` that creates the `NotificationChannel` (importance DEFAULT, lights/sound per guidelines), idempotent.
    3. Call it from `App.onCreate()` after `FirebaseApp.initializeApp`.
- **Criterion:** opening Settings > App > Notifications shows "Study reminders" as a category.
- **Estimate:** 20 min.
- **Status:** [ ]
- **Depends on:** N1-T1.

### N1-T3: Global due-cards count query + use case

- **Files:** `data/src/main/sqldelight/com/emm/data/Flashcard.sq` (new query), `domain/src/main/kotlin/com/emm/domain/flashcard/CountDueFlashcardsUseCase.kt` (new), `FlashcardRepository.kt` (interface), `DefaultFlashcardRepository.kt` (impl).
- **What to do:**
    1. SQL query:
        ```sql
        countDueFlashcards:
        SELECT COUNT(*)
        FROM Flashcard f
        LEFT JOIN ReviewProjection rp
          ON f.id = rp.flashcardId
        WHERE f.deletedAt IS NULL
          AND (rp.nextReviewAt IS NULL OR rp.nextReviewAt <= :now);
        ```
    2. `FlashcardRepository.countDueFlashcards(nowMillis: Long): Long` — new method.
    3. `CountDueFlashcardsUseCase(repo, clock)` — pure use case returning `Long`.
    4. Test: `CountDueFlashcardsUseCaseTest` with a fake repo.
- **Criterion:** test green. Counts cards with `nextReviewAt IS NULL` (new) plus cards with `nextReviewAt <= now`.
- **Estimate:** 30 min.
- **Status:** [ ]

### N1-T4: DueCardsReminderWorker

- **File:** `app/src/main/kotlin/com/emm/hello/notifications/DueCardsReminderWorker.kt` (new).
- **What to do:**
    1. `class DueCardsReminderWorker(context, params, useCase, clock) : CoroutineWorker` — injected via Koin (`WorkerFactory` or `koin-androidx-workmanager`).
    2. `doWork()`: invokes `CountDueFlashcardsUseCase`. If count == 0, return `Result.success()` without notifying. If count > 0, post notification with title "Your daily review" and body "You have N cards to review".
    3. PendingIntent target: `MainActivity` with immutable flags (`FLAG_IMMUTABLE`).
    4. Constant notification ID (`STUDY_REMINDER_NOTIFICATION_ID = 1001`) → re-posts over the previous one if the user doesn't open it.
- **Criterion:** running the worker manually from a unit-style test (or WorkManager test runner) with count > 0 produces a visible notification.
- **Estimate:** 45 min.
- **Status:** [ ]
- **Depends on:** N1-T2, N1-T3.

### N1-T5: Scheduler at app startup (PeriodicWorkRequest 24h, first run at 19:00)

- **Files:** `app/src/main/kotlin/com/emm/hello/notifications/StudyReminderScheduler.kt` (new), `App.kt` (invocation).
- **What to do:**
    1. `StudyReminderScheduler.scheduleDaily()` configures `PeriodicWorkRequest<DueCardsReminderWorker>` with `repeatInterval = 24h`, `flexInterval = 1h`, `initialDelay` computed to align with 19:00 local today or tomorrow.
    2. Enqueue with `ExistingPeriodicWorkPolicy.UPDATE` (not `KEEP`) so time changes are reflected without reinstalling.
    3. Call from `App.onCreate()` after `startKoin`.
- **Criterion:** `adb shell dumpsys jobscheduler | grep emm` shows the scheduled job. Change the clock to 19:00 and the notification appears within the 1h window.
- **Estimate:** 30 min.
- **Status:** [ ]
- **Depends on:** N1-T4.

## Sprint 2 — Opt-out + polish (goal: 1.5 h)

### N2-T6: Settings toggle on/off

- **Files:** `data/.../UserPreferences` (new or extend), `app/.../settings/SettingsViewModel.kt`, `SettingsScreen.kt`.
- **What to do:**
    1. Store preference in `DataStore`/`SharedPreferences`: `study_reminder_enabled: Boolean` (default `true`).
    2. UI in Settings: `Switch` "Daily study reminder" with sublabel "Every day at 19:00".
    3. On OFF → `WorkManager.cancelUniqueWork(...)`. On ON → re-enqueue.
- **Criterion:** toggle off → `adb shell dumpsys jobscheduler` no longer shows the job. Toggle on → it reappears.
- **Estimate:** 1 h.
- **Status:** [ ]
- **Depends on:** N1-T5.

### N2-T7: i18n strings + final icon

- **Files:** `values/strings.xml`, `values-en/strings.xml` (if it exists), `res/drawable/ic_notification.xml`.
- **What to do:**
    1. Extract strings: `notification_title`, `notification_body` (plurals), `notification_channel_name`, `notification_channel_description`, `settings_study_reminder_title`, `settings_study_reminder_subtitle`.
    2. Use `<plurals>` for "1 card" vs "N cards".
    3. Replace mock icon with a final one (white vector, 24×24, no background — Material guideline).
- **Criterion:** notification renders the correct plural. Changing device locale respects the language.
- **Estimate:** 30 min.
- **Status:** [ ]

## Known follow-ups (NOT in this iteration)

- **F-Time-Picker**: let the user pick the reminder time (not fixed 19:00). Requires DataStore + `initialDelay` re-computation logic.
- **F-Deep-Link**: tap on notification → `Study` for a specific deck (or the deck with the most due).
- **F-Onboarding-Consent**: ask `POST_NOTIFICATIONS` permission with contextual UI during onboarding (on Android 13+) instead of at first post.
- **F-Multi-Reminder**: multiple notifications per deck instead of a single global one (richer UX but noisier).

## Decisions that aren't obvious

- **Why `flexInterval = 1h`?** WorkManager `PeriodicWorkRequest` allows a flex window the system uses to batch wakes and save battery. Without flex, the system might attempt an exact wake, which is not possible in Doze mode. 1h is a good tradeoff: the user sees the notif between 18:00 and 19:00, not strictly at 19:00.
- **Why Worker in `app/` and not `:data`?** The worker depends on notifications (Android API) which don't fit in pure `:data`. It lives with UI/scaffolding.
- **Why `UPDATE` instead of `KEEP`?** The plan allows changing the time later (F-Time-Picker). `UPDATE` re-enqueues with the new spec without requiring manual cancellation.
