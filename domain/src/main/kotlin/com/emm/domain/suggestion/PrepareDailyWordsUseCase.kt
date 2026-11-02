package com.emm.domain.suggestion

import com.emm.domain.connectivity.ConnectivityRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

class PrepareDailyWordsUseCase(
    private val observeSuggestedWords: ObserveSuggestedWordsUseCase,
    private val refreshSuggestedWords: RefreshSuggestedWordsUseCase,
    private val connectivityRepository: ConnectivityRepository,
) {

    suspend operator fun invoke(): WordSuggestions? {
        val cached: WordSuggestions? = observeSuggestedWords().first()
        if (cached != null && cached.isUnseenWithWords()) return cached
        if (!connectivityRepository.observeOnline().first()) return null
        return fetchNewBatch()?.takeIf { batch -> batch.words.isNotEmpty() }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchNewBatch(): WordSuggestions? = try {
        refreshSuggestedWords()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (ignored: Throwable) {
        null
    }

    private fun WordSuggestions.isUnseenWithWords(): Boolean = firstSeenAt == null && words.isNotEmpty()
}
