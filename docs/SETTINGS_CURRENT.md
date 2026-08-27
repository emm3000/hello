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

Layout (redesign Phase 2.8): a mono eyebrow, a headline and a subtitle, a single `instrumentSurface` "Tus datos" section with the export and import rows separated by an `HSeparator`, and a footer with a tagline plus a muted mono meta line. The import row's subtitle ("Reemplazar todo lo que hay ahora") is rendered in `instrumentBad` to signal the destructive nature of the action.

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

## Export flow

1. User triggers `ExportData` (no-op intent in VM — the `Route` opens the SAF picker).
2. When a `Uri` is resolved, the `Route` calls `viewModel.onExportUri(uri)`.
3. `SettingsViewModel.onExportUri`:
   - `isExporting = true`
   - `BackupExporter.export(uri)`
   - `onSuccess` → `ShowSuccess("Backup exportado correctamente")`
   - `onFailure` → `ShowError("No se pudo exportar el backup")` + log

## Import flow

1. User triggers `ImportData` (no-op intent in VM — the `Route` opens the SAF picker).
2. When a `Uri` is resolved, the `Route` calls `viewModel.onImportUri(uri)`.
3. `onImportUri` stores `pendingImportUri` and shows the confirmation dialog.
4. User confirms:
   - `ConfirmImport` → `BackupImporter.import(uri)`
     - `onSuccess` → `ShowSuccess("Backup restaurado correctamente")`
     - `onFailure` → `ShowError(humanizeImportError(error))` + log; `humanizeImportError` returns a fixed Spanish string — `IncompatibleSchemaException` (or a cause of it) maps to a version-mismatch message, all other errors map to `"No se pudo restaurar el backup."`. Raw `error.message` is never surfaced to the UI.
   - `CancelImport` → clears `pendingImportUri` and closes the dialog

## Effects

`SettingsUiEffect`:

- `ShowSuccess(message)`
- `ShowError(message)`

## MVI notes

`SettingsViewModel` partially breaks the pure `onIntent(intent)` contract: the `ExportData` and `ImportData` intents are handled in `Route` (SAF launcher), and the VM also exposes `onExportUri(uri)` / `onImportUri(uri)` as additional entry points. This is intentional because the URI comes from the system, not directly from UI.
