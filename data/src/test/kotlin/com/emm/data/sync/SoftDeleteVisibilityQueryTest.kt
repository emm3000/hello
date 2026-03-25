package com.emm.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SoftDeleteVisibilityQueryTest {

    private lateinit var db: HelloDb

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
    }

    @Test
    fun `read queries hide soft deleted rows`() {
        seedDecks()
        seedFlashcards()
        seedQuotes()

        assertEquals(listOf("deck-active"), db.deckQueries.all().executeAsList().map { it.id })
        assertEquals(1, db.deckQueries.deckWithFlashcardCount().executeAsOne().flashcardCount)
        assertEquals(
            listOf("card-active"),
            db.flashcardQueries.selectByDeck("deck-active").executeAsList().map { it.id },
        )
        assertEquals(listOf("quote-active"), db.quotesQueries.all().executeAsList().map { it.id })
        assertEquals("quote-active", db.quotesQueries.lastInsertedQuote().executeAsOne().id)
    }

    private fun seedDecks() {
        db.deckQueries.insert(
            id = "deck-active",
            name = "Active",
            description = null,
            createdAt = 1,
            updatedAt = 1,
            deletedAt = null,
            originDeviceId = "device-1",
            lastModifiedByDeviceId = "device-1",
            versionLamport = 1,
        )
        db.deckQueries.insert(
            id = "deck-deleted",
            name = "Deleted",
            description = null,
            createdAt = 2,
            updatedAt = 2,
            deletedAt = 3,
            originDeviceId = "device-1",
            lastModifiedByDeviceId = "device-1",
            versionLamport = 2,
        )
    }

    private fun seedFlashcards() {
        insertFlashcard(
            id = "card-active",
            createdAt = 1,
            updatedAt = 1,
            deletedAt = null,
        )
        insertFlashcard(
            id = "card-deleted",
            createdAt = 2,
            updatedAt = 2,
            deletedAt = 3,
        )
    }

    private fun insertFlashcard(
        id: String,
        createdAt: Long,
        updatedAt: Long,
        deletedAt: Long?,
    ) {
        db.flashcardQueries.create(
            id = id,
            deckId = "deck-active",
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
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            originDeviceId = "device-1",
            lastModifiedByDeviceId = "device-1",
            versionLamport = createdAt,
        )
    }

    private fun seedQuotes() {
        db.quotesQueries.insert(
            id = "quote-active",
            title = "Title",
            phrase = "Phrase",
            description = "Desc",
            translation = "Tr",
            example = "Ex",
            context = "Ctx",
            pronunciation = "Pron",
            formality = "Formal",
            tags = "tag",
            category = "cat",
            createdAt = 1,
            updatedAt = 1,
            deletedAt = null,
            originDeviceId = "device-1",
            lastModifiedByDeviceId = "device-1",
            versionLamport = 1,
        )
        db.quotesQueries.insert(
            id = "quote-deleted",
            title = "Title",
            phrase = "Phrase",
            description = "Desc",
            translation = "Tr",
            example = "Ex",
            context = "Ctx",
            pronunciation = "Pron",
            formality = "Formal",
            tags = "tag",
            category = "cat",
            createdAt = 2,
            updatedAt = 2,
            deletedAt = 3,
            originDeviceId = "device-1",
            lastModifiedByDeviceId = "device-1",
            versionLamport = 2,
        )
    }
}
