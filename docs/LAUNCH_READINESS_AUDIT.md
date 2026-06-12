# Launch Readiness Audit

| Field | Value |
|---|---|
| Status | Active |
| Role | Full pre-launch audit + atomic phased plan |
| Source of Truth | Yes (until everything closes) |
| Read this when | You're working on any pre-launch hardening task |
| Last verified against code | 2026-06-11 |
| Sprint 1 progress | 6/8 done (T1, T2, T4, T5, T6, T8) · T3 discarded · T7 in progress (draft published, missing URL + manifest + Data Safety form) |
| Sprint 2 progress | S2-T1 done (retry + timeout + Crashlytics logging) · S2-T2, T3, T5, T6 open · S2-T4 done (SCHEDULER.md) |

## TL;DR

**No need to redo the project.** The base is solid and honest about its local-first contract. There are ~10 concrete fixes (mostly configuration + prompts + one model change) that block Play Store or degrade UX in production. I estimate **2-3 effective days of work**, organized in 3 sprints.

| Layer | Score | Status |
|---|---|---|
| Domain | 7.5/10 | Solid. Firm value objects, separated policies. Cracks in anemic `Flashcard` + undocumented SM-2 scheduler. |
| Data | 8/10 | Clean schema, consistent soft-delete, tested backup round-trip. 2 real hotfixes. |
| AI / Prompts | 6/10 | Good architecture, but model too lightweight + self-sealed quality checks + fragile error handling. Weakest layer. |
| Architecture | 7/10 | Boundaries respected, coherent DI, MVI without overengineering. Missing observability and release hardening. |

---

## 1. Per-layer analysis

### 1.1 Domain (`:domain`)

**Strengths**
- Value objects with normalization in constructor: `Expression`, `IntendedMeaningEs`, `DefinitionEn`, `FlashcardId`, `DeckId` (`@JvmInline`, trim, whitespace collapse).
- `FlashcardReview` with real invariants (`easeFactor ≥ 1.3`, non-negative).
- Separated, testable policies: `CoreFieldsPolicy`, `CardsPolicy`, `QualityChecksPolicy`, `TypeRequirementsPolicy` for `GeneratedLearningNote`; `ContextSentence`, `Disambiguation`, `InputTypeRules`, `WordCount` for `FlashcardGenerationInput`.
- Cohesive use cases (`CreateFlashcardUseCase`, `ScheduleFlashcardReviewUseCase`) without unnecessary orchestration.
- JVM-only domain respected, repositories as interfaces.

**Cracks**
- 🟡 `Flashcard` is anemic: `word/meaning/translation` are raw `String`s without invariants (`domain/src/main/kotlin/com/emm/domain/flashcard/Flashcard.kt`). The value objects already exist; they're just not used in the main aggregate.
- 🟡 `SpacedRepetitionScheduler` uses a simplified SM-2 with an undocumented custom formula (`domain/src/main/kotlin/com/emm/domain/study/SpacedRepetitionScheduler.kt`). The ease delta (`0.1 - qualityDistance * (0.08 + qualityDistance * 0.02)`) has no reference paper. For a study app, that's the heart.
- 🟢 `UpdateFlashcardUseCase` and `SoftDeleteFlashcardUseCase` are trivial forwarders wrapping the repo. They add no logic.
- 🟢 Inconsistent naming between policies: `FlashcardGeneration*Policy` vs `GeneratedLearningNote*Policy`. Not blocking, adds cognitive friction.

### 1.2 Data (`:data`)

**Strengths**
- Normalized schema with natural UUIDs; FKs with `ON DELETE CASCADE`; soft-delete (`deletedAt`) consistent across `Deck`, `Flashcard`, `FlashcardExample`, `Tag`.
- Indexes on `deletedAt` and composites (`deckId + deletedAt`); DESC index on `createdAt`.
- Explicit transactions in multi-table operations; soft-delete visibility tests (`SoftDeleteVisibilityQueryTest`).
- Backup round-trip validated (`ExportImportIntegrationTest`); idempotent import inside a transaction; JSON with `ignoreUnknownKeys = true` for forward-compat.
- `LocalDeviceIdentity` thread-safe (`INSERT OR IGNORE`, fixed singletonId).

