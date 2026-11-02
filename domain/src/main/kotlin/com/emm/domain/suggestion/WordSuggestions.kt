package com.emm.domain.suggestion

import java.time.Instant

data class WordSuggestions(
    val situation: String,
    val words: List<SuggestedWord>,
    val firstSeenAt: Instant? = null,
)
