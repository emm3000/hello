package com.emm.data.flashcard

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

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

        db.flashcardExampleQueries.softDelete(now = earlyTs, id = earlyExampleId)

        db.flashcardQueries.softDelete(now = lateTs, id = cardId)
        db.flashcardExampleQueries.softDelete(now = lateTs, id = lateExampleId)

        db.flashcardQueries.restoreFlashcard(id = cardId, deletedAt = lateTs)
        db.flashcardQueries.restoreExamplesByFlashcard(flashcardId = cardId, deletedAt = lateTs)

        assertNull(db.flashcardQueries.findById(cardId).executeAsOne().deletedAt)
        assertNull(db.flashcardExampleQueries.findById(lateExampleId).executeAsOne().deletedAt)

        assertEquals(earlyTs, db.flashcardExampleQueries.findById(earlyExampleId).executeAsOne().deletedAt)
    }

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
            enrichmentStatus = "ENRICHED",
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