**Cracks**
- 🔴 No `migrations/` folder under `data/src/main/sqldelight/`. If v1.0 ships and v1.1 changes schema, upgrade breaks. The baseline must be created BEFORE there are users.
- 🔴 `DeckTag` does not propagate soft-delete: `Tag` has `deletedAt` but `DeckTag` does not. `ON DELETE CASCADE` only fires on a hard delete. The export includes orphan `DeckTag`s.
- 🟡 No `schemaVersion` validation on import; a v2 backup imported in v1 can truncate the DB.
- 🟢 Static catalogs (`StaticCategories`, `CommunicativeIntent`) without i18n.

### 1.3 AI / Prompts (`data/.../flashcard/`)

**Strengths**
- Explicit role + principles in the main prompt (bilingual EN-learning assistant for native Spanish speakers).
- Clear decision policy: prioritized by level, frequency > reusability.
- Structured JSON schema with 7 quality checks + discrimination by nota_type.
- Partial regenerations (Field/Cloze/Example/StudyCard) reuse the existing note; they don't redo the work.
- Typed parser with separated DTOs (`data/.../flashcard/iadto/`).

**Critical cracks**
- 🔴 The `gemini-2.5-flash-lite` model is the weakest in the 2.5 family. Expect ~30% mediocre notes (literal translations, textbook examples, occasionally wrong IPA). Switch to `gemini-2.5-flash`.
- 🔴 No `responseSchema` in `generationConfig` (`app/.../di/RepositoryModule.kt:30-31`, only `responseMimeType = "application/json"`). The model can return enums with inconsistent casing; the parser blows up with a generic `IllegalArgumentException`.
- 🔴 Fragile error handling in `GeminiService`: if Gemini returns null/error, returns `""`, parser throws without original cause. No retry, no backoff, no explicit timeout, no log of the raw response.
- 🟡 Quality checks are **self-sealed**: the prompt asks the model to fill `passed: true/false` for its own outputs. `QualityChecksPolicy` only reads the model's decision. Useful bureaucracy but not validation.
- 🟡 Mixed language (es/en) in the same prompt. Spanish inputs interpolated into an English system prompt confuse the model.
- 🟢 No few-shot examples in regeneration prompts. `gemini-2.5-flash-lite` performs better with 1-2 examples.

### 1.4 Architecture and wiring

**Strengths**
- Module boundaries respected (`app -> data`, `app -> domain`, `data -> domain`). JVM-only domain verified.
- Coherent Koin: every domain `Repository` has an impl bound in data; every VM registered (including `AppStartupViewModel` in `NewModule.kt:167`).
- MVI base (`MviViewModel<S, I, E>`) does just enough. No middleware, no saga.
- Navigation 3 well integrated: `rememberNavBackStack`, correct decorators, consistent transitions.
- Domain tests well covered (~30 files).

**Cracks**
- 🔴 `App.kt` only starts Koin + `AppStartupCoordinator.start()`. Firebase Crashlytics and Analytics are in gradle but **never initialized**. Without these, launching = flying blind.
- 🔴 `app/proguard-rules.pro` and `data/proguard-rules.pro` are empty (just comments). On release with minify, Firebase AI / kotlinx-serialization / SQLDelight can break at runtime.
- 🟡 No timeout in startup: if `LocalIdentityInitializer.ensureReady()` hangs, infinite loading.
- 🟡 `POST_NOTIFICATIONS` permission in `AndroidManifest.xml` without notifications implemented. Play Console will ask.
- 🟡 ViewModels lack unit tests. Only domain is well covered.
- 🟢 No privacy policy URL in `AndroidManifest`. Required by Play Store (Data Safety section).

---

## 2. Atomic phased plan

Each task is independent, has an **affected file**, **acceptance criterion**, and **estimate**.
Mark as `[x]` when complete. Dependencies between tasks are explicit.

### Sprint 1 — Play Store blockers (goal: 1-2 days)

