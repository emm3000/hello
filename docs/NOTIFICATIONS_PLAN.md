# Daily Notification Plan

| Field | Value |
|---|---|
| Status | Active |
| Role | Atomic plan to implement the daily due-cards notification |
| Source of Truth | Yes (until all tasks close) |
| Read this when | You're going to touch `WorkManager`, `NotificationChannel`, or `Settings` for opt-in |
| Last verified against code | 2026-09-06 |

## TL;DR

**Sprint 1 and Sprint 2 are both complete.** `POST_NOTIFICATIONS` is declared in `AndroidManifest.xml` (verified). Notification infra is live: channel, periodic worker that counts globally due cards, scheduler synced at every startup. The user can turn the reminder off, pick its time (default 19:00), and tapping it opens `Study` for all due cards. Verified end to end on device (`medium_phone`, API 36, 2026-09-06): a reminder scheduled for 00:05 fired at 00:05:03 and tapping it opened Study.

**Open gap:** `POST_NOTIFICATIONS` is declared but the app never requests it (see "Known follow-ups" → `F-Onboarding-Consent`, next up).

## Explicit decisions

- **Default time 19:00, user-editable** — `StudyReminderSettings.DEFAULT_TIME`. Shipped in Sprint 2 (`N2-T8`); no longer fixed.
- **Deep link to Study on tap** — the `PendingIntent` opens `MainActivity` with a launch-destination extra that routes to `Study` for all due cards. Shipped in Sprint 2 (`N2-T9`); no per-deck targeting.
- **Default ON** — users get the reminder on install. Toggle off lives in Settings. Follow-up: onboarding consent for the runtime permission (`F-Onboarding-Consent`).
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
- **Criterion:** build passes. **Superseded (verified on device, 2026-09-06):** Android 13+ shows no automatic permission prompt when posting is attempted — there is no such system behavior. `POST_NOTIFICATIONS` must be requested explicitly, and until it is, `DueCardsReminderWorker` silently no-ops. See `F-Onboarding-Consent`.
- **Estimate:** 15 min.
- **Status:** [x] — shipped in `29d11c1`.

### N1-T2: NotificationChannel registered in App.onCreate

- **Files:** `app/src/main/kotlin/com/emm/hello/notifications/StudyReminderChannel.kt` (new), `App.kt`.
- **What to do:**
    1. Constant `STUDY_REMINDER_CHANNEL_ID = "study_reminders"`.
    2. Function `ensureStudyReminderChannel(context: Context)` that creates the `NotificationChannel` (importance DEFAULT, lights/sound per guidelines), idempotent.
    3. Call it from `App.onCreate()` after `FirebaseApp.initializeApp`.
- **Criterion:** opening Settings > App > Notifications shows "Study reminders" as a category.
- **Estimate:** 20 min.
- **Status:** [x] — shipped in `29d11c1`.
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
- **Status:** [x] — shipped in `29d11c1`.

### N1-T4: DueCardsReminderWorker

- **File:** `app/src/main/kotlin/com/emm/hello/notifications/DueCardsReminderWorker.kt` (new).
- **What to do:**
    1. `class DueCardsReminderWorker(context, params) : CoroutineWorker`. **Superseded:** it does not take an injected use case or `Clock`, and there is no `WorkerFactory`. `doWork()` resolves `CountDueFlashcardsUseCase` through `GlobalContext.get()` (a Koin service locator call), because `WorkManager` constructs workers itself via reflection on the two-arg constructor.
    2. `doWork()`: invokes `CountDueFlashcardsUseCase`. On failure, `Result.retry()`. If count == 0, `Result.success()` without notifying. If count > 0, posts the notification with title/body strings and returns `Result.success()`.
    3. PendingIntent target: `MainActivity` with `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`, carrying the `LaunchDestination` extra (see N2-T9).
    4. Constant notification ID (`STUDY_REMINDER_NOTIFICATION_ID = 1001`) → re-posts over the previous one if the user doesn't open it.
- **Criterion:** running the worker manually from a unit-style test (or WorkManager test runner) with count > 0 produces a visible notification.
- **Estimate:** 45 min.
- **Status:** [x] — shipped in `29d11c1`.
- **Depends on:** N1-T2, N1-T3.

### N1-T5: Scheduler at app startup (PeriodicWorkRequest 24h, first run at 19:00)

- **Files:** `app/src/main/kotlin/com/emm/hello/notifications/StudyReminderScheduler.kt` (new), `App.kt` (invocation).
- **What to do:**
    1. `StudyReminderScheduler.scheduleDaily()` configures `PeriodicWorkRequest<DueCardsReminderWorker>` with `repeatInterval = 24h`, `flexInterval = 1h`, `initialDelay` computed to align with 19:00 local today or tomorrow.
    2. Enqueue with `ExistingPeriodicWorkPolicy.UPDATE` (not `KEEP`) so time changes are reflected without reinstalling.
    3. Call from `App.onCreate()` after `startKoin`.
