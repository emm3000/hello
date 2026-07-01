package com.emm.domain.deck

import com.emm.domain.ids.DeckId

data class CreateDeckInput(
    val name: String,
    val description: String,
    val tags: List<String> = emptyList(),
    // Optional caller-provided id. Mirrors CreateFlashcardInput.id so callers that need to
    // reference the deck right after creation (e.g. seeding a starter deck with its cards)
    // can supply a deterministic id instead of relying on the repository-generated UUID.
    val id: DeckId? = null,
)

data class UpdateDeckInput(
    val deckId: DeckId,
    val name: String,
    val description: String,
    val tags: List<String> = emptyList(),
)