#### S1-T1: Initialize Crashlytics and Analytics in App
- **File:** `app/src/main/kotlin/com/emm/hello/App.kt`
- **Why:** without it there are no field signals in production.
- **What to do:** in `onCreate()` before `startKoin`, call `FirebaseApp.initializeApp(this)`, enable `FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true`, instantiate `FirebaseAnalytics.getInstance(this)`.
- **Criterion:** open the debug app, force a crash, see it in the Crashlytics console within 5 min. Verify `first_open` event in Analytics DebugView.
- **Estimate:** 30 min.
- **Status:** [x] — `App.kt` initializes `FirebaseApp`, enables Crashlytics, and instantiates Analytics before Koin (commit `8e8e5dd`). On-device manual validation pending.

#### S1-T2: R8/ProGuard rules for Firebase and serialization
- **Files:** `app/proguard-rules.pro`, `data/proguard-rules.pro`
- **Why:** a release build with minify can break Gemini response deserialization without a compile error.
- **What to do:** add keep rules for `com.google.firebase.**`, `kotlinx.serialization.**`, all project `@Serializable data class`es (DTOs in `data/.../flashcard/iadto/`, `BackupEnvelope`), SQLDelight runtime classes.
- **Criterion:** `./gradlew :app:assembleRelease` and run the APK on a real device. Create a flashcard via Gemini without deserialization error. Export/import backup without errors.
- **Estimate:** 1-2 h (includes iterating when something breaks at runtime).
- **Status:** [x] — keep rules added in `app/proguard-rules.pro` (Crashlytics deobfuscation, official kotlinx.serialization, defensive Firebase, project `@Serializable`s) and `data/consumer-rules.pro` (DTOs from `:data`, SQLDelight `HelloDb`). `data/proguard-rules.pro` stays as-is because `:data` does not minify. `assembleRelease` verified with R8 (commit `8e8e5dd`). Functional on-device validation pending.
- **Depends on:** S1-T1 (so crashes show in Crashlytics if something fails).

#### S1-T3: Switch Gemini model to `gemini-2.5-flash` ~~(discarded by user decision)~~
- **File:** `app/src/main/kotlin/com/emm/hello/di/RepositoryModule.kt:29`
- **Why:** flash-lite produces ~30% mediocre linguistic notes. Estimated extra cost: ~$40/day at 10k users × 5 cards/day.
- **What to do:** change `modelName = "gemini-2.5-flash-lite"` to `"gemini-2.5-flash"`.
- **Criterion:** generate 10 flashcards of varied words (high-frequency, phrasal verbs, idioms, latinate). Compare manually with previous outputs. Naturalness of examples and IPA should improve.
- **Estimate:** 15 min + 30 min of manual validation.
- **Status:** [~] — discarded: user decided to keep `gemini-2.5-flash-lite` for now. Re-evaluate after beta feedback if linguistic quality degrades the product.

#### S1-T4: Add explicit `responseSchema` in `generationConfig`
- **Files:** `data/src/main/kotlin/com/emm/data/flashcard/LearningNoteResponseSchema.kt` (new), `data/src/main/kotlin/com/emm/data/flashcard/GeminiService.kt`, `data/src/main/kotlin/com/emm/data/flashcard/DefaultFlashcardRepository.kt`, `app/src/main/kotlin/com/emm/hello/di/RepositoryModule.kt`.
- **Why:** without a schema, the model can return enums with inconsistent casing and the parser blows up with a generic error.
- **What to do:** declare `responseSchema` with the structure of `GeneratedLearningNoteResponseDto` (wrapper `{success, data, error}`) including `Schema.enumeration(...)` for `note_type`, `part_of_speech`, `register`, `level_band`, `domain`, `card_type`, `evaluation_mode`, `quality_checks.code`. Scope: only affects main generation; partial regenerations keep the generic model.
- **Criterion:** generate 20 flashcards; the parser should never throw `IllegalArgumentException` for casing/enum. Functional validation pending on device (requires release build + a batch of generations).
- **Estimate:** 2-3 h (defining the full schema is tedious but mechanical).
- **Status:** [x] — full schema in `LearningNoteResponseSchema` (note + study cards + quality checks + error envelope) with `optionalProperties` aligned to DTO defaults. `GeminiService` now exposes `processLearningNote(prompt)` that uses a dedicated `GenerativeModel` with `responseSchema`; partial regenerations keep using `process(prompt)` with the schema-less model (preserves different shapes). `DefaultFlashcardRepository.generateLearningNote` rerouted to the new method. `:data` tests + detekt green. On-device manual validation deferred to S2-T6.

