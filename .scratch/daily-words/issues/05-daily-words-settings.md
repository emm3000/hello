# 05: Settings: a New words section with a switch, a time and weekdays

Status: ready-for-agent
Device: emulator
Blocked by: 04
**Branch:** feat/05-daily-words-settings

Covers the first bullet of the spec's "What the learner sees". Gate row 3 (Koin `di/NewModule.kt`). Load `mattpocock-skills:tdd` for the ViewModel rules. Read `docs/SETTINGS_CURRENT.md` (the Reminders section, the reminder flow and its permission gating) and update it.
Code: `newfeatures/settings/` (`SettingsUiState`, `SettingsUiIntent`, `SettingsViewModel`, `SettingsScreen`), the `core/ui` `HSwitch`, `HTimePickerDialog` and `HChip`, and ticket 04's settings repository and sync use case. Values are read synchronously, like `GetStudyReminderSettingsUseCase`, so the section has no loading or error state.

## Acceptance
- [ ] `SettingsViewModelTest`:
  - The initial state carries the persisted daily-words settings.
  - Toggling the switch, setting the time and toggling a weekday each persist and re-sync the daily-words schedule.
  - Turning the switch on without notification permission requests it, as the study reminder does.
  - The last selected weekday cannot be deselected.
- [ ] The existing study reminder cases in `SettingsViewModelTest` pass without edits
- [ ] The screen has a "New words" / "Palabras nuevas" section after `settings_section_reminders`, separate from the study reminder row. It holds:
  - a switch row,
  - a time row that opens `HTimePickerDialog`,
  - seven `HChip` weekdays labelled with the locale's short day names, with `active` marking the selected days.
  The time and day controls are disabled while the switch is off, and the row shows the same blocked state as the reminder row when permission is denied.
- [ ] Every new string exists in both `values/strings.xml` and `values-es/strings.xml`
- [ ] Emulator, fresh install:
  - The section shows on, 09:00 and all seven days.
  - With the time set two minutes ahead, the notification fires.
  - With today deselected, nothing fires at that time.
  - The study reminder still fires at its own time.
  - Captures are taken in English and in Spanish (`cmd locale set-app-locales`).
- [ ] `scripts/check` green
