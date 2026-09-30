---
description: Run the standard gate and area-specific checks for the requested change
argument-hint: "[commit or base..head; defaults to pending changes]"
allowed-tools: Bash(./gradlew:*) Bash(git status:*) Bash(git diff:*) Bash(git show:*) Bash(git ls-files:*) Bash(deno task --cwd supabase/functions check) Bash(mktemp:*) Bash(rg:*) Read Glob
disable-model-invocation: true
---

Read `.claude/rules/verification.md`. This command runs checks and reports evidence; it does not fix files, review code, commit or authorize delivery.

1. Determine the requested scope from `$ARGUMENTS`. With no argument, inspect `git status --short`, `git diff`, `git diff --cached` and new files from `git ls-files --others --exclude-standard`. For a commit use `git show`; for a range use `git diff` with that range. Quote arguments as data; do not evaluate them as shell code. Do not confuse `git diff HEAD` with the last commit.
2. Always run `./gradlew detekt testDebugUnitTest :domain:test`. Add the checks required by the affected areas in `verification.md`; a successful Android gate cannot verify backend changes.
3. Redirect each check to a distinct log created with `mktemp`. Capture its exit status immediately after the command. Report the log path and read only decisive excerpts. Do not infer success from a filtered log or the last command in a pipeline.
4. Continue independent checks after a failure. For a check requiring an unavailable runtime or unauthorized database/device action, report `not run` with the exact reason.

Return one row per check: command, scope, exit status and `passed`, `failed` or `not run`. Include file:line and the test/rule for failures when available. End with `checks passed` only when all required checks passed; otherwise state the failures and missing evidence. Never label the result `ready to commit`.
