# Design Brief — English-Learning Redesign

| Field | Value |
|---|---|
| Status | Active |
| Role | Design direction and rationale for the visual redesign that runs alongside the session-first restructure |
| Source of Truth | Yes for visual direction and design rationale; `RESTRUCTURE_PLAN.md` owns phase scope and order, `*_CURRENT.md` and the code win for behavior |
| Read this when | You're about to design or implement any screen of the redesign, or you want to know why a visual decision was made |
| Last verified | 2026-08-27 |

## Why this document exists

The structural restructure (`RESTRUCTURE_PLAN.md`) fixes *what the app is organized around*. It does not fix *how the app looks and teaches*. The owner's verdict on the current design: it never convinced, and it did not work well for the actual goal — learning English.

This brief captures the design interview held on 2026-08-27. It is the input for the design canvas and for every redesign work unit. It records decisions **and their rationale**, so a future reader can tell a deliberate constraint from an accident.

Scope: visual language, information hierarchy, and the learning mechanics that the UI must express. It does not restate module boundaries (`CLAUDE.md`, `.claude/rules/`) or phase order (`RESTRUCTURE_PLAN.md`).

## Product goal

Hello is a personal English-learning app built on personal capture: the user saves words they actually encounter, and the app makes them stick. Every design decision below is judged against one question — does it help the user learn English, or does it only look good?

## Visual references

| Reference | What we take | What we leave |
|---|---|---|
| **Notion** | Hierarchy carried by typography, not by color. Near-monochrome. Zero ornament. | Its density on primary surfaces. |
| **Starlink (Android)** | Deep near-black, generous air, precise and instrument-like. | Metrics as the hero. Hello's hero is an action, not a dashboard. |

Shared and adopted: dark, cold-neutral, unornamented. The discipline in both apps comes from **how little color they use**, and both are neutral rather than warm — the redesign follows them on both counts.

## Identity

### The Ember mascot is removed

Decision: the mascot does not survive the redesign. The app reads as adult, quiet and focused; an illustrated character contradicts both references.

Consequences to handle in implementation:

| Orphaned by removal | Introduced in | Needs |
|---|---|---|
| Launcher icon | `ba0149c` | A non-illustrated mark |
| Empty library and empty deck states | `44ffb23` | Typographic empty states |
| Session-finished screen | `21a12ac` | A non-illustrated completion moment |
| Study empty state | `21a12ac` | Typographic empty state |

### The palette goes cold and neutral

Direction chosen on 2026-08-27 after comparing four candidates on the same screen: **Instrument** — the Starlink-leaning direction.

The first draft kept the warm Ember palette on the argument that the accent was the only remaining carrier of identity once the mascot was gone. That was wrong for this product. Both stated references are cold and neutral, and warm-tinted greys with a terracotta accent read as neither. Keeping them would also have preserved the exact look the owner said never convinced them.

The Ember palette in `app/src/main/kotlin/com/emm/hello/core/theme/Color.kt` is replaced, not renamed.

## Color rules

The table below is the **migration target**, not a permanent reference. Once these values land in `core/theme/Color.kt`, that file is the source of truth and this table is superseded — do not read hex codes from here to write code, and do not maintain both.

| Role | Value | Note |
|---|---|---|
| Background | `#08090A` | Cold near-black, **not** pure black |
| Surface | `#101315` | Cards, inputs |
| Surface raised | `#181B1E` | Chips, secondary controls |
| Border / divider | `rgba(255,255,255,0.08)` | Hairlines only |
| Primary text | `#F2F5F7` | Off-white, **not** pure white |
| Body text | `#DDE3E8` | |
| Muted | `#79838B` | Labels, secondary copy |
| Faint | `rgba(255,255,255,0.45)` | Metadata |
| Accent | `#6BA3D6` | Steel blue, desaturated |
| Accent soft | `rgba(107,163,214,0.14)` | Selected states |
| On accent | `#06181F` | Text on the accent fill |

Radii: 12px on containers, 4px on controls. Small radii carry the instrument feel; the previous 8/12/100 set softens it.

Pure black paired with pure white causes halation — text appears to bleed and vibrate, markedly worse for readers with astigmatism. Saturated accents on dark backgrounds increase visual fatigue, which is why Material 3 asks for desaturated accents in dark themes. Both values above respect that.

