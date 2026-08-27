---
description: Run detekt and unit tests, summarize failures by module
allowed-tools: Bash, Read
---

Run the standard pre-commit checks for this Android repo:

1. `./gradlew detekt` — report any style violations.
2. `./gradlew testDebugUnitTest :domain:test` — report any failing tests. Note: `testDebugUnitTest` is an Android task and does NOT run the JVM `:domain` module tests, so `:domain:test` must be listed explicitly or domain coverage silently rots.

Group findings by module (`:app`, `:data`, `:domain`). For each violation include `file:line` and the rule/test name.

Do NOT fix anything in this turn — only report. End with one line: **"ready to commit"** if both pass, or a short list of what to fix next.
