# 01: Suggest: history table and seen marker schema

Status: done
Device: none
Blocked by: None
**Branch:** feat/01-suggestion-history-schema

Schema for `.scratch/daily-words/spec.md` decisions 3 and 4. Gate row 4 (SQLDelight migration), wave of one: read `.claude/rules/sqldelight.md` before the first edit.
In `data/src/main/sqldelight/com/emm/data/Suggestion.sq`: a new table `SuggestionHistory` (`kind TEXT NOT NULL` with `CHECK (kind IN ('SITUATION', 'ITEM'))`, `text TEXT NOT NULL`, `shownAt INTEGER NOT NULL`, an index on `shownAt`), and a nullable `firstSeenAt INTEGER` appended as the last column of `WordSuggestionBatch`.
The current schema is 11 (`10.sqm` → `databases/11.db`), so this change ships `11.sqm` and `databases/12.db`. Queries that read or write the new table and column belong to ticket 03, not here.
Pattern to follow: `data/src/test/kotlin/com/emm/data/migration/SuggestionCacheMigrationTest.kt`.

## Acceptance
- [x] `fd '^(11\.sqm|12\.db)$' data/src/main/sqldelight` prints both files
- [x] `./gradlew :data:verifySqlDelightMigration` passes
- [x] `./gradlew :data:testDebugUnitTest --tests '*SuggestionHistoryMigrationTest'` passes. It builds schema 11 by hand, inserts a batch with two candidates, migrates to `HelloDb.Schema.version`, then asserts the batch and candidates survive with `firstSeenAt` NULL and a `SuggestionHistory` row inserts and reads back
- [x] With the test's old-version constant set to 12, both cases fail. The constant is then restored (the non-vacuous check from the rule)
- [x] `./gradlew :data:testDebugUnitTest --tests '*DefaultWordSuggestionCacheTest'` still passes without edits
- [x] `scripts/check` green

## Comments

- Done in 3ec9e066. `fd '^(11\.sqm|12\.db)$' data/src/main/sqldelight` prints both files; snapshots 1-11 are unchanged.
- `./gradlew :data:verifySqlDelightMigration :data:testDebugUnitTest --tests '*SuggestionHistoryMigrationTest' --tests '*DefaultWordSuggestionCacheTest' --tests '*SuggestionCacheMigrationTest'`: exit 0, 3/4/6 tests, no failures. `DefaultWordSuggestionCacheTest` was not edited.
- `SuggestionHistoryMigrationTest` has three cases: the batch and two candidates survive with `firstSeenAt` NULL, a `SuggestionHistory` row inserts and reads back, and a kind outside `SITUATION`/`ITEM` fails the CHECK constraint.
- Non-vacuous check: with `PRE_SUGGESTION_HISTORY_SCHEMA_VERSION` set to 12, all three cases fail (`no such column: firstSeenAt`, `no such table: SuggestionHistory`, and the CHECK assertion seeing `no such table`). The constant was restored to 11.
- `scripts/check` green in the pre-commit hook (detekt, testDebugUnitTest, :domain:test, :data:verifySqlDelightMigration).
