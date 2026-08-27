# Current Hoy

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Hoy` flow (session-first home) |
| Source of Truth | No |
| Read this when | You need to understand what the home screen shows and what it does not |
| Last verified | 2026-08-27 |

## Summary

`Hoy` is the app's start destination. It answers one question — is there a
session to run right now — and enters it in one tap. It holds no list, no
search and no filter: browsing cards is `Biblioteca` (`LIBRARY_CURRENT.md`)
and managing decks is Settings → Mazos (`DECK_CURRENT.md`).

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/hoy/HoyRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/hoy/HoyScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/hoy/HoyStatsSection.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/hoy/HoyViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/hoy/HoyUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/hoy/HoyUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/hoy/HoyUiEffect.kt`

## State

`HoyUiState` holds two fields:

- `isLoading` — true until the first `GetDashboardStatsUseCase` result arrives
- `stats: DashboardStats?` — cards studied today, cards due today, current
  streak, cards due this week, and `nextDue: NextDueBatch?`

Four values are computed from `stats`, not stored:

- `cardsDueToday`
- `hasSessionReady` — `cardsDueToday > 0`
- `nextDue` — non-null only when nothing is due today
- `estimatedSessionMinutes` — `cardsDueToday × 15 s`, rounded up, floor 1 min

## Intents

- `ScreenVisible` — loads stats and clears `isLoading`.
- `StudyClicked` — emits `NavigateToStudy(StudyRoute.ALL_DUE_DECKS)`. The CTA
  keys off `stats.cardsDueToday` across every deck, so the session must study
  all of them; `StudyViewModel` resolves the sentinel to the all-decks session.

## Effects

`HoyUiEffect`:

- `NavigateToStudy(deckId)` — collected in `HoyDestination`, navigates to
  `StudyRoute(deckId)`.

## Layout

A wordmark row (`Hello.` plus a library icon and a settings icon), then one of
two heroes, then the metrics.

- **Session ready** — one full-width `HButtonSize.Xl` accent pill. The label
  is the action; the count and the estimate sit inside it in mono
  (`8 TARJETAS · ~2 MIN`). `docs/DESIGN_BRIEF.md` rejects a large metric as the
  hero, so the number is supporting context and never the tappable element.
- **Nothing due** — "Nada que repasar hoy" in muted, a mono line naming the
  next batch (`nextDueLabel` renders later today / tomorrow / in N days, or
  "NADA PROGRAMADO" when `nextDue` is null), then a capture CTA. A truly empty
  library lands here too: no separate empty state exists.

`HoyStatsSection` renders under a "Tu progreso" section label, below the
fold, where metrics reward after a session instead of competing with the CTA.

An `HFab` opens `Capturar` from anywhere on the screen.

## Persistence

Read-only. All numbers come from `GetDashboardStatsUseCase` over `HelloDb`.
