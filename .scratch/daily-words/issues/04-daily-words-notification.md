# 04: Notifications: a daily words notification that opens Suggest

Status: done
Device: emulator
Blocked by: 03
**Branch:** feat/04-daily-words-notification

Covers the spec's "What the learner sees" and decisions 1 to 3. Gate row 3 (`navigation/`, Koin `di/NewModule.kt`, the startup sync in `App`). Load `mattpocock-skills:tdd`. Read `docs/NOTIFICATIONS_PLAN.md` and add a daily-words section to it.
Mirror the study reminder: `notifications/DueCardsReminderWorker`, `StudyReminderChannel.kt`, `WorkManagerStudyReminderScheduler`, `domain/reminder/SyncStudyReminderUseCase`, `data/reminder/DataStoreStudyReminderSettingsRepository`, `navigation/LaunchDestination`, `newfeatures/NewRoot.kt` `toNavKey`, and `App.onCreate`.
The settings model and its persistence ship here with their defaults (on, 09:00, every day). Their Settings UI is ticket 05.

## Acceptance
- [x] Domain JVM tests: `DailyWordsSettings` defaults to enabled, 09:00 and all seven `DayOfWeek`. The sync use case schedules when the feature is enabled with at least one day and cancels otherwise. A run on an unselected weekday posts nothing
- [x] The worker returns early without fetching when notifications are disabled. Otherwise it calls `PrepareDailyWordsUseCase` (ticket 03) and posts only for a non-null batch
- [x] The study reminder is untouched: `git diff main -- app/src/main/kotlin/com/emm/hello/notifications/DueCardsReminderWorker.kt` prints nothing, and the new channel, notification id and unique work name differ from `study_reminders`, `1001` and `study_reminder_daily`
- [x] `App.onCreate` creates the new channel and syncs the daily-words schedule, next to `SyncStudyReminderUseCase`
- [x] `values/strings.xml` and `values-es/strings.xml` hold the channel name ("New words" / "Palabras nuevas") and the notification copy. The copy names the situation and the count with a plural, as in the spec: "Today: <situation> · 6 new words and phrases" / "Hoy: <situation> · 6 palabras y frases nuevas"
- [x] `LaunchDestination` gains a daily-words value that `toNavKey` maps to `SuggestRoute`, with `TodayRoute` beneath it, so back returns to `Hoy`
- [x] On the emulator with a debug build (canned AI), force the periodic work with `adb shell cmd jobscheduler run -f`:
  - A first run posts the situation and count, and tapping opens `Suggest` on that same situation.
  - A second run without opening re-posts the same situation.
  - After `Suggest` has shown the batch, the next run brings a new situation.
  - In airplane mode after a seen batch, nothing is posted.
  - Captures are taken in both locales.
- [x] `scripts/check` green

## Comments

- Done in 32aa1190 (code and tests) and the docs commit that closes this ticket (`docs/NOTIFICATIONS_PLAN.md` gains a "Daily words notification" section and a note on forcing periodic work; `docs/SUGGEST_CURRENT.md` lists the notification as a third entry point).
- Tests, each red before its implementation: `DailyWordsSettingsTest` (defaults), `SyncDailyWordsUseCaseTest` (enabled with days schedules the time; disabled cancels; enabled with no day cancels, which failed against a study-reminder copy that only checked `isEnabled`), `PrepareScheduledDailyWordsUseCaseTest` (disabled and an unselected weekday return null with `FakeWordSuggestionRepository.calls == 0` and the cache untouched; a selected weekday in the injected zone fetches, checked with a clock at Monday 03:00 UTC that is still Sunday in Lima), `LaunchDestinationTest` (`daily_words`). `KoinModulesTest` got `ZoneId` in its extra types, as `Context` already is.
- The worker returns before resolving the use case when the app's notifications or the `daily_words` channel are blocked, so no fetch is spent on a notification nobody can see. Feature-off and the weekday filter are in `PrepareScheduledDailyWordsUseCase`. A null batch posts nothing and never retries.
- The study reminder is untouched: `git diff main -- .../DueCardsReminderWorker.kt` prints nothing. New channel `daily_words`, id `1002`, unique work `daily_words_daily`. The daily-words `PendingIntent` uses request code 1002, not the study reminder's 0, because `Intent.filterEquals` ignores extras and `FLAG_UPDATE_CURRENT` would otherwise rewrite the other notification's destination.
- Device, hello-1 (emulator-5620), debug build with canned AI, captures on 32aa1190:
  - First run: "Your new words / Today: Ordering food at a busy restaurant · 6 new words and phrases". /Users/emm/AndroidStudioProjects/Hello-notify/.evidence/04-notification-en-32aa1190.png
  - Second and third runs without opening, app locale `es`: "Tus palabras nuevas / Hoy: Ordering food at a busy restaurant · 6 palabras y frases nuevas". The batch's `createdAt` and the history count (7) did not change, and the worker finished in 0.2 s, under the canned repository's 600 ms delay, so nothing was fetched. /Users/emm/AndroidStudioProjects/Hello-notify/.evidence/04-repost-es-32aa1190.png
  - Tapping it opened `Suggest` on that same situation and set `firstSeenAt`; back returned to `Hoy`. /Users/emm/AndroidStudioProjects/Hello-notify/.evidence/04-suggest-from-tap-es-32aa1190.png, /Users/emm/AndroidStudioProjects/Hello-notify/.evidence/04-back-to-hoy-es-32aa1190.png
  - Next run after the batch was seen, locale `en`: "Today: Asking for directions in a new city · 6 new words and phrases", history 7 to 14; tapping opened `Suggest` on it. /Users/emm/AndroidStudioProjects/Hello-notify/.evidence/04-new-situation-en-32aa1190.png, /Users/emm/AndroidStudioProjects/Hello-notify/.evidence/04-suggest-from-tap-en-32aa1190.png
  - Airplane mode after that batch was seen: worker `SUCCESS`, no notification 1002, batch unchanged. /Users/emm/AndroidStudioProjects/Hello-notify/.evidence/04-airplane-nothing-posted-32aa1190.png
- Forcing the work: `cmd jobscheduler run -f` needs `-n androidx.work.systemjobscheduler` and the job id from `SystemIdInfo`, and WorkManager refuses a run before the override time and within 15 minutes of the last run (`WorkSpec.calculateNextRunTime`). Each run was pinned by writing `DAILY_WORDS_HOUR`/`DAILY_WORDS_MINUTE` a few minutes ahead and relaunching the app, then forced or left to fire.
- As with the study reminder, the destination is applied only once onboarding has been seen. This AVD had not seen it, so the first tap opened onboarding; onboarding was finished and the tap repeated on the next re-post.
- The emulator was overloaded during the session: one app start hit a startup ANR and the AVD died once; it was restarted and the app reinstalled. Neither involved this change's code.
