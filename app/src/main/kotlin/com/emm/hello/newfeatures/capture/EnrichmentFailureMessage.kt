package com.emm.hello.newfeatures.capture

import androidx.annotation.StringRes
import com.emm.domain.generation.EnrichmentFailureCause
import com.emm.domain.generation.InputProblem
import com.emm.hello.R

@StringRes
fun InputProblem.messageRes(): Int = when (this) {
    InputProblem.EmptyInput -> R.string.capture_failure_empty_input
    InputProblem.Unintelligible -> R.string.capture_failure_unintelligible
    InputProblem.Contradictory -> R.string.capture_failure_contradictory
    InputProblem.Unmappable -> R.string.capture_failure_unmappable
}

@StringRes
fun EnrichmentFailureCause.captureReasonRes(): Int = when (this) {
    EnrichmentFailureCause.Technical -> R.string.capture_failure_technical
    EnrichmentFailureCause.AppCheckRejected -> R.string.capture_failure_app_check_rejected
    is EnrichmentFailureCause.WordProblem -> problem.messageRes()
    EnrichmentFailureCause.CreditsExhausted -> R.string.capture_failure_credits_exhausted
}
