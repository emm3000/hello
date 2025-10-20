package com.emm.data.quote

import kotlinx.serialization.Serializable

@Serializable
data class CreateQuoteRequest(
    val id: String,
    val title: String,
    val phrase: String,
    val description: String,
    val translation: String,
    val example: String,
    val context: String,
    val pronunciation: String,
    val formality: String,
    val tags: String,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long,
)