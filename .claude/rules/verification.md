# Verification for Hello

Select checks from the complete work-unit scope, including staged, unstaged and new files. For a committed change, use the intended commit or base-to-head range supplied for the task; `git diff HEAD` does not include the last commit or untracked contents.

| Affected area | Required evidence |
|---|---|
| Kotlin or Android build configuration | `./gradlew detekt testDebugUnitTest :domain:test`; add `:app:assembleDebug` for packaging, resources, manifest or dependency changes |
| `data/src/main/sqldelight/**` or migration tests | Read `sqldelight.md` before design; run `./gradlew :data:verifySqlDelightMigration` and the affected migration tests |
| `supabase/functions/**` | Run `deno task check` from `supabase/functions`; this includes formatting, lint, type checking and tests |
| `supabase/migrations/**` | Inspect SQL and exercise the migration and affected queries on a disposable local database; record the target and command. Never reset a shared or production database |
| UI behavior | Exercise the relevant flow on the intended emulator after installation has been authorized; record expected and observed behavior |
| `.claude/**` or `CLAUDE.md` | Validate JSON/frontmatter and references, inspect rule/example consistency; instantiate and compile changed Kotlin templates when feasible |
| Descriptive docs only | Check facts, links and the diff; no extra runtime test solely for prose |

Before any commit, `./gradlew detekt testDebugUnitTest :domain:test` must pass, including for docs/config changes. `testDebugUnitTest` does not run the JVM-only `:domain` tests. This gate is additional to the area-specific checks above.

Run the relevant checks after the last applicable edit. Reuse a successful result only while the checked inputs remain unchanged; do not run the same suite again just because a different agent finished.

Record each command, exit status and decisive result. Use a unique temporary log per invocation to avoid concurrent runs overwriting evidence. Read only the relevant log excerpts. A command that failed to start, a missing dependency or an unavailable device is **not run/blocked**, never a pass.

Keep regression tests proportional to the behavior and existing test conventions. Temporary diagnostics need not become permanent tests. Report unrelated failures without silently fixing them or reducing the requested scope.

Completion requires the requested behavior and its evidence. Checks, review and authorization to commit/push are separate; report each accurately.
