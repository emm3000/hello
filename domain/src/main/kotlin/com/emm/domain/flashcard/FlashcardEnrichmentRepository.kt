package com.emm.domain.flashcard

import com.emm.domain.ids.FlashcardId
import kotlinx.coroutines.flow.Flow

interface FlashcardEnrichmentRepository {
    fun observeBacklog(): Flow<EnrichmentBacklog>

    suspend fun findIdsByStatus(status: EnrichmentStatus): List<FlashcardId>

    suspend fun markPending(ids: List<FlashcardId>)
}
