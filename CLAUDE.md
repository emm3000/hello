# CLAUDE.md

Manifest Claude Code para este repo. **Reglas operativas: `AGENTS.md`** (source of truth).

## Orden de lectura

1. `README.md` · 2. `AGENTS.md` · 3. `ARCHITECTURE.md` · 4. `LOCAL_FIRST.md` · 5. `docs/README.md`

## Slash commands custom (`.claude/commands/`)

- `/checks` — `./gradlew detekt` + `testDebugUnitTest`, agrupa fallas por módulo.
- `/feature <Name>` — scaffold MVI completo (`UiState/UiIntent/UiEffect/ViewModel/Route/Screen`).
- `/agents-review` — review del diff pendiente contra reglas de `AGENTS.md`.
- `/h-component <Name>` — scaffold de componente `H*` en `core/ui/`.

## Gotchas que se rompen seguido

(reglas completas en `AGENTS.md`)

- Producto **local-first single-device**. No asumir sync, pairing ni bootstrap remoto.
- `:domain` es **JVM-only**. Sin Android, DB ni network.
- UI: solo `core/ui/H*`. **Nunca** Material3 raw (`OutlinedTextField`, `Button`, etc.).

## Regla final

Si una doc contradice el código actual, manda el código y luego se actualiza la doc.
