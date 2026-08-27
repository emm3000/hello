package com.emm.domain.authoring

import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.FlashcardEnrichmentRepository
import com.emm.domain.ids.FlashcardId

class RetryFailedEnrichmentsUseCase(
    private val repository: FlashcardEnrichmentRepository,
) {

    suspend operator fun invoke(): List<FlashcardId> {
        val failed: List<FlashcardId> = repository.findIdsByStatus(EnrichmentStatus.FAILED)
        if (failed.isEmpty()) return emptyList()

        repository.markPending(failed)
        return failed
    }
}
