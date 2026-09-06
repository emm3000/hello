# Redesign Plan — the hybrid experience

| Field | Value |
|---|---|
| Status | Active |
| Role | Scope, order and falsifier of every work unit that moves the app to the hybrid design |
| Source of Truth | Yes for stage order, unit scope and the decisions listed here; the design canvas owns visuals; `*_CURRENT.md` and the code win on behavior |
| Read this when | You're about to implement, review or re-plan any part of the redesign |
| Last verified | 2026-08-28 |

## Why this document exists

On 2026-08-28 the owner rejected the Instrument direction (`DESIGN_BRIEF.md`, visual sections): dark, cold, instrument-like, it never convinced. The learning mechanics in that brief were not rejected — hero is the action, two-beat back face, no semantic color on grades, direction by card maturity — and they carry into this plan unchanged.

The replacement was designed on a canvas from a client's need rather than from the existing screens: a Spanish speaker learning English through the words they personally capture; fluidity, minimalism, and the goal of actually learning English.

Design canvas (visual source of truth): https://claude.ai/code/artifact/3f674270-631e-41f3-89ea-47233820cfe1 — page *Prototipo* holds the clickable prototype and an all-screens sheet; page *Direcciones* holds the three explored directions.

## The design in one paragraph

Structure from Dirección A: open the app and the session is already there. No dashboard, no tab bar, no navigation before the first card. The answer leads on the back face, grading is a tap. Skin from Dirección C: every card is a flat color block whose hue rotates by card position, display type has character, controls are ink on a light page, progress is one ring. The chrome is in English; Spanish appears in exactly one place — the answer.

## Design system

Values below are the migration target. Once they land in `core/theme`, those files are the source of truth and this table is superseded.

### Color — light, ink, four hues

| Token | Value | Role |
|---|---|---|
| `pageBackground` | `#F4F3F1` | Every non-card screen |
| `surface` | `#FBFAF9` | Inputs, sheets, dialogs |
| `surfaceRaised` | `#E9E7E3` | Chips at rest, secondary containers, selected tint |
| `ink` | `#15141A` | Text, primary fills, icons |
| `onInk` | `#F4F3F1` | Text on an ink fill |
| `inkMuted` | `#6F6D75` | Secondary copy, labels |
| `inkFaint` | ink at 45 % | Metadata, placeholders |
| `hairline` | ink at 12 % | Dividers, progress tracks |
| `outline` | ink at 30 % | Secondary control borders |
| `cardPeach` `cardMint` `cardPeriwinkle` `cardLavender` | `#F5C9A8` `#BFE3CB` `#C6D3F5` `#DCC8F0` | Card blocks; `cardHues[index % 4]`, never by grade |
| `successInk` / `successContainer` | `#2F6B4F` / `#DDEBE2` | System states only |
| `warningInk` / `warningContainer` | `#8A5A12` / `#F3E6C8` | System states only |
| `destructiveInk` / `destructiveContainer` | `#A33A3A` / `#F3DADA` | Destructive actions, load errors |

There is no accent color. The primary action is an ink fill. Selected states are ink fill with `onInk` text. Semantic colors never touch the grade buttons or the study screen.

### Typography — two families

| Family | Roles | Notes |
|---|---|---|
| Bricolage Grotesque | `display*` 52/44/40 sp (800/700/800), `headline*` 32/26/22 sp (700), primary button labels 18 sp (700) | `-0.02em` tracking, line height 1.02–1.05. The word, the answer, big numbers. |
| Schibsted Grotesk | `title*` 20/17/15 sp (600), `body*` 16/15/13 sp (400, line height 1.5), `label*` 13/12/11 sp (500) | Everything else. |

`Typography.metadata` becomes Schibsted 12 sp, weight 500, `0.12em` tracking; uppercase is applied at the call site. Both families load through the existing Google Fonts provider. If the provider cannot serve a family, the fallback is bundling the OFL TTFs under `res/font/`, decided at Stage 0 on the device.

### Shape and size

| Token | Value |
|---|---|
| `HelloShapes.control` | 16 dp |
| `HelloShapes.container` | 28 dp (card blocks) |
| `HelloShapes.pill` | 100 dp |
| Primary button | 60 dp tall |
| Secondary button / pill | 52 dp tall |
| Text button, icon button, list row | ≥ 44 dp tall |
| Progress track | 3 dp |

Spacing scale is unchanged.

### Rules carried from `DESIGN_BRIEF.md`

