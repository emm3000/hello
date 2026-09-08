package com.emm.domain.curated

import com.emm.domain.deck.Deck
import com.emm.domain.generation.LevelBand
import com.emm.domain.ids.DeckId
import com.emm.domain.time.SystemClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetCuratedDecksUseCaseTest {

    @Test
    fun `nothing installed lists every deck as not installed`() = runTest {
        val useCase = GetCuratedDecksUseCase(
            catalog = FakeCuratedDeckCatalog(catalogDecks),
            deckRepository = FakeDeckRepository(emptyList()),
        )

        val listings: List<CuratedDeckListing> = useCase().first()

        assertEquals(listOf("first", "second"), listings.map { it.deck.id })
        assertTrue(listings.none { it.isInstalled })
        assertTrue(listings.all { it.installedDeckId == null })
    }

    @Test
    fun `an installed deck carries its deck id`() = runTest {
        val useCase = GetCuratedDecksUseCase(
            catalog = FakeCuratedDeckCatalog(catalogDecks),
            deckRepository = FakeDeckRepository(listOf(deckWithId("curated-second"))),
        )

        val listings: List<CuratedDeckListing> = useCase().first()

        assertEquals(listOf("first", "second"), listings.map { it.deck.id })
        assertFalse(listings.first().isInstalled)
        assertNull(listings.first().installedDeckId)
        assertTrue(listings.last().isInstalled)
        assertEquals(DeckId.from("curated-second"), listings.last().installedDeckId)
    }

    @Test
    fun `a deck with an unrelated id does not count as installed`() = runTest {
        val useCase = GetCuratedDecksUseCase(
            catalog = FakeCuratedDeckCatalog(catalogDecks),
            deckRepository = FakeDeckRepository(listOf(deckWithId("second"))),
        )

        val listings: List<CuratedDeckListing> = useCase().first()

        assertTrue(listings.none { it.isInstalled })
    }
}

private val catalogDecks: List<CuratedDeck> = listOf(curatedDeck("first"), curatedDeck("second"))

private fun curatedDeck(id: String): CuratedDeck {
    return CuratedDeck(
        id = id,
        name = "Deck $id",
        description = "Description $id",
        tags = listOf(id),
        levelBand = LevelBand.A1_A2,
        notes = emptyList(),
    )
}

private fun deckWithId(rawDeckId: String): Deck = Deck.empty(SystemClock).copy(id = DeckId.from(rawDeckId))
