# Proposal: Study stats and streak on dashboard

Agregar 4 métricas de estudio al dashboard principal: cartas estudiadas hoy, cartas debidas hoy, racha actual de estudio y cartas debidas esta semana. El usuario necesita feedback inmediato de su progreso sin entrar a una pantalla de estadísticas separada.

## Scope

### In Scope
- 4 queries SQL sobre `ReviewEvent` y `ReviewProjection` (studied today, due today, due this week, streak)
- Nuevo `StudyStatsRepository` en domain + implementación en data
- 4 use cases en domain: `GetCardsStudiedToday`, `GetCardsDueToday`, `GetCurrentStreak`, `GetCardsDueThisWeek`
- Extender `DashboardViewModel` y `DashboardUiState` con estado de stats
- Sección de 2x2 `StatCard` en `DashboardScreen` entre `SessionSummaryBanner` y la lista de decks
- Reutilizar componente `StatCard` existente de `core/ui`

### Out of Scope
- Nueva pantalla de estadísticas detalladas
- Gráficos, historial semanal/mensual o tendencias
- Persistencia/caching de stats (se computan on-demand)
- Personalización de qué stats mostrar
- Streaks por deck (streak es global)

## Capabilities

### New Capabilities
- `study-stats`: expone métricas de estudio (cards today, due today, due this week, current streak) consumibles por la UI del dashboard.

### Modified Capabilities
- None.

## Approach

**Domain**: interfaz `StudyStatsRepository` con 4 métodos suspending. 4 use cases independientes, cada uno con su propio contrato de entrada/salida. Streak se computa contando días consecutivos hacia atrás desde hoy usando `ZoneId.systemDefault()`.

**Data**: queries nuevas en `LocalFirst.sq` sobre `ReviewEvent` (date-range counts, distinct dates para streak) y `ReviewProjection` (due cards). `DefaultStudyStatsRepository` implementa la interfaz del domain. `Clock` inyectado para tiempo testeable.

**App**: `DashboardViewModel` dispara los 4 use cases en paralelo al entrar al dashboard. `DashboardUiState` se extiende con campos de stats. `DashboardScreen` renderiza un grid 2x2 de `StatCard` entre el banner de resumen y la sección de decks.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/src/main/kotlin/.../study/StudyStatsRepository.kt` | New | Interfaz de repositorio para stats |
| `domain/src/main/kotlin/.../study/usecase/GetCardsStudiedToday.kt` | New | Use case |
| `domain/src/main/kotlin/.../study/usecase/GetCardsDueToday.kt` | New | Use case |
| `domain/src/main/kotlin/.../study/usecase/GetCurrentStreak.kt` | New | Use case |
| `domain/src/main/kotlin/.../study/usecase/GetCardsDueThisWeek.kt` | New | Use case |
| `data/src/main/kotlin/.../LocalFirst.sq` | Modified | Queries SQL para stats |
| `data/src/main/kotlin/.../DefaultStudyStatsRepository.kt` | New | Implementación del repositorio |
| `data/src/main/kotlin/.../KoinModule.kt` | Modified | Registrar nuevo repositorio |
| `app/src/main/kotlin/.../DashboardViewModel.kt` | Modified | Agregar estado y disparo de use cases |
| `app/src/main/kotlin/.../DashboardUiState.kt` | Modified | Campos de stats |
| `app/src/main/kotlin/.../DashboardScreen.kt` | Modified | Grid 2x2 de StatCards |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Queries costosos en DB grande | Low | Indexes existentes en `reviewedAt` y `nextReviewAt` cubren los rangos. Medir con >10k rows si es necesario. |
| Streak computation incorrecta con timezones | Medium | Usar `ZoneId.systemDefault()` consistente y tests con edge cases (midnight, DST). |
| Dashboard load time impact | Low | 4 queries en paralelo, sin blocking I/O. Si se vuelve lento, agregar debounce o caching. |
| Review budget exceeded (>400 lines) | Medium | Forecast: ~250-350 lines. Si crece, split en 2 PRs: data+domain primero, app UI después. |

## Rollback Plan

Revertir el commit del change completo. No hay migraciones de DB ni cambios irreversibles. Los queries SQL nuevos son aditivos y no afectan queries existentes.

## Dependencies

- `ReviewEvent` y `ReviewProjection` tables ya existen en SQLDelight
- `StatCard` composable ya existe en `core/ui`
- `Clock` interface ya existe para tiempo testeable
- Koin DI ya configurado en app

## Success Criteria

- [ ] Dashboard muestra 4 StatCards con valores correctos después de una review
- [ ] Streak se resetea correctamente al saltar un día
- [ ] "Studied today" refleja reviews del día actual (timezone local)
- [ ] "Due today" y "Due this week" coinciden con `ReviewProjection`
- [ ] Tests unitarios para los 4 use cases con `Clock` fake
- [ ] detekt clean sin warnings nuevos
- [ ] PR bajo 400 lines changed (o split en chained PRs si excede)
