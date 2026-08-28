package com.emm.domain.deck

import com.emm.domain.ids.DeckId

data class CreateDeckInput(
    val name: String,
    val description: String,
    val tags: List<String> = emptyList(),
    // A caller-supplied id lets the starter-deck seed reference the deck before it is inserted.
    val id: DeckId? = null,
)

data class UpdateDeckInput(
    val deckId: DeckId,
    val name: String,
    val description: String,
    val tags: List<String> = emptyList(),
)
