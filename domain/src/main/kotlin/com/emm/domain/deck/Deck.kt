package com.emm.domain.deck

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.time.Clock
import com.emm.domain.time.SystemClock
import java.time.LocalDateTime
import java.time.ZoneId

data class Deck(
    val id: String,
    val name: String,
    val description: String,
    val createdAt: LocalDateTime,
    val cards: List<Flashcard>,
    val cardsCount: Long,
) {

    companion object {

        fun empty(clock: Clock): Deck = Deck(
            id = "",
            name = "",
            description = "",
            createdAt = LocalDateTime.ofInstant(clock.now(), ZoneId.systemDefault()),
            cards = emptyList(),
            cardsCount = 0L,
        )

        @Deprecated(
            message = "Use empty(clock) for deterministic time.",
            replaceWith = ReplaceWith(
                expression = "empty(SystemClock)",
                imports = ["com.emm.domain.time.SystemClock"],
            ),
        )
        val Empty: Deck
            get() = empty(SystemClock)
    }
}
