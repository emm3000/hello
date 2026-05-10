# Dashboard Actual

| Field | Value |
|---|---|
| Status | Active |
| Role | Referencia factual de feature |
| Scope | Flujo `Dashboard` |
| Source of Truth | No |
| Read this when | Necesitás entender la lista de decks, búsqueda y filtros actuales |

## Resumen

`Dashboard` es la pantalla principal para ver decks, navegar a detalle, crear deck/card y aplicar búsqueda + filtros por tags sobre datos locales de `HelloDb`.

## Archivos clave

- `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardUiIntent.kt`

## Estado y criterios

`DashboardUiState` concentra en una sola fuente:

- `searchQuery`
- `selectedTags`
- `availableTags`
- `decks` (lista renderizada)
- `totalDeckCount`
- `emptyState` (`LibraryEmpty`, `NoResults`, `None`)

La lista renderizada se calcula desde criterios activos (query + tags) y no desde fuentes paralelas.

## Búsqueda y filtros

- búsqueda por nombre de deck case-insensitive
- filtros por tags con intersección (match ALL)
- acción `ClearFilters` limpia query + tags en un solo paso

## Decisión de persistencia de filtros

Actualmente **no se persisten** filtros entre sesiones.

- al abrir/recrear la pantalla, `searchQuery` arranca vacío
- `selectedTags` arranca vacío

Motivo: mantener comportamiento predecible y evitar estado stale entre sesiones mientras el producto sigue local-first single-device.

## Empty states

- `LibraryEmpty`: no hay decks en base local
- `NoResults`: hay decks, pero ningún resultado con los criterios activos

## Componentes UI reutilizados

La UI usa componentes compartidos de `core/ui`:

- `HSearchBar`
- `HTagChip`
- `HButton`
- `HBadge`

No se introducen componentes Material3 raw para controles de búsqueda/filtro.
