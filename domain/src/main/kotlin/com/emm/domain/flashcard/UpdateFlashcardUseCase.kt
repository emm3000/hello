package com.emm.domain.flashcard

class UpdateFlashcardUseCase(
    private val flashcardRepository: FlashcardRepository,
) {

    suspend operator fun invoke(input: UpdateFlashcardInput) {
        require(input.word.isNotBlank()) { "Flashcard word must not be blank." }

        flashcardRepository.update(input)
        if (completesFailedEnrichment(input)) {
            flashcardRepository.updateEnrichmentStatus(
                input.flashcardId,
                EnrichmentStatus.ENRICHED,
                failure = null,
            )
        }
    }

    private suspend fun completesFailedEnrichment(input: UpdateFlashcardInput): Boolean {
        if (input.meaning.isBlank()) return false
        val status: EnrichmentStatus = flashcardRepository.fetchById(input.flashcardId).flashcard.enrichmentStatus
        return status == EnrichmentStatus.FAILED
    }
}
