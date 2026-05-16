# Current Settings

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Settings` flow (backup export/import) |
| Source of Truth | No |
| Read this when | You need to understand exporting and importing local data |

## Summary

`Settings` lets you export local state to a file and restore the database from a backup, using the Storage Access Framework (SAF). It's the only feature that interacts with OS `Uri`s.

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
- `showConfirmDialog`
- `pendingImportUri: Uri?`

## Export flow

1. User triggers `ExportData` (no-op intent in VM — the `Route` opens the SAF picker).
2. When a `Uri` is resolved, the `Route` calls `viewModel.onExportUri(uri)`.
3. `SettingsViewModel.onExportUri`:
   - `isExporting = true`
   - `BackupExporter.export(uri)`
   - `onSuccess` → `ShowSuccess("Backup exported successfully")`
   - `onFailure` → `ShowError(message)` + log

## Import flow

1. User triggers `ImportData` (no-op intent in VM — the `Route` opens the SAF picker).
2. When a `Uri` is resolved, the `Route` calls `viewModel.onImportUri(uri)`.
3. `onImportUri` stores `pendingImportUri` and shows the confirmation dialog.
4. User confirms:
   - `ConfirmImport` → `BackupImporter.import(uri)` with success/error feedback
   - `CancelImport` → clears `pendingImportUri` and closes the dialog

## Effects

`SettingsUiEffect`:

- `ShowSuccess(message)`
- `ShowError(message)`

## MVI notes

`SettingsViewModel` partially breaks the pure `onIntent(intent)` contract: the `ExportData` and `ImportData` intents are handled in `Route` (SAF launcher), and the VM also exposes `onExportUri(uri)` / `onImportUri(uri)` as additional entry points. This is intentional because the URI comes from the system, not directly from UI.
