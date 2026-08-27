package com.emm.data.flashcard

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class EnrichmentBacklogQueryTest {

    private lateinit var db: HelloDb

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        seed()
    }

    @Test
    fun `countsByEnrichmentStatus counts pending and failed apart`() {
        val counts = db.flashcardQueries.countsByEnrichmentStatus().executeAsOne()

        assertEquals(2L, counts.pending)
        assertEquals(1L, counts.failed)
    }

    @Test
    fun `countsByEnrichmentStatus ignores soft-deleted cards`() {
        db.flashcardQueries.softDelete(now = 2, id = "card-pending-b")

        val counts = db.flashcardQueries.countsByEnrichmentStatus().executeAsOne()

        assertEquals(1L, counts.pending)
        assertEquals(1L, counts.failed)
    }

    @Test
    fun `findIdsByEnrichmentStatus returns only the matching cards`() {
        val ids: List<String> = db.flashcardQueries
            .findIdsByEnrichmentStatus(enrichmentStatus = "FAILED")
            .executeAsList()

        assertEquals(listOf("card-failed"), ids)
    }

    @Test
    fun `markPendingEnrichment moves the given cards to pending`() {
        db.flashcardQueries.markPendingEnrichment(updatedAt = 5, ids = listOf("card-failed"))

        val counts = db.flashcardQueries.countsByEnrichmentStatus().executeAsOne()

        assertEquals(3L, counts.pending)
        assertEquals(0L, counts.failed)
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
        insertFlashcard(id = "card-pending-a", enrichmentStatus = "PENDING")
        insertFlashcard(id = "card-pending-b", enrichmentStatus = "PENDING")
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
    }
}
