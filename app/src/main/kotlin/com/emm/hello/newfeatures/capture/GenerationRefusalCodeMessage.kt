package com.emm.hello.newfeatures.capture

import androidx.annotation.StringRes
import com.emm.domain.generation.GenerationRefusalCode
import com.emm.hello.R

@StringRes
fun GenerationRefusalCode.messageRes(): Int = when (this) {
    GenerationRefusalCode.EmptyInput -> R.string.capture_failure_empty_input
    GenerationRefusalCode.Unintelligible -> R.string.capture_failure_unintelligible
    GenerationRefusalCode.Contradictory -> R.string.capture_failure_contradictory
    GenerationRefusalCode.Unmappable -> R.string.capture_failure_unmappable
    GenerationRefusalCode.CreditsExhausted -> R.string.capture_failure_credits_exhausted
}
