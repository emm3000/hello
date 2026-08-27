package com.emm.domain.flashcard

data class EnrichmentBacklog(
    val pending: Int = 0,
    val failed: Int = 0,
)
