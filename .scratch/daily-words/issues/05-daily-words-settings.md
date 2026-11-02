# 05: Settings: a New words section with a switch, a time and weekdays

Status: done
Device: emulator
Blocked by: 04
**Branch:** feat/05-daily-words-settings

Covers the first bullet of the spec's "What the learner sees". Gate row 3 (Koin `di/NewModule.kt`). Load `mattpocock-skills:tdd` for the ViewModel rules. Read `docs/SETTINGS_CURRENT.md` (the Reminders section, the reminder flow and its permission gating) and update it.
Code: `newfeatures/settings/` (`SettingsUiState`, `SettingsUiIntent`, `SettingsViewModel`, `SettingsScreen`), the `core/ui` `HSwitch`, `HTimePickerDialog` and `HChip`, and ticket 04's settings repository and sync use case. Values are read synchronously, like `GetStudyReminderSettingsUseCase`, so the section has no loading or error state.

## Acceptance
- [x] `SettingsViewModelTest`:
  - The initial state carries the persisted daily-words settings.
  - Toggling the switch, setting the time and toggling a weekday each persist and re-sync the daily-words schedule.
  - Turning the switch on without notification permission requests it, as the study reminder does.
  - The last selected weekday cannot be deselected.
- [x] The existing study reminder cases in `SettingsViewModelTest` pass without edits
- [x] The screen has a "New words" / "Palabras nuevas" section after `settings_section_reminders`, separate from the study reminder row. It holds:
  - a switch row,
  - a time row that opens `HTimePickerDialog`,
  - seven `HChip` weekdays labelled with the locale's short day names, with `active` marking the selected days.
  The time and day controls are disabled while the switch is off, and the row shows the same blocked state as the reminder row when permission is denied.
- [x] Every new string exists in both `values/strings.xml` and `values-es/strings.xml`
- [x] Emulator, fresh install:
  - The section shows on, 09:00 and all seven days.
  - With the time set two minutes ahead, the notification fires.
  - With today deselected, nothing fires at that time.
  - The study reminder still fires at its own time.
  - Captures are taken in English and in Spanish (`cmd locale set-app-locales`).
- [x] `scripts/check` green

## Comments

- Done in 0e2c0950 (domain use cases), 32b029f5 (`HChip(enabled)`), 9e2ca26f (section, ViewModel, strings, DI, tests, `docs/SETTINGS_CURRENT.md` "Daily words flow", `docs/NOTIFICATIONS_PLAN.md` pointer).
- Tests, each red before its implementation, in `SettingsViewModelTest` through `onIntent` with the real use cases over `FakeDailyWordsSettingsRepository` and `RecordingDailyWordsScheduler`: initial state from persisted settings; switch on, switch off (cancels, no permission check), set time, deselect and select a weekday each persist and re-sync; switch on without permission emits `RequestNotificationPermission` and persists nothing; a granted settle after the daily-words request enables daily words only, and after the reminder request leaves daily words untouched; the last selected weekday cannot be deselected (no persist, no sync). 51 tests in the file pass.
- Existing test bodies are unchanged: two of them call the `SettingsViewModel` constructor positionally, so the test file gains a private top-level `SettingsViewModel(...)` with the old 15 parameters that fills in the daily-words fakes. `LargeClass` is suppressed on the test class.
- The "at least one day" rule lives in `ToggleDailyWordsDayUseCase`. `NotificationPermissionSettled` now enables only the switch recorded in `pendingNotificationPermission` (`StudyReminder` / `DailyWords`); a settle with nothing pending only updates the permission flag.
- Device, hello-1 (emulator-5620), API 36, captures on 9e2ca26f. The app was not wiped (App Check token, multi-session.md §Devices); to reproduce a fresh install the leftover `DAILY_WORDS_*` keys from ticket 04 were removed from the prefs with the app stopped.
  - On, 09:00, all seven days; chips wrap Mon–Fri then Sat–Sun: /Users/emm/AndroidStudioProjects/Hello-settings/.evidence/05-on-en-9e2ca26f.png, /Users/emm/AndroidStudioProjects/Hello-settings/.evidence/05-on-es-9e2ca26f.png
  - Time picker: /Users/emm/AndroidStudioProjects/Hello-settings/.evidence/05-timepicker-en-9e2ca26f.png, /Users/emm/AndroidStudioProjects/Hello-settings/.evidence/05-timepicker-es-9e2ca26f.png
  - Last day locked (tapping the only selected day keeps it): /Users/emm/AndroidStudioProjects/Hello-settings/.evidence/05-lastday-en-9e2ca26f.png, /Users/emm/AndroidStudioProjects/Hello-settings/.evidence/05-lastday-es-9e2ca26f.png
  - Off, time and chips dimmed, taps ignored: /Users/emm/AndroidStudioProjects/Hello-settings/.evidence/05-off-en-9e2ca26f.png, /Users/emm/AndroidStudioProjects/Hello-settings/.evidence/05-off-es-9e2ca26f.png, /Users/emm/AndroidStudioProjects/Hello-settings/.evidence/05-off-after-taps-en-9e2ca26f.png, /Users/emm/AndroidStudioProjects/Hello-settings/.evidence/05-off-after-taps-es-9e2ca26f.png
  - Permission revoked, same blocked subtitle as the reminder row: /Users/emm/AndroidStudioProjects/Hello-settings/.evidence/05-blocked-en-9e2ca26f.png, /Users/emm/AndroidStudioProjects/Hello-settings/.evidence/05-blocked-es-9e2ca26f.png
  - Rescheduling, from `no_backup/androidx.work.workdb` row `daily_words_daily`: before, override 2026-11-03 09:00 generation 9; time set to 10:00, override 10:00 generation 10; Tuesday deselected, generation 11 (override unchanged, still the next 10:00); switch off then on cancels and enqueues a new work id.
  - Time set to now + 2 min (16:10): the `daily_words` notification "Today: A job interview for a junior position · 6 new words and phrases" posted at 16:10:49. /Users/emm/AndroidStudioProjects/Hello-settings/.evidence/05-fires-en-9e2ca26f.png
  - Monday (today) deselected, time 16:15: JobScheduler started the worker late, at 16:30:19; it succeeded in 0.1 s and posted nothing.
  - Study reminder set to 16:35: the `study_reminders` notification posted at about 16:36. /Users/emm/AndroidStudioProjects/Hello-settings/.evidence/05-reminder-fires-en-9e2ca26f.png
  - Settings restored afterwards: daily words on, 09:00, all days; reminder 19:00; app locale en; permission granted.
- Not in this ticket, seen while checking: after a run, the periodic work's next run is 24 h after the actual run, not the configured time, until the next app launch re-pins it through `SyncDailyWordsUseCase`. The study reminder's scheduler works the same way.
