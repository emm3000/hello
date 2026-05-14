# Settings Actual

| Field | Value |
|---|---|
| Status | Active |
| Role | Referencia factual de feature |
| Scope | Flujo `Settings` (export/import de backup) |
| Source of Truth | No |
| Read this when | Necesitás entender la exportación e importación de datos locales |

## Resumen

`Settings` permite exportar el estado local a un archivo y restaurar la base desde un backup, usando Storage Access Framework (SAF). Es la única feature que interactúa con `Uri` del sistema operativo.

## Archivos clave

- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsUiEffect.kt`

## Dependencias de :data

- `com.emm.data.export.BackupExporter`
- `com.emm.data.export.BackupImporter`

## Estado

`SettingsUiState`:

- `isExporting`
- `isImporting`
- `showConfirmDialog`
- `pendingImportUri: Uri?`

## Flujo de export

1. Usuario gatilla `ExportData` (intent no-op en VM — el `Route` abre SAF picker).
2. Cuando hay `Uri` resuelta, el `Route` llama `viewModel.onExportUri(uri)`.
3. `SettingsViewModel.onExportUri`:
   - `isExporting = true`
   - `BackupExporter.export(uri)`
   - `onSuccess` → `ShowSuccess("Backup exported successfully")`
   - `onFailure` → `ShowError(message)` + log

## Flujo de import

1. Usuario gatilla `ImportData` (intent no-op en VM — el `Route` abre SAF picker).
2. Cuando hay `Uri` resuelta, el `Route` llama `viewModel.onImportUri(uri)`.
3. `onImportUri` guarda `pendingImportUri` y muestra diálogo de confirmación.
4. Usuario confirma:
   - `ConfirmImport` → `BackupImporter.import(uri)` con feedback success/error
   - `CancelImport` → limpia `pendingImportUri` y cierra diálogo

## Efectos

`SettingsUiEffect`:

- `ShowSuccess(message)`
- `ShowError(message)`

## Notas sobre MVI

`SettingsViewModel` rompe parcialmente el contrato `onIntent(intent)` puro: las intents `ExportData` e `ImportData` se manejan en `Route` (SAF launcher), y el VM expone también `onExportUri(uri)` / `onImportUri(uri)` como entry points adicionales. Es intencional porque la URI viene del sistema y no de la UI directamente.
