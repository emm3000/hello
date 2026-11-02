# 04: Notifications: a daily words notification that opens Suggest

Status: ready-for-agent
Device: emulator
Blocked by: 03
**Branch:** feat/04-daily-words-notification

Covers the spec's "What the learner sees" and decisions 1 to 3. Gate row 3 (`navigation/`, Koin `di/NewModule.kt`, the startup sync in `App`). Load `mattpocock-skills:tdd`. Read `docs/NOTIFICATIONS_PLAN.md` and add a daily-words section to it.
Mirror the study reminder: `notifications/DueCardsReminderWorker`, `StudyReminderChannel.kt`, `WorkManagerStudyReminderScheduler`, `domain/reminder/SyncStudyReminderUseCase`, `data/reminder/DataStoreStudyReminderSettingsRepository`, `navigation/LaunchDestination`, `newfeatures/NewRoot.kt` `toNavKey`, and `App.onCreate`.
The settings model and its persistence ship here with their defaults (on, 09:00, every day). Their Settings UI is ticket 05.

## Acceptance
- [ ] Domain JVM tests: `DailyWordsSettings` defaults to enabled, 09:00 and all seven `DayOfWeek`. The sync use case schedules when the feature is enabled with at least one day and cancels otherwise. A run on an unselected weekday posts nothing
- [ ] The worker returns early without fetching when notifications are disabled. Otherwise it calls `PrepareDailyWordsUseCase` (ticket 03) and posts only for a non-null batch
- [ ] The study reminder is untouched: `git diff main -- app/src/main/kotlin/com/emm/hello/notifications/DueCardsReminderWorker.kt` prints nothing, and the new channel, notification id and unique work name differ from `study_reminders`, `1001` and `study_reminder_daily`
- [ ] `App.onCreate` creates the new channel and syncs the daily-words schedule, next to `SyncStudyReminderUseCase`
- [ ] `values/strings.xml` and `values-es/strings.xml` hold the channel name ("New words" / "Palabras nuevas") and the notification copy. The copy names the situation and the count with a plural, as in the spec: "Today: <situation> · 6 new words and phrases" / "Hoy: <situation> · 6 palabras y frases nuevas"
- [ ] `LaunchDestination` gains a daily-words value that `toNavKey` maps to `SuggestRoute`, with `TodayRoute` beneath it, so back returns to `Hoy`
- [ ] On the emulator with a debug build (canned AI), force the periodic work with `adb shell cmd jobscheduler run -f`:
  - A first run posts the situation and count, and tapping opens `Suggest` on that same situation.
  - A second run without opening re-posts the same situation.
  - After `Suggest` has shown the batch, the next run brings a new situation.
  - In airplane mode after a seen batch, nothing is posted.
  - Captures are taken in both locales.
- [ ] `scripts/check` green
