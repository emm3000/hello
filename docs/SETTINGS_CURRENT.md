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

`Settings` lets you export local state to a file and restore the database from a backup, using the Storage Access Framework (SAF), and configure the daily study reminder (on/off, time). It's the only feature that interacts with OS `Uri`s. Enabling the reminder also gates the `POST_NOTIFICATIONS` runtime permission (Android 13+): turning it on requests the permission if not already granted, and a blocked state is surfaced directly on the reminder row.

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
- `app/src/main/kotlin/com/emm/hello/notifications/NotificationPermission.kt` (port, `isGranted()`)
- `app/src/main/kotlin/com/emm/hello/notifications/SystemNotificationPermission.kt` (impl over `NotificationManagerCompat.areNotificationsEnabled()`)

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
- `isNotificationPermissionGranted: Boolean` — default `true`; overwritten in `init` with `NotificationPermission.isGranted()`

## Intents

`SettingsUiIntent`:

- `ExportData` → emits `LaunchExportPicker`
- `ImportData` → emits `LaunchImportPicker`
- `ExportUriReceived(uri)` → runs the export
- `ImportUriReceived(uri)` → stores `pendingImportUri` and opens the confirmation dialog
- `ConfirmImport` → runs the import
- `CancelImport` → clears `pendingImportUri` and closes the dialog
- `SetReminderEnabled(isEnabled)` → turning it off persists the flag directly; turning it on checks `NotificationPermission.isGranted()` first — see "Reminder flow"
- `EditReminderTime` → shows the time picker dialog
- `DismissReminderTimePicker` → hides it without saving
- `SetReminderTime(time)` → persists the time, syncs the scheduler, and hides the dialog
- `NotificationPermissionSettled` → re-reads `NotificationPermission.isGranted()` after the system permission dialog closes; if granted, also enables and persists the reminder
- `RefreshNotificationPermission` → re-reads `NotificationPermission.isGranted()` and updates state; sent by the `Route` on `ON_RESUME`
- `OpenNotificationSettings` → emits `OpenNotificationSettings`

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

A "Reminders" section sits between "Organization" and "Your data": one row with a bell icon, title "Daily study reminder", a trailing `HSwitch`, and a subtitle that depends on `isNotificationPermissionGranted`:

- **Granted:** subtitle "Every day at HH:mm" (the current `reminderTime`, 24h format), muted tone. Tapping the row (outside the switch) opens `HTimePickerDialog`.
- **Blocked:** subtitle `settings_notifications_blocked` ("Notifications are blocked. Tap to allow them in system settings."), rendered in `destructiveInk`. Tapping the row calls `OpenNotificationSettings` instead of opening the time picker.

The switch itself always toggles the reminder, independent of the row tap target.

`SettingsViewModel.init` loads the current settings via `GetStudyReminderSettingsUseCase` and seeds `isReminderEnabled` / `reminderTime`, and also reads `NotificationPermission.isGranted()` to seed `isNotificationPermissionGranted` — before the screen is shown, no loading state for this section.

- **Toggle off:** `SetReminderEnabled(false)` persists the flag directly (`SetStudyReminderEnabledUseCase(false)`) and sets `isReminderEnabled = false`. No permission check.
- **Toggle on:** `SetReminderEnabled(true)` first checks `NotificationPermission.isGranted()`.
  - If granted, persists the flag (`SetStudyReminderEnabledUseCase(true)`) and sets `isReminderEnabled = true`.
  - If not granted, the flag is **not** persisted and `isReminderEnabled` stays `false`; the ViewModel emits `RequestNotificationPermission` instead. The `Route` launches the system `POST_NOTIFICATIONS` prompt via `ActivityResultContracts.RequestPermission()`.
- **After the system dialog closes:** the `Route` always sends `NotificationPermissionSettled`, regardless of the launcher's own `Boolean` result (that result is ignored on purpose). The ViewModel re-reads `NotificationPermission.isGranted()` directly:
  - Granted → `isNotificationPermissionGranted = true`, and the reminder is now enabled and persisted (`SetStudyReminderEnabledUseCase(true)`, `isReminderEnabled = true`).
  - Denied → `isNotificationPermissionGranted = false`, the switch stays off, and the row shows the blocked subtitle.
