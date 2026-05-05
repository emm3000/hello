package com.emm.domain.deck

import com.emm.domain.ids.DeckId
import org.junit.Assert.assertEquals
import org.junit.Test

class SetDefaultDeckUseCaseTest {

    @Test
    fun `invoke normalizes deck id before persisting selection`() {
        val repository = FakeDefaultDeckSelectionForSetRepository()
        val useCase = SetDefaultDeckUseCase(repository)

        useCase("  deck-1  ")

        assertEquals("deck-1", repository.lastSetDefaultDeckId)
        assertEquals(0, repository.clearDefaultDeckIdCalls)
    }

    @Test
    fun `invoke clears default deck when id is blank`() {
        val repository = FakeDefaultDeckSelectionForSetRepository()
        val useCase = SetDefaultDeckUseCase(repository)

        useCase("   ")

        assertEquals(null, repository.lastSetDefaultDeckId)
        assertEquals(1, repository.clearDefaultDeckIdCalls)
    }
}

private class FakeDefaultDeckSelectionForSetRepository : DefaultDeckSelectionRepository {
    var lastSetDefaultDeckId: String? = null
    var clearDefaultDeckIdCalls: Int = 0

    override fun getDefaultDeckId(): String = ""

    override fun setDefaultDeckId(deckId: DeckId) {
        lastSetDefaultDeckId = deckId.value
    }

    override fun clearDefaultDeckId() {
        clearDefaultDeckIdCalls += 1
    }
}
