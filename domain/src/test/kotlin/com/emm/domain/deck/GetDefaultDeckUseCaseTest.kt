package com.emm.domain.deck

import com.emm.domain.ids.DeckId
import com.emm.domain.ids.toDeckId
import org.junit.Assert.assertEquals
import org.junit.Test

class GetDefaultDeckUseCaseTest {

    @Test
    fun `invoke normalizes deck id from repository`() {
        val repository = FakeDefaultDeckSelectionRepository(defaultDeckId = "deck-1".toDeckId())
        val useCase = GetDefaultDeckUseCase(repository)

        val result = useCase()

        assertEquals("deck-1", result?.value)
    }

    @Test
    fun `invoke returns null when repository has no default deck`() {
        val useCase = GetDefaultDeckUseCase(
            FakeDefaultDeckSelectionRepository(defaultDeckId = null)
        )

        val result = useCase()

        assertEquals(null, result)
    }
}

private class FakeDefaultDeckSelectionRepository(
    private val defaultDeckId: DeckId?,
) : DefaultDeckSelectionRepository {
    override fun getDefaultDeckId(): DeckId? = defaultDeckId

    override fun setDefaultDeckId(deckId: DeckId?) = Unit
}
