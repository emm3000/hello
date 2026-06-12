package com.emm.data.flashcard

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Verifies the restoreFlashcard / restoreExamplesByFlashcard SQLDelight queries in isolation.
 * Tests that:
 * - Matching-timestamp rows are restored.
 * - Non-matching-timestamp rows (deleted earlier) are left intact.
 */
class RestoreFlashcardQueryTest {

    private lateinit var db: HelloDb

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        insertDeck("deck-1")
    }

    @Test
    fun `restoreFlashcard restores card and examples with matching timestamp`() {
        val cardId = insertFlashcard("deck-1")
        val exampleId = insertExample(cardId)
        val deleteTs = 5_000_000L

        db.flashcardQueries.softDelete(now = deleteTs, id = cardId)
        db.flashcardExampleQueries.softDelete(now = deleteTs, id = exampleId)

        db.flashcardQueries.restoreFlashcard(id = cardId, deletedAt = deleteTs)
        db.flashcardQueries.restoreExamplesByFlashcard(flashcardId = cardId, deletedAt = deleteTs)

        assertNull(db.flashcardQueries.findById(cardId).executeAsOne().deletedAt)
        assertNull(db.flashcardExampleQueries.findById(exampleId).executeAsOne().deletedAt)
    }

    @Test
    fun `restoreFlashcard does NOT restore an example deleted earlier with a different timestamp`() {
        val cardId = insertFlashcard("deck-1")
        val earlyExampleId = insertExample(cardId)
        val lateExampleId = insertExample(cardId)

        val earlyTs = 1_000_000L
        val lateTs = 5_000_000L

        // Early example deleted before the card cascade
        db.flashcardExampleQueries.softDelete(now = earlyTs, id = earlyExampleId)

        // Card + late example deleted together
        db.flashcardQueries.softDelete(now = lateTs, id = cardId)
        db.flashcardExampleQueries.softDelete(now = lateTs, id = lateExampleId)

        // Restore using the cascade timestamp
        db.flashcardQueries.restoreFlashcard(id = cardId, deletedAt = lateTs)
        db.flashcardQueries.restoreExamplesByFlashcard(flashcardId = cardId, deletedAt = lateTs)

        // Card and late example restored
        assertNull(db.flashcardQueries.findById(cardId).executeAsOne().deletedAt)
        assertNull(db.flashcardExampleQueries.findById(lateExampleId).executeAsOne().deletedAt)

        // Early example still deleted (different timestamp)
        assertEquals(earlyTs, db.flashcardExampleQueries.findById(earlyExampleId).executeAsOne().deletedAt)
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private fun insertDeck(deckId: String) {
        db.deckQueries.insert(
            id = deckId,
            name = "Test deck",
            description = null,
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = null,
        )
    }

    private fun insertFlashcard(deckId: String): String {
        val id = UUID.randomUUID().toString()
        db.flashcardQueries.create(
            id = id,
            deckId = deckId,
            word = "word",
            meaning = "meaning",
            translation = null,
            phonetic = null,
            partOfSpeech = null,
            type = null,
            note = null,
            register = null,
            levelBand = null,
            domain = null,
            lemma = null,
            whyUseful = null,
            usagePattern = null,
            irregularFormsJson = null,
            collocationsJson = null,
            commonMistake = null,
            confusableWithJson = null,
            clozeSentence = null,
            sourceContext = null,
            warningsJson = null,
            studyCardsJson = null,
            qualityChecksJson = null,
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = null,
        )
        return id
    }

    private fun insertExample(flashcardId: String): String {
        val id = UUID.randomUUID().toString()
        db.flashcardExampleQueries.insert(
            id = id,
            flashcardId = flashcardId,
            text = "example",
            translation = "",
            type = "",
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = null,
        )
        return id
    }
}
