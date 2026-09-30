package com.emm.hello.newfeatures.card

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.generation.EnrichmentFailureCause
import com.emm.domain.time.SystemClock
import com.emm.hello.core.mvi.MviState
import java.time.Instant

data class FlashcardDetailUiState(
    val flashcard: Flashcard = Flashcard.empty(SystemClock),
    val isLoading: Boolean = true,
    val isDeleteConfirmationVisible: Boolean = false,
    val failedEnrichment: FailedEnrichment? = null,
) : MviState

data class FailedEnrichment(
    val cause: EnrichmentFailureCause,
    val canRetry: Boolean,
    val creditsResetAt: Instant?,
) {

    val action: FailedEnrichmentAction
        get() = when {
            canRetry -> FailedEnrichmentAction.TryAgain
            cause == EnrichmentFailureCause.CreditsExhausted -> FailedEnrichmentAction.None
            else -> FailedEnrichmentAction.WriteItMyself
        }
}

enum class FailedEnrichmentAction {
    TryAgain,
    WriteItMyself,
    None,
}
