package com.emm.domain.deck

import com.emm.domain.ids.DeckId
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveCaptureDeckUseCaseTest {

    @Test
    fun `curated installs are not candidates when a user deck exists`() {
        val starter: Deck = deck(id = "starter", createdAt = OLDEST)
        val curated: Deck = deck(id = "curated-phrasal-verbs", createdAt = OLDEST.minusDays(1))

        val choice: CaptureDeckChoice = resolve(defaultDeckId = null, starter, curated)

        assertEquals(listOf(starter), choice.candidates)
        assertEquals(starter, choice.target)
    }

    @Test
    fun `every live deck is a candidate when only curated installs exist`() {
        val newer: Deck = deck(id = "curated-phrasal-verbs", createdAt = OLDEST.plusDays(1))
        val older: Deck = deck(id = "curated-spanish-traps", createdAt = OLDEST)

        val choice: CaptureDeckChoice = resolve(defaultDeckId = null, newer, older)

        assertEquals(listOf(newer, older), choice.candidates)
        assertEquals(older, choice.target)
    }

    @Test
    fun `a stored default outside the candidates falls back to the oldest candidate`() {
        val newer: Deck = deck(id = "travel", createdAt = OLDEST.plusDays(1))
        val older: Deck = deck(id = "starter", createdAt = OLDEST)
        val curated: Deck = deck(id = "curated-phrasal-verbs", createdAt = OLDEST.plusDays(2))

        val choice: CaptureDeckChoice = resolve(defaultDeckId = curated.id, newer, older, curated)

        assertEquals(older, choice.target)
    }

    private fun resolve(defaultDeckId: DeckId?, vararg liveDecks: Deck): CaptureDeckChoice {
        val useCase = ResolveCaptureDeckUseCase(FakeDefaultDeckSelectionRepository(defaultDeckId))
        return useCase(liveDecks.toList())
    }

    private fun deck(id: String, createdAt: LocalDateTime): Deck = Deck(
        id = DeckId.from(id),
        name = id,
        description = "",
        createdAt = createdAt,
        cards = emptyList(),
        cardsCount = 0L,
    )

    private class FakeDefaultDeckSelectionRepository(
        private var defaultDeckId: DeckId?,
    ) : DefaultDeckSelectionRepository {

        override fun getDefaultDeckId(): DeckId? = defaultDeckId

        override fun setDefaultDeckId(deckId: DeckId?) {
            defaultDeckId = deckId
        }
    }

    private companion object {
        val OLDEST: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)
    }
}
