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
- `:data` — repositories, SQLDelight, local identity, Firebase AI, Supabase backend transport
- `:domain` — **JVM-only** models and use cases. No Android, no DB, no network.

## Non-negotiable rules

These bind on every change, including a new file created before any Kotlin has been read.

- **No comments.** No KDoc, no `//`, no banners, no commented-out code. The code explains itself or it gets renamed. Three narrow exceptions in `.claude/rules/kotlin-style.md`.
- **Explicit types** on every property and local `val` / `var`, and the supertype when the abstraction is what matters. Exceptions for constructors, delegates and lambda parameters are in `.claude/rules/kotlin-style.md`.
- **Use `core/ui/H*` controls** in feature screens. Layout primitives, theme access and a non-interactive `Surface` container are allowed; see `.claude/rules/ui-components.md`.
- **MVI per feature**: one `UiState` (all `val`), one `onIntent(intent)` entry point, effects consumed once and never stored in state.
- **`:domain` stays JVM-only.** If it needs to reach outward, invert with an interface in `:domain`.
- **`./gradlew detekt testDebugUnitTest :domain:test` green** before every commit. `:domain` is a JVM module, so `testDebugUnitTest` never reaches it.
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
| `.claude/rules/sqldelight.md` | Schema changes: the three artifacts a migration ships, the migration test |
| `.claude/rules/verification.md` | Which checks each changed area requires, on top of the Gradle gate. Not path-scoped: read it when selecting checks |

That last one is scoped to `data/src/main/sqldelight/**` and `data/src/test/kotlin/com/emm/data/migration/**`, so it loads when a migration or its test is opened, never while you are only writing other Kotlin. **Read it before specifying any schema change**, not after — the spec is written before any of those files is opened, and a migration specified without it reaches the writer already missing its migration test. `checkSqlDelightSnapshots` catches a missing snapshot; nothing catches a missing test.

## Work protocol

This workflow is repository-local and uses Claude Code's native tools, agents and skills. It does not require external review CLIs or receipts.

### Scope and autonomy

For a change, state the scope and evidence of success briefly before editing. A small fix needs a sentence, not a separate specification document. An assessment or question ends with findings; it does not authorize implementation or a commit.

Inspect `git status --short` and the current branch before work. Preserve existing changes. Read relevant code before relying on memory; do not assume local `main`, the upstream or the last session's state.

The main thread writes prose, configuration, templates and one mechanical, already-understood single-file change. Non-trivial Kotlin across 2+ files goes to the `writer` with the decisions already made; exploration across 4+ files goes to the `explorer`; device verification goes to `device-check`. The main thread's context stays for deciding and verifying, not for holding code.

Proceed with reversible steps covered by the request. Ask only when a missing decision materially changes the scope or when an action needs authorization. If part is blocked, finish the independent parts and report the blocker. Do not stop after announcing an available next step.

Keep unrelated cleanup out of the change, even in a file already being edited. For repeated fixes in the same area, identify the invariant and its owner before adding another guard; broaden into a redesign only if the requested behavior requires it or the owner agrees.

### Delegation

| Work | Actor | Model, effort |
|---|---|---|
| Clear, localized implementation; decisions, integration and git | Main thread | Session selection |
| Bounded implementation that benefits from a separate context | `writer` | opus, medium |
| Uncertain behavior or ownership across the codebase | `explorer`, returns path:line evidence | sonnet, medium |
| Mechanical substitution | Deterministic tools plus diff verification | No agent |
| Device or visual verification | `device-check`, returns observations and capture paths | sonnet, medium |
| Feature documentation drift needing a separate reading pass | `docs-keeper`, returns proposed corrections | sonnet, medium |

Give each delegate the goal, owned paths, constraints, success criteria and relevant context. Implementation details can be decided within those boundaries. Agents must report a contradicted assumption with evidence rather than force the code to fit it.

Agents share the checkout: assign disjoint ownership, preserve others' edits, and integrate their results in the main thread. While they run, do independent work without modifying their files. Wait for every required result before declaring completion.

Model and effort defaults live in `.claude/agents/*.md`; do not override them routinely. For a model experiment, record the effective model, effort, correctness, elapsed time and user interventions on comparable tasks before changing defaults.

### Verification and review

Read `.claude/rules/verification.md` and select checks for the changed paths. The pre-commit Gradle gate remains mandatory; it does not replace evidence that the requested behavior works.

Match evidence to the failure mode:

| Change | Evidence |
|---|---|
| Values, renames, moves | Diff against the intended result and targeted searches |
| Logic or state | Focused behavioral test; for a bug, demonstrate the regression where feasible |
| Visual behavior | Exercise the screen on the intended emulator; inspect the capture |
| Instructions, configuration or templates | Parse configuration, inspect consistency and exercise changed executable examples where feasible |

Inspect the final diff and new files, not only an agent's report. A clean textual merge is not evidence that combined behavior is correct. Screenshots remain available for the main thread to inspect when a device report is ambiguous.

Review the finished change once against the applicable rules. `/agents-review` is an optional manual checklist, not a second mandatory pass. For authentication, permissions, data loss, migrations or concurrency, use a focused independent review when available; give it the exact scope and evidence. Do not start additional review services or repeated broad reviews automatically. After a correction, recheck the affected behavior; reopen broader review only for new evidence or a materially changed scope.

A finding needs a reachable scenario, violated invariant and supporting evidence. A race is work when the interleaving is written down — which two writers, which shared state, which order breaks it — even if it cannot be reproduced on demand; it is information when the finding only says it "could race". Separate demonstrated defects from unresolved hypotheses.

### Worktrees

Before creating a worktree, choose and record the intended base commit. Worktree defaults may start from the remote default branch while local work is ahead. Compare the new checkout with the intended base before building or installing; do not automatically rebase unrelated work or discard local changes.

### Communication and completion

Say what you are doing before the first tool call. During longer work, briefly report meaningful findings, blockers or a change of plan. Finish with what changed, the checks and their results, and any remaining limitation. Use plain language.

Commit only when requested or already authorized; use a conventional message without AI attribution. Push only when authorized. Check the exact staged diff before committing and run the required checks against the final content. Passing checks does not itself authorize a commit, push or release.

For a handoff or compaction, preserve the user's scope and constraints, decisions, changed paths, completed checks, unresolved failures and next required action. Keep memory to durable facts; avoid archiving routine transcripts or treating old environment failures as permanent facts.

## Read when relevant

- `README.md` — setup and project overview
- `ARCHITECTURE.md` and `LOCAL_FIRST.md` — boundaries and data behavior
- `docs/README.md` — current feature documentation
- `docs/DESIGN_BRIEF.md` — visual direction for UI changes
- `docs/AI_BACKEND_PLAN.md` — backend context; verify historical plan entries against current code

## Custom slash commands

- `/checks` — run the mandatory gate and checks selected by affected paths; report evidence and failures.
- `/feature <Name>` — full MVI scaffold (`UiState` / `UiIntent` / `UiEffect` / `ViewModel` / `Route` / `Screen`).
- `/agents-review` — review the pending diff against these rules.
- `/h-component <Name>` — scaffold an `H*` component in `core/ui/`.

Personal skills linked from `.agents/` are optional and are not required by this workflow. Shared behavior belongs in the tracked `.claude/` files; keep machine-specific approvals in `.claude/settings.local.json`. Start a new Claude Code session after changing agent definitions or instructions to verify the loaded setup.

## Final rule

Descriptive docs must match current code. If code contradicts a normative rule or the requested behavior, investigate the discrepancy; do not silently rewrite the rule to bless a defect. Update affected descriptive docs within the work unit.
