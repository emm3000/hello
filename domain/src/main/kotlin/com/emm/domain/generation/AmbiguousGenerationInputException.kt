package com.emm.domain.generation

class AmbiguousGenerationInputException(
    val reason: String,
) : RuntimeException(reason)
