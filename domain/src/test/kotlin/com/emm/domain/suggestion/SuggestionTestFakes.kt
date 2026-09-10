package com.emm.domain.suggestion

import com.emm.domain.connectivity.ConnectivityRepository
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.generation.EnrichmentFailure
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeFlashcardRepository(
    var recentWords: List<String>,
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
    override suspend fun updateEnrichmentStatus(
        flashcardId: FlashcardId,
        status: EnrichmentStatus,
        failure: EnrichmentFailure?,
    ): Unit = error("not used")
    override suspend fun recordPromptVersion(flashcardId: FlashcardId, promptVersion: Int): Unit = error("not used")
    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = error("not used")
    override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long): Unit = error("not used")
    override suspend fun countDueFlashcards(nowMillis: Long): Long = error("not used")
    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId): Unit = error("not used")
}

internal class FakeWordSuggestionRepository(
    private val result: WordSuggestions = WordSuggestions(situation = "", words = emptyList()),
    private val failure: Throwable? = null,
    private val gate: CompletableDeferred<Unit>? = null,
) : WordSuggestionRepository {

    var receivedRecentWords: List<String> = emptyList()
        private set

    var calls: Int = 0
        private set

    override suspend fun suggest(recentWords: List<String>): WordSuggestions {
        calls += 1
        receivedRecentWords = recentWords
        gate?.await()
        failure?.let { throw it }
        return result
    }
}

internal class FakeWordSuggestionCache(initial: WordSuggestions? = null) : WordSuggestionCache {

    private val stored: MutableStateFlow<WordSuggestions?> = MutableStateFlow(initial)

    val current: WordSuggestions?
        get() = stored.value

    override fun observe(): Flow<WordSuggestions?> = stored

    override suspend fun replace(suggestions: WordSuggestions) {
        stored.value = suggestions
    }

    fun emit(suggestions: WordSuggestions?) {
        stored.value = suggestions
    }
}

internal class FakeConnectivityRepository(online: Boolean = true) : ConnectivityRepository {

    private val online: MutableStateFlow<Boolean> = MutableStateFlow(online)

    override fun observeOnline(): Flow<Boolean> = online
}
