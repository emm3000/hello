---
description: Self-review pending changes against the repo rules
allowed-tools: Bash, Read, Grep
---

Review the pending changes (staged + unstaged) against `CLAUDE.md`, `.claude/rules/` and `LOCAL_FIRST.md`. Use `git status`, `git diff` and `git diff --cached` to see what changed.

## Checklist

1. **Module boundaries**
   - Any file in `:domain/` importing Android, SQLDelight, Firebase, or network?
   - Any file in `:data/` importing `:app`?
   - Allowed dependencies: `app -> data`, `app -> domain`, `data -> domain`.

2. **MVI**
   - New features have `UiState`, `UiIntent`, `UiEffect`, and `onIntent(intent)`?
   - Naming: `*ViewModel`, `*Route`, `*UiState`, `*UiIntent`, `*UiEffect`?

3. **UI**
   - Any direct use of raw Material3 (`OutlinedTextField`, `Button`, `TextField`)?
   - New shared components use the `H` prefix and live in `core/ui/`?

4. **Detekt (config/detekt/detekt.yml)**
   - Nesting ≤ 3?
   - No nested `also/apply/run/let`?
   - ≤ 5 returns per function (excluding labeled returns)?

5. **Local-first**
   - Any code assuming remote sync, pairing, remote bootstrap, or multi-device?
   - Writes go through `HelloDb`?

6. **Hygiene**
   - Obvious or "what it does" comments instead of "why"?
   - Sensitive files in the diff (`keystore.properties`, `local.properties`, `key/`)?

## Output

For each violation: `file:line` + rule + suggestion on a single line. If everything is clean, reply **"clean — ready to commit"**. Do not edit files in this turn.
