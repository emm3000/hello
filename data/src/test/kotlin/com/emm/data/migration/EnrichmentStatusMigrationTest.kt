package com.emm.data.migration

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import org.junit.Assert.assertEquals
import org.junit.Test

class EnrichmentStatusMigrationTest {

    @Test
    fun `existing cards are marked enriched after migration`() {
        val driver: JdbcSqliteDriver = createSchema2Driver()
        insertLegacyFlashcard(driver, id = "card-legacy")

        HelloDb.Schema.migrate(driver = driver, oldVersion = 2L, newVersion = HelloDb.Schema.version)

        assertEquals(listOf("ENRICHED"), readEnrichmentStatuses(driver))
    }

    private fun createSchema2Driver(): JdbcSqliteDriver {
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
                    PRIMARY KEY (id)
                )
            """.trimIndent(),
            0,
        )
        return driver
    }

    private fun insertLegacyFlashcard(driver: JdbcSqliteDriver, id: String) {
        driver.execute(
            identifier = null,
            sql = "INSERT INTO Flashcard (id, deckId, word, meaning, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?)",
            parameters = 6,
        ) {
            bindString(0, id)
            bindString(1, "deck-1")
            bindString(2, "borrow")
            bindString(3, "to take something and return it later")
            bindLong(4, 1L)
            bindLong(5, 1L)
        }
    }

    private fun readEnrichmentStatuses(driver: JdbcSqliteDriver): List<String> {
        val statuses = mutableListOf<String>()
        driver.executeQuery(
            identifier = null,
            sql = "SELECT enrichmentStatus FROM Flashcard",
            parameters = 0,
            mapper = { cursor ->
                while (cursor.next().value) {
                    statuses += cursor.getString(0).orEmpty()
                }
                QueryResult.Unit
            },
        )
        return statuses
    }
}
