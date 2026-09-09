package com.emm.data.seed

import android.content.SharedPreferences
import com.emm.data.remote.DataStore
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class DefaultSeedDataInitializerTest {

    private val editor = mockk<SharedPreferences.Editor>(relaxed = true) {
        every { putBoolean(any(), any()) } returns this
    }
    private val prefs = mockk<SharedPreferences>(relaxed = true) {
        every { edit() } returns editor
    }
    private val deckRepository = mockk<DeckRepository>()
    private val flashcardRepository = mockk<FlashcardRepository>()
    private val deckSelectionRepository = mockk<DefaultDeckSelectionRepository>()

    @Test
    fun `seeding the starter deck stamps that deck as the default selection`() = runTest {
        every { prefs.getBoolean(KEY_SEEDED_STARTER_DECK, false) } returns false
        every { deckRepository.fetchAll() } returns flowOf(emptyList())
        val createdDeck = slot<CreateDeckInput>()
        coEvery { deckRepository.create(capture(createdDeck)) } just Runs
        coEvery { flashcardRepository.create(any()) } returns "card-1".toFlashcardId()
        coEvery { flashcardRepository.upsertExamples(any(), any()) } just Runs
        val stamped = slot<DeckId>()
        every { deckSelectionRepository.setDefaultDeckId(capture(stamped)) } just Runs

        buildInitializer().ensureSeeded()

        assertEquals(createdDeck.captured.id, stamped.captured)
    }

    @Test
    fun `an install that already has decks is not seeded and stamps nothing`() = runTest {
        every { prefs.getBoolean(KEY_SEEDED_STARTER_DECK, false) } returns false
        every { deckRepository.fetchAll() } returns flowOf(listOf(existingDeck()))

        buildInitializer().ensureSeeded()

        coVerify(exactly = 0) { deckRepository.create(any()) }
        verify(exactly = 0) { deckSelectionRepository.setDefaultDeckId(any()) }
    }

    private fun buildInitializer(): DefaultSeedDataInitializer = DefaultSeedDataInitializer(
        deckRepository = deckRepository,
        flashcardRepository = flashcardRepository,
        dataStore = DataStore(prefs),
        deckSelectionRepository = deckSelectionRepository,
        deckName = "Primeras palabras",
    )

    private fun existingDeck(): Deck = Deck(
        id = "deck-1".toDeckId(),
        name = "Primeras palabras",
        description = "",
        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        cards = emptyList(),
        cardsCount = 0L,
    )

    private companion object {
        const val KEY_SEEDED_STARTER_DECK: String = "HAS_SEEDED_STARTER_DECK"
    }
}
