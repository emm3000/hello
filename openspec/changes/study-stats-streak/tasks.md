# Tasks: Study Stats Streak

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 350-420 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR (domain+data+app tightly coupled, no independent slice) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Domain + Data layer (repo interface, use case, SQL queries, impl) | PR 1 | Fully testable without UI, ~180 lines |
| 2 | App layer (UI state, ViewModel, composables, DI wiring) | PR 2 | Depends on PR 1, ~170 lines |

## Phase 1: Domain

- [ ] 1.1 Create `DashboardStats` data class in `domain/src/main/kotlin/com/emm/domain/study/DashboardStats.kt` with 4 Int fields: cardsStudiedToday, cardsDueToday, currentStreak, cardsDueThisWeek
- [ ] 1.2 Create `StudyStatsRepository` interface in `domain/src/main/kotlin/com/emm/domain/study/StudyStatsRepository.kt` with 4 suspend methods matching the 4 metrics, each accepting `clock: Clock` for testable time
- [ ] 1.3 Create `GetDashboardStatsUseCase` in `domain/src/main/kotlin/com/emm/domain/study/GetDashboardStatsUseCase.kt` as a single suspend function invoking all 4 repo methods and returning `DashboardStats`
- [ ] 1.4 Write domain unit tests in `domain/src/test/kotlin/com/emm/domain/study/GetDashboardStatsUseCaseTest.kt` with fake `StudyStatsRepository`, covering: all-zero scenario, populated scenario, streak algorithm edge cases (no reviews, 1-day streak, broken streak, multi-day streak)

## Phase 2: Data

- [ ] 2.1 Add 4 SQL queries to `data/src/main/sqldelight/com/emm/data/LocalFirst.sq`: countStudiedToday (COUNT DISTINCT flashcardId with reviewedAt range), countDueToday (COUNT where nextReviewAt <= now), countDistinctReviewDatesDesc (SELECT DISTINCT date for streak), countDueThisWeek (COUNT where nextReviewAt in [now, now+7d])
- [ ] 2.2 Create `DefaultStudyStatsRepository` in `data/src/main/kotlin/com/emm/data/study/DefaultStudyStatsRepository.kt` implementing `StudyStatsRepository`, using SQLDelight generated queries with epoch millis range parameters computed via `ZoneId.systemDefault()`
- [ ] 2.3 Write data integration tests in `data/src/test/kotlin/com/emm/data/study/DefaultStudyStatsRepositoryTest.kt` using in-memory SQLDelight, covering: empty DB returns 0, single review today, streak continuity and break, due-this-week boundary

## Phase 3: App

- [ ] 3.1 Extend `DashboardUiState` in `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardUiState.kt` with optional `stats: DashboardStats? = null` field
- [ ] 3.2 Modify `DashboardViewModel` in `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardViewModel.kt` to accept `GetDashboardStatsUseCase`, launch a coroutine on init to fetch stats and update state
- [ ] 3.3 Create `DashboardStatsSection` composable in `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardStatsSection.kt` rendering a 2x2 grid of `StatCard` components using existing `StatCard` from `core/ui`
- [ ] 3.4 Integrate `DashboardStatsSection` into `DashboardScreen` in `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardScreen.kt` as a LazyColumn item between SessionSummaryBanner and Decks section header, gated on `state.stats != null`
- [ ] 3.5 Wire DI in `app/src/main/kotlin/com/emm/hello/di/NewModule.kt`: add `factoryOf(::DefaultStudyStatsRepository) bind StudyStatsRepository::class` in `repository()`, `factoryOf(::GetDashboardStatsUseCase)` in `useCases()`, update `DashboardViewModel` constructor in `viewModels()`

## Phase 4: Verification

- [ ] 4.1 Extend `DashboardViewModelTest` in `app/src/test/java/com/emm/hello/newfeatures/dashboard/DashboardViewModelTest.kt` with tests verifying stats are fetched on init and emitted in state, using a fake `StudyStatsRepository`
- [ ] 4.2 Run detekt and fix any lint issues
- [ ] 4.3 Run all tests (`./gradlew test`) and verify green
