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
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class FakeFlashcardRepository(
    var words: List<String>,
    private val wordsGate: CompletableDeferred<Unit>? = null,
) : FlashcardRepository {

    var receivedLimit: Int = -1
        private set

    override suspend fun fetchRecentWords(limit: Int): List<String> {
        receivedLimit = limit
        wordsGate?.await()
        return words.take(limit)
    }

    override suspend fun fetchAllWords(): List<String> {
        wordsGate?.await()
        return words
    }

    override fun fetchAll(): Flow<List<Flashcard>> = error("not used")
    override fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>> = error("not used")
    override fun observeById(id: FlashcardId): Flow<FlashcardDetail?> = throw UnsupportedOperationException()
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
    failure: Throwable? = null,
    private val gate: CompletableDeferred<Unit>? = null,
) : WordSuggestionRepository {

    private var failure: Throwable? = failure

    var receivedRequest: WordSuggestionRequest? = null
        private set

    var calls: Int = 0
        private set

    fun clearFailure() {
        failure = null
    }

    override suspend fun suggest(request: WordSuggestionRequest): WordSuggestions {
        calls += 1
        receivedRequest = request
        gate?.await()
        failure?.let { throw it }
        return result
    }
}

internal class FakeWordSuggestionCache(
    initial: WordSuggestions? = null,
    private val seenAt: Instant = Instant.EPOCH,
) : WordSuggestionCache {

    private val stored: MutableStateFlow<WordSuggestions?> = MutableStateFlow(initial)
    private var observeFailure: Throwable? = null

    val current: WordSuggestions?
        get() = stored.value

    override fun observe(): Flow<WordSuggestions?> = flow {
        val pendingFailure: Throwable? = observeFailure
        if (pendingFailure != null) throw pendingFailure
        emitAll(stored)
    }

    override suspend fun replace(suggestions: WordSuggestions) {
        stored.value = suggestions
    }

    override suspend fun markSeen() {
        val batch: WordSuggestions = stored.value ?: return
        if (batch.firstSeenAt == null) stored.value = batch.copy(firstSeenAt = seenAt)
    }

    fun emit(suggestions: WordSuggestions?) {
        stored.value = suggestions
    }

    fun failNextObserveWith(error: Throwable) {
        observeFailure = error
    }
}

internal class FakeConnectivityRepository(online: Boolean = true) : ConnectivityRepository {

    private val online: MutableStateFlow<Boolean> = MutableStateFlow(online)
    private var observeFailure: Throwable? = null

    override fun observeOnline(): Flow<Boolean> = flow {
        val pendingFailure: Throwable? = observeFailure
        if (pendingFailure != null) throw pendingFailure
        emitAll(online)
    }

    fun setOnline(value: Boolean) {
        online.value = value
    }

    fun failNextObserveWith(error: Throwable) {
        observeFailure = error
    }

    fun clearFailure() {
        observeFailure = null
    }
}

internal class FakeSuggestionHistoryRepository(
    entries: List<SuggestionHistoryEntry> = emptyList(),
) : SuggestionHistoryRepository {

    var entries: List<SuggestionHistoryEntry> = entries
        private set

    override suspend fun fetch(): SuggestionHistory {
        val newestFirst: List<SuggestionHistoryEntry> = entries.sortedByDescending { entry -> entry.shownAt }
        return SuggestionHistory(
            situations = newestFirst.filter { entry -> entry.isSituation }.map { entry -> entry.text },
            words = newestFirst.filterNot { entry -> entry.isSituation }.map { entry -> entry.text },
        )
    }

    override suspend fun deleteShownBefore(cutoff: Instant) {
        entries = entries.filterNot { entry -> entry.shownAt.isBefore(cutoff) }
    }
}

internal data class SuggestionHistoryEntry(val text: String, val isSituation: Boolean, val shownAt: Instant)
