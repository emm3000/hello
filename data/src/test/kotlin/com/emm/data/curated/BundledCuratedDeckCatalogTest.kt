package com.emm.data.curated

import com.emm.domain.curated.CuratedDeck
import org.junit.Assert.assertEquals
import org.junit.Test

class BundledCuratedDeckCatalogTest {

    private val decks: List<CuratedDeck> = BundledCuratedDeckCatalog().decks()

    @Test
    fun `no expression appears in more than one deck`() {
        val expressionsPerDeck: List<String> = decks.flatMap { deck ->
            deck.notes.map { note -> note.expression.value.trim().lowercase() }.distinct()
        }

        assertEquals(emptyList<String>(), expressionsPerDeck.repeatedValues())
    }

    private fun List<String>.repeatedValues(): List<String> =
        groupBy { it }.filterValues { it.size > 1 }.keys.sorted()
}
