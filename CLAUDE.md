# CLAUDE.md

Claude Code manifest for this repo. **Operating rules: `AGENTS.md`** (source of truth).

## Reading order

1. `README.md` · 2. `AGENTS.md` · 3. `ARCHITECTURE.md` · 4. `LOCAL_FIRST.md` · 5. `docs/README.md`

## Custom slash commands (`.claude/commands/`)

- `/checks` — `./gradlew detekt` + `testDebugUnitTest`, groups failures by module.
- `/feature <Name>` — full MVI scaffold (`UiState/UiIntent/UiEffect/ViewModel/Route/Screen`).
- `/agents-review` — review the pending diff against `AGENTS.md` rules.
- `/h-component <Name>` — scaffold an `H*` component in `core/ui/`.

## Common gotchas

(full rules in `AGENTS.md`)

- **Local-first single-device** product. Don't assume sync, pairing or remote bootstrap.
- `:domain` is **JVM-only**. No Android, DB or network.
- UI: only `core/ui/H*`. **Never** raw Material3 (`OutlinedTextField`, `Button`, etc.).

## Final rule

If a doc contradicts the current code, the code wins and the doc gets updated afterwards.