1. Grade buttons carry no semantic color: `Forgot` outlined on the left, `Knew it` ink-filled on the right.
2. The back face: the answer dominant, the English sentence with the target word underlined (no color change), its Spanish translation, one small always-visible reference line — part of speech, IPA, English meaning.
3. Today's hero is the action. No metric as hero, no preview of due words.
4. Direction is decided by card maturity, latched one-way, never by a setting.

### Language rule

The chrome is English. Spanish is content: the translation of a word and the translation of its example sentence. `strings.xml` is rewritten in place — this is a single-user, single-device product with one locale; no `values-es/` is created.

## Work protocol for this plan

Every unit is one commit. Before a writer runs, the unit's scope, falsifier and actor are declared here. `./gradlew detekt testDebugUnitTest :domain:test` is the floor before every commit, never the proof. After every stage the app is built and installed on `medium_phone` (the owner is asked first), every touched screen is screenshotted, and the owner reviews before the next stage starts.

Falsifier by failure mode, from `CLAUDE.md`: transcription → the diff read against the spec and a targeted `rg` returning zero; logic → a test red before, green after; visual → the screen on the device; rule compliance → `/agents-review`.

## Stages

### Stage 0 — Foundation

The whole app changes color and type in one stage with the least code. Un-redesigned screens will look wrong in the new palette; that is expected and is the reason Stage 1 and Stage 6 exist.

| Unit | Scope | Falsifier | Actor |
|---|---|---|---|
| 0.1 `test: delete the proof test that reads source files by path` | `app/src/test/java/com/emm/hello/design/RefineDesignSystemVerificationProofTest.kt` asserts the shape of a past refactor by reading Kotlin sources as text; it breaks on any file move and proves nothing about behavior. | File gone; tests green. | `rm` |
| 0.2 `refactor(theme): a light palette named by role replaces the instrument tokens` | `Color.kt` re-valued and renamed by the table above; `Theme.kt` becomes a `lightColorScheme` (the `darkTheme`/`dynamicColor` parameters are dropped if no call site passes them); `Foundation.kt` shapes 16/28/100 and material shapes 12/16/28; the rename applied across the 48 files that reference `instrument*` (571 usages) with `sd`. Map: `instrumentBg → pageBackground`, `instrumentSurface → surface`, `instrumentSurface2 → surfaceRaised`, `instrumentElev → surface`, `instrumentDivider → hairline`, `instrumentOnBg → ink`, `instrumentPrimary → ink`, `instrumentMuted → inkMuted`, `instrumentFaint → inkFaint`, `instrumentAccent → ink`, `instrumentOnAccent → onInk`, `instrumentAccentSoft → surfaceRaised`, `instrumentGood/Warn/Bad → successInk/warningInk/destructiveInk`, `*Soft → *Container`; `instrumentVariant` and `instrumentButtonTokens` resolved by the writer and reported. | `rg 'instrument' --glob '*.kt'` returns zero; the diff outside `core/theme` is renames only; detekt and tests green; every screen on the device. | One writer for the four theme files; `sd` for the rename |
| 0.3 `refactor(theme): bricolage and schibsted replace geist` | `Type.kt` rebuilt per the typography table; `res/font/geist.xml` and `geist_mono.xml` replaced by `bricolage_grotesque.xml` and `schibsted_grotesk.xml`; `geist`/`geistMono` call sites renamed with `sd` (`geistMono → schibsted`, `geist → schibsted`; display roles already route through `MaterialTheme.typography`). | `rg -i geist` returns zero; the fonts visibly render on the device (not the Roboto fallback). | One writer; `sd` |
| 0.4 Device review | Build, install on `medium_phone`, screenshot every route. | The owner's eyes. | This thread |

### Stage 1 — Shared components

| Unit | Scope | Falsifier | Actor |
|---|---|---|---|
| 1.1 `refactor(ui): buttons carry the ink anatomy` | `HButton`: Primary (ink fill, 60 dp, Bricolage 18 sp 700), Secondary (outlined, 52 dp), Text (≥ 44 dp). Any accent-based variant deleted. Disabled resolves to `surfaceRaised` + `inkFaint`, never alpha. | Previews render every variant beside its disabled twin; `SharedControlsTest` on the device. | One writer |
| 1.2 `refactor(ui): top bar, chips, inputs and progress take the new tokens` | `HTopBar` (44 dp targets, tracked label), `HChip`/`HTagChip` (48 dp pills, ink when selected), `HInput`/`FieldShell` (adds an underline variant for the capture field), `HProgressBar` (3 dp), `HSearchBar` (44 dp, hairline), `HEmptyState` (typographic, no illustration). | Previews; device. | One writer |

### Stage 2 — Today

