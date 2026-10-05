# Current Product Analytics

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | Product events logged to Firebase Analytics (release) or logcat (debug) |
| Source of Truth | No |
| Read this when | You need to know which usage events the app records, with which parameters, and how to watch them |
| Last verified | 2026-10-05 |

## Summary

The app records a small, fixed set of product events so a test period yields usage data. Every event goes through the `ProductAnalytics` port in `:app`. Release builds send events to Firebase Analytics; debug builds only write them to logcat and have Analytics collection disabled. Parameters are counts, modes, limits, curated deck ids and failure causes. No event carries a word, a meaning, a translation or any other text the user types.

## Key files

- `app/src/main/kotlin/com/emm/hello/analytics/ProductAnalytics.kt` (port, `track(event)`)
- `app/src/main/kotlin/com/emm/hello/analytics/ProductEvent.kt` (sealed events plus `CaptureMode`, `StudyScope`, `ExtraNewCardsSource`)
- `app/src/main/kotlin/com/emm/hello/analytics/AnalyticsPayload.kt` (`ProductEvent.toPayload()`: event name and `String`/`Long` params)
- `app/src/main/kotlin/com/emm/hello/analytics/FirebaseProductAnalytics.kt` (release: `FirebaseAnalytics.logEvent`)
- `app/src/main/kotlin/com/emm/hello/analytics/LogcatProductAnalytics.kt` (debug: `Log.i("ProductAnalytics", ...)`)
- `app/src/main/kotlin/com/emm/hello/di/RepositoryModule.kt` (binding chosen by `BuildConfig.DEBUG`)
- `app/src/main/AndroidManifest.xml` and `app/build.gradle.kts` (`firebase_analytics_collection_enabled` placeholder)

## Events

| Event | Params | Origin |
|---|---|---|
| `onboarding_completed` | none | `OnboardingViewModel.startLearning()`, right after the welcome flag is saved |
| `word_captured` | `mode`: `"ai"` or `"manual"` | `CaptureViewModel.saveForEnrichment()` / `saveWrittenCard()`, only after the card is saved |
| `study_session_completed` | `reviewed`, `knew`, `forgot` (Long); `scope`: `"all"` or `"deck"`; `extra`: `"true"` or `"false"` | `StudyViewModel.showNextCard()`, once when `sessionFinished` flips to true |
| `daily_new_card_limit_selected` | `limit` (Long, cards per day) | `SettingsViewModel.selectDailyNewCardLimit()`, after the limit is stored |
| `extra_new_cards_requested` | `source`: `"today"` or `"session_end"` | `TodayViewModel` and `StudyViewModel` on `StudyMoreClicked` |
| `curated_deck_installed` | `deck_id` (curated deck id, e.g. `spanish-traps`) | `StoreViewModel.install()`, only after the install succeeds |
| `enrichment_failed` | `cause`: `"technical"`, `"app_check_rejected"`, `"word_problem"` or `"credits_exhausted"` | `FlashcardEnrichmentWorker.markFailed()`, when a card is marked failed |

Notes:

- `scope` is `"all"` when the session was opened with `StudyRoute.ALL_DUE_DECKS`, `"deck"` otherwise.
- `extra` is `"true"` when the finished session was loaded with extra new cards, either from the route (Today's "study more") or from the session-end "study more". It is a string so it reads as a dimension in reports.
- `cause` maps from the domain `EnrichmentFailureCause`; every `WordProblem` sub-reason reports as `"word_problem"`.

## Debug vs release

| | Debug | Release |
|---|---|---|
| `ProductAnalytics` binding | `LogcatProductAnalytics` | `FirebaseProductAnalytics` |
| `firebase_analytics_collection_enabled` | `false` | `true` |
| Product events sent to Firebase | No | Yes |
| Automatic Firebase events sent | No | Yes |

## Watching events on a debug build

```
adb logcat -s ProductAnalytics
```

Each line reads `<event name> <params map>`, for example `word_captured {mode=ai}`.

## Tests

- `app/src/test/java/com/emm/hello/analytics/AnalyticsPayloadTest.kt` covers the name, keys and value types of every event.
- The Onboarding, Capture, Study, Today, Settings and Store ViewModel tests check that the success path tracks its event, using `FakeProductAnalytics`. The worker's `enrichment_failed` call site has no unit test; the cause mapping is covered by the payload test.
