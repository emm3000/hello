# Current Settings

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Settings` flow (backup export/import, daily study reminder) |
| Source of Truth | No |
| Read this when | You need to understand exporting/importing local data, or the daily study reminder |
| Last verified | 2026-09-06 |

## Summary

`Settings` lets you export local state to a file and restore the database from a backup, using the Storage Access Framework (SAF), and configure the daily study reminder (on/off, time). It's the only feature that interacts with OS `Uri`s.

Layout: an `HTopBar` with only a back arrow, then a `metadata` eyebrow, a `displayMedium` headline and a subtitle; an "Organization" section whose "Decks" row opens deck management; a "Reminders" section with a single row for the daily study reminder; a "Your data" section with the export and import rows separated by an `HSeparator`; and a footer with a tagline plus a `metadata` meta line in `inkFaint`. All sections are `surface` panels shaped with `helloShapes.control`, labelled via `HSectionLabel`; each row shows `titleSmall` title, `bodySmall` subtitle and a chevron, a trailing control, or an `HLoadingSpinner` while busy. The import row's subtitle ("Replaces everything you have now.") is rendered in `destructiveInk` to signal the destructive nature of the action.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsUiEffect.kt`
- `app/src/main/kotlin/com/emm/hello/core/ui/Switch.kt` (`HSwitch`)
- `app/src/main/kotlin/com/emm/hello/core/ui/TimePicker.kt` (`HTimePickerDialog`)

## :data / :domain dependencies

- `com.emm.data.export.BackupExporter`
- `com.emm.data.export.BackupImporter`
- `com.emm.domain.reminder.GetStudyReminderSettingsUseCase`
- `com.emm.domain.reminder.SetStudyReminderEnabledUseCase`
- `com.emm.domain.reminder.SetStudyReminderTimeUseCase`

## State

`SettingsUiState`:

- `isExporting`
- `isImporting`
- `isConfirmDialogVisible`
- `pendingImportUri: Uri?`
- `isReminderEnabled: Boolean` — default `true`
- `reminderTime: LocalTime` — default `StudyReminderSettings.DEFAULT_TIME` (19:00)
- `isReminderTimePickerVisible: Boolean`

## Intents

`SettingsUiIntent`:

- `ExportData` → emits `LaunchExportPicker`
- `ImportData` → emits `LaunchImportPicker`
- `ExportUriReceived(uri)` → runs the export
- `ImportUriReceived(uri)` → stores `pendingImportUri` and opens the confirmation dialog
- `ConfirmImport` → runs the import
- `CancelImport` → clears `pendingImportUri` and closes the dialog
- `SetReminderEnabled(isEnabled)` → persists the flag and syncs the scheduler
- `EditReminderTime` → shows the time picker dialog
- `DismissReminderTimePicker` → hides it without saving
- `SetReminderTime(time)` → persists the time, syncs the scheduler, and hides the dialog

## Export flow

1. User taps the export row → `ExportData` → `LaunchExportPicker`.
2. The `Route` launches `ActivityResultContracts.CreateDocument("application/json")` with a `hello-backup-<millis>.json` suggestion.
3. A resolved `Uri` comes back as `ExportUriReceived(uri)`; a cancelled picker sends nothing.
4. `SettingsViewModel`:
   - `isExporting = true`
   - `BackupExporter.export(uri)`
   - `onSuccess` → `ShowSuccess("Backup exported successfully")`
   - `onFailure` → `ShowError("Couldn't export the backup")` + log
   - `isExporting = false`

## Import flow

1. User taps the import row → `ImportData` → `LaunchImportPicker`.
2. The `Route` launches `ActivityResultContracts.OpenDocument()` filtered to `application/json`.
3. A resolved `Uri` comes back as `ImportUriReceived(uri)`, which stores `pendingImportUri` and shows the `HAlertDialog` ("Replace all data?", dangerous, confirm "Replace").
4. User confirms:
   - `ConfirmImport` → closes the dialog, `isImporting = true`, `BackupImporter.import(uri)`
     - `onSuccess` → `ShowSuccess("Backup restored")`
     - `onFailure` → `ShowError(humanizeImportError(error))` + log; `humanizeImportError` returns a fixed string — `IncompatibleSchemaException` (or a cause of it) maps to "This backup was created with another version of the app. Update the app and try again.", all other errors map to "Couldn't restore the backup.". Raw `error.message` is never surfaced to the UI.
     - then `isImporting = false`, `pendingImportUri = null`
   - `CancelImport` → clears `pendingImportUri` and closes the dialog

## Reminder flow

A "Reminders" section sits between "Organization" and "Your data": one row with a bell icon, title "Daily study reminder", subtitle "Every day at HH:mm" (the current `reminderTime`, 24h format), and a trailing `HSwitch`. Tapping the row (outside the switch) opens `HTimePickerDialog`; the switch toggles the reminder directly.

`SettingsViewModel.init` loads the current settings via `GetStudyReminderSettingsUseCase` and seeds `isReminderEnabled` / `reminderTime` before the screen is shown — no loading state for this section.

- **Toggle:** `SetReminderEnabled(isEnabled)` → `SetStudyReminderEnabledUseCase(isEnabled)`, which persists the flag through `StudyReminderSettingsRepository` and then runs `SyncStudyReminderUseCase`.
- **Time:** `EditReminderTime` shows `HTimePickerDialog` (24h, Save/Cancel) seeded with the current `reminderTime`. Confirming sends `SetReminderTime(time)` → `SetStudyReminderTimeUseCase(time)`, which persists the time and also runs `SyncStudyReminderUseCase`, then hides the dialog. `DismissReminderTimePicker` hides it without saving.
- **Sync contract:** `SyncStudyReminderUseCase` reads `StudyReminderSettingsRepository.get()` and calls `StudyReminderScheduler.schedule(time)` when enabled, or `StudyReminderScheduler.cancel()` when not. The same use case also runs once on every app launch (`App.onCreate()`), so the schedule is re-pinned even if it was never touched in this session — see `docs/NOTIFICATIONS_PLAN.md` for the scheduler mechanics.
- **Persistence:** `DataStoreStudyReminderSettingsRepository` (`:data`) stores the flag and the hour/minute as three separate `SharedPreferences` entries through the `DataStore` wrapper (`STUDY_REMINDER_ENABLED`, `STUDY_REMINDER_HOUR`, `STUDY_REMINDER_MINUTE`), not a serialized `LocalTime`.

There is no reminder-specific effect: all four reminder intents update state synchronously, and the dialog's visibility is state, not a one-shot effect.

## Effects

`SettingsUiEffect`:

- `ShowSuccess(message)` — snackbar
- `ShowError(message)` — snackbar
- `LaunchExportPicker` — the `Route` opens the SAF create-document picker
- `LaunchImportPicker` — the `Route` opens the SAF open-document picker

## MVI notes

`SettingsViewModel` follows the pure `onIntent(intent)` contract. The SAF pickers are side effects: the VM asks for them through `LaunchExportPicker` / `LaunchImportPicker`, and the `Route` feeds the resulting `Uri` back as `ExportUriReceived` / `ImportUriReceived` intents. The reminder intents (`SetReminderEnabled`, `EditReminderTime`, `DismissReminderTimePicker`, `SetReminderTime`) never emit an effect — they only call a use case and/or update state directly. `SettingsViewModel` takes three extra constructor dependencies for this (`GetStudyReminderSettingsUseCase`, `SetStudyReminderEnabledUseCase`, `SetStudyReminderTimeUseCase`) and loads the initial reminder settings in `init`. There are no extra VM entry points.