#### S1-T5: SQLDelight baseline migrations
- **Files:** `data/build.gradle.kts`, `data/src/main/sqldelight/databases/1.db` (generated), `ARCHITECTURE.md`.
- **Why:** if v1.0 ships and v1.1 changes schema, upgrade breaks. It's free to do now.
- **What to do:** configure `schemaOutputDirectory` + `verifyMigrations = true` in the sqldelight block. Generate baseline with `./gradlew :data:generateDebugHelloDbSchema`. Document policy in `ARCHITECTURE.md`.
- **Criterion:** `./gradlew :data:verifySqlDelightMigration` passes. Document policy in `ARCHITECTURE.md`: "every schema change requires a corresponding `N.sqm`".
- **Estimate:** 1 h.
- **Status:** [x] — `schemaOutputDirectory.set(file("src/main/sqldelight/databases"))` + `verifyMigrations.set(true)` added to the `sqldelight` block in `data/build.gradle.kts`. Baseline `1.db` generated and committed under `data/src/main/sqldelight/databases/`. `verifySqlDelightMigration` passes. Migration policy documented in `ARCHITECTURE.md`. Decision: no empty `1.sqm` was created (would bump schema to v2 with no real changes); the baseline `.db` is enough for `verifyMigrations` to detect future divergences.

