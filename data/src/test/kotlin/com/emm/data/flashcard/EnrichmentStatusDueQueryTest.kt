package com.emm.data.flashcard

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class EnrichmentStatusDueQueryTest {

    private lateinit var db: HelloDb

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        seed()
    }

    @Test
    fun `countDueFlashcards counts only enriched cards`() {
        assertEquals(1L, db.flashcardQueries.countDueFlashcards(now = NOW).executeAsOne())
    }

    @Test
    fun `flashcardsToReviewByDeck returns only enriched cards`() {
        val ids: List<String> = db.flashcardQueries
            .flashcardsToReviewByDeck(deckId = DECK_ID, now = NOW)
            .executeAsList()
            .map { it.id }

        assertEquals(listOf("card-enriched"), ids)
    }

    @Test
    fun `flashcardsToReviewAllDecks returns only enriched cards`() {
        val ids: List<String> = db.flashcardQueries
            .flashcardsToReviewAllDecks(now = NOW)
            .executeAsList()
            .map { it.id }

        assertEquals(listOf("card-enriched"), ids)
    }

    private fun seed() {
        db.deckQueries.insert(
            id = DECK_ID,
            name = "Deck",
            description = null,
            createdAt = 1,
            updatedAt = 1,
            deletedAt = null,
        )
        insertFlashcard(id = "card-enriched", enrichmentStatus = "ENRICHED")
        insertFlashcard(id = "card-pending", enrichmentStatus = "PENDING")
        insertFlashcard(id = "card-failed", enrichmentStatus = "FAILED")
    }

    private fun insertFlashcard(id: String, enrichmentStatus: String) {
        db.flashcardQueries.create(
            id = id,
            deckId = DECK_ID,
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
            enrichmentStatus = enrichmentStatus,
            createdAt = 1,
            updatedAt = 1,
            deletedAt = null,
        )
    }

    private companion object {
        const val DECK_ID = "deck-1"
        const val NOW = 1_000_000L
    }
}
