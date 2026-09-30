package com.emm.domain.generation

data class EnrichmentFailure(
    val cause: EnrichmentFailureCause,
    val reason: String?,
)
