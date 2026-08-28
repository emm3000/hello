package com.emm.data.deck

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.domain.deck.Tag
import com.emm.domain.deck.TagRepository
import com.emm.domain.ids.DeckId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

class RestoreDeckRepositoryTest {

    private lateinit var db: HelloDb
    private lateinit var subject: DefaultDeckRepository

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        subject = DefaultDeckRepository(db, NoOpTagRepository())
    }

    @Test
    fun `softDeleteDeck returns the timestamp used for the cascade`() = runTest {
        val deckId = insertDeck()
        val flashcardId = insertFlashcard(deckId)

        val deletedAt = subject.softDeleteDeck(DeckId.from(deckId))

        val deck = db.deckQueries.findById(deckId).executeAsOne()
        val card = db.flashcardQueries.findById(flashcardId).executeAsOne()
        assertEquals(deletedAt, deck.deletedAt)
        assertEquals(deletedAt, card.deletedAt)
    }

    @Test
    fun `restoreDeck restores deck and cards with matching timestamp`() = runTest {
        val deckId = insertDeck()
        val flashcardId = insertFlashcard(deckId)

        val deletedAt = subject.softDeleteDeck(DeckId.from(deckId))
        subject.restoreDeck(DeckId.from(deckId), deletedAt)

        assertNull(db.deckQueries.findById(deckId).executeAsOne().deletedAt)
        assertNull(db.flashcardQueries.findById(flashcardId).executeAsOne().deletedAt)
    }

    @Test
    fun `restoreDeck does NOT restore a card deleted earlier with a different timestamp`() = runTest {
        val deckId = insertDeck()
        val earlyCardId = insertFlashcard(deckId)

        val earlyTs = 1_000_000L
        db.flashcardQueries.softDelete(now = earlyTs, id = earlyCardId)

        val lateCardId = insertFlashcard(deckId)

        val deletedAt = subject.softDeleteDeck(DeckId.from(deckId))

        subject.restoreDeck(DeckId.from(deckId), deletedAt)

        assertNull(db.deckQueries.findById(deckId).executeAsOne().deletedAt)
        assertNull(db.flashcardQueries.findById(lateCardId).executeAsOne().deletedAt)

        assertEquals(earlyTs, db.flashcardQueries.findById(earlyCardId).executeAsOne().deletedAt)
    }

    @Test
    fun `softDeleteDeck cascades to examples and restoreDeck brings them back`() = runTest {
        val deckId = insertDeck()
        val flashcardId = insertFlashcard(deckId)
        val exampleId = insertExample(flashcardId)

        val deletedAt = subject.softDeleteDeck(DeckId.from(deckId))

        val deletedExample = db.flashcardExampleQueries.findById(exampleId).executeAsOne()
        assertEquals(deletedAt, deletedExample.deletedAt)

        subject.restoreDeck(DeckId.from(deckId), deletedAt)

        assertNull(db.flashcardExampleQueries.findById(exampleId).executeAsOne().deletedAt)
    }

    private class NoOpTagRepository : TagRepository {
        override suspend fun findOrCreate(tags: List<String>): List<Tag> = emptyList()
        override fun fetchTagsForDeck(deckId: DeckId): Flow<List<Tag>> = flowOf(emptyList())
    }

    private fun insertDeck(): String {
        val id = UUID.randomUUID().toString()
        db.deckQueries.insert(
            id = id,
            name = "Test deck",
            description = null,
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
            text = "example text",
            translation = "traducción",
            type = "usage",
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = null,
        )
        return id
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
}
