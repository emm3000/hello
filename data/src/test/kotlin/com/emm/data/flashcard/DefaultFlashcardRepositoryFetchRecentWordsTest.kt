package com.emm.data.flashcard

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DefaultFlashcardRepositoryFetchRecentWordsTest {

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
    fun `fetchRecentWords returns words newest-first, excludes deleted and honors the limit`() = runTest {
        val deckId = insertDeck()
        insertFlashcard(deckId, word = "oldest", createdAt = 1_000L)
        insertFlashcard(deckId, word = "middle", createdAt = 2_000L)
        insertFlashcard(deckId, word = "newest", createdAt = 3_000L)
        insertFlashcard(deckId, word = "deleted", createdAt = 4_000L, deletedAt = 4_500L)

        val recentWords = subject.fetchRecentWords(limit = 2)

        assertEquals(listOf("newest", "middle"), recentWords)
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

    private fun insertFlashcard(
        deckId: String,
        word: String,
        createdAt: Long,
        deletedAt: Long? = null,
    ): String {
        val id = UUID.randomUUID().toString()
        db.flashcardQueries.create(
            id = id,
            deckId = deckId,
            word = word,
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
            createdAt = createdAt,
            updatedAt = createdAt,
            deletedAt = deletedAt,
        )
        return id
    }
}
