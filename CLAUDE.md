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
