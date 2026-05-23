package com.emm.domain.generation

/**
 * Thrown when the AI deliberately refuses to generate a learning note because
 * the user's input is too vague, empty, or impossible to map to an English
 * expression. The [reason] is suitable for showing to the user directly.
 *
 * Distinct from JSON parse failures or generic [IllegalArgumentException]s so
 * the UI can present a friendlier, actionable message.
 */
class AmbiguousGenerationInputException(
    val reason: String,
) : RuntimeException(reason)