- **Criterion:** `adb shell dumpsys jobscheduler | grep emm` shows the scheduled job. Change the clock to 19:00 and the notification appears within the 1h window.
- **Estimate:** 30 min.
- **Status:** [x] — shipped in `29d11c1`, **superseded in `8779630`**. The `flexInterval` + `initialDelay` scheme above never actually re-pinned the run time after the first enqueue (see "Decisions that aren't obvious" for why); `WorkManagerStudyReminderScheduler.schedule(time)` now enqueues with `setNextScheduleTimeOverride(nextOccurrence(time, now))` instead, and no flex.
- **Depends on:** N1-T4.

## Sprint 2 — Opt-out + polish (goal: 1.5 h) — done

### N2-T6: Settings toggle on/off

- **Files:** `data/src/main/kotlin/com/emm/data/remote/DataStore.kt` (`isStudyReminderEnabled`, backed by `SharedPreferences` key `STUDY_REMINDER_ENABLED`, default `true`), `data/src/main/kotlin/com/emm/data/reminder/DataStoreStudyReminderSettingsRepository.kt`, `domain/src/main/kotlin/com/emm/domain/reminder/*` (`StudyReminderSettings`, `StudyReminderSettingsRepository`, `StudyReminderScheduler`, `GetStudyReminderSettingsUseCase`, `SetStudyReminderEnabledUseCase`, `SyncStudyReminderUseCase`), `app/.../newfeatures/settings/SettingsUiState.kt`, `SettingsUiIntent.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt`, `core/ui/Switch.kt` (new `HSwitch`).
- **What shipped:** a "Reminders" section in Settings with an `HSwitch`. `SetStudyReminderEnabledUseCase` persists the flag, then `SyncStudyReminderUseCase` schedules or cancels the `WorkManager` unique work accordingly. `App.onCreate()` also calls `SyncStudyReminderUseCase` on every launch, so the schedule is re-pinned (or cancelled) even if it drifted.
- **Criterion:** toggle off → `adb shell dumpsys jobscheduler` no longer shows the job. Toggle on → it reappears. Verified on device.
- **Status:** [x] — shipped in `1988e5c`.
- **Depends on:** N1-T5.

### N2-T7: i18n strings + final icon

- **Files:** `app/src/main/res/values/strings.xml`, `app/src/main/res/drawable/ic_notification.xml`.
- **What shipped:** `notification_channel_name`, `notification_channel_description`, `notification_reminder_title`, `<plurals name="notification_reminder_body">`, plus the Settings row strings. The icon is a white vector, no background.
- **Criterion:** notification renders the correct plural. Changing device locale respects the language.
- **Status:** [x] — closed in Sprint 1, commit `29d11c1` (it shipped earlier than planned, alongside the base infra).

### N2-T8: Reminder time picker

- **Files:** `domain/src/main/kotlin/com/emm/domain/reminder/SetStudyReminderTimeUseCase.kt`, `data/src/main/kotlin/com/emm/data/remote/DataStore.kt` (`studyReminderHour`, `studyReminderMinute`, keys `STUDY_REMINDER_HOUR` / `STUDY_REMINDER_MINUTE`), `data/src/main/kotlin/com/emm/data/reminder/DataStoreStudyReminderSettingsRepository.kt`, `app/.../newfeatures/settings/SettingsUiState.kt` (`reminderTime`, `isReminderTimePickerVisible`), `SettingsUiIntent.kt` (`EditReminderTime`, `SetReminderTime`, `DismissReminderTimePicker`), `SettingsViewModel.kt`, `SettingsScreen.kt`, `core/ui/TimePicker.kt` (new `HTimePickerDialog`), `app/.../notifications/StudyReminderSchedule.kt` (new `nextOccurrence`), `app/.../notifications/WorkManagerStudyReminderScheduler.kt` (rewritten to use `setNextScheduleTimeOverride`).
- **What shipped:** tapping the reminder row opens `HTimePickerDialog` (24h, Save/Cancel); confirming persists the time and re-syncs the scheduler. Time is stored as separate hour/minute ints, not millis, so it survives a timezone or DST change unaffected.
- **Criterion (used on device, 2026-09-06):** with the old `initialDelay` + `flexInterval` scheme, picking 07:30 at 23:47 produced a first run at 06:16 the day after tomorrow. With `setNextScheduleTimeOverride`, the same pick runs at the next 07:30. A reminder set for 00:05 fired at 00:05:03.
- **Status:** [x] — shipped in `8779630`, which also carries the N1-T5 scheduler rewrite above.
- **Depends on:** N2-T6.

