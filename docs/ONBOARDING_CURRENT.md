# Current Onboarding

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | First-run onboarding (welcome carousel + starter deck) |
| Source of Truth | No |
| Read this when | You need to understand what a fresh install sees before the Dashboard |

## Summary

On a fresh install the app opens on a three-page welcome carousel instead of the Dashboard, and the local database is pre-populated with a small starter deck so the user never lands on an empty library. Both halves are independent: the carousel is gated by the `hasSeenWelcome` flag, the starter deck by its own `hasSeededStarterDeck` flag. Both flags live in `DataStore`, so the flow runs once per install and survives process death.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/onboarding/OnboardingRoute.kt` (route + `OnboardingDestination`)
- `app/src/main/kotlin/com/emm/hello/newfeatures/onboarding/OnboardingScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/onboarding/OnboardingViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/onboarding/OnboardingUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/onboarding/OnboardingUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/onboarding/OnboardingUiEffect.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/onboarding/OnboardingPage.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/onboarding/OnboardingIllustration.kt`
- `app/src/main/kotlin/com/emm/hello/startup/AppStartupCoordinator.kt` (gate + seeding)
- `app/src/main/kotlin/com/emm/hello/newfeatures/NewRoot.kt` (start destination)

## :domain / :data dependencies

- `com.emm.domain.onboarding.OnboardingStateRepository` → `com.emm.data.onboarding.DataStoreOnboardingStateRepository`
- `com.emm.domain.seed.SeedDataInitializer` → `com.emm.data.seed.DefaultSeedDataInitializer`

## Startup gate

`AppStartupCoordinator.start()`:

1. `LocalIdentityInitializer.ensureReady()`
2. `SeedDataInitializer.ensureSeeded()`
3. on success emits `AppStartupState.Ready(hasSeenWelcome = onboardingStateRepository.hasSeenWelcome())`
4. on failure emits `AppStartupState.Error`

`NewRoot` renders the loading/error screens for the first two states. On `Ready` it calls `AppNavigation(hasSeenWelcome)`, which picks the start key:

```
if (hasSeenWelcome) DashboardRoute else OnboardingRoute
```

`hasSeenWelcome` is backed by `DataStore.hasSeenOnboarding`.

## Starter deck seeding

`DefaultSeedDataInitializer.ensureSeeded()` is flag-gated in every branch:

1. `dataStore.hasSeededStarterDeck` already `true` → return, do nothing.
2. Flag unset but decks already exist → existing user; do **not** seed, just set the flag.
3. Flag unset and no decks → new install; create the deck plus the starter cards, then set the flag.

Details:

- The deck name comes from the app layer (`R.string.onboarding_seed_deck_name`, "Primeras palabras"), injected as a `deckName` string in `NewModule`, so `:data` stays free of Android resource lookups.
- Cards are inserted through `FlashcardRepository.create` — the same path the UI uses — so they land in FSRS `NEW` state via the existing `ReviewProjection` defaults.
- The starter cards are English front / Spanish back with an English gloss, IPA and part of speech. They are deliberately not localized: the product targets Spanish speakers learning English.

## State

`OnboardingUiState`:

- `pages: List<OnboardingPage>` — static, always the full ordered list of `OnboardingPage.entries`
- `currentPage: Int` — zero-based index of the visible page
- `isLastPage: Boolean` — derived; `currentPage == pages.lastIndex`

`OnboardingPage` is an enum of three pages, each carrying a title string res, a body string res and an `OnboardingIllustration` (drawable key):

| Page | Title | Illustration |
|---|---|---|
| `Decks` | `onboarding_decks_title` ("Tus mazos") | `onboarding_decks` |
| `SpacedRepetition` | `onboarding_srs_title` ("Repaso espaciado") | `onboarding_spaced` |
| `Grading` | `onboarding_grading_title` ("Tú calificas") | `onboarding_grading` |

## Intents

`OnboardingUiIntent`:

- `PageChanged(page)` — pager swipe settled on a new index; updates `currentPage`
- `NextClicked` — primary CTA; finishes if `isLastPage`, otherwise emits `ScrollToPage(currentPage + 1)`
- `SkipClicked` — finishes from any page
- `FinishClicked` — explicit "Empezar" CTA on the last page; finishes
- `BackPressed` — system back; emits `ScrollToPage(currentPage - 1)` if not on the first page, otherwise `CloseOnboarding`

Finishing always means the same two steps: `onboardingState.markWelcomeSeen()` then `NavigateToDashboard`.

## Effects

`OnboardingUiEffect`:

- `ScrollToPage(page)` — `OnboardingDestination` calls `pagerState.animateScrollToPage(page)`
- `NavigateToDashboard` — `navigator.replaceAll(DashboardRoute)`, so onboarding cannot be reached again with back
- `CloseOnboarding` — `navigator.goBack()`

`OnboardingDestination` also installs a `BackHandler` that forwards system back to `BackPressed`, letting the viewmodel decide between paging back and closing.

## Related one-time hint

`OnboardingStateRepository` also stores the Study screen's grade hint (`hasSeenGradeHint` / `markGradeHintSeen`), consumed by `StudyViewModel` to show `isGradeHintVisible` once. It shares the repository but is not part of this flow — see `docs/STUDY_CURRENT.md`.
