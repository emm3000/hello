package com.emm.domain.deck

data class CreateDeckInput(
    val name: String,
    val description: String,
    val tags: List<String> = emptyList(),
)
