# AGENTS.md

## Verify Like CI
- For non-trivial changes, match CI's sequence: `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, `./gradlew detekt`.
- `connectedDebugAndroidTest` requires a connected emulator/device; do not treat it as the default verification path.
- Run focused tests when possible: `./gradlew :app:testDebugUnitTest --tests "com.emm.hello.newfeatures.deck.NewDeckViewModelTest"` and `./gradlew :domain:test --tests "com.emm.domain.deck.CreateDeckUseCaseTest"`.

## Module Boundaries
- This is a 3-module Gradle repo: `:app`, `:data`, `:domain` from `settings.gradle.kts`.
- Keep dependency direction strict: `app -> data`, `app -> domain`, `data -> domain`. `domain` is JVM-only and should stay free of Android/DB code.
- `:data` owns SQLDelight, Supabase sync, and Firebase AI integration. `:app` owns Compose UI, ViewModels, Koin wiring, and WorkManager startup.

## Real Startup Path
- App startup is `App` -> Koin modules `repositoryModule`, `newModule`, `networkModule` -> `AppStartupCoordinator.start()`.
- Sync infrastructure is started from `app/src/main/kotlin/com/emm/hello/startup/AppStartupCoordinator.kt`, where `EnsureLinkedIdentityUseCase`, `Sync.initialize(appContext)`, and `PendingOperationsSyncScheduler.start()` run. If you change initialization, check this flow first.

## Local-First Rules
- SQLDelight `HelloDb` is the source of truth for UI reads/writes. New mutations should persist local state and `OperationLog` together before any network sync.
- Remote sync is RPC-only. Do not assume direct Supabase table CRUD; current contracts are the `sync_*` and pairing RPCs documented in `LOCAL_FIRST.md`.
- Keep remote apply logic idempotent: `AppliedRemoteOperation` and `SyncCheckpoint` are part of the core sync contract, not optional implementation details.

## UI / Feature Conventions
- Feature ViewModels follow MVI: `UiState`, `UiIntent`, `UiEffect`, with a single `onIntent(intent)` entrypoint.
- Feature wiring lives under `app/src/main/kotlin/com/emm/hello/newfeatures/`; preserve existing naming like `*ViewModel`, `*Route`, `*UiState`.

## Build / Config Gotchas
- Toolchain is Java 17 with AGP `9.2.0` and Kotlin `2.3.21`.
- Gradle expects `app/google-services.json`; CI creates a dummy file when secrets are unavailable. If local Gradle tasks fail on Firebase config, check for that file first.
- `:data` injects `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `xmm` from `local.properties` first, then `keystore.properties`. Missing values become empty strings, which may still compile but break runtime behavior.
- App signing also reads `keystore.properties`; debug and staging builds still reference the `config` signing config.
- SQLDelight lives in `:data` (`HelloDb`). After changing `.sq` schema/query files, rerun a Gradle build/test task so generated sources refresh.
- Detekt is build-breaking and configured per module; baselines live at each module root as `detekt-baseline.xml`.

## Useful Source Docs
- `LOCAL_FIRST.md`: actual sync lifecycle, RPC contracts, and invariants.
- `ARCHITECTURE.md`: current layer responsibilities and startup wiring summary.
- `.github/copilot-instructions.md`: short project conventions already aligned with the current repo.
