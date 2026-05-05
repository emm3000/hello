package com.emm.domain.deck

import com.emm.domain.ids.DeckId
import com.emm.domain.ids.toDeckId
import org.junit.Assert.assertEquals
import org.junit.Test

class SetDefaultDeckUseCaseTest {

    @Test
    fun `invoke normalizes deck id before persisting selection`() {
        val repository = FakeDefaultDeckSelectionForSetRepository()
        val useCase = SetDefaultDeckUseCase(repository)

        useCase("deck-1".toDeckId())

        assertEquals("deck-1", repository.lastSetDefaultDeckId)
    }

    @Test
    fun `invoke clears default deck when id is null`() {
        val repository = FakeDefaultDeckSelectionForSetRepository()
        val useCase = SetDefaultDeckUseCase(repository)

        useCase(null)

        assertEquals(null, repository.lastSetDefaultDeckId)
    }
}

private class FakeDefaultDeckSelectionForSetRepository : DefaultDeckSelectionRepository {
    var lastSetDefaultDeckId: String? = null

    override fun getDefaultDeckId(): DeckId? = null

    override fun setDefaultDeckId(deckId: DeckId?) {
        lastSetDefaultDeckId = deckId?.value
    }
}
