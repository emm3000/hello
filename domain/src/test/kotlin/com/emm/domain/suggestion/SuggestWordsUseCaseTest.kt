package com.emm.domain.suggestion

import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestWordsUseCaseTest {

    @Test
    fun `invoke asks the flashcard repository for exactly the recent words limit`() = runBlocking {
        val flashcardRepository = FakeFlashcardRepository(recentWords = listOf("hello", "world"))
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(situation = "At a cafe", words = listOf(SuggestedWord("order", "pedir"))),
        )
        val useCase = SuggestWordsUseCase(flashcardRepository, suggestionRepository)

        useCase()

        assertEquals(SuggestWordsUseCase.RECENT_WORDS_LIMIT, flashcardRepository.receivedLimit)
        assertEquals(listOf("hello", "world"), suggestionRepository.receivedRecentWords)
    }

    @Test
    fun `invoke drops a candidate already captured, case-insensitive`() = runBlocking {
        val flashcardRepository = FakeFlashcardRepository(recentWords = listOf("Serendipity"))
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(
                situation = "At a cafe",
                words = listOf(
                    SuggestedWord("serendipity", "casualidad"),
                    SuggestedWord("order", "pedir"),
                ),
            ),
        )
        val useCase = SuggestWordsUseCase(flashcardRepository, suggestionRepository)

        val result = useCase()

        assertEquals(listOf(SuggestedWord("order", "pedir")), result.words)
    }

    @Test
    fun `invoke collapses duplicate candidates to the first occurrence`() = runBlocking {
        val flashcardRepository = FakeFlashcardRepository(recentWords = emptyList())
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(
                situation = "At a cafe",
                words = listOf(
                    SuggestedWord("order", "pedir"),
                    SuggestedWord("Order", "ordenar"),
                ),
            ),
        )
        val useCase = SuggestWordsUseCase(flashcardRepository, suggestionRepository)

        val result = useCase()

        assertEquals(listOf(SuggestedWord("order", "pedir")), result.words)
    }

    @Test
    fun `invoke drops candidates with a blank word or translation`() = runBlocking {
        val flashcardRepository = FakeFlashcardRepository(recentWords = emptyList())
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(
                situation = "At a cafe",
                words = listOf(
                    SuggestedWord("", "pedir"),
                    SuggestedWord("order", ""),
                    SuggestedWord("table", "mesa"),
                ),
            ),
        )
        val useCase = SuggestWordsUseCase(flashcardRepository, suggestionRepository)

        val result = useCase()

        assertEquals(listOf(SuggestedWord("table", "mesa")), result.words)
    }

    @Test
    fun `invoke passes the situation through untouched`() = runBlocking {
        val flashcardRepository = FakeFlashcardRepository(recentWords = emptyList())
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(situation = "Ordering food at a busy restaurant", words = emptyList()),
        )
        val useCase = SuggestWordsUseCase(flashcardRepository, suggestionRepository)

        val result = useCase()

        assertEquals("Ordering food at a busy restaurant", result.situation)
    }
}

private class FakeFlashcardRepository(
    private val recentWords: List<String>,
) : FlashcardRepository {

    var receivedLimit: Int = -1
        private set

    override suspend fun fetchRecentWords(limit: Int): List<String> {
        receivedLimit = limit
        return recentWords
    }

    override fun fetchAll(): Flow<List<Flashcard>> = error("not used")
    override fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>> = error("not used")
    override suspend fun fetchById(id: FlashcardId): FlashcardDetail = error("not used")
    override suspend fun create(input: CreateFlashcardInput): FlashcardId = error("not used")
    override suspend fun update(input: UpdateFlashcardInput): Unit = error("not used")
    override suspend fun updateEnrichmentStatus(flashcardId: FlashcardId, status: EnrichmentStatus): Unit =
        error("not used")
    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = error("not used")
    override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long): Unit = error("not used")
    override suspend fun countDueFlashcards(nowMillis: Long): Long = error("not used")
    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId): Unit = error("not used")
}

private class FakeWordSuggestionRepository(
    private val result: WordSuggestions,
) : WordSuggestionRepository {

    var receivedRecentWords: List<String> = emptyList()
        private set

    override suspend fun suggest(recentWords: List<String>): WordSuggestions {
        receivedRecentWords = recentWords
        return result
    }
}
