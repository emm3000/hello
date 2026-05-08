# Design: Study Stats and Streak on Dashboard

## Technical Approach

Add 4 study metrics (studied today, due today, current streak, due this week) as a 2x2 StatCard grid on DashboardScreen. Single use case returns all stats via one repository call. SQL queries added to existing `LocalFirst.sq`. No DB migration needed — `ReviewEvent` and `ReviewProjection` tables already exist.

## Architecture Decisions

| Decision | Options | Tradeoff | Choice |
|----------|---------|----------|--------|
| Use case shape | 4 separate vs 1 combined | 4 use cases = more boilerplate, 4 Flow subscriptions in VM. 1 use case = single call, simpler VM | **1 `GetDashboardStatsUseCase`** returning `DashboardStats` — stats always displayed together, no independent consumption |
| Data delivery | Flow vs suspend | Flow auto-refreshes on DB change. Suspend = simpler, load-once | **Suspend** — stats change rarely (only after reviews), VM calls in `init`, refresh on re-entry |
| Streak timezone | `ZoneId.systemDefault()` vs injectable | System default = simpler, matches user expectation. Injectable = testable across zones | **`ZoneId.systemDefault()`** — local-first single-device, no server sync. Tests use explicit zone via `Clock` |
| Where SQL lives | `LocalFirst.sq` vs new `Stats.sq` | Existing file = fewer files, logical grouping with ReviewEvent/Projection. New file = separation | **`LocalFirst.sq`** — queries touch ReviewEvent/ReviewProjection already defined there |
| StatCards loading | Parallel queries vs single transaction | Parallel = faster, independent. Transaction = consistent snapshot | **Parallel** — stats are independent aggregates, eventual consistency acceptable for dashboard |

## Data Flow

```
DashboardScreen
    │ (composable reads state)
    ▼
DashboardUiState (stats: DashboardStats?)
    │
DashboardViewModel.init()
    │ getDashboardStatsUseCase()  ← suspend, called once
    ▼
GetDashboardStatsUseCase.invoke()
    │ studyStatsRepository.getDashboardStats()
    ▼
DefaultStudyStatsRepository
    ├─ countStudiedToday()        ← SQL: COUNT WHERE reviewedAt in [todayStart, todayEnd)
    ├─ countDueToday()            ← SQL: COUNT WHERE nextReviewAt in [todayStart, todayEnd)
    ├─ countDueThisWeek()         ← SQL: COUNT WHERE nextReviewAt in [weekStart, weekEnd)
    └─ getDistinctReviewDates()   ← SQL: SELECT DISTINCT date(reviewedAt/1000, 'unixepoch')
         │
         └─ computeStreak()       ← domain logic: count consecutive days back from today
    ▼
DashboardStats(studiedToday, dueToday, streak, dueThisWeek)
```

### Sequence Diagram

```
DashboardScreen          DashboardViewModel        GetDashboardStats      StudyStatsRepository      DefaultStudyStatsRepo      SQLDelight
     │                         │                         │                         │                         │                    │
     │  collect uiState        │                         │                         │                         │                    │
     │◄────────────────────────┤                         │                         │                         │                    │
     │                         │                         │                         │                         │                    │
     │                         │ init()                  │                         │                         │                    │
     │                         │ ──launchIn─────────►    │                         │                         │                    │
     │                         │                         │  invoke()               │                         │                    │
     │                         │                         │ ──────────────────────► │                         │                    │
     │                         │                         │                         │  getDashboardStats()    │                    │
     │                         │                         │                         │ ──────────────────────► │                    │
     │                         │                         │                         │                         │  countStudiedToday()│
     │                         │                         │                         │                         │ ──────────────────►│
     │                         │                         │                         │                         │◄───────────────────│
     │                         │                         │                         │                         │  Long               │
     │                         │                         │                         │                         │                     │
     │                         │                         │                         │                         │  countDueToday()    │
     │                         │                         │                         │                         │ ──────────────────►│
     │                         │                         │                         │                         │◄───────────────────│
     │                         │                         │                         │                         │  Long               │
     │                         │                         │                         │                         │                     │
     │                         │                         │                         │                         │  countDueThisWeek() │
     │                         │                         │                         │                         │ ──────────────────►│
     │                         │                         │                         │                         │◄───────────────────│
     │                         │                         │                         │                         │  Long               │
     │                         │                         │                         │                         │                     │
     │                         │                         │                         │                         │  getDistinctDates() │
     │                         │                         │                         │                         │ ──────────────────►│
     │                         │                         │                         │                         │◄───────────────────│
     │                         │                         │                         │                         │  List<String>       │
     │                         │                         │                         │                         │                     │
     │                         │                         │                         │                         │  computeStreak()    │
     │                         │                         │                         │  DashboardStats ◄───────│                     │
     │                         │                         │  DashboardStats ◄──────┤                         │                    │
     │                         │  DashboardStats ◄──────┤                         │                         │                    │
     │                         │  mutableState.value = state.copy(stats = it)     │                         │                    │
     │  uiState emits ◄───────┤                         │                         │                         │                    │
     │  (recompose)            │                         │                         │                         │                    │
```

