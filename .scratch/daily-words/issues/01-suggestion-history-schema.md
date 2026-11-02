# 01: Suggest: history table and seen marker schema

Status: ready-for-agent
Device: none
Blocked by: None
**Branch:** feat/01-suggestion-history-schema

Schema for `.scratch/daily-words/spec.md` decisions 3 and 4. Gate row 4 (SQLDelight migration), wave of one: read `.claude/rules/sqldelight.md` before the first edit.
In `data/src/main/sqldelight/com/emm/data/Suggestion.sq`: a new table `SuggestionHistory` (`kind TEXT NOT NULL` with `CHECK (kind IN ('SITUATION', 'ITEM'))`, `text TEXT NOT NULL`, `shownAt INTEGER NOT NULL`, an index on `shownAt`), and a nullable `firstSeenAt INTEGER` appended as the last column of `WordSuggestionBatch`.
The current schema is 11 (`10.sqm` → `databases/11.db`), so this change ships `11.sqm` and `databases/12.db`. Queries that read or write the new table and column belong to ticket 03, not here.
Pattern to follow: `data/src/test/kotlin/com/emm/data/migration/SuggestionCacheMigrationTest.kt`.

## Acceptance
- [ ] `fd '^(11\.sqm|12\.db)$' data/src/main/sqldelight` prints both files
- [ ] `./gradlew :data:verifySqlDelightMigration` passes
- [ ] `./gradlew :data:testDebugUnitTest --tests '*SuggestionHistoryMigrationTest'` passes. It builds schema 11 by hand, inserts a batch with two candidates, migrates to `HelloDb.Schema.version`, then asserts the batch and candidates survive with `firstSeenAt` NULL and a `SuggestionHistory` row inserts and reads back
- [ ] With the test's old-version constant set to 12, both cases fail. The constant is then restored (the non-vacuous check from the rule)
- [ ] `./gradlew :data:testDebugUnitTest --tests '*DefaultWordSuggestionCacheTest'` still passes without edits
- [ ] `scripts/check` green