#### S1-T6: Soft-delete cascade in `DeckTag`
- **Files:** `data/src/main/sqldelight/com/emm/data/Export.sq`, `data/src/test/kotlin/com/emm/data/export/ExportImportIntegrationTest.kt`.
- **Why:** soft-deleted tags leave orphan `DeckTag`s in the export.
- **What to do:** filter `allDeckTagsPaged` with a JOIN to `Tag` and `Deck` requiring `deletedAt IS NULL` in both. Decision: do NOT add `deletedAt` to `DeckTag` (avoids migration and keeps `DeckTag` as a pure join table; filtering in the query is enough because the importer rewrites `DeckTag` from the envelope).
- **Criterion:** two new tests in `ExportImportIntegrationTest`: (1) soft-deleted tag with active DeckTag → import on clean DB leaves only 1 DeckTag (the active tag's). (2) soft-deleted deck with DeckTag → import on clean DB leaves 0 DeckTag.
- **Estimate:** 2 h.
- **Status:** [x] — `allDeckTagsPaged` now does `JOIN Tag JOIN Deck` with double `deletedAt IS NULL` filter. Tests `soft-deleted tag does not leak DeckTag rows into export` and `soft-deleted deck does not leak DeckTag rows into export` added and green. No schema changes → no `N.sqm` required.

#### S1-T7: Privacy policy + Data Safety
- **Files:** publish external policy (URL), add `meta-data` or reference in `app/src/main/AndroidManifest.xml`, complete Data Safety form in Play Console.
- **Why:** Play Store rejects apps that send user input to an LLM without declaring it.
- **What to do:** write a minimal policy covering: local deviceId, user input sent to Firebase AI / Gemini, Crashlytics, Analytics. Publish it (GitHub Pages, public Notion, etc.). In Play Console mark: "data collected: app activity, app info, device IDs", "shared with third parties: Google Firebase AI".
- **Criterion:** Play Console accepts the Data Safety form in pre-validation.
- **Estimate:** 2 h (writing + publishing + form).
- **Status:** [~] — policy draft in `docs/privacy-policy.md` (covers local data, Firebase AI/Gemini, Crashlytics, Analytics, permissions, retention, contact). Pending: (1) enable GitHub Pages on `main/docs` and get final URL, (2) add `<meta-data>` to `AndroidManifest.xml` with that URL, (3) complete Data Safety form in Play Console.

#### S1-T8: Clean up unused permissions
- **File:** `app/src/main/AndroidManifest.xml`
- **What to do:** check `POST_NOTIFICATIONS` — if no notifications are implemented, remove it. Check `RECORD_AUDIO` — confirm STT (`rememberSpeechToTextManager`) is still active in the NewCard wizard; if disabled, drop it.
- **Criterion:** the app requests only what it uses. Play Console doesn't flag unjustified permissions.
- **Estimate:** 30 min.
- **Status:** [x] — `POST_NOTIFICATIONS` removed from the manifest and the orphan `LaunchedEffect` in `DashboardRoute` deleted (no notifications were implemented at the time). `RECORD_AUDIO` kept (STT active in `NewCardInputStepScreen`). `READ/WRITE_EXTERNAL_STORAGE` with `maxSdkVersion=32` stay (legacy < Android 13 usage). Commit `8e8e5dd`. **Update (2026-06-11):** `POST_NOTIFICATIONS` was re-added intentionally in commit `29d11c1` when the daily-reminder notification feature (Sprint 1) shipped. The permission is now backed by actual notification code and is no longer a launch-blocker concern.

---

### Sprint 2 — Before inviting beta testers (goal: 1 day)

#### S2-T1: Retry + timeout + logging in `GeminiService`
- **File:** `data/src/main/kotlin/com/emm/data/flashcard/GeminiService.kt`
- **Why:** a network drop or timeout leaves the user without useful feedback.
- **What to do:** wrap `generateContent` with: explicit timeout (10-15 s), 3 retries with exponential backoff (1s, 2s, 4s), try-catch that captures `FirebaseException` and logs the raw response (truncated) to Crashlytics as non-fatal.
- **Criterion:** mock network down → user sees a clear "couldn't connect, retrying" and after ~7s "couldn't generate, try again later". Crashlytics receives a non-fatal with the original error stack.
- **Estimate:** 3 h.
- **Status:** [x] — `GeminiService` wraps `process` and `processLearningNote` with `withTimeout(15s)` + 4 total attempts (backoff 1s/2s/4s). `GeminiTelemetry` interface with `NoOp` default in `:data`; `CrashlyticsGeminiTelemetry` impl in `:app` (`setCustomKey` + `recordException` as non-fatal). `DefaultFlashcardRepository` also catches parse failures and reports `recordParseFailure` with raw response truncated to 4 000 chars. Wiring in `RepositoryModule`. Tests: `GeminiServiceRetryTest` covers success without retry, success after retry, and non-fatal report when all attempts fail. Error-message UI and real-device verification deferred to S2-T6.
- **Depends on:** S1-T1.

#### S2-T2: Deterministic quality checks in Kotlin
- **Files:** `domain/src/main/kotlin/com/emm/domain/generation/GeneratedLearningNoteQualityChecksPolicy.kt`, `data/src/main/kotlin/com/emm/data/flashcard/Prompt.kt`
- **Why:** current checks are self-sealed by the model. Replacing 2-3 with real validators adds value without rewriting everything.
- **What to do:** pick 2-3 checks with deterministic criteria (e.g. `required_fields_present` is already checkable; `single_meaning` can be verified with a regex over `cards`; `natural_example` with a textbook-ism wordlist). Remove them from the prompt and validate in Kotlin. Keep the rest of the prompt as informative hint.
- **Criterion:** a note with an empty field that the model marked `passed: true` now fails validation.
- **Estimate:** 4 h.
- **Status:** [ ]

#### S2-T3: Normalize inputs to English in the prompt builder
- **File:** `data/src/main/kotlin/com/emm/data/flashcard/Prompt.kt`
- **Why:** mixed language in the system prompt produces inconsistent outputs.
- **What to do:** in each builder, map Spanish inputs (e.g. `communicativeIntentLabel`) to their English version before interpolation. If the catalog is in Spanish, add an `englishLabel` field or inline translation table.
- **Criterion:** final prompts sent to Gemini are 100% in English (verifiable by logging the full prompt in debug).
- **Estimate:** 2 h.
- **Status:** [ ]

#### S2-T4: Document `SpacedRepetitionScheduler`
- **File:** `domain/src/main/kotlin/com/emm/domain/study/SpacedRepetitionScheduler.kt`
- **Why:** it's the heart of the product. If a user reports "this card came back too fast", you need to explain why.
- **What to do:** comment in the file explaining: the SM-2 variant used, rationale for the ease-delta coefficients, how `nextInterval` is computed. If the decision is to migrate to standard SM-2 or FSRS, decide here and track as an S3 task.
- **Criterion:** an external dev can read the file and understand the algorithm without additional grepping.
- **Estimate:** 1-2 h.
- **Status:** [x] — `docs/SCHEDULER.md` covers: grade→quality mapping (non-canonical, HARD passes by design), easeAdjustment table per grade, constants with their meaning, step-by-step algorithm, edge cases covered by tests, invariants guaranteed by `FlashcardReview`, known limitations (no leech model, monotonic clock assumed, integer-day intervals), criteria for evaluating FSRS migration. Also removed the tautology `assertTrue(easeFactor >= 1.3)` from the use case test (replaced with exact assert at 1.3).

#### S2-T5: Startup with timeout
- **File:** `app/src/main/kotlin/com/emm/hello/newfeatures/NewRoot.kt`
- **What to do:** wrap the `AppStartupViewModel` `collect` with `withTimeoutOrNull(5_000L)`, show error with retry on timeout.
- **Criterion:** simulate a corrupt DB (rename SQLite file on device) → app shows an error with a "retry" button in <6s, not infinite loading.
- **Estimate:** 1 h.
- **Status:** [ ]

#### S2-T6: Real release build on device + manual walkthrough
- **What to do:** generate signed release APK, install on a physical device, do a full walkthrough: create deck → create card (all 3 modes) → study → edit → delete → export backup → reinstall → import.
- **Criterion:** the 5 flows work in release with minify on, no crashes in Crashlytics, no data loss in backup round-trip.
- **Estimate:** 2 h.
- **Status:** [ ]
- **Depends on:** all S1 + S2-T1.

---

### Sprint 3 — Post-launch, with real feedback (goal: ongoing)

These tasks do NOT block launch. Prioritize by signals from real users.

#### S3-T1: `Flashcard` with value objects
- Migrate `word/meaning/translation` to `Expression`/`IntendedMeaningEs`/`DefinitionEn`. Implies schema migration (existing cards must pass VO validation) and mapper adjustments.
- **Status:** [ ]

#### S3-T2: Per-feature ViewModel tests
- Start with the feature with the most reported bugs. Pattern: intent routing, state updates, effect emissions.
- **Status:** [ ]

#### S3-T3: Decide between standard SM-2 / FSRS
- Validate with retention feedback whether the current scheduler performs. If not, migrate to FSRS (modern Anki) with default parameter tables.
- **Status:** [ ]

#### S3-T4: Version prompts
- Add `promptVersion` to each generated note. Enables A/B testing and tracking quality regressions per prompt version.
- **Status:** [ ]

#### S3-T5: i18n for static catalogs
- `StaticCategories`, `CommunicativeIntent` with translatable labels.
- **Status:** [ ]

#### S3-T6: Few-shot examples in regeneration prompts
- Add 1-2 examples in each regeneration builder. Improves consistency with `gemini-2.5-flash`.
- **Status:** [ ]

#### S3-T7: `UpdateFlashcardUseCase` / `SoftDeleteFlashcardUseCase` — decide
- If they won't have logic, inline in ViewModel and delete. If they'll carry validation logic, add it and tests.
- **Status:** [ ]

#### S3-T8: Clean up redundancy in docs
- Reconcile `docs/*_CURRENT.md` with current code post-UI refactor.
- **Status:** [ ]

---

## 3. Recommended execution order

```
S1-T1 (Crashlytics)
  └─→ S1-T2 (R8 rules)
S1-T3 (Gemini model)
  └─→ S1-T4 (responseSchema)
S1-T5 (migrations baseline)
  └─→ S1-T6 (DeckTag cascade, if schema change chosen)
S1-T7 (privacy policy) — independent, parallelizable
S1-T8 (manifest cleanup) — independent

After S1 complete:
S2-T1 (retry/timeout/log) — depends on S1-T1
S2-T2, S2-T3, S2-T4, S2-T5 — independent
S2-T6 (release build) — last, depends on everything above

After launch:
S3-* per real signals
```

## 4. Definition of "ready for Play Store"

All Sprint 1 items closed + S2-T1 + S2-T6 verified on a real device.

## 5. Related documents

- `AGENTS.md` — operating rules
- `ARCHITECTURE.md` — technical structure
- `LOCAL_FIRST.md` — runtime contract
- `README.md` — repo entry point
