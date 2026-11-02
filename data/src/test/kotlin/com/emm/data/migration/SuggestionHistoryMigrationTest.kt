package com.emm.data.migration

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import java.sql.SQLException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

internal const val PRE_SUGGESTION_HISTORY_SCHEMA_VERSION: Long = 11L

class SuggestionHistoryMigrationTest {

    private fun createSchema11Driver(): JdbcSqliteDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            """
                CREATE TABLE WordSuggestionBatch (
                    singletonId INTEGER NOT NULL PRIMARY KEY CHECK (singletonId = 1),
                    situation TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent(),
            0,
        )
        driver.execute(
            null,
            """
                CREATE TABLE SuggestedWordCandidate (
                    position INTEGER NOT NULL PRIMARY KEY,
                    batchId INTEGER NOT NULL REFERENCES WordSuggestionBatch(singletonId) ON DELETE CASCADE,
                    word TEXT NOT NULL,
                    translation TEXT NOT NULL
                )
            """.trimIndent(),
            0,
        )
        return driver
    }

    private fun applyMigration(driver: JdbcSqliteDriver) {
        HelloDb.Schema.migrate(
            driver = driver,
            oldVersion = PRE_SUGGESTION_HISTORY_SCHEMA_VERSION,
            newVersion = HelloDb.Schema.version,
        )
    }

    private fun insertBatch(driver: JdbcSqliteDriver, situation: String, createdAt: Long) {
        driver.execute(
            identifier = null,
            sql = "INSERT INTO WordSuggestionBatch (singletonId, situation, createdAt) VALUES (1, ?, ?)",
            parameters = 2,
        ) {
            bindString(0, situation)
            bindLong(1, createdAt)
        }
    }

    private fun insertCandidate(driver: JdbcSqliteDriver, position: Long, word: String, translation: String) {
        driver.execute(
            identifier = null,
            sql = "INSERT INTO SuggestedWordCandidate (position, batchId, word, translation) VALUES (?, 1, ?, ?)",
            parameters = 3,
        ) {
            bindLong(0, position)
            bindString(1, word)
            bindString(2, translation)
        }
    }

    private fun insertHistory(driver: JdbcSqliteDriver, kind: String, text: String, shownAt: Long) {
        driver.execute(
            identifier = null,
            sql = "INSERT INTO SuggestionHistory (kind, text, shownAt) VALUES (?, ?, ?)",
            parameters = 3,
        ) {
            bindString(0, kind)
            bindString(1, text)
            bindLong(2, shownAt)
        }
    }

    private fun queryBatches(driver: JdbcSqliteDriver): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()
        driver.executeQuery(
            identifier = null,
            sql = "SELECT singletonId, situation, createdAt, firstSeenAt FROM WordSuggestionBatch",
            parameters = 0,
            mapper = { cursor ->
                while (cursor.next().value) {
                    result += mapOf(
                        "singletonId" to cursor.getLong(0),
                        "situation" to cursor.getString(1),
                        "createdAt" to cursor.getLong(2),
                        "firstSeenAt" to cursor.getLong(3),
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

    private fun queryHistory(driver: JdbcSqliteDriver): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()
        driver.executeQuery(
            identifier = null,
            sql = "SELECT kind, text, shownAt FROM SuggestionHistory ORDER BY shownAt",
            parameters = 0,
            mapper = { cursor ->
                while (cursor.next().value) {
                    result += mapOf(
                        "kind" to cursor.getString(0),
                        "text" to cursor.getString(1),
                        "shownAt" to cursor.getLong(2),
                    )
                }
                QueryResult.Unit
            },
        )
        return result
    }

    @Test
    fun `a stored batch and its candidates survive the migration unseen`() {
        val driver: JdbcSqliteDriver = createSchema11Driver()
        insertBatch(driver, situation = "job interview", createdAt = 1_700_000_000_000L)
        insertCandidate(driver, position = 0L, word = "onboarding", translation = "incorporacion")
        insertCandidate(driver, position = 1L, word = "leverage", translation = "aprovechar")

        applyMigration(driver)

        val batch: Map<String, Any?> = queryBatches(driver).single()
        assertEquals(1L, batch["singletonId"])
        assertEquals("job interview", batch["situation"])
        assertEquals(1_700_000_000_000L, batch["createdAt"])
        assertNull(batch["firstSeenAt"])

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
    fun `a SuggestionHistory row inserts and reads back after the migration`() {
        val driver: JdbcSqliteDriver = createSchema11Driver()

        applyMigration(driver)

        insertHistory(driver, kind = "SITUATION", text = "job interview", shownAt = 1_700_000_000_000L)
        insertHistory(driver, kind = "ITEM", text = "leverage", shownAt = 1_700_000_000_001L)

        val history: List<Map<String, Any?>> = queryHistory(driver)
        assertEquals(
            listOf(
                mapOf("kind" to "SITUATION", "text" to "job interview", "shownAt" to 1_700_000_000_000L),
                mapOf("kind" to "ITEM", "text" to "leverage", "shownAt" to 1_700_000_000_001L),
            ),
            history,
        )
    }

    @Test
    fun `a SuggestionHistory row with an unknown kind is rejected`() {
        val driver: JdbcSqliteDriver = createSchema11Driver()

        applyMigration(driver)

        val failure: SQLException = assertThrows(SQLException::class.java) {
            insertHistory(driver, kind = "PHRASE", text = "leverage", shownAt = 1_700_000_000_000L)
        }

        assertTrue(
            "expected a CHECK constraint failure, got ${failure.message}",
            failure.message.orEmpty().contains("CHECK constraint failed"),
        )
        assertEquals(emptyList<Map<String, Any?>>(), queryHistory(driver))
    }
}
