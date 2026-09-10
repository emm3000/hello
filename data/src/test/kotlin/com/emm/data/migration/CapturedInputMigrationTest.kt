package com.emm.data.migration

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal const val PRE_CAPTURED_INPUT_SCHEMA_VERSION: Long = 9L

class CapturedInputMigrationTest {

    private fun createSchema9Driver(): JdbcSqliteDriver {
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
                    PRIMARY KEY (id)
                )
            """.trimIndent(),
            0,
        )
        return driver
    }

    private fun insertPreMigrationFlashcard(
        driver: JdbcSqliteDriver,
        id: String,
        word: String,
        enrichmentStatus: String,
        enrichmentFailureCode: String?,
    ) {
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO Flashcard (
                    id, deckId, word, meaning, createdAt, updatedAt, enrichmentStatus, enrichmentFailureCode
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            parameters = 8,
        ) {
            bindString(0, id)
            bindString(1, "deck-1")
            bindString(2, word)
            bindString(3, "meaning")
            bindLong(4, 1L)
            bindLong(5, 1L)
            bindString(6, enrichmentStatus)
            bindString(7, enrichmentFailureCode)
        }
    }

    private fun applyMigration(driver: JdbcSqliteDriver) {
        HelloDb.Schema.migrate(
            driver = driver,
            oldVersion = PRE_CAPTURED_INPUT_SCHEMA_VERSION,
            newVersion = HelloDb.Schema.version,
        )
    }

    private fun queryAllFlashcardsWithCapturedInput(driver: JdbcSqliteDriver): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()
        driver.executeQuery(
            identifier = null,
            sql = """
                SELECT id, word, enrichmentStatus, enrichmentFailureCode, capturedInput
                FROM Flashcard
            """.trimIndent(),
            parameters = 0,
            mapper = { cursor ->
                while (cursor.next().value) {
                    result += mapOf(
                        "id" to cursor.getString(0),
                        "word" to cursor.getString(1),
                        "enrichmentStatus" to cursor.getString(2),
                        "enrichmentFailureCode" to cursor.getString(3),
                        "capturedInput" to cursor.getString(4),
                    )
                }
                QueryResult.Unit
            },
        )
        return result
    }

    @Test
    fun `capturedInput column exists and is NULL for every legacy row after migration`() {
        val driver: JdbcSqliteDriver = createSchema9Driver()

        insertPreMigrationFlashcard(
            driver,
            id = "card-a",
            word = "borrow",
            enrichmentStatus = "ENRICHED",
            enrichmentFailureCode = null,
        )
        insertPreMigrationFlashcard(
            driver,
            id = "card-b",
            word = "compelling",
            enrichmentStatus = "FAILED",
            enrichmentFailureCode = "unintelligible",
        )

        applyMigration(driver)

        val rows: List<Map<String, Any?>> = queryAllFlashcardsWithCapturedInput(driver)
        assertEquals(2, rows.size)
        rows.forEach { row ->
            assertNull(
                "capturedInput must be NULL for legacy row ${row["id"]}",
                row["capturedInput"],
            )
        }
    }

    @Test
    fun `a legacy row keeps its word and failure code after migration`() {
        val driver: JdbcSqliteDriver = createSchema9Driver()

        insertPreMigrationFlashcard(
            driver,
            id = "card-unchanged",
            word = "compelling",
            enrichmentStatus = "FAILED",
            enrichmentFailureCode = "unintelligible",
        )

        applyMigration(driver)

        val row: Map<String, Any?> = queryAllFlashcardsWithCapturedInput(driver).single()
        assertEquals("card-unchanged", row["id"])
        assertEquals("compelling", row["word"])
        assertEquals("FAILED", row["enrichmentStatus"])
        assertEquals("unintelligible", row["enrichmentFailureCode"])
    }
}
