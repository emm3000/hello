# Current Onboarding

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | First-run onboarding (welcome screen + starter deck) |
| Source of Truth | No |
| Read this when | You need to understand what a fresh install sees before Today |
| Last verified | 2026-08-28 |

## Summary

On a fresh install the app opens on a single welcome screen instead of Today, and the local database is pre-populated with a small starter deck so the user never lands on an empty library. Both halves are independent: the welcome screen is gated by the `hasSeenWelcome` flag, the starter deck by its own `hasSeededStarterDeck` flag. Both flags live in `DataStore`, so the flow runs once per install and survives process death.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/onboarding/OnboardingRoute.kt` (route + `OnboardingDestination`)
- `app/src/main/kotlin/com/emm/hello/newfeatures/onboarding/OnboardingScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/onboarding/OnboardingViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/onboarding/OnboardingUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/onboarding/OnboardingUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/onboarding/OnboardingUiEffect.kt`
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
if (hasSeenWelcome) TodayRoute else OnboardingRoute
```

`hasSeenWelcome` is backed by `DataStore.hasSeenOnboarding`.

## Starter deck seeding

`DefaultSeedDataInitializer.ensureSeeded()` is flag-gated in every branch:

1. `dataStore.hasSeededStarterDeck` already `true` → return, do nothing.
2. Flag unset but decks already exist → existing user; do **not** seed, just set the flag.
3. Flag unset and no decks → new install; create the deck plus the starter cards, then set the flag.

Details:

- The deck name comes from the app layer (`R.string.onboarding_seed_deck_name`, "First words"), injected as a `deckName` string in `NewModule`, so `:data` stays free of Android resource lookups.
- Cards are inserted through `FlashcardRepository.create` — the same path the UI uses — so they land in FSRS `NEW` state via the existing `ReviewProjection` defaults. Each card then gets one `main` example through `FlashcardRepository.upsertExamples`.
- The starter cards are English front / Spanish back with an English gloss, IPA, part of speech and an example sentence with its Spanish translation. They are deliberately not localized: the product targets Spanish speakers learning English.

## Screen

One static page, no pager: a `displaySmall` headline (`onboarding_headline`, "Save the words you meet."), a `bodyLarge` body in `inkMuted` (`onboarding_body`, "We'll make them stick.") and a full-width primary `HButton` (`onboarding_cta_start`, "Start").

## State

`OnboardingUiState` is a `data object` with no fields; the screen has nothing to remember.

## Intents

`OnboardingUiIntent`:

- `StartClicked` — the only CTA; calls `onboardingState.markWelcomeSeen()` then emits `NavigateToToday`
- `BackPressed` — system back; emits `CloseOnboarding`

## Effects

`OnboardingUiEffect`:

- `NavigateToToday` — `navigator.replaceAll(TodayRoute)`, so onboarding cannot be reached again with back
- `CloseOnboarding` — `navigator.goBack()`

`OnboardingDestination` installs a `BackHandler` that forwards system back to `BackPressed`.