Hue choice beyond that is **identity, not pedagogy**. Claims that a given hue improves focus or motivation have weak, poorly-replicated, culturally-variable support. Do not justify a color decision with them.

### Rule 1 — grade buttons carry no semantic color

Neither grade button is red or green. They are differentiated by fill weight and position only: the left one sits on the page background with a hairline border, the right one on `#181B1E`.

Rationale, and this is the most important rule in this document: **in spaced repetition, forgetting is the mechanism working, not an error.** Failing retrieval and then re-encountering the word is precisely where learning happens. Painting "No la sabía" as an error marks the honest answer as failure, and users respond by lying to the algorithm — pressing "la sabía" to avoid the red. That feeds FSRS false input, which corrupts scheduling, which damages real learning. The cost is data integrity, not aesthetics.

Secondary reason: red/green coding fails for roughly 8% of men (red-green color deficiency), and WCAG 1.4.1 requires that color never be the sole carrier of meaning.

### Rule 2 — one accent job per screen

The accent marks the primary action: the Hoy CTA, "Ver respuesta", "Guardar", the active tab, a selected chip. It appears **nowhere on the study back face** — at the moment of self-grading nothing should stand out.

The study progress bar is `rgba(255,255,255,0.45)`, not accent, so it never competes with the primary action.

### Rule 3 — semantic colors are for the system, not for self-assessment

Success, warning and destructive colors stay for genuine system states: load errors, destructive actions, warnings. They are never used to score the user's own recall.

## Typography

Two families, down from three. **Instrument Serif is dropped**: a display serif pulls the app toward editorial warmth, which is the opposite of the chosen direction.

| Family | Role |
|---|---|
| Geist (`geist.xml`) | Everything readable. Headlines at weight 600 with `-0.02em` to `-0.03em` tracking, body and labels at 400/500. |
| Geist Mono (`geist_mono.xml`) | All metadata: eyebrows, IPA, counters, due dates, the session count. Uppercase at `+0.14em`. |

Hierarchy is expressed by family, size, weight and tracking — not by color. Mono carries more weight here than in the first draft: precise metadata is a large part of what makes the direction read as an instrument.

Consequence for implementation: `instrument_serif_regular.ttf` and `instrument_serif_italic.ttf` become dead assets, and every `displayLarge` / `displayMedium` / `displaySmall` / `headline*` role in `Type.kt` is re-pointed to Geist.

## Surfaces

The three surfaces come from `RESTRUCTURE_PLAN.md`. This brief defines what each one leads with.

### Hoy — the hero is the action

One dominant element that says what is due and enters the session in one tap.

Explicitly rejected, with reasons:

- **A large metric as the hero.** Starlink makes numbers the hero because looking *is* the task there. In Hello the task is studying. A non-tappable number competes with the primary button, and on a broken streak it punishes the user at the exact moment they returned.
- **A preview of the due cards.** It spoils retrieval — seeing the word before attempting recall destroys the review — and forces a decision the app should be making.

The due count lives *inside* the primary element as supporting context (`23 tarjetas · ~6 min`), in mono. Progress and metrics move below the fold, or after the session, where they reward instead of intimidate.

### Hoy with zero due — the day that decides retention

An empty Hoy is the day the app gets uninstalled. It must offer something, and that something must not turn Hello into a course.

Flow: the AI proposes a **situation** in natural language (e.g. "ordering in a café") and offers ~6 candidate words; **the user picks** which ones to add.

- Picking, not auto-adding, is the point. A word you chose has a memory hook; a word an algorithm handed you does not. It also preserves the personal-capture model instead of replacing it.
- The situation is **derived, not generic**: the prompt receives the user's last 15–20 captured words (~40 tokens) so the model infers level and domain and proposes vocabulary one step above current level. In language acquisition, material only helps when it sits just above the learner's level — and a generic catalog cannot calibrate that. Level calibration *is* the product here.
- Cold start is already solved: `data/.../seed/DefaultSeedDataInitializer.kt` seeds a starter deck on first run, so there are always words to infer from. No generic fallback is required.
- Infrastructure exists: `data/.../flashcard/GeminiService.kt` and `DailyGenerationQuota.kt` (50 generations/day). One suggestion costs one generation.

### Capturar

Unchanged from `RESTRUCTURE_PLAN.md`: one field, type or dictate, save. AI enrichment runs in the background.

### Biblioteca

