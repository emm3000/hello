package com.emm.domain.authoring

import com.emm.domain.flashcard.FlashcardEnrichmentRepository
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.generation.EnrichmentFailureCause
import com.emm.domain.generation.GenerationCredits
import com.emm.domain.generation.GenerationCreditsRepository
import com.emm.domain.generation.canRetryAt
import com.emm.domain.ids.FlashcardId
import com.emm.domain.time.Clock
import kotlinx.coroutines.flow.first

class RetryEnrichmentUseCase(
    private val flashcardRepository: FlashcardRepository,
    private val enrichmentRepository: FlashcardEnrichmentRepository,
    private val creditsRepository: GenerationCreditsRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(flashcardId: FlashcardId): Boolean {
        val cause: EnrichmentFailureCause =
            flashcardRepository.fetchById(flashcardId).flashcard.enrichmentFailureCause ?: return false
        val credits: GenerationCredits? = creditsRepository.observe().first()
        if (!cause.canRetryAt(clock.now(), credits)) return false

        enrichmentRepository.markPending(listOf(flashcardId))
        return true
    }
}
