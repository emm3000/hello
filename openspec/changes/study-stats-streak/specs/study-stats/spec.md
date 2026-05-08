# Study Stats Specification

## Purpose

Expose study metrics (cards studied today, cards due today, cards due this week, current streak) for display on the dashboard, computed from existing `ReviewEvent` and `ReviewProjection` data.

## Requirements

### Requirement: Cards Studied Today

The system MUST return the count of unique flashcards reviewed on the current calendar day, based on `ReviewEvent.reviewedAt` resolved to `ZoneId.systemDefault()`.

#### Scenario: Cards reviewed today

- GIVEN 3 flashcards were reviewed at timestamps within the current local day
- WHEN `GetCardsStudiedToday` is invoked
- THEN the result is `3`

#### Scenario: No reviews today

- GIVEN no `ReviewEvent` exists with `reviewedAt` in the current local day
- WHEN `GetCardsStudiedToday` is invoked
- THEN the result is `0`

#### Scenario: Multiple reviews of same card today

- GIVEN flashcard A was reviewed twice today, flashcard B once
- WHEN `GetCardsStudiedToday` is invoked
- THEN the result is `2` (counts unique flashcards, not events)

### Requirement: Cards Due Today

The system MUST return the count of flashcards whose `ReviewProjection.nextReviewAt` falls on or before the end of the current local day.

#### Scenario: Cards due today

- GIVEN 5 flashcards have `nextReviewAt` ≤ end of current local day
- WHEN `GetCardsDueToday` is invoked
- THEN the result is `5`

#### Scenario: No cards due today

- GIVEN all flashcards have `nextReviewAt` after the current local day
- WHEN `GetCardsDueToday` is invoked
- THEN the result is `0`

### Requirement: Cards Due This Week

The system MUST return the count of flashcards whose `ReviewProjection.nextReviewAt` falls within the current local week (Monday 00:00 through Sunday 23:59:59).

#### Scenario: Cards due within the week

- GIVEN 8 flashcards have `nextReviewAt` between Monday 00:00 and Sunday 23:59:59 of the current week
- WHEN `GetCardsDueThisWeek` is invoked
- THEN the result is `8`

#### Scenario: Week boundary excludes next Monday

- GIVEN a flashcard has `nextReviewAt` at Monday 00:00 of the following week
- WHEN `GetCardsDueThisWeek` is invoked
- THEN the result does NOT include that flashcard

### Requirement: Current Streak

The system MUST compute the number of consecutive calendar days ending on the current day (or the most recent day with a review) that contain at least one `ReviewEvent`, using `ZoneId.systemDefault()` for day resolution.

#### Scenario: Streak of 5 consecutive days including today

- GIVEN `ReviewEvent` entries exist on each of the last 5 consecutive local days (including today)
- WHEN `GetCurrentStreak` is invoked
- THEN the result is `5`

#### Scenario: Streak broken by a missed day

- GIVEN reviews exist on today, yesterday, and 3 days ago — but NOT 2 days ago
- WHEN `GetCurrentStreak` is invoked
- THEN the result is `2` (today + yesterday, broken at the gap)

#### Scenario: No reviews ever

- GIVEN no `ReviewEvent` exists in the database
- WHEN `GetCurrentStreak` is invoked
- THEN the result is `0`

#### Scenario: First review today

- GIVEN exactly one `ReviewEvent` exists with `reviewedAt` in the current local day
- WHEN `GetCurrentStreak` is invoked
- THEN the result is `1`

#### Scenario: Streak from past — today has no review yet

- GIVEN reviews exist on yesterday and the 2 days before it, but NOT today
- WHEN `GetCurrentStreak` is invoked
- THEN the result is `3` (counts backward from the most recent day with a review)

### Requirement: Dashboard Display

The dashboard MUST display 4 StatCards in a 2×2 grid between the `SessionSummaryBanner` and the deck list section header, showing: streak, studied today, due today, and due this week.

#### Scenario: Stats loaded and displayed

- GIVEN the dashboard is opened and all 4 use cases return values
- THEN 4 StatCards render with their respective values and labels

#### Scenario: Stats loading state

- GIVEN the dashboard is opened and stats are being fetched
- THEN StatCards show a loading indicator or placeholder until values arrive

#### Scenario: Stats refresh after study session

- GIVEN the user completes a study session and returns to the dashboard
- THEN the 4 StatCards reflect updated values

### Requirement: Repository Interface

The domain module MUST define `StudyStatsRepository` with 4 suspending methods, each returning an `Int`. The data module MUST provide `DefaultStudyStatsRepository` implementing this interface using SQLDelight queries.

#### Scenario: All 4 methods return non-negative integers

- GIVEN the repository is invoked under any data state
- WHEN any of the 4 methods is called
- THEN the result is an `Int ≥ 0` (never null, never negative)

### Requirement: Timezone Consistency

All date-range computations MUST use `ZoneId.systemDefault()` consistently. The `Clock` dependency MUST be injectable for testability.

#### Scenario: Midnight boundary

- GIVEN a review occurred at 23:59 local time and another at 00:01 the next day
- WHEN `GetCardsStudiedToday` is invoked at 00:05
- THEN only the 00:01 review is counted for "today"