### N2-T9: Deep link to Study

- **Files:** `app/src/main/kotlin/com/emm/hello/navigation/LaunchDestination.kt` (new enum, `StudyDue` → extra value `"study_due"`), `app/.../notifications/DueCardsReminderWorker.kt` (the `PendingIntent` carries extra `com.emm.hello.extra.LAUNCH_DESTINATION`), `app/src/main/kotlin/com/emm/hello/MainActivity.kt` (reads the extra on `onCreate` only when `savedInstanceState == null`, and again in `onNewIntent`), `app/src/main/kotlin/com/emm/hello/newfeatures/NewRoot.kt` (a `LaunchedEffect` consumes it and calls `Navigator.resetTo(TodayRoute, StudyRoute(deckId = null))`, only once onboarding has been seen; the request is cleared right after).
- **What shipped:** tapping the notification opens `Study` for all due cards. Rotating the device does not re-trigger the navigation, because `MainActivity` only reads the extra on a truly fresh start.
- **Criterion (verified on device, 2026-09-06):** tapping a fired notification opened Study directly, with the back stack becoming Today → Study.
- **Status:** [x] — shipped in `6a2c6ad`.
- **Depends on:** N1-T4.

## Known follow-ups (NOT in this iteration)

- **F-Onboarding-Consent** (next up): `POST_NOTIFICATIONS` is declared in the manifest but never requested. On Android 13+ there is no automatic system prompt when a notification is posted — verified on the test emulator, where the permission was `granted=false` and `DueCardsReminderWorker` silently returned early on `areNotificationsEnabled() == false`. The reminder never shows unless the user grants it manually in system settings. Fix: request the permission when the Settings toggle is switched ON, and reflect a denied state in the Settings row (e.g. a hint to open system settings).
- **F-Multi-Reminder**: multiple notifications per deck instead of a single global one (richer UX but noisier).

## Decisions that aren't obvious

- **Why `UPDATE` + `setNextScheduleTimeOverride`, no `initialDelay`, no flex?** `ExistingPeriodicWorkPolicy.UPDATE` preserves the original `WorkSpec.lastEnqueueTime` and `periodCount` (see `WorkSpec.calculateNextRunTime` in the WorkManager source). An `initialDelay` computed from "now" is only honored on the very first enqueue of a `WorkSpec` — every later `UPDATE` (toggling the reminder, changing its time) ignores it, because the delay is baked into that first `lastEnqueueTime`. The old 1h `flexInterval` compounded this: the first run landed `interval - flex` (23h) after that anchor, in the next day's window rather than at the picked time. On device, the old scheme sent a 07:30 pick made at 23:47 to a first run at 06:16 the day after tomorrow. `setNextScheduleTimeOverride(nextOccurrence(time, now))` sidesteps all of this: it tells WorkManager the exact next fire time directly, independent of `lastEnqueueTime` or `periodCount`, so every `UPDATE` — including the one `App.onCreate()` performs via `SyncStudyReminderUseCase` on every launch — re-pins the next run correctly. `nextOccurrence` itself is pure and unit-tested against a fixed `ZonedDateTime`.
- **Why Worker in `app/` and not `:data`?** The worker depends on notifications (Android API) which don't fit in pure `:data`. It lives with UI/scaffolding.
- **Why `GlobalContext.get()` instead of a Koin `WorkerFactory`?** `WorkManager` instantiates `CoroutineWorker` subclasses itself via reflection on the `(Context, WorkerParameters)` constructor; there is no injection point for extra dependencies without wiring a custom `WorkerFactory`. `DueCardsReminderWorker.doWork()` reaches into the running Koin container directly instead, since a `WorkerFactory` was not worth the ceremony for a single dependency.

### How to verify on device

- The `WorkManager` database lives at `no_backup/androidx.work.workdb` inside the app's data directory. Pull it, and its `-wal`/`-shm` files, via `adb shell run-as com.emm.hello`; pulling the `.workdb` file alone shows stale rows, since SQLite keeps recent writes in the WAL until it checkpoints.
- The scheduled fire time is `WorkSpec.next_schedule_time_override` in that database.
- `adb shell dumpsys jobscheduler`, look for the job under the app's package; the `Delay=` field shows the remaining time until the next run.
- WorkManager can also dump its own diagnostics on demand: broadcast `androidx.work.diagnostics.REQUEST_DIAGNOSTICS` and read logcat.
- `adb shell cmd jobscheduler run -f -n androidx.work.systemjobscheduler <package> <job-id>` starts the JobScheduler job, but WorkManager refuses to run the worker before its schedule (`WM-WorkerWrapper` logs "executed before schedule" and re-enqueues). The reliable end-to-end test is to pick a time a few minutes ahead in Settings and wait for it to fire.
