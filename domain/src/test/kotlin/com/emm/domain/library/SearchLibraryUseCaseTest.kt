package com.emm.domain.library

import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchLibraryUseCaseTest {

    private val travel: DeckId = "deck-travel".toDeckId()
    private val work: DeckId = "deck-work".toDeckId()

    private val library: List<LibraryFlashcard> = listOf(
        card(
            id = "1",
            deckId = travel,
            deckName = "Viajes",
            word = "Compelling",
            translation = "convincente",
            meaning = "very interesting",
        ),
        card(
            id = "2",
            deckId = travel,
            deckName = "Viajes",
            word = "coffee",
            translation = "café",
            meaning = "a hot drink",
        ),
        card(
            id = "3",
            deckId = work,
            deckName = "Trabajo",
            word = "leverage",
            translation = "aprovechar",
            meaning = "to use something to maximum advantage",
        ),
    )

    @Test
    fun `invoke with a blank query returns the whole library`() = runTest {
        val result: List<LibraryFlashcard> = useCase()(query = "   ").first()

        assertEquals(listOf("1", "2", "3"), result.ids())
    }

    @Test
    fun `invoke matches the word ignoring case`() = runTest {
        val result: List<LibraryFlashcard> = useCase()(query = "COMPEL").first()

        assertEquals(listOf("1"), result.ids())
    }

    @Test
    fun `invoke matches the translation ignoring diacritics in the card`() = runTest {
        val result: List<LibraryFlashcard> = useCase()(query = "cafe").first()

        assertEquals(listOf("2"), result.ids())
    }

    @Test
    fun `invoke matches the translation ignoring diacritics in the query`() = runTest {
        val result: List<LibraryFlashcard> = useCase()(query = "CAFÉ").first()

        assertEquals(listOf("2"), result.ids())
    }

    @Test
    fun `invoke matches the english meaning`() = runTest {
        val result: List<LibraryFlashcard> = useCase()(query = "maximum advantage").first()

        assertEquals(listOf("3"), result.ids())
    }

    @Test
    fun `invoke with a deck filter narrows the results before matching`() = runTest {
        val result: List<LibraryFlashcard> = useCase()(query = "", deckId = work).first()

        assertEquals(listOf("3"), result.ids())
    }

    @Test
    fun `invoke combines the deck filter and the query`() = runTest {
        val result: List<LibraryFlashcard> = useCase()(query = "cafe", deckId = work).first()

        assertEquals(emptyList<String>(), result.ids())
    }

    @Test
    fun `invoke with no match returns nothing`() = runTest {
        val result: List<LibraryFlashcard> = useCase()(query = "xyzzy").first()

        assertEquals(emptyList<String>(), result.ids())
    }

    @Test
    fun `invoke preserves the order the repository emits`() = runTest {
        val reversed: List<LibraryFlashcard> = library.reversed()

        val result: List<LibraryFlashcard> = SearchLibraryUseCase(FakeLibraryRepository(reversed))(query = "").first()

        assertEquals(listOf("3", "2", "1"), result.ids())
    }

    private fun useCase(): SearchLibraryUseCase = SearchLibraryUseCase(FakeLibraryRepository(library))

    private fun List<LibraryFlashcard>.ids(): List<String> = map { it.id.value }

    @Suppress("LongParameterList")
    private fun card(
        id: String,
        deckId: DeckId,
        deckName: String,
        word: String,
        translation: String,
        meaning: String,
    ): LibraryFlashcard = LibraryFlashcard(
        id = id.toFlashcardId(),
        deckId = deckId,
        deckName = deckName,
        word = word,
        translation = translation,
        meaning = meaning,
        enrichmentStatus = EnrichmentStatus.ENRICHED,
        nextReviewAt = null,
    )

    private class FakeLibraryRepository(private val cards: List<LibraryFlashcard>) : LibraryRepository {
        override fun observeLibrary(): Flow<List<LibraryFlashcard>> = flowOf(cards)
    }
}
