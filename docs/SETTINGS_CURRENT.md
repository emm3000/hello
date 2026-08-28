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

Layout: a `metadata` eyebrow, a `displayMedium` headline and a subtitle; an "Organization" section whose "Decks" row opens deck management; a "Your data" section with the export and import rows separated by an `HSeparator`; and a footer with a tagline plus a `metadata` meta line in `inkFaint`. Both sections are `surface` panels shaped with `helloShapes.control`, labelled via `HSectionLabel`; each row shows `titleSmall` title, `bodySmall` subtitle and a chevron (or an `HLoadingSpinner` while busy). The import row's subtitle ("Replaces everything you have now.") is rendered in `destructiveInk` to signal the destructive nature of the action.

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
