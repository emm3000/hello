package com.emm.domain.authoring

import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.ids.FlashcardId

class MarkEnrichmentFailedUseCase(
    private val repository: FlashcardRepository,
) {

    suspend operator fun invoke(flashcardId: FlashcardId) {
        repository.updateEnrichmentStatus(flashcardId, EnrichmentStatus.FAILED)
    }
}
