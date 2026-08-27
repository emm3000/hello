package com.emm.data.flashcard

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.domain.flashcard.Example
import com.emm.domain.ids.toFlashcardId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DefaultFlashcardRepositoryUpsertExamplesTest {

    private lateinit var db: HelloDb
    private lateinit var subject: DefaultFlashcardRepository

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        subject = DefaultFlashcardRepository(
            db = db,
            json = Json,
            ioDispatcher = Dispatchers.IO,
        )
    }

    @Test
    fun `upsertExamples called twice for the same flashcard leaves exactly one visible example`() = runTest {
        val deckId: String = insertDeck()
        val cardId: String = insertFlashcard(deckId)
        val examples: List<Example> = listOf(
            Example(exampleId = "", text = "example", translation = "ejemplo", type = "main"),
        )

        subject.upsertExamples(examples, cardId.toFlashcardId())
        subject.upsertExamples(examples, cardId.toFlashcardId())

        val visibleExamples = db.flashcardExampleQueries
            .findByFlashcardId(cardId)
            .executeAsList()
            .filter { it.deletedAt == null }

        assertEquals(1, visibleExamples.size)
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
