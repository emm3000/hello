# Current Settings

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Settings` flow (backup export/import) |
| Source of Truth | No |
| Read this when | You need to understand exporting and importing local data |
| Last verified | 2026-08-28 |

## Summary

`Settings` lets you export local state to a file and restore the database from a backup, using the Storage Access Framework (SAF). It's the only feature that interacts with OS `Uri`s.

Layout: an `HTopBar` with only a back arrow, then a `metadata` eyebrow, a `displayMedium` headline and a subtitle; an "Organization" section whose "Decks" row opens deck management; a "Your data" section with the export and import rows separated by an `HSeparator`; and a footer with a tagline plus a `metadata` meta line in `inkFaint`. Both sections are `surface` panels shaped with `helloShapes.control`, labelled via `HSectionLabel`; each row shows `titleSmall` title, `bodySmall` subtitle and a chevron (or an `HLoadingSpinner` while busy). The import row's subtitle ("Replaces everything you have now.") is rendered in `destructiveInk` to signal the destructive nature of the action.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsUiEffect.kt`

## :data dependencies

- `com.emm.data.export.BackupExporter`
- `com.emm.data.export.BackupImporter`

## State

`SettingsUiState`:

- `isExporting`
- `isImporting`
- `isConfirmDialogVisible`
- `pendingImportUri: Uri?`

## Intents

`SettingsUiIntent`:

- `ExportData` → emits `LaunchExportPicker`
- `ImportData` → emits `LaunchImportPicker`
- `ExportUriReceived(uri)` → runs the export
- `ImportUriReceived(uri)` → stores `pendingImportUri` and opens the confirmation dialog
- `ConfirmImport` → runs the import
- `CancelImport` → clears `pendingImportUri` and closes the dialog

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

## Effects

`SettingsUiEffect`:

- `ShowSuccess(message)` — snackbar
- `ShowError(message)` — snackbar
- `LaunchExportPicker` — the `Route` opens the SAF create-document picker
- `LaunchImportPicker` — the `Route` opens the SAF open-document picker

## MVI notes

`SettingsViewModel` follows the pure `onIntent(intent)` contract. The SAF pickers are side effects: the VM asks for them through `LaunchExportPicker` / `LaunchImportPicker`, and the `Route` feeds the resulting `Uri` back as `ExportUriReceived` / `ImportUriReceived` intents. There are no extra VM entry points.
