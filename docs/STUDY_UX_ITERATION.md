# Study UX Iteration

| Field | Value |
|---|---|
| Status | Closed (10/10 merged locally; on-device validation pending) |
| Role | Atomic plan to iterate design + animation + interaction of the card in `Study` |
| Source of Truth | No (kept as history; see `STUDY_CURRENT.md` for current state) |
| Read this when | You want to trace the rationale of the iteration or audit the decisions |
| Last verified against code | 2026-05-16 |

## Closing summary

The 10 tasks were implemented in a single session. `:app:compileDebugKotlin`, `detekt`, `:domain:test`, and `testDebugUnitTest` green.

**Deviations from the original plan (intentional):**

- **S2-T6**: the plan said move `StudyStageHeader` as a top-start overlay on the card. Final decision: drop it entirely, because (a) `study_answer_guidance` is already shown inside the dock in Check, and (b) `study_prompt_guidance` ("Try to recall before peeking") overlaps with the dock's `TapToRevealHint` in Recall. Keeping both was noise.
- **S3-T9**: the plan put the `HBadge(cardType)` top-start inside the outer Box. Implemented that way (moved out of `FlashcardFrontContent` and lives in `StudyCanvas`'s `Box` next to TTS).
- **S1-T2 v1**: I implemented the gesture (tap → flip; horizontal drag → grade) with 25% / 50% thresholds. **Missing** the tinted overlay with grade label during the drag — today's only visual affordance is the card's translation. It's a cheap follow-up (~1h) but kept out of this iteration to avoid bloating the session. Tracked as **F1-Overlay-Swipe** below.

## Known follow-ups

- **F1-Overlay-Swipe**: add tinted overlay (errorContainer/tertiaryContainer/primaryContainer/secondaryContainer) over the back face with label "Hard"/"Easy"/etc. while `dragOffset != 0`. Alpha proportional to `|dragOffset| / widthPx`. Lives inside the re-rotated `Box` of the back content (so it doesn't end up mirrored).
- **F2-Device-Validation**: none of the 10 tasks were tested on a physical device. The final feel of the 420ms flip with cameraDistance 30 + scale-in + coordinated swipe is only confirmed with real hardware.
- **F3-Hard-Color**: the plan maps HARD to `tertiaryContainer` (usually green/teal in M3). Semantically HARD is "struggled" — closer to warning (amber). Consider using `semanticColors.warning` that already exists in the theme.

## TL;DR

The study session works, but the interaction does not flow: tapping the card and the "Reveal" button compete, animations are serial (~1s per card), the 4 grade buttons are dense, TTS is buried in the back content, and there's no informed feedback of the SRS interval when grading. This iteration redesigns the interaction in **10 atomic tasks** organized in 3 sprints. Each task is independent; no change touches `:domain` except T8 (pure interval-preview helper).

**Total estimate**: 1.5-2 effective days. Each task has an affected file, acceptance criterion, and estimate.

## Design principles

1. **One affordance per intent**: if the card is tappable to reveal, there's no parallel "Reveal" button.
2. **Canonical gestures before buttons**: horizontal swipe for grade on the back face; buttons remain as accessible fallback.
3. **One live surface**: card + dock + hints live within the same `Surface`; they change with `SizeTransform` spring, not as separate screens.
4. **Short, parallel animation**: ≤ 420 ms per flip, ≤ 220 ms per card transition, fade + scale instead of horizontal slide.
5. **Informed feedback**: each grade shows its resulting SRS interval before and after selecting.
6. **Visual hierarchy proportional to recall**: the word is ~70% of the front's visual weight; hints are progressive (on-demand), not automatic.

---

## Sprint 1 — Core interaction (goal: ½ day)

Removes perceptual noise and resolves the most obvious friction: where I tap, what happens when I drag.

### S1-T1: Drop the redundant "Reveal" button in `StudyStage.Recall`

- **File:** `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyScreen.kt:532-547`
- **Why:** the card already flips on tap (`StudyScreen.kt:255-258`), but the dock shows an `HButton "Reveal answer"`. Two affordances for the same action confuse and break the expectation when the user taps the back by mistake.
- **What to do:** in the `StudyStage.Recall` branch, replace the `HButton` with a subtle hint chip using `HBadge(variant = Outline)` + `Icons.Outlined.TouchApp` icon, text "Tap the card to reveal". Keep the `HButton` "Answer" when `needsTypedAnswer == true` (that case does need a dock-driven CTA because the next step is input).
- **Criterion:** in Recall without typed-answer, no big button in the dock. Tap on card → flip. Tap on the chip → does nothing (purely visual affordance).
- **Estimate:** 30 min.
- **Status:** [x]
- **Depends on:** none.

### S1-T2: Horizontal swipe on back face → grade

- **Files:** `app/src/main/kotlin/com/emm/hello/newfeatures/study/FlippableCard.kt` (add gesture handling), `StudyScreen.kt:429-456` (wire callbacks).
- **Why:** the whole grade flow is tap on buttons. Swipe is the canonical flashcard interaction (Anki Pro, Quizlet, RemNote). Speeds up 50+ card sessions.
- **What to do:**
    1. Replace `Modifier.clickable` in `FlippableCard` with `Modifier.pointerInput` combining `detectTapGestures` (for flip) + `detectHorizontalDragGestures` (for grade).
    2. Add new parameters to `FlippableCard`: `gradeEnabled: Boolean` and `onGradeSwipe: (ReviewGrade) -> Unit`. Only enable drag gestures when `cardFace == Back && gradeEnabled`.
    3. While the user drags, paint a tinted overlay on top of the card (`errorContainer` / `tertiaryContainer` / `primaryContainer` / `secondaryContainer`) whose opacity grows with `dragOffset / widthPx`.
    4. Define thresholds: |offset| < 25% width → snap back (canceled); 25% ≤ |offset| < 50% → `HARD` (left) or `GOOD` (right); |offset| ≥ 50% → `AGAIN` (left) or `EASY` (right).
    5. Show centered text on the overlay with the pending grade ("Hard", "Easy"...) in `headlineSmall`.
    6. In `StudyScreen.kt`, pass `gradeEnabled = sessionStage == StudyStage.Grade && enabledGrades` and `onGradeSwipe = callbacks.onReviewAnswer`.
- **Criterion:** on back face during Grade, short left swipe marks Hard, long marks Again; short right marks Good, long marks Easy; release without crossing threshold snaps back. The 4 dock buttons remain available as fallback. If `enabledGrades` excludes a grade (case where correct typed-answer blocks Again), swipe in that direction is disabled.
- **Estimate:** 3-4 h (gesture logic + overlay + accessibility).
- **Status:** [x]
- **Depends on:** none strictly, but reads better after S1-T1.

### S1-T3: Move TTS to a floating `IconButton` in the card corner

- **Files:** `StudyScreen.kt:818-824` (remove the back's `HButton`), `StudyScreen.kt:399-461` (`StudyCanvas` wraps `FlippableCard` in a `Box` with top-end overlay).
- **Why:** today the "Speak" button lives inside the back content (`FlashcardBackContent`), breaking the answer hierarchy and being unreachable while the user is on Front (where it's most useful: validating pronunciation before revealing).
- **What to do:**
    1. Remove the TTS ghost `HButton` from `FlashcardBackContent` (`StudyScreen.kt:818-824`).
    2. In `StudyCanvas`, wrap the `AnimatedContent` with a `Box` that has an `IconButton` aligned to `TopEnd` with 12.dp padding. Icon `Icons.AutoMirrored.Filled.VolumeUp` when idle, `Icons.Outlined.Stop` when `isSpeaking`. The button **does not rotate with the flip** (lives outside `FlippableCard`).
    3. The `onSpeak`/`onStop` callback reads the word from `currentItem` regardless of face. If `!ttsReady`, disable.
    4. Correct contentDescription per state.
- **Criterion:** TTS accessible on Front and Back, fixed at the top-right corner, doesn't rotate with flip. Works identically to before (same `TextToSpeechManager`).
- **Estimate:** 1 h.
- **Status:** [x]
- **Depends on:** none.

---

## Sprint 2 — Motion & polish (goal: ½ day)

Makes the interaction feel fluid. Small individually, synergistic together.

### S2-T4: Shorten and replace the card-to-card transition

- **File:** `StudyScreen.kt:82-87` (constants), `StudyScreen.kt:412-425` (`transitionSpec`).
- **Why:** today `CARD_TRANSITION_DURATION_MS = 350` with horizontal slide + `CARD_FLIP_DURATION_MS = 600` flip + `CARD_EXIT_FADE_DURATION_MS = 250` fade out. Chained on grade they're ~1s perceptual. The horizontal slide also competes with S1-T2's swipe.
- **What to do:**
    1. `CARD_TRANSITION_DURATION_MS = 350` → **220**.
    2. `CARD_EXIT_FADE_DURATION_MS = 250` → **160**.
    3. Replace `slideInHorizontally + fadeIn` with `fadeIn + scaleIn(initialScale = 0.96f)`.
    4. Replace `slideOutHorizontally + fadeOut` with `fadeOut + scaleOut(targetScale = 0.92f)`.
    5. Use explicit `FastOutSlowInEasing` in the `tween`s.
- **Criterion:** on grade (or on swipe if S1-T2 already merged), the outgoing card fades while shrinking slightly and the new one enters growing from 0.96 with fade. No horizontal slide (frees that axis so the gestural swipe doesn't compete).
- **Estimate:** 45 min.
- **Status:** [x]
- **Depends on:** none.

### S2-T5: Increase `cameraDistance` and smooth the flip

- **File:** `FlippableCard.kt:26-27, 67`.
- **Why:** `CARD_CAMERA_DISTANCE_MULTIPLIER = 12f` gives a "glued to the lens" flip, cartoony. Compose guideline is ~30. The 600ms flip is also excessive combined with S2-T4.
- **What to do:**
    1. `CARD_CAMERA_DISTANCE_MULTIPLIER = 12f` → **30f**.
    2. `CARD_FLIP_DURATION_MS = 600` → **420**.
    3. Change `tween` to `tween(durationMillis = 420, easing = FastOutSlowInEasing)`.
- **Criterion:** the flip feels organically 3D (not flat-paper), and completes in ~420ms. Validate on a physical device (not just emulator) because depth perception depends on DPI.
- **Estimate:** 30 min.
- **Status:** [x]
- **Depends on:** none.

### S2-T6: Unify Card + Dock + Hint into a single surface

- **Files:** `StudyScreen.kt:216-315` (root `Column` inside `Scaffold`), `StudyScreen.kt:361-382` (remove `StudyStageHeader`), `StudyScreen.kt:399-461` (`StudyCanvas`), `StudyScreen.kt:463-611` (`StudyActionDock`).
- **Why:** today there are 3 separate visual blocks — `HProgressBar`, `StudyStageHeader` (floating Text), `StudyCanvas` (Surface), `StudyActionDock` (Surface). Each with its own paddings, shapes, and backgrounds. They visually compete instead of feeling like one object.
- **What to do:**
    1. Wrap `StudyCanvas` + `StudyActionDock` in a single `Surface` (`shape = extraLarge`, `color = surfaceContainerLowest`) with an inner `Column`.
    2. Remove the inner `Surface` from `StudyActionDock` (which lives at `StudyScreen.kt:473-476`); now it's just a `Column` with padding.
    3. Drop `StudyStageHeader` as a separate composable. The hint guidance renders inside the card as a `Text` overlay top-start (with `bodySmall` + `onSurfaceVariant`), only in `StudyStage.Recall` and `StudyStage.Check`.
    4. The dock's `AnimatedContent` uses `SizeTransform(clip = false) { initial, target -> spring(stiffness = StiffnessMediumLow) }` so the height change across stages (Recall → Check → Grade) is fluid spring.
- **Criterion:** one visual body with the card on top and controls below, no "seams" between surfaces. Stage change animates the height with spring, no jumps.
- **Estimate:** 2 h.
- **Status:** [x]
- **Depends on:** facilitates S1-T3 (TTS floating already inside the unified wrapper); ideally after S1-T3.

### S2-T7: Remove dual-source `prevStudyItem`

- **Files:** `StudyScreen.kt:130, 259, 389, 446` (remove `prevStudyItem` and references).
- **Why:** today `frontContent` uses `currentItem` and `backContent` uses `prevStudyItem.value` to avoid flicker on advance while the card is flipping. Having two data sources in the same view is fragile — and stops making sense once S2-T4 replaces the horizontal slide with fade+scale (the outgoing card fades as a unit; there's no moment where the back needs freezing).
- **What to do:**
    1. Remove `prevStudyItem` (`StudyScreen.kt:130`) and the `onCardAnimationFinished` callback (`StudyScreen.kt:259`).
    2. In `FlashcardBackContent`, read `card = currentItem?.flashcard` and `studyCard = currentItem?.studyCard` directly.
    3. Drop the `onFinished` parameter of `FlippableCard` (`FlippableCard.kt:40, 48`).
- **Criterion:** on grade, the outgoing card does fade+scale out with its own consistent data; the new card enters with its own. No flash of empty content on the back face. Verify visually with 5-10 grades in a row.
- **Estimate:** 45 min.
- **Status:** [x]
- **Depends on:** S2-T4 (the new transition is what makes this safe).

---

## Sprint 3 — Informed feedback (goal: ½ day)

Turns each interaction into an informed decision. This is where a user with 50 cards/day goes from "tapping Good by inertia" to "evaluating honestly".

### S3-T8: SRS interval preview per grade (domain helper)

- **Files:** `domain/src/main/kotlin/com/emm/domain/study/PreviewNextInterval.kt` (new), `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyUiState.kt`, `StudyViewModel.kt:43-55` (`showNextCard`).
- **Why:** today the user picks Again/Hard/Good/Easy without knowing what SRS interval each produces. Showing the preview turns the grade into an informed decision and respects the already-implemented scheduler logic (`SpacedRepetitionScheduler.kt`).
- **What to do:**
    1. Create `domain/.../study/PreviewNextInterval.kt`:
        ```kotlin
        object PreviewNextInterval {
            fun previewAll(review: FlashcardReview, clock: Clock): Map<ReviewGrade, Long> =
                ReviewGrade.entries.associateWith { grade ->
                    SpacedRepetitionScheduler.schedule(review, grade, review.flashcardId, clock).interval
                }
        }
        ```
       Returns days per grade. Pure function, JVM-only, no side effects.
    2. Add `intervalPreviews: Map<ReviewGrade, Long> = emptyMap()` to `StudyUiState`.
    3. In `StudyViewModel.showNextCard()`, after setting `currentItem`, compute `PreviewNextInterval.previewAll(currentItem.review, clock)` and store in state. Inject `Clock` in the VM (use the domain's `SystemClock` as default in `NewModule.kt`).
    4. Tests: `PreviewNextIntervalTest.kt` in `domain/src/test/` covering new cards (repetitions=0) and mature ones (repetitions=5, interval=14).
- **Criterion:** `StudyUiState.intervalPreviews` contains the 4 intervals in days at the start of each card. Tests green. No visible changes yet (S3-T10 renders them).
- **Estimate:** 1.5 h.
- **Status:** [x]
- **Depends on:** none.

### S3-T9: Front visual hierarchy — dominant word + progressive hint

- **File:** `StudyScreen.kt:673-744` (`FlashcardFrontContent`), `StudyScreen.kt:864-911` (`CardTypePromptBlock`).
- **Why:** today the front stacks 5-6 competing elements (cardType badge + frontTitle "Recognition" + prompt + phonetic + frontSupport + separators). `frontTitle` repeats what the badge already says. The always-visible `frontSupport` provides automatic hints that break the SRS "desirable difficulty" principle.
- **What to do:**
    1. Remove the `Text(frontTitle)` (`StudyScreen.kt:704-712`) — the `cardType` badge already conveys that information.
    2. Move the `HBadge(cardType)` to the card's top-start corner (aligned top-start inside the outer `Box`, next to S1-T3's TTS at top-end). Padding 12.dp. Take it out of the central flow.
    3. Bump the `prompt` to `displayMedium` (from `headlineMedium`) and leave it in `FontWeight.Bold`.
    4. Phonetic in `bodySmall`, with no separator before it (visual weight is already low).
    5. **Progressive hint**: `frontSupport` is no longer shown by default. Add a small `IconButton` below the phonetic with `Icons.Outlined.Info`. On tap, show `frontSupport` in an inline `HAlert` (`AlertVariant.Default`). Tap again hides it.
    6. For `StudyCardType.Cloze`, keep the "study_cloze_prompt_title" label because it indicates the answer is a completion, not the whole word (semantically necessary, not noise).
- **Criterion:** the word holds the visual center and represents ≥ 60% of the weight. Hints appear only on-demand. Cloze remains visually distinguishable (label "Complete the phrase" above the word).
- **Estimate:** 2 h.
- **Status:** [x]
- **Depends on:** S1-T3 (because the cardType badge goes at top-start of the wrapper that already has TTS at top-end).

### S3-T10: Grade buttons as 2x2 grid with interval + semantic color

- **Files:** `StudyScreen.kt:1091-1152` (`AnswerButtons`), new private composable `GradeChip` in the same file.
- **Why:** the 4 rectangular `HButton`s with leadingIcon are dense, take up little click-target relative to width, and don't show the interval. The Anki Pro/RemNote standard is 1:1 chips with label + interval + semantic color.
- **What to do:**
    1. Create `GradeChip(grade, intervalDays, enabled, onClick)` private composable in `StudyScreen.kt`. Renders a `Surface` with:
        - Aspect ratio 1:1 (or `heightIn(min = 88.dp)`).
        - Background: `errorContainer` (Again), `tertiaryContainer` (Hard), `primaryContainer` (Good), `secondaryContainer` (Easy).
        - 1dp border in `contentColorFor(...)` with alpha 0.2.
        - Layout: icon top-start, label `titleMedium` SemiBold center, interval `labelSmall` bottom-center with `formatInterval(days)` ("1 day", "6 days", "2 weeks", "1 month" — local helper).
    2. Replace the two `Row`s of `HButton`s with a `Column` of 2 `Row`s holding a `GradeChip` per cell, `Modifier.weight(1f)` and `aspectRatio(1f)` or fixed height.
    3. `formatInterval(days: Long): String` local: <1 → "Today", 1 → "Tomorrow", <7 → "$days days", <30 → "$weeks week(s)", else "$months month(s)".
    4. Pass `intervalPreviews: Map<ReviewGrade, Long>` from state to `AnswerButtons`.
    5. Keep `enabledGrades` and `guidance` as today (S1-T1's policy still holds).
- **Criterion:** dock shows a 2x2 grid of colored chips; each one with icon + label + interval dynamically computed for the current card. Tapping one records the grade. If a grade is `enabledGrades = false`, the chip renders with alpha 0.4 and receives no taps. The intervals match what `SpacedRepetitionScheduler.schedule` would return.
- **Estimate:** 2 h.
- **Status:** [x]
- **Depends on:** S3-T8 (to have previews in state).

---

## Anti-tasks (what is NOT done in this iteration)

- **Not** introducing a vertical gesture system (swipe up = skip, swipe down = back). Too ambiguous, and back already has a navigation icon.
- **Not** adding new haptic feedback beyond what already exists (`TextHandleMove` on flip, `LongPress` on grade). If S1-T2 needs haptic at threshold crossing, add it there specifically.
- **Not** creating a new `H*` component for the hint chip or the grade chip. They stay as private composables inside `StudyScreen.kt`. If they end up used on other screens, promote them to `core/ui/` in their own PR.
- **Not** changing the aggregated-grade logic in `StudyViewModel.processReviewAnswer` (`StudyViewModel.kt:68-93`). The S3-T8 preview shows the interval "if this were the last studyCard of the flashcard"; the actual behavior for flashcards with multiple studyCards remains the already-implemented moreConservativeGrade. This is deliberate: complicating the preview with pending-studyCard logic makes the plan unestimable. Can be revisited later.
- **Not** touching `:data` or the schema. All change lives in `app/` except S3-T8's pure helper in `:domain`.

---

## Recommended implementation order

1. **S1-T1** (clean afterReveal) → confirms the dock simplifies without breaking Recall.
2. **S1-T3** (floating TTS) → frees space in the back content.
3. **S2-T5** (cameraDistance + flip duration) → trivial change, immediate improvement.
4. **S2-T4** (fade+scale transition) → enables S2-T7.
5. **S2-T7** (remove prevStudyItem) → closes dual-source debt.
6. **S2-T6** (unify surface) → visually consolidates the above.
7. **S3-T8** (preview interval helper + state) → infrastructure for S3-T10.
8. **S3-T9** (front hierarchy) → with TTS and badge already positioned.
9. **S3-T10** (2x2 grid with interval) → visible close-out.
10. **S1-T2** (swipe) → last because it's the longest task and benefits from having everything else stable to validate the feel.

## Plan closing criteria

- A 20-card session on a physical device feels like "one continuous action", not "20 separate screens".
- Tap to reveal has exactly one destination (the card itself when there's no typed-answer; the dock when there is).
- Each grade shows its resulting SRS interval before being chosen.
- Left/right swipe works as a fluid alternative to the grade chips.
- TTS accessible from Front and Back without rotating with flip.
- No regressions in typed-answer (Exact / FlexibleText / ManualSelfCheck still work as today).