## Streak Algorithm

```kotlin
// In DefaultStudyStatsRepository.computeStreak(distinctDates: List<String>): Int
// distinctDates = ["2026-05-08", "2026-05-07", "2026-05-06", ...] sorted DESC

fun computeStreak(distinctDates: List<String>): Int {
    if (distinctDates.isEmpty()) return 0

    val today = LocalDate.now(ZoneId.systemDefault())
    val mostRecent = LocalDate.parse(distinctDates.first())  // "yyyy-MM-dd"

    // Streak broken if most recent review is not today
    if (mostRecent != today) return 0

    var streak = 1
    for (i in 1 until distinctDates.size) {
        val expected = today.minusDays(i.toLong())
        val actual = LocalDate.parse(distinctDates[i])
        if (actual == expected) streak++ else break
    }
    return streak
}
```

**Edge cases handled:**
- No reviews → streak = 0
- Most recent review before today → streak = 0 (gap detected)
- Review today only → streak = 1
- DST transitions → irrelevant, `LocalDate` ignores time-of-day
- Multiple reviews same day → `DISTINCT` deduplicates

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `domain/src/main/kotlin/com/emm/domain/study/DashboardStats.kt` | Create | Data class: `studiedToday: Int, dueToday: Int, streak: Int, dueThisWeek: Int` |
| `domain/src/main/kotlin/com/emm/domain/study/StudyStatsRepository.kt` | Create | Interface: `suspend fun getDashboardStats(): DashboardStats` |
| `domain/src/main/kotlin/com/emm/domain/study/GetDashboardStatsUseCase.kt` | Create | `operator fun invoke(): DashboardStats` delegating to repository |
| `data/src/main/sqldelight/com/emm/data/LocalFirst.sq` | Modify | Add 4 queries: `countStudiedToday`, `countDueToday`, `countDueThisWeek`, `distinctReviewDates` |
| `data/src/main/kotlin/com/emm/data/study/DefaultStudyStatsRepository.kt` | Create | Implements `StudyStatsRepository` with SQLDelight queries + `computeStreak()` |
| `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardUiState.kt` | Modify | Add `val stats: DashboardStats? = null` |
| `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardViewModel.kt` | Modify | Inject `GetDashboardStatsUseCase`, call in `init` via `viewModelScope.launch` |
| `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardScreen.kt` | Modify | Add `StatsSection` composable with 2x2 grid between SessionSummaryBanner and deck list |
| `app/src/main/kotlin/com/emm/hello/di/NewModule.kt` | Modify | Register `DefaultStudyStatsRepository`, `GetDashboardStatsUseCase`, update `DashboardViewModel` binding |
| `domain/src/test/kotlin/com/emm/domain/study/GetDashboardStatsUseCaseTest.kt` | Create | Unit test with fake repository |
| `data/src/test/kotlin/com/emm/data/study/DefaultStudyStatsRepositoryTest.kt` | Create | Streak algorithm tests + query tests with in-memory DB |

## Interfaces / Contracts

