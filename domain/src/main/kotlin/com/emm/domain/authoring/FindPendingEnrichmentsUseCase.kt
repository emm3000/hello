package com.emm.domain.authoring

import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.FlashcardEnrichmentRepository
import com.emm.domain.ids.FlashcardId

class FindPendingEnrichmentsUseCase(
    private val repository: FlashcardEnrichmentRepository,
) {

    suspend operator fun invoke(): List<FlashcardId> = repository.findIdsByStatus(EnrichmentStatus.PENDING)
}
