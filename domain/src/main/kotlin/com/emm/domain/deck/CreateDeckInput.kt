package com.emm.domain.deck

import com.emm.domain.ids.DeckId

data class CreateDeckInput(
    val name: String,
    val description: String,
    val tags: List<String> = emptyList(),
)

data class UpdateDeckInput(
    val deckId: DeckId,
    val name: String,
    val description: String,
    val tags: List<String> = emptyList(),
)
