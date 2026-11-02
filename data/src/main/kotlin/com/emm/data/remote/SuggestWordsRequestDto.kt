package com.emm.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SuggestWordsRequestDto(
    @SerialName("recent_words") val recentWords: List<String>,
    @SerialName("excluded_situations") val excludedSituations: List<String>,
    @SerialName("excluded_words") val excludedWords: List<String>,
)
