package com.emm.domain.deck

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.ids.DeckId
import com.emm.domain.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

data class Deck(
    val id: DeckId,
    val name: String,
    val description: String,
    val createdAt: LocalDateTime,
    val cards: List<Flashcard>,
    val cardsCount: Long,
    val tags: List<Tag> = emptyList(),
) {

    companion object {

        fun empty(clock: Clock): Deck = Deck(
            id = DeckId.from("empty-deck"),
            name = "",
            description = "",
            createdAt = LocalDateTime.ofInstant(clock.now(), ZoneId.systemDefault()),
            cards = emptyList(),
            cardsCount = 0L,
            tags = emptyList(),
        )
    }
}
