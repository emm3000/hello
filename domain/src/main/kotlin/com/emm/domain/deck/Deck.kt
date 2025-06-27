package com.emm.domain.deck

import com.emm.domain.flashcard.Flashcard
import java.time.LocalDateTime

data class Deck(
    val id: String,
    val name: String,
    val description: String,
    val createdAt: LocalDateTime,
    val cards: List<Flashcard>,
) {

    companion object {

        val Empty: Deck
            get() = Deck(
                id = "",
                name = "",
                description = "",
                createdAt = LocalDateTime.now(),
                cards = emptyList(),
            )
    }
}