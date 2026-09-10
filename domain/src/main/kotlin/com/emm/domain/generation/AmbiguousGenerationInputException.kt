package com.emm.domain.generation

class AmbiguousGenerationInputException(
    val reason: String,
    val code: GenerationRefusalCode? = null,
) : RuntimeException(reason)
