package com.emm.domain.authoring

import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.generation.EnrichmentFailure
import com.emm.domain.ids.FlashcardId

class MarkEnrichmentFailedUseCase(
    private val repository: FlashcardRepository,
) {

    suspend operator fun invoke(flashcardId: FlashcardId, failure: EnrichmentFailure?) {
        repository.updateEnrichmentStatus(flashcardId, EnrichmentStatus.FAILED, failure = failure)
    }
}
