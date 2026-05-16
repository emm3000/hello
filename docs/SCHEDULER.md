# SpacedRepetitionScheduler

| Field | Value |
|---|---|
| Status | Active |
| Role | Documentation of the review scheduling algorithm |
| Source of Truth | `domain/src/main/kotlin/com/emm/domain/study/SpacedRepetitionScheduler.kt` |
| Tests | `domain/src/test/kotlin/com/emm/domain/study/SpacedRepetitionSchedulerTest.kt` |
| Read this when | You touch the scheduler, explain to a user "why did it come back so soon", or decide to migrate to FSRS |
| Last verified against code | 2026-05-15 |

## TL;DR

Simplified variant of **SM-2** (classic Anki) with a deliberate design choice:
cards graded **HARD pass** instead of failing. Only **AGAIN** fails.
That makes the algorithm **more permissive** than standard Anki to reduce
new-user friction, at the cost of slower progression.

If a user complains "this card came back too fast", the answer is almost always:
- They graded HARD several times → ease dropped to the 1.3 floor → short intervals.
- Or they graded AGAIN → reset to `interval = 1`.

## Grade → Quality mapping (non-canonical)

Internally each `ReviewGrade` translates to an SM-2 "quality" value.
Our mapping is **shifted upward** relative to canonical Anki:

| Grade | Quality (Hello) | Canonical Anki quality | Effect |
|---|---|---|---|
| `AGAIN` | 1 | 0 | Fails (quality < 3) |
| `HARD`  | 3 | 2 | **Passes** in Hello, fails in canonical Anki |
| `GOOD`  | 4 | 3 | Passes |
| `EASY`  | 5 | 4 | Passes |

Failure threshold: `quality < QUALITY_THRESHOLD_FOR_RESET (3)`.
Since HARD = 3, **HARD passes by one point**.

### Why this shift

Product decision: in a local-first app with no social pressure or gamification,
grading yourself HARD already implies humility. Penalizing it with a reset
disincentivizes honest self-assessment. The user ends up mechanically tapping
GOOD to keep momentum. We prefer HARD to pass but **lower the ease**,
shortening upcoming intervals without throwing progress in the trash.

## Algorithm step by step

```
input:  review (ease, repetitions, interval, lapses), grade, flashcardId, clock
output: new FlashcardReview

1. quality = mapGradeToQuality(grade)
2. if quality < 3:
     // FAIL path
     newEaseFactor   = review.easeFactor   (unchanged)
     newRepetitions  = 0                   (reset)
     newInterval     = 1                   (see it again tomorrow)
     newLapses       = review.lapses + 1
   else:
     // PASS path
     qualityDistance = MAX_QUALITY - quality
     easeAdjustment  = 0.10 - qualityDistance * (0.08 + qualityDistance * 0.02)
     newEaseFactor   = max(1.3, review.easeFactor + easeAdjustment)
     newRepetitions  = review.repetitions + 1
     newInterval     = when newRepetitions is:
                         1L → 1 day
                         2L → 6 days
                         3L+ → round(review.interval * newEaseFactor)
     newLapses       = review.lapses  (unchanged)

3. lastReviewedAt = clock.now()
4. nextReviewAt   = clock.now() + newInterval days
```

## Constants (what each number means)

```kotlin
MINIMUM_EASE_FACTOR        = 1.3   // ease floor — never goes below this
QUALITY_THRESHOLD_FOR_RESET = 3    // quality < this → fail path
MAX_QUALITY                = 5     // EASY's quality
EASE_DELTA_BASE            = 0.1   // adjustment when quality == 5
EASE_DELTA_FACTOR          = 0.08  // linear decay slope
EASE_DELTA_PENALTY         = 0.02  // quadratic term penalizing low quality
SECOND_REVIEW_INTERVAL_DAYS = 6L   // interval after the 2nd successful repetition
```

The 4 ease values (`MINIMUM_EASE_FACTOR`, `EASE_DELTA_BASE`, `EASE_DELTA_FACTOR`,
`EASE_DELTA_PENALTY`) are the **same as Anki**. The ease-adjustment formula is
literally SM-2's:

> `EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))`

The only difference is **when it's invoked** (reset threshold) and the interval
table for early reps (Anki uses 10 days for the 2nd; we use 6).

## easeAdjustment table per grade

| Grade | qualityDistance | Ease adjustment |
|---|---|---|
| `AGAIN` (q=1) | 4 | n/a (not applied, takes fail path) |
| `HARD`  (q=3) | 2 | `0.10 - 2*(0.08 + 2*0.02) = -0.14` |
| `GOOD`  (q=4) | 1 | `0.10 - 1*(0.08 + 1*0.02) = 0.00` |
| `EASY`  (q=5) | 0 | `0.10` |

Implications:
- **GOOD doesn't change ease**. It's the "neutral" grade — the algorithm respects you.
- **HARD drops 0.14**. You reach the 1.3 floor from 2.5 in ~9 consecutive HARDs.
- **EASY rises 0.10**. Rewards you, but modestly.

## Edge cases covered by tests

`SpacedRepetitionSchedulerTest.kt` covers:

- Grade → behavior mapping (correct ease delta per grade).
- Ease floor: HARD at 1.3 stays at 1.3; HARD at 1.4 floors to 1.3.
- Interval progression: 1 → 6 → ease * interval.
- Brand-new card with AGAIN: `lapses = 1, repetitions = 0, interval = 1`.
- Brand-new card with EASY: `ease = 2.6, repetitions = 1, interval = 1`.
- Multiple AGAINs accumulate lapses correctly.
- Mid-stream lapse resets repetitions but **preserves ease**.
- Timestamps: `lastReviewedAt = clock.now()`, `nextReviewAt` exactly N days later.
- `flashcardId` is **replaced** by the argument, not read from the review (important
  to avoid carrying the placeholder id from `FlashcardReview.empty()`).

## Invariants guaranteed by `FlashcardReview`

The `FlashcardReview` constructor (not the scheduler) `require()`s:

- `easeFactor >= 1.3`
- `nextReviewAt >= lastReviewedAt`
- `interval >= 0`
- `repetitions >= 0`
- `lapses >= 0`

That's why the scheduler never checks these conditions on output: if it violates them,
`copy()` throws in the `init` block.

## Known limitations

1. **No "leech" model**: cards with very many lapses are not flagged or removed
   from the deck. Anki suspends them at 8. Decide when to add it once we have
   real retention data.
2. **Clock assumed monotonic**: if the device clock goes backwards between one
   review and the next, `nextReviewAt < lastReviewedAt` would break the
   constructor. We don't defend against this explicitly — we trust the system.
3. **No "learning" vs "review" card distinction**: Anki has two separate
   queues. We simplify to one.
4. **Integer-day intervals**: `(interval * ease).roundToLong()`. Loses sub-day
   precision. Fine for a daily study app; not fine if we ever do minute/hour
   reviews.

## If we ever decide to migrate to FSRS

FSRS (Free Spaced Repetition Scheduler) is the modern successor to SM-2 that uses
ML to predict retention. Migrating would require:

- Per-user parameter table (FSRS learns them from review history).
- Schema change in `FlashcardReview` (FSRS uses "stability" and "difficulty",
  not `easeFactor`).
- Completely different scheduling logic.
- Rewritten scheduler tests.

**When to evaluate**: once we have ≥3 months of beta user review data and can
measure real vs predicted retention. Before that is premature.

## Related documents

- `LAUNCH_READINESS_AUDIT.md` — `S2-T4` originally requested this doc.
- `ARCHITECTURE.md` — scheduler location in the domain.
- `AGENTS.md` — rules for modifying the domain.
