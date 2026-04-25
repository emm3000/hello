# Project Guidelines

## Code Style

- Kotlin code style is official (`kotlin.code.style=official`).
- Keep module boundaries strict: `app -> domain`, `app -> data`, `data -> domain`.
- Follow existing feature naming in `app/src/main/kotlin/com/emm/hello/newfeatures/`: `*ViewModel`, `*UiState`, `*UiIntent`, `*UiEffect`, `*Route`.
- Preserve MVI flow: UI sends a single `onIntent(intent)` entry point to each ViewModel.

## Architecture

- This is a local-first Android app. SQLDelight (`HelloDb`) is the source of truth for UI reads and writes.
- Domain module is pure Kotlin (contracts + use cases with a single `invoke`).
- Data module implements repositories, SQLDelight queries, and sync orchestration.
- Remote sync is Supabase RPC-only (`sync_bootstrap_anonymous`, `sync_push`, `sync_pull`, `sync_ack`, pairing RPCs). Avoid direct table CRUD assumptions.
- Sync flow and invariants are documented in `LOCAL_FIRST.md`.

## Build And Test

Run these from workspace root:

```bash
./gradlew assemble
./gradlew build
./gradlew test
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew detekt
./gradlew lint
./gradlew lintFix
```

Useful targeted tests:

```bash
./gradlew :app:testDebugUnitTest --tests "com.emm.hello.newfeatures.deck.NewDeckViewModelTest"
./gradlew :domain:test --tests "com.emm.domain.deck.CreateDeckUseCaseTest"
```

## Conventions

- Local mutations should remain local-first: persist locally (including outbox metadata) before async sync.
- Keep sync logic idempotent and cursor-driven. Respect `OperationLog` states (`Pending`, `Acked`, `Failed`, `Dead`) and `AppliedRemoteOperation` deduplication.
- For ViewModel tests, use `MainDispatcherRule` and Turbine when asserting Flow/effects.
- Detekt is build-breaking (`maxIssues: 0`), so code changes should be detekt-clean.

## Pitfalls

- SQLDelight schema/query changes require regeneration via Gradle build tasks.
- `connectedDebugAndroidTest` requires a connected emulator/device.
- Gradle is tuned for higher memory (`org.gradle.jvmargs=-Xmx4048m`); low-memory environments may fail large builds.

## Project Docs

Link to existing docs instead of duplicating details:

- `CLAUDE.md` for quick command and architecture summary.
- `ARCHITECTURE.md` for module/layer design and runtime wiring.
- `LOCAL_FIRST.md` for sync lifecycle, invariants, and RPC contracts.
- `docs/PRODUCTION_READINESS_SYNC.md` for production sync readiness.
- `docs/MANUAL_TESTING_PLAN.md` for manual validation coverage.
- `docs/SYNC_ACTION_PLAN.md` and `docs/SYNC_ANALYSIS.md` for sync roadmap and analysis.
- `docs/flashcard-refactor/` for flashcard-generation contracts and implementation phases.
