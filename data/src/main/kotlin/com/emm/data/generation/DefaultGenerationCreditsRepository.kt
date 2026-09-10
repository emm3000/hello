package com.emm.data.generation

import com.emm.data.remote.DataStore
import com.emm.domain.generation.GenerationCredits
import com.emm.domain.generation.GenerationCreditsRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultGenerationCreditsRepository(
    private val dataStore: DataStore,
) : GenerationCreditsRepository {

    private val credits: MutableStateFlow<GenerationCredits?> = MutableStateFlow(storedCredits())

    override fun observe(): Flow<GenerationCredits?> = credits.asStateFlow()

    override fun record(remaining: Int, observedAt: Instant) {
        dataStore.generationCreditsRemaining = remaining
        dataStore.generationCreditsObservedAtMillis = observedAt.toEpochMilli()
        credits.value = GenerationCredits(remaining = remaining, observedAt = observedAt)
    }

    private fun storedCredits(): GenerationCredits? {
        val remaining: Int = dataStore.generationCreditsRemaining
        val observedAtMillis: Long = dataStore.generationCreditsObservedAtMillis
        if (remaining == DataStore.NO_GENERATION_CREDITS_REMAINING) return null
        if (observedAtMillis == DataStore.NO_GENERATION_CREDITS_OBSERVED_AT) return null
        return GenerationCredits(remaining = remaining, observedAt = Instant.ofEpochMilli(observedAtMillis))
    }
}
