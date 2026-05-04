package com.emm.domain.deck

import com.emm.domain.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class DeckTest {

    @Test
    fun `empty uses injected clock for created at`() {
        val instant = Instant.parse("2026-05-04T12:30:45Z")

        val deck = Deck.empty(Clock { instant })

        assertEquals(
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault()),
            deck.createdAt,
        )
        assertEquals(emptyList<Nothing>(), deck.cards)
        assertEquals(0L, deck.cardsCount)
    }
}
