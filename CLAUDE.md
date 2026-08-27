# CLAUDE.md

Operating manifest for this repo. Loaded in every session.

## Mandatory state

The product runs in **local-first single-device** mode. Don't assume active remote sync, pairing, remote bootstrap or multi-device.

`HelloDb` is the source of truth for reads and writes.

## Modules

Three modules, and these dependencies only:

```
app -> data
app -> domain
data -> domain
```

- `:app` — UI, navigation, DI, startup
- `:data` — repositories, SQLDelight, local identity, Firebase AI
- `:domain` — **JVM-only** models and use cases. No Android, no DB, no network.

## Non-negotiable rules

These bind on every change, including a new file created before any Kotlin has been read.

- **No comments.** No KDoc, no `//`, no banners, no commented-out code. The code explains itself or it gets renamed. Three narrow exceptions in `.claude/rules/kotlin-style.md`.
- **Explicit types** on every property and local `val` / `var`, and the supertype when the abstraction is what matters. Omit only when the right-hand side is a constructor call that already names the type.
- **Only `core/ui/H*` components** in feature screens. Never raw Material3.
- **MVI per feature**: one `UiState` (all `val`), one `onIntent(intent)` entry point, effects consumed once and never stored in state.
- **`:domain` stays JVM-only.** If it needs to reach outward, invert with an interface in `:domain`.
- **`./gradlew detekt` and `testDebugUnitTest` green** before every commit.
- **Never add `Co-Authored-By`** from Claude, Anthropic or any AI assistant to a commit message. Applies to `git commit`, `--amend`, rebases and any generated message flow.

## Detailed rules

Path-scoped, loaded when Kotlin files are touched:

| File | Covers |
|---|---|
| `.claude/rules/architecture.md` | Layer boundaries, dependency inversion, the MVI contract |
| `.claude/rules/naming.md` | Uncle Bob, official Kotlin, naming patterns by layer |
| `.claude/rules/kotlin-style.md` | Explicit types, comment policy, Kotlin idioms, detekt |
| `.claude/rules/principles.md` | YAGNI, KISS, SOLID, DRY with its caveat, what is rejected |
| `.claude/rules/ui-components.md` | The `core/ui` `H*` iron rule, theme tokens |

## Work protocol

How work is orchestrated in this repo. Loaded every session, applies to every change regardless of which files it touches.

Every change is a **work unit**: a scope, a falsifier and a topology, all three declared *before* any writer runs.

### 1. Declare the falsifier before delegating

Write down what would prove the unit wrong. If you cannot name it, you do not understand the unit yet.

The falsifier is matched to the failure mode. It is never uniform:

| Failure mode | What proves it | What does not |
|---|---|---|
| Transcription — values, renames, moves | Read the diff against the spec; a targeted `rg` returning zero | detekt and tests: green passes a wrong hex |
| Logic or state | A test that fails before the change and passes after | Reading the code |
| Visual | The screen running on `medium_phone` | Any gate |
| Rule compliance | `/agents-review` against `.claude/rules/` | The writer's own report |

`./gradlew detekt testDebugUnitTest :domain:test` is the floor, never the proof. It shows nothing broke. It never shows the change is right.

### 2. Pick the cheapest actor whose output can be verified

| Work | Actor |
|---|---|
| Decisions, verification, git | This thread. Never delegated. |
| 2+ files with the decisions already made | One writer, cheaper model, full spec in the prompt |
| Understanding spread across 4+ files | One read-only explorer that returns a map, not file dumps |
| Mechanical substitution | `sd` and `rg`. No model at all. |

"No model" is a first-class answer. Where a deterministic tool applies, it beats a probabilistic one on both cost and correctness.

Decisions never travel to a writer as a question. They travel as a spec.

### 3. Verify the artifact, not the report

Agents claim completion they did not deliver. `git status`, `git diff --stat` and `rg` are the evidence. An agent's summary is a hypothesis until an artifact confirms it.

### 4. Approve the unit, not each step

Approval covers scope, falsifier and topology, once. The unit then runs to completion and reports with evidence.

Stop mid-unit only for a genuine fork or a failed falsifier. Never to confirm the next tool call.

### 5. Escalate only where there is judgment to attack

Adversarial review (`judgment-day`) costs up to four judge runs plus a fix actor. It earns that on concurrency, scheduling, data migration and contracts.

It is waste on constants, renames and moves, where the falsifier is already deterministic. A probabilistic reviewer stacked on top of a certain proof trades certainty for opinion.

## Reading order

1. `README.md`
2. `ARCHITECTURE.md`
3. `LOCAL_FIRST.md`
4. `docs/README.md`
5. `docs/DESIGN_BRIEF.md` — visual direction and its rationale

## Custom slash commands

- `/checks` — `./gradlew detekt` + `testDebugUnitTest`, failures grouped by module.
- `/feature <Name>` — full MVI scaffold (`UiState` / `UiIntent` / `UiEffect` / `ViewModel` / `Route` / `Screen`).
- `/agents-review` — review the pending diff against these rules.
- `/h-component <Name>` — scaffold an `H*` component in `core/ui/`.

## Final rule

If a doc contradicts the current code, the code wins and the doc gets updated afterwards.
