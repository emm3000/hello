package com.emm.data.flashcard

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.library.DefaultLibraryRepository
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.generation.EnrichmentFailure
import com.emm.domain.generation.EnrichmentFailureCause
import com.emm.domain.generation.InputProblem
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.library.LibraryFlashcard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DefaultFlashcardRepositoryEnrichmentFailureTest {

    private lateinit var db: HelloDb
    private lateinit var subject: DefaultFlashcardRepository
    private lateinit var library: DefaultLibraryRepository

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        subject = DefaultFlashcardRepository(db = db, json = Json, ioDispatcher = Dispatchers.IO)
        library = DefaultLibraryRepository(db = db, ioDispatcher = Dispatchers.IO)
    }

    @Test
    fun `every cause is stored under its wire value and reads back as the same cause`() = runTest {
        val expectedWires: Map<EnrichmentFailureCause, String> = mapOf(
            EnrichmentFailureCause.Technical to "technical",
            EnrichmentFailureCause.AppCheckRejected to "app_check_rejected",
            EnrichmentFailureCause.WordProblem(InputProblem.EmptyInput) to "empty_input",
            EnrichmentFailureCause.WordProblem(InputProblem.Unintelligible) to "unintelligible",
            EnrichmentFailureCause.WordProblem(InputProblem.Contradictory) to "contradictory",
            EnrichmentFailureCause.WordProblem(InputProblem.Unmappable) to "unmappable",
            EnrichmentFailureCause.CreditsExhausted to "credits_exhausted",
        )

        expectedWires.forEach { (cause, wire) ->
            val cardId: FlashcardId = createFlashcard()
            subject.updateEnrichmentStatus(cardId, EnrichmentStatus.FAILED, EnrichmentFailure(cause, "reason"))

            assertEquals(wire, storedCode(cardId))
            assertEquals(cause, subject.fetchById(cardId).flashcard.enrichmentFailureCause)
            assertEquals(EnrichmentFailure(cause, "reason"), libraryCard(cardId).enrichmentFailure)
        }
    }

    @Test
    fun `a legacy failed card without a code reads as a technical failure`() = runTest {
        val cardId: FlashcardId = createFlashcard()
        db.flashcardQueries.setEnrichmentStatus(
            enrichmentStatus = EnrichmentStatus.FAILED.name,
            enrichmentFailureReason = null,
            enrichmentFailureCode = null,
            updatedAt = 1L,
            id = cardId.value,
        )

        assertEquals(EnrichmentFailureCause.Technical, subject.fetchById(cardId).flashcard.enrichmentFailureCause)
        assertEquals(EnrichmentFailure(EnrichmentFailureCause.Technical, null), libraryCard(cardId).enrichmentFailure)
    }

    @Test
    fun `a card that has not failed carries no cause`() = runTest {
        val cardId: FlashcardId = createFlashcard()

        assertNull(subject.fetchById(cardId).flashcard.enrichmentFailureCause)
        assertNull(libraryCard(cardId).enrichmentFailure)
    }

    @Test
    fun `observeById reflects a card moved back to pending`() = runTest {
        val cardId: FlashcardId = createFlashcard()
        subject.updateEnrichmentStatus(
            cardId,
            EnrichmentStatus.FAILED,
            EnrichmentFailure(EnrichmentFailureCause.Technical, null),
        )

        DefaultFlashcardEnrichmentRepository(db = db, ioDispatcher = Dispatchers.IO).markPending(listOf(cardId))

        val observed: Flashcard? = subject.observeById(cardId).first()?.flashcard
        assertEquals(EnrichmentStatus.PENDING, observed?.enrichmentStatus)
        assertNull(observed?.enrichmentFailureCause)
    }

    private fun storedCode(cardId: FlashcardId): String? =
        db.flashcardQueries.findById(cardId.value).executeAsOne().enrichmentFailureCode

    private suspend fun libraryCard(cardId: FlashcardId): LibraryFlashcard =
        library.observeLibrary().first().first { it.id == cardId }

    private suspend fun createFlashcard(): FlashcardId {
        return subject.create(
            CreateFlashcardInput(
                deckId = insertDeck(),
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
