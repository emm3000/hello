package com.emm.data.quote

import kotlinx.serialization.Serializable

@Serializable
data class QuoteResponse(

    val category: String,

    val context: String,

    val description: String,

    val example: String,

    val formality: String,

    val phrase: String,

    val pronunciation: String,

    val tags: List<String>,

    val title: String,

    val translation: String,
)