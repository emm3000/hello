package com.emm.data.migration

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import java.sql.SQLException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

internal const val PRE_SUGGESTION_CACHE_SCHEMA_VERSION: Long = 10L

class SuggestionCacheMigrationTest {

    private fun createSchema10Driver(): JdbcSqliteDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            """
                CREATE TABLE Flashcard (
                    id TEXT NOT NULL,
                    deckId TEXT NOT NULL,
                    word TEXT NOT NULL,
                    meaning TEXT NOT NULL,
                    translation TEXT,
                    phonetic TEXT,
                    partOfSpeech TEXT,
                    type TEXT,
                    note TEXT,
                    register TEXT,
                    levelBand TEXT,
                    domain TEXT,
                    lemma TEXT,
                    whyUseful TEXT,
                    usagePattern TEXT,
                    irregularFormsJson TEXT,
                    collocationsJson TEXT,
                    commonMistake TEXT,
                    confusableWithJson TEXT,
                    clozeSentence TEXT,
                    sourceContext TEXT,
                    warningsJson TEXT,
                    studyCardsJson TEXT,
                    qualityChecksJson TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    deletedAt INTEGER,
                    enrichmentStatus TEXT NOT NULL DEFAULT 'ENRICHED',
                    enrichmentFailureReason TEXT,
                    promptVersion INTEGER NOT NULL DEFAULT 0,
                    enrichmentFailureCode TEXT,
                    capturedInput TEXT,
                    PRIMARY KEY (id)
                )
            """.trimIndent(),
            0,
        )
        return driver
    }

    private fun applyMigration(driver: JdbcSqliteDriver) {
        HelloDb.Schema.migrate(
            driver = driver,
            oldVersion = PRE_SUGGESTION_CACHE_SCHEMA_VERSION,
            newVersion = HelloDb.Schema.version,
        )
    }

    private fun enableForeignKeys(driver: JdbcSqliteDriver) {
        driver.execute(identifier = null, sql = "PRAGMA foreign_keys = ON", parameters = 0)
    }

    private fun insertBatch(driver: JdbcSqliteDriver, singletonId: Long, situation: String, createdAt: Long) {
        driver.execute(
            identifier = null,
            sql = "INSERT INTO WordSuggestionBatch (singletonId, situation, createdAt) VALUES (?, ?, ?)",
            parameters = 3,
        ) {
            bindLong(0, singletonId)
            bindString(1, situation)
            bindLong(2, createdAt)
        }
    }

    private fun insertCandidate(
        driver: JdbcSqliteDriver,
        position: Long,
        batchId: Long,
        word: String,
        translation: String,
    ) {
        driver.execute(
            identifier = null,
            sql = "INSERT INTO SuggestedWordCandidate (position, batchId, word, translation) VALUES (?, ?, ?, ?)",
            parameters = 4,
        ) {
            bindLong(0, position)
            bindLong(1, batchId)
            bindString(2, word)
            bindString(3, translation)
        }
    }

    private fun deleteBatch(driver: JdbcSqliteDriver) {
        driver.execute(identifier = null, sql = "DELETE FROM WordSuggestionBatch", parameters = 0)
    }

    private fun queryBatches(driver: JdbcSqliteDriver): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()
        driver.executeQuery(
            identifier = null,
            sql = "SELECT singletonId, situation, createdAt FROM WordSuggestionBatch",
            parameters = 0,
            mapper = { cursor ->
                while (cursor.next().value) {
                    result += mapOf(
                        "singletonId" to cursor.getLong(0),
                        "situation" to cursor.getString(1),
                        "createdAt" to cursor.getLong(2),
                    )
                }
                QueryResult.Unit
            },
        )
        return result
    }

    private fun queryCandidatesOrderedByPosition(driver: JdbcSqliteDriver): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()
        driver.executeQuery(
            identifier = null,
            sql = "SELECT position, batchId, word, translation FROM SuggestedWordCandidate ORDER BY position",
            parameters = 0,
            mapper = { cursor ->
                while (cursor.next().value) {
                    result += mapOf(
                        "position" to cursor.getLong(0),
                        "batchId" to cursor.getLong(1),
                        "word" to cursor.getString(2),
                        "translation" to cursor.getString(3),
                    )
                }
                QueryResult.Unit
            },
        )
        return result
    }

    @Test
    fun `both suggestion cache tables are usable after migration`() {
        val driver: JdbcSqliteDriver = createSchema10Driver()

        applyMigration(driver)

        insertBatch(driver, singletonId = 1L, situation = "job interview", createdAt = 1_700_000_000_000L)
        insertCandidate(driver, position = 1L, batchId = 1L, word = "leverage", translation = "aprovechar")
        insertCandidate(driver, position = 0L, batchId = 1L, word = "onboarding", translation = "incorporacion")

        val batch: Map<String, Any?> = queryBatches(driver).single()
        assertEquals(1L, batch["singletonId"])
        assertEquals("job interview", batch["situation"])
        assertEquals(1_700_000_000_000L, batch["createdAt"])

        val candidates: List<Map<String, Any?>> = queryCandidatesOrderedByPosition(driver)
        assertEquals(2, candidates.size)
        assertEquals(0L, candidates[0]["position"])
        assertEquals(1L, candidates[0]["batchId"])
        assertEquals("onboarding", candidates[0]["word"])
        assertEquals("incorporacion", candidates[0]["translation"])
        assertEquals(1L, candidates[1]["position"])
        assertEquals(1L, candidates[1]["batchId"])
        assertEquals("leverage", candidates[1]["word"])
        assertEquals("aprovechar", candidates[1]["translation"])
    }

    @Test
    fun `a WordSuggestionBatch row outside the singleton id is rejected`() {
        val driver: JdbcSqliteDriver = createSchema10Driver()

        applyMigration(driver)

        val failure: SQLException = assertThrows(SQLException::class.java) {
            insertBatch(driver, singletonId = 2L, situation = "job interview", createdAt = 1_700_000_000_000L)
        }

        assertTrue(
            "expected a CHECK constraint failure, got ${failure.message}",
            failure.message.orEmpty().contains("CHECK constraint failed"),
        )
        assertEquals(emptyList<Map<String, Any?>>(), queryBatches(driver))
    }

    @Test
    fun `deleting the batch cascades to its candidates`() {
        val driver: JdbcSqliteDriver = createSchema10Driver()

        applyMigration(driver)
        enableForeignKeys(driver)

        insertBatch(driver, singletonId = 1L, situation = "job interview", createdAt = 1_700_000_000_000L)
        insertCandidate(driver, position = 0L, batchId = 1L, word = "onboarding", translation = "incorporacion")
        insertCandidate(driver, position = 1L, batchId = 1L, word = "leverage", translation = "aprovechar")
        assertEquals(2, queryCandidatesOrderedByPosition(driver).size)

        deleteBatch(driver)

        assertEquals(emptyList<Map<String, Any?>>(), queryBatches(driver))
        assertEquals(emptyList<Map<String, Any?>>(), queryCandidatesOrderedByPosition(driver))
    }

    @Test
    fun `a candidate without its batch is rejected`() {
        val driver: JdbcSqliteDriver = createSchema10Driver()

        applyMigration(driver)
        enableForeignKeys(driver)

        val failure: SQLException = assertThrows(SQLException::class.java) {
            insertCandidate(driver, position = 0L, batchId = 1L, word = "onboarding", translation = "incorporacion")
        }

        assertTrue(
            "expected a FOREIGN KEY constraint failure, got ${failure.message}",
            failure.message.orEmpty().contains("FOREIGN KEY constraint failed"),
        )
        assertEquals(emptyList<Map<String, Any?>>(), queryCandidatesOrderedByPosition(driver))
    }
}
