package com.emm.data.flashcard

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DefaultFlashcardRepositoryPromptVersionTest {

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
    fun `a freshly created card carries prompt version zero`() = runTest {
        val cardId: FlashcardId = createFlashcard()

        assertEquals(0, subject.fetchById(cardId).flashcard.promptVersion)
    }

    @Test
    fun `recordPromptVersion stores the version on the card`() = runTest {
        val cardId: FlashcardId = createFlashcard()

        subject.recordPromptVersion(cardId, 2)

        assertEquals(2, subject.fetchById(cardId).flashcard.promptVersion)
    }

    @Test
    fun `a later update never rewrites the recorded prompt version`() = runTest {
        val cardId: FlashcardId = createFlashcard()
        subject.recordPromptVersion(cardId, 2)

        subject.update(
            UpdateFlashcardInput(
                flashcardId = cardId,
                word = "lantern",
                meaning = "a portable lamp",
                translation = "farol",
            ),
        )

        val flashcard: Flashcard = subject.fetchById(cardId).flashcard
        assertEquals("lantern", flashcard.word)
        assertEquals(2, flashcard.promptVersion)
    }

    @Test
    fun `fetchByDeckId carries the recorded prompt version`() = runTest {
        val deckId: DeckId = insertDeck()
        val cardId: FlashcardId = createFlashcard(deckId)
        subject.recordPromptVersion(cardId, 2)

        val flashcards: List<Flashcard> = subject.fetchByDeckId(deckId).first()

        assertEquals(listOf(2), flashcards.map { it.promptVersion })
    }

    private suspend fun createFlashcard(deckId: DeckId = insertDeck()): FlashcardId {
        return subject.create(
            CreateFlashcardInput(
                deckId = deckId,
                word = "word",
                meaning = "meaning",
                translation = "palabra",
                phonetic = "",
            ),
        )
    }

    private fun insertDeck(): DeckId {
        val id: String = UUID.randomUUID().toString()
        db.deckQueries.insert(
            id = id,
            name = "Test deck",
            description = null,
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = null,
        )
        return DeckId.from(id)
    }
}
