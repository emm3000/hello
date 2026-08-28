package com.emm.domain.generation

// reason is the AI's own refusal message and reaches the user unmodified.
class AmbiguousGenerationInputException(
    val reason: String,
) : RuntimeException(reason)