| Unit | Scope | Falsifier | Actor |
|---|---|---|---|
| 2.1 `refactor(today): the feature is called what the product calls it` | Package `newfeatures/hoy → newfeatures/today`, `Hoy*` types → `Today*`, `hoy_*` string keys → `today_*`. Same rule that renamed `dashboard → hoy`; the product now says Today. | `rg -i '\bhoy\b|Hoy[A-Z]' app` returns zero; tests green. | `git mv`, `sd`. No model. |
| 2.2 `feat(today): the ring knows the day's progress and the streak` | `DashboardStats` already carries `currentStreak`; add `reviewedToday` and `dueToday` to `GetDashboardStatsUseCase` if absent. Ring fill = reviewed / due; label = `Day N`. | Use-case test red → green. | One writer, TDD |
| 2.3 `refactor(today): the screen is the stack, the start action and two doors` | `TodayScreen` per the prototype: card stack with count and estimate, `Start`, `Add a word` and `Library` pills, the ring. Zero due: the stack reads `Nothing due` with the next-due copy, `Add a word` becomes primary. `TodayStatsSection` deleted. Screen strings rewritten in English. | `TodayViewModelTest` green; device. | One writer |

### Stage 3 — The card

| Unit | Scope | Falsifier | Actor |
|---|---|---|---|
| 3.1 `refactor(study): grading is a tap, never a swipe` | Swipe-to-grade in `FlippableCard.kt` deleted with its overlay tokens (they paint grades in semantic color, which Rule 1 forbids), `study_grade_swipe_hint`, `FlippableCardSwipeTest`. Tap-to-flip stays. | `rg -i swipe app` returns zero; tests green. | One writer |
| 3.2 `feat(study): the session remembers what you knew and what you forgot` | `StudyUiState` gains `knewCount` / `forgotCount`; the ViewModel tallies per grade. | `StudyViewModelTest` red → green. | One writer, TDD |
| 3.3 `refactor(study): the card is a color block and the answer leads` | Front and back per the prototype: block color `cardHues[index % 4]`, `n / total`, 3 dp progress, prompt label (`What does it mean?`), answer 44 sp Bricolage, sentence 20 sp with the word underlined, Spanish 15 sp at 70 %, reference line; `Forgot` outlined / `Knew it` ink. Study strings in English. | Device; `/agents-review`. | One writer |
| 3.4 `refactor(study): the finished session is a screen, not a dialog` | `SessionFinishedDialog` and `mascot_celebrate` deleted; the Done screen (ring at 100 %, `Done for today.`, the tallies from 3.2, `Add a word`, `Back to Today`) renders in place of the card. `Get new words` arrives in Stage 8. | `rg mascot` returns zero; device. | One writer |

### Stage 4 — Add a word and Library

| Unit | Scope | Falsifier | Actor |
|---|---|---|---|
| 4.1 `refactor(capture): add a word is one field on a mint block` | `CaptureScreen` per the prototype: underline field, mic, `Save`, `Done`, the recent list with `Preparing…` / `Ready`. Strings in English. | `CaptureViewModelTest` green; device. | One writer |
| 4.2 `refactor(library): the library is a dense list under a search field` | `LibraryScreen` per the prototype: 60 dp rows — word, translation, schedule status (`due today`, `in 3 days`, `new`). If `LibraryFlashcard` lacks `nextReviewAt`, it is added in `:domain` with a test. Strings in English. | `LibraryViewModelTest` created and green; device. | One writer, TDD for the domain change |

### Stage 5 — English chrome sweep

| Unit | Scope | Falsifier | Actor |
|---|---|---|---|
| 5.1 `refactor(strings): every remaining screen speaks english` | Settings, decks, card editor and detail, onboarding, notifications, errors. Spanish survives only as content. | The diff read line by line; `rg` for Spanish diacritics in `strings.xml` returns only content keys. | One writer; transcription review in this thread |

### Stage 6 — Surfaces the prototype did not draw

Each of these is decided with the owner before its unit runs. Proposals:

| Unit | Proposal | Falsifier |
|---|---|---|
| 6.1 Onboarding | One screen: `Save the words you meet. We'll make them stick.` and `Start`. Pages and illustration deleted. | `OnboardingViewModelTest`; device. |
| 6.2 Card detail and edit | Detail is the same color-block card with an `Edit` action; Edit is the field list (word, translation, example, its translation, part of speech, IPA) on a block. | ViewModel tests; device. |
| 6.3 New-card wizard | Candidate for deletion (~2.5k lines): capture + background enrichment + edit covers the manual path once 6.2 handles an unenriched card. Blocked on the owner's decision. | `rg NewCard` returns zero if deleted. |
| 6.4 Decks | Stay in the schema, reskinned under Settings. Whether decks die is decided after Library tags prove sufficient. | Device. |
| 6.5 Settings | Reskin only. The time picker stays deferred. | `SettingsScreenTest` on the device. |

