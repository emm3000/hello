package com.emm.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SoftDeleteVisibilityQueryTest {
    private val accountId = "account-1"

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

        assertEquals(listOf("deck-active"), db.deckQueries.all(accountId).executeAsList().map { it.id })
        assertEquals(1, db.deckQueries.deckWithFlashcardCount(accountId).executeAsOne().flashcardCount)
        assertEquals(
            listOf("card-active"),
            db.flashcardQueries.selectByDeck(accountId, "deck-active").executeAsList().map { it.id },
        )
    }

    private fun seedDecks() {
        db.deckQueries.insert(
            appAccountId = accountId,
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
            appAccountId = accountId,
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
            appAccountId = accountId,
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
}
