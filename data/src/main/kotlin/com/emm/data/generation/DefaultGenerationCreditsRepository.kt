package com.emm.data.generation

import com.emm.data.remote.DataStore
import com.emm.data.remote.StoredGenerationCredits
import com.emm.domain.generation.GenerationCredits
import com.emm.domain.generation.GenerationCreditsRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultGenerationCreditsRepository(
    private val dataStore: DataStore,
) : GenerationCreditsRepository {

    private val stored: MutableStateFlow<GenerationCredits?> = MutableStateFlow(readStoredCredits())

    override fun observe(): Flow<GenerationCredits?> = stored.asStateFlow()

    override suspend fun record(credits: GenerationCredits) {
        dataStore.generationCredits = credits.toStored()
        stored.value = credits
    }

    private fun readStoredCredits(): GenerationCredits? {
        val saved: StoredGenerationCredits = dataStore.generationCredits ?: return null
        return GenerationCredits(
            remaining = saved.remaining,
            resetAt = Instant.ofEpochMilli(saved.resetAtMillis),
        )
    }
}

private fun GenerationCredits.toStored(): StoredGenerationCredits = StoredGenerationCredits(
    remaining = remaining,
    resetAtMillis = resetAt.toEpochMilli(),
)
