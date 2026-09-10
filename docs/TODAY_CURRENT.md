# Current Today

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Today` flow (session-first home) |
| Source of Truth | No |
| Read this when | You need to understand what the home screen shows and what it does not |
| Last verified | 2026-09-10 |

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
  streak, cards due this week, `nextDue: NextDueBatch?`, and
  `heldBackNewCards`. `cardsDueToday` is due reviews plus the new cards still
  allowed today (`NewCardBudget`, 10 per local calendar day), so it matches
  the session `GetStudySessionUseCase` will actually build.
  `heldBackNewCards` is the other side of that budget: never-reviewed cards
  that exist and are ready but were not admitted today. Both come from one
  budget computed once in `GetDashboardStatsUseCase`, so they can never
  disagree

Eight values are computed from `stats`, not stored:

- `cardsDueToday`
- `cardsStudiedToday`
- `hasSessionReady` — `cardsDueToday > 0`
- `nextDue` — non-null only when nothing is due today
- `moreNewCards` — `min(heldBackNewCards, EXTRA_NEW_CARDS_PER_REQUEST)`; the
  number the "Study N more" CTA promises and the number the session it opens
  will deliver. `0` while loading and whenever nothing is held back
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
- `StudyMoreClicked` — emits
  `NavigateToStudy(StudyRoute.ALL_DUE_DECKS, EXTRA_NEW_CARDS_PER_REQUEST)`.
  It is the only way into a session when the daily cap is spent and nothing is
  due, so the CTA that fires it is what keeps Today from being a dead end.

Capture, Library, Settings and Suggest are not intents: `TodayDestination`
navigates to `CaptureRoute`, `LibraryRoute`, `SettingsRoute` and
`SuggestRoute` directly from the screen callbacks, without touching the
ViewModel.

## Effects

`TodayUiEffect`:

- `NavigateToStudy(deckId, extraNewCards = 0)` — collected in
  `TodayDestination`, navigates to `StudyRoute(deckId, extraNewCards)`. The
  extra travels in the route key, so `StudyViewModel` applies it to its very
  first load instead of making the user press a second button on arrival.

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
  - Nothing due — "Nothing due" in `displaySmall`, then `nothingDueCopy`.
    With `moreNewCards > 0` that is the `today_new_waiting` plural
    ("10 new words are waiting"); otherwise it falls through to `nextDueCopy`:
    "N words later today" / "N words tomorrow" / "N words in D days", or
    "Nothing scheduled yet" when `nextDue` is null. A truly empty library
    lands here too: no separate empty state exists.
- **Actions, session ready** — a full-width `Primary` "Start" (`onStudy`),
  then one row with two `Secondary` half-width buttons: "Add a word" (`Add`
  icon, `onCapture`) and "Library" (`List` icon, `onLibrary`).
- **Actions, nothing due** — three full-width stacked buttons: `Primary`
  "Get new words" (`AutoAwesome` icon, navigates to `SuggestRoute`), then
  `Secondary` "Add a word" (`onCapture`), then `Text` "Library" (`onLibrary`).
- **Actions, nothing due but new cards held back** (`moreNewCards > 0`) — a
  `Primary` `study_more_cta` ("Study N more", `onStudyMore`) is inserted
  first and "Get new words" drops to `Secondary`, keeping its icon. "Add a
  word" and "Library" are unchanged. The accent stays on exactly one button,
  as `.claude/rules/ui-components.md` requires.

There is no metrics section and no FAB on this screen. `docs/DESIGN_BRIEF.md`
rejects a large metric as the hero, so the count lives on the stack as
supporting context and never as the tappable element.

## Persistence

Read-only. All numbers come from `GetDashboardStatsUseCase` over `HelloDb`.