```kotlin
// domain/.../study/DashboardStats.kt
data class DashboardStats(
    val studiedToday: Int,
    val dueToday: Int,
    val streak: Int,
    val dueThisWeek: Int,
)

// domain/.../study/StudyStatsRepository.kt
interface StudyStatsRepository {
    suspend fun getDashboardStats(): DashboardStats
}

// domain/.../study/GetDashboardStatsUseCase.kt
class GetDashboardStatsUseCase(
    private val repository: StudyStatsRepository,
) {
    suspend operator fun invoke(): DashboardStats = repository.getDashboardStats()
}
```

**SQL queries added to `LocalFirst.sq`:**

```sql
-- Uses reviewedAt (epoch millis). Divide by 1000 for unixepoch modifier.
-- todayStart = epoch millis of 00:00 today, todayEnd = 00:00 tomorrow

countStudiedToday:
SELECT COUNT(*) FROM ReviewEvent
WHERE reviewedAt >= :startMillis AND reviewedAt < :endMillis;

countDueToday:
SELECT COUNT(*) FROM ReviewProjection
WHERE nextReviewAt >= :startMillis AND nextReviewAt < :endMillis;

countDueThisWeek:
SELECT COUNT(*) FROM ReviewProjection
WHERE nextReviewAt >= :startMillis AND nextReviewAt < :endMillis;

-- Returns dates as "yyyy-MM-dd" strings, sorted DESC
distinctReviewDates:
SELECT DISTINCT strftime('%Y-%m-%d', reviewedAt / 1000, 'unixepoch') AS date
FROM ReviewEvent
ORDER BY date DESC;
```

**Date boundary computation** (in `DefaultStudyStatsRepository`):

```kotlin
private fun todayRange(): Pair<Long, Long> {
    val now = LocalDate.now(ZoneId.systemDefault())
    val start = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val end = now.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return start to end
}

private fun weekRange(): Pair<Long, Long> {
    val now = LocalDate.now(ZoneId.systemDefault())
    val start = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val end = now.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return start to end
}
```

## UI Composition

```
DashboardScreen (LazyColumn)
├── SessionSummaryBanner          ← existing
├── StatsSection                  ← NEW: 2x2 grid
│   ├── StatCard("12", "Estudiadas hoy")
│   ├── StatCard("5", "Debidas hoy")
│   ├── StatCard("7", "Racha 🔥")
│   └── StatCard("23", "Debidas esta semana")
├── Section Header ("Decks")      ← existing
└── DeckItem list                 ← existing
```

`StatsSection` composable:
- Placed after `SessionSummaryBanner` item, before section header
- Uses `LazyGrid` (2 columns) or `Row` + `Column` composition
- Only renders when `state.stats != null`
- Uses existing `StatCard` with `StatCardStatus.Default`

## DI Wiring

In `NewModule.kt`:

```kotlin
fun Module.repository() {
    // ... existing ...
    factoryOf(::DefaultStudyStatsRepository) bind StudyStatsRepository::class
}

fun Module.useCases() {
    // ... existing ...
    factoryOf(::GetDashboardStatsUseCase)
}

fun Module.viewModels() {
    viewModel {
        DashboardViewModel(
            getDecksUseCase = get(),
            getDashboardStatsUseCase = get(),
        )
    }
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Domain — UseCase | `GetDashboardStatsUseCase` delegates to repository, returns result | Fake `StudyStatsRepository`, `runTest` |
| Domain — Streak | Streak = 0 (no dates), 1 (today only), N (consecutive), 0 (gap before today) | Pure function tests, no coroutines needed |
| Data — Repository | SQL queries return correct counts for known data | In-memory SQLDelight driver, insert test data, assert |
| Data — Date ranges | `todayRange()` and `weekRange()` produce correct boundaries | Fixed `Clock` or `LocalDate.now()` mocking |
| App — ViewModel | Stats loaded on init, state updated correctly | `Turbine` on `uiState`, fake use case |

## Migration / Rollout

No migration needed. All changes are additive: new SQL queries, new classes, new UI section. Rollback = revert commits.

## Open Questions

- [ ] Should streak display use a fire emoji or text label? (UI detail,不影响 architecture)
- [ ] Should stats section show a loading skeleton while stats load? (UX polish, can be follow-up)