### Stage 7 — Direction by maturity (domain change, adversarial review)

This is the one stage that changes learning behavior and touches a migration; it earns `judgment-day`.

| Unit | Scope | Falsifier |
|---|---|---|
| 7.1 `feat(domain): a card graduates to production once and never comes back` | `Flashcard.productionSince: Instant?`; `ScheduleFlashcardReviewUseCase` sets it when `state == REVIEW` and `stability ≥ threshold` (proposed 21 days) and it is null; it is never cleared. | Tests: graduates exactly once; a failure after graduation does not revert; below-threshold cards never graduate. |
| 7.2 `feat(data): persist when a card graduated` | Migration adds the nullable column; row mapping both ways. | `:data:verifySqlDelightMigration`; mapping test. |
| 7.3 `feat(study): mature cards ask for the english` | `StudySessionItem.direction`; front shows the Spanish and `How do you say it?`; back shows the English word with its IPA. | Item mapping test; device. Judgment Day over 7.1–7.3. |

### Stage 8 — New words

Every AI generation fails in the field: `Firebase AI Logic has been deactivated in this project. To resume using Firebase AI Logic, you must enforce Firebase App Check.` The owner enforces App Check in the Firebase console; no code workaround exists. **Update 2026-09-06:** enforced, and the app attests itself through `App.installAppCheck()`; a real capture on the emulator ended `READY`. Then:

| Unit | Scope | Falsifier |
|---|---|---|
| 8.1 `feat(domain): suggest words for a situation one step above the learner` | Interface in `:domain`, Gemini implementation in `:data`; prompt seeded with the last 15–20 captured words; returns a situation and ~6 candidates. | Use-case tests with a fake; a quota test (one suggestion = one generation). |
| 8.2 `feat(today): a day with nothing due offers new words` | `Get new words` on the Done screen and the zero-due Today; the Suggest screen with toggle chips; picks become captures with background enrichment. | ViewModel tests; device. |

### Stage 9 — Cleanup and docs

| Unit | Scope | Falsifier |
|---|---|---|
| 9.1 `refactor: remove what the redesign left behind` | Orphaned `H*` components (`HStat`, `HStatCard`, `TodaySkeleton` and whatever else has zero consumers), unused strings, unused drawables (`mascot_*`), the swipe hint. | `rg` zero consumers; detekt. |
| 9.2 `docs: resync the references with the hybrid design` | `*_CURRENT.md`, `ARCHITECTURE.md`, `DESIGN_BRIEF.md` visual sections rewritten from the canvas. | Read against the code. |

## Decisions taken in this plan

| Decision | Rationale |
|---|---|
| Tap-only grading; swipe deleted | A swipe that misfires is a false grade, and FSRS schedules on it. The overlay also colored grades, which Rule 1 forbids. |
| No accent color | The primary action is ink. One fewer thing to get wrong on every screen, and selection reads as ink fill everywhere. |
| Light theme only | The design is light. A dark variant is a second palette to maintain for one user who did not ask for it. |
| English chrome, Spanish only as the answer | Chrome teaches ~15 words and nothing more; the rule makes Spanish the visible exception and keeps the eye in English by default. |
| Strings rewritten in place, one locale | Single user, single device. A second `values-*` directory is machinery for a product this is not. |
| The path-reading proof test is deleted | It asserts the shape of a finished refactor by reading source text; it fails on moves and proves nothing about behavior. |
| `hoy → today` rename | The repo's own rule: the feature is called what the product calls it. |
| Finished session is a screen | A dialog over a dimmed card is a modal interruption; the Done state is the end of the flow, not an alert. |
| Stats grid becomes one ring | Metrics as hero were rejected in the brief; one ring carries progress and streak without competing with the action. |
| Direction by maturity stays, latched | Unchanged from the brief: production is the goal, recognition is the on-ramp, and a recomputed direction oscillates forever. |

## Out of scope

Dark mode. Multi-locale. Multi-device. Any change to `:domain` other than Stages 2.2, 4.2, 7 and 8. The notifications time picker.

## Rule

This plan owns stage order, unit scope and the decisions above. The canvas owns visuals. `*_CURRENT.md` and the code own behavior; when a unit lands, its `*_CURRENT.md` is updated afterwards.
