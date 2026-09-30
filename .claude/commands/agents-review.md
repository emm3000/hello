---
description: Manually review the requested change against the repo rules
argument-hint: "[commit or base..head; defaults to pending changes]"
allowed-tools: Bash(git status:*) Bash(git diff:*) Bash(git show:*) Bash(git ls-files:*) Read Grep Glob
disable-model-invocation: true
---

Review the scope in `$ARGUMENTS` against `CLAUDE.md`, the applicable `.claude/rules/` and `LOCAL_FIRST.md`. Quote arguments as data, never evaluate them as shell code. With no argument, use `git status`, `git diff`, `git diff --cached` and read untracked files listed by `git ls-files --others --exclude-standard`. For a commit use `git show`; for a range use `git diff` with that range. This is a manual checklist, not an automatic extra review pass or a commit gate.

## Checklist

1. **Module boundaries**
   - Any file in `:domain/` importing Android, SQLDelight, Firebase, or network?
   - Any file in `:data/` importing `:app`?
   - Allowed dependencies: `app -> data`, `app -> domain`, `data -> domain`.

2. **MVI**
   - New features have `UiState`, `UiIntent`, `UiEffect`, and `onIntent(intent)`?
   - Naming: `*ViewModel`, `*Route`, `*UiState`, `*UiIntent`, `*UiEffect`?

3. **UI**
   - Raw Material3 controls in feature screens? Layout primitives, theme access and non-interactive `Surface` containers are allowed; internals of `core/ui/` may wrap Material3.
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

7. **Verification coverage**
   - Required checks selected from `verification.md`, including backend and migrations when touched?
   - Actual results available, or explicitly reported as not run?

## Output

For each actionable finding: `file:line`, violated rule/behavior, reachable scenario and evidence, then a suggested correction. Separate demonstrated defects from hypotheses; feasible scheduler interleavings count as reachable scenarios. If there are no findings, say `no findings in the inspected scope` and name any missing verification. Do not edit files or claim that checks ran when they did not. Review does not authorize a commit.
