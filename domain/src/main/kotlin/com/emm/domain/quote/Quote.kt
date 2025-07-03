package com.emm.domain.quote

data class Quote(
    val id: String,
    val title: String,
    val phrase: String,
    val description: String,
    val translation: String,
    val example: String,
    val context: String,
    val pronunciation: String,
    val formality: String,
    val tags: List<String>,
    val category: String,
    val hasCard: Boolean,
)