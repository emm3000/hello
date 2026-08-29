# Current Today

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Today` flow (session-first home) |
| Source of Truth | No |
| Read this when | You need to understand what the home screen shows and what it does not |
| Last verified | 2026-08-28 |

## Summary

`Today` is the app's start destination. It answers one question — is there a
session to run right now — and enters it in one tap. It holds no list, no
search and no filter: browsing cards is `Library` (`LIBRARY_CURRENT.md`)
and managing decks is Settings → Decks (`DECK_CURRENT.md`).

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/today/TodayRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/today/TodayScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/today/TodayViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/today/TodayUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/today/TodayUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/today/TodayUiEffect.kt`

The progress ring is `core/ui/HRing`, shared, not a feature-local file.

## State

`TodayUiState` holds two fields:

- `isLoading` — true until the first `GetDashboardStatsUseCase` result arrives
- `stats: DashboardStats?` — cards studied today, cards due today, current
  streak, cards due this week, and `nextDue: NextDueBatch?`

Seven values are computed from `stats`, not stored:

- `cardsDueToday`
- `cardsStudiedToday`
- `hasSessionReady` — `cardsDueToday > 0`
- `nextDue` — non-null only when nothing is due today
- `estimatedSessionMinutes` — `cardsDueToday × 15 s`, rounded up, floor 1 min;
  `0` when nothing is due
- `ringProgress` — `cardsStudiedToday / (cardsStudiedToday + cardsDueToday)`,
  clamped to `0f..1f`; `0f` when both are zero
- `dayNumber` — `currentStreak` (floor 1) once something was studied today,
  otherwise `currentStreak + 1`

## Intents

- `ScreenVisible` — loads stats and clears `isLoading`.
- `StudyClicked` — emits `NavigateToStudy(StudyRoute.ALL_DUE_DECKS)`. The CTA
  keys off `stats.cardsDueToday` across every deck, so the session must study
  all of them; `StudyViewModel` resolves the sentinel to the all-decks session.

Capture, Library, Settings and Suggest are not intents: `TodayDestination`
navigates to `CaptureRoute`, `LibraryRoute`, `SettingsRoute` and
`SuggestRoute` directly from the screen callbacks, without touching the
ViewModel.

## Effects

`TodayUiEffect`:

- `NavigateToStudy(deckId)` — collected in `TodayDestination`, navigates to
  `StudyRoute(deckId)`.

## Layout

A top bar, a centered card stack that fills the remaining height, then the
action column pinned to the bottom. Nothing renders in the stack or the
actions while `isLoading`.

- **Top bar** — the `today_label` ("Today") uppercased in `metadata` and
  `inkMuted`, a settings `HIconButton`, and — once loaded — an `HRing` fed by
  `ringProgress` (content description "Today's progress, N percent") followed
  by `today_day_number` ("Day N") in `titleSmall`.
- **Due stack** — three rotated `cardHues` panels, max width 300 dp, aspect
  ratio 300:220. The front panel carries the copy bottom-left:
  - Session ready — the `today_word_count` plural ("8 words") in
    `displaySmall`, then `today_estimate` ("about 2 min") in muted.
  - Nothing due — "Nothing due" in `displaySmall`, then `nextDueCopy`:
    "N words later today" / "N words tomorrow" / "N words in D days", or
    "Nothing scheduled yet" when `nextDue` is null. A truly empty library
    lands here too: no separate empty state exists.
- **Actions, session ready** — a full-width `Primary` "Start" (`onStudy`),
  then one row with two `Secondary` half-width buttons: "Add a word" (`Add`
  icon, `onCapture`) and "Library" (`List` icon, `onLibrary`).
- **Actions, nothing due** — three full-width stacked buttons: `Primary`
  "Get new words" (`AutoAwesome` icon, navigates to `SuggestRoute`), then
  `Secondary` "Add a word" (`onCapture`), then `Text` "Library" (`onLibrary`).

There is no metrics section and no FAB on this screen. `docs/DESIGN_BRIEF.md`
rejects a large metric as the hero, so the count lives on the stack as
supporting context and never as the tappable element.

## Persistence

Read-only. All numbers come from `GetDashboardStatsUseCase` over `HelloDb`.
