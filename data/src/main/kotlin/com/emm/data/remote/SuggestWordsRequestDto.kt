package com.emm.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SuggestWordsRequestDto(
    @SerialName("recent_words") val recentWords: List<String>,
)
