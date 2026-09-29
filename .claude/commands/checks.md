---
description: Run detekt and unit tests, summarize failures by module
allowed-tools: Bash(./gradlew:*) Bash(rg:*) Read
disable-model-invocation: true
---

Run the standard pre-commit gate for this Android repo without letting Gradle's output into the context. Redirect it to a log and read back only what decides the outcome:

```
./gradlew detekt testDebugUnitTest :domain:test > /tmp/hello-gate.log 2>&1; echo "exit=$?"
rg -n 'BUILD (SUCCESSFUL|FAILED)|FAILED|^e: |error:' /tmp/hello-gate.log | tail -40
```

`testDebugUnitTest` is an Android task and does NOT run the JVM `:domain` module tests, so `:domain:test` stays listed explicitly or domain coverage silently rots.

Group findings by module (`:app`, `:data`, `:domain`). For each violation include `file:line` and the rule/test name; open the log with Read only around a failure, never whole.

Do NOT fix anything in this turn — only report. End with one line: **"ready to commit"** if the exit is 0 and the log says `BUILD SUCCESSFUL`, or a short list of what to fix next.
