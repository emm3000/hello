package com.emm.domain.flashcard

import com.emm.domain.ids.FlashcardId

interface FlashcardEnrichmentRepository {
    suspend fun findIdsByStatus(status: EnrichmentStatus): List<FlashcardId>

    suspend fun markPending(ids: List<FlashcardId>)
}