- **Opening system settings:** `OpenNotificationSettings` (row tap while blocked) emits the `OpenNotificationSettings` effect; the `Route` starts `Settings.ACTION_APP_NOTIFICATION_SETTINGS` for the app's package.
- **Resuming the screen:** `SettingsRoute` re-sends `RefreshNotificationPermission` on `Lifecycle.Event.ON_RESUME` via `LifecycleEventEffect`, so returning from system settings (or from any permission change) re-reads `NotificationPermission.isGranted()` and updates the row without extra user action.
- **Existing install, reminder already ON but notifications later blocked:** the switch still shows ON (`isReminderEnabled` is untouched) while the row shows the blocked subtitle, because `isNotificationPermissionGranted` is independent, read-only state. The stored preference is never mutated by a permission check alone.
- **Time:** `EditReminderTime` shows `HTimePickerDialog` (24h, Save/Cancel) seeded with the current `reminderTime`. Confirming sends `SetReminderTime(time)` → `SetStudyReminderTimeUseCase(time)`, which persists the time and also runs `SyncStudyReminderUseCase`, then hides the dialog. `DismissReminderTimePicker` hides it without saving. This flow is unaffected by the permission state.
- **Sync contract:** `SyncStudyReminderUseCase` reads `StudyReminderSettingsRepository.get()` and calls `StudyReminderScheduler.schedule(time)` when enabled, or `StudyReminderScheduler.cancel()` when not. The same use case also runs once on every app launch (`App.onCreate()`), so the schedule is re-pinned even if it was never touched in this session — see `docs/NOTIFICATIONS_PLAN.md` for the scheduler mechanics.
- **Persistence:** `DataStoreStudyReminderSettingsRepository` (`:data`) stores the flag and the hour/minute as three separate `SharedPreferences` entries through the `DataStore` wrapper (`STUDY_REMINDER_ENABLED`, `STUDY_REMINDER_HOUR`, `STUDY_REMINDER_MINUTE`), not a serialized `LocalTime`.
- **Permission port:** `NotificationPermission.isGranted()` (`com.emm.hello.notifications`) is implemented by `SystemNotificationPermission` over `NotificationManagerCompat.from(context).areNotificationsEnabled()`, and wired as a Koin `single` in `NewModule.kt`.
- **Tests:** `SettingsViewModelTest` has 8 tests covering this flow (24 total in the file).

## Effects

`SettingsUiEffect`:

- `ShowSuccess(message)` — snackbar
- `ShowError(message)` — snackbar
- `LaunchExportPicker` — the `Route` opens the SAF create-document picker
- `LaunchImportPicker` — the `Route` opens the SAF open-document picker
- `RequestNotificationPermission` — the `Route` launches the system `POST_NOTIFICATIONS` prompt
- `OpenNotificationSettings` — the `Route` opens `Settings.ACTION_APP_NOTIFICATION_SETTINGS` for the app

## MVI notes

`SettingsViewModel` follows the pure `onIntent(intent)` contract. The SAF pickers are side effects: the VM asks for them through `LaunchExportPicker` / `LaunchImportPicker`, and the `Route` feeds the resulting `Uri` back as `ExportUriReceived` / `ImportUriReceived` intents. `EditReminderTime`, `DismissReminderTimePicker` and `SetReminderTime` never emit an effect — they only call a use case and/or update state directly. `SetReminderEnabled` conditionally emits `RequestNotificationPermission` (turning on while blocked); `OpenNotificationSettings` (intent) always emits `OpenNotificationSettings` (effect); `NotificationPermissionSettled` and `RefreshNotificationPermission` never emit an effect, they only re-read the port and update state. The permission round trip follows the same effect-in / intent-back shape as the SAF pickers: the VM asks for the system dialog, the `Route` shows it and reports back once it's settled, and the VM never touches `Context` directly. `SettingsViewModel` takes four extra constructor dependencies for this (`GetStudyReminderSettingsUseCase`, `SetStudyReminderEnabledUseCase`, `SetStudyReminderTimeUseCase`, `NotificationPermission`) and loads the initial reminder settings and permission state in `init`. There are no extra VM entry points.