The one surface where Notion-style density is appropriate — it is a browsing and search surface, not a focus surface.

## The study card

The most-seen screen in the app. Everything else is seen for seconds.

**Do not copy Anki's card layout.** Anki's value is its scheduler, and Hello already runs FSRS-6, which outperforms Anki's default SM-2. Anki dumps every field at once because it is a format-agnostic engine that does not know what is on the card. Hello *does* know — `word`, `translation`, `meaning`, `phonetic`, `irregularForms`, `usagePattern` are typed. Copying the generic dump copies Anki's weakest part.

### Why the current back face fails

Today it renders six elements with no dominant one: a `SIGNIFICADO` eyebrow, the translation, the IPA, the English meaning in italic, irregular forms and `usagePattern`. The user is asked to read a spec sheet at the exact moment they must make a fast binary judgement about their own memory. That load produces careless grades, which feed FSRS bad data.

Worse for the product goal: **there is no example sentence.** The back is a dictionary entry, and dictionary entries do not teach usage. You learn that `leverage` means "aprovechar" and still cannot use it in a sentence. The design optimizes recognition, not English.

### Two beats

| Beat | Content | Job |
|---|---|---|
| 1 — Verdict | The translation, alone, large, nothing competing | Readable in under a second. This is what the grade is judged against. |
| 2 — Lesson | The example sentence with the target word highlighted, plus its translation | The word in context is what actually builds English. |

Everything else — IPA, part of speech, English meaning, irregular forms — compresses into a single small mono reference line that is **always visible**. Do not fold it behind a tap: a tap during grading is friction, and in a study app what is collapsed is never opened. The line is small enough that hiding it saves no space and only buries it.

`example` / `exampleTranslation` / `partOfSpeech` are additive fields already scheduled as Phase 1 work unit 3 in `RESTRUCTURE_PLAN.md`. Beat 2 depends on that work unit.

This composition needs no color to work: hierarchy comes from typography (Notion) and air comes from a single dominant element (Starlink).

## Review direction

Direction is decided by **card maturity**, not by the user and not by a setting.

| Card maturity | Direction | Trains |
|---|---|---|
| Young | English → Spanish | Recognition |
| Mature | Spanish → English | Production |

Rationale: the two are asymmetric. Practising production yields recognition as a by-product; practising recognition does not yield production. A recognition-only app teaches you to understand English and leaves you unable to produce it. But production from day one spikes the failure rate and drives abandonment — the same effect that burns out Anki users who enable reverse cards wholesale.

Constraints this design respects:

- **One flashcard = one review = one grade stays intact.** Direction is a property of that single review, not a second scheduled item. This does not reintroduce the N-items-per-card problem removed in `64cfe69`.
- **No typed input.** Production means recalling silently and revealing, using the existing mechanic. Typing on mobile is slow and penalises spelling rather than memory — failing `recieve` while knowing the word perfectly. Do not reintroduce what commit `64cfe69` removed.
- **The threshold uses existing data.** `FsrsCard` already exposes `state`, `stability`, `reps`, `lapses`. One knob: `state == REVIEW` plus a stability threshold.

### Known cost — graduation must latch

If direction is recomputed on every review, the card oscillates forever: it matures, switches to production, fails because production is harder, stability collapses, it drops back to recognition, matures again, fails again. It never graduates.

Graduation must therefore be **one-way**: once a card moves to production it stays there, even after failures. That requires a new persisted flag on the flashcard — a schema migration. This is the real cost of the decision and it is accepted knowingly.

## Open items for the canvas

Decisions this brief deliberately does not make:

1. **Density on primary surfaces.** Notion is dense, Starlink is spacious, and Hoy/Study must pick a point between them. Proposal to validate visually: Starlink-level air on Hoy and Study, Notion-level density on Biblioteca. Not yet decided.
2. **The launcher mark** replacing the ember icon.
3. **The completion moment** for a finished session, now that the celebrating mascot is gone.
4. **How the token migration is staged.** The `ember*` names now hold cold values, so the rename is no longer cosmetic — decide whether to rename in the same commit as the value swap or after.

## Rule

This brief owns visual direction and design rationale. It does not override `RESTRUCTURE_PLAN.md` on phase scope or order, and it never overrides `*_CURRENT.md` or the code on current behavior. When a design decision here is implemented, the corresponding `*_CURRENT.md` is updated afterwards.
