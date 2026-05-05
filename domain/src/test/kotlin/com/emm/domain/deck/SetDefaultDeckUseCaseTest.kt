package com.emm.domain.deck

import org.junit.Assert.assertEquals
import org.junit.Test

class SetDefaultDeckUseCaseTest {

    @Test
    fun `invoke normalizes deck id before persisting selection`() {
        val repository = FakeDefaultDeckSelectionForSetRepository()
        val useCase = SetDefaultDeckUseCase(repository)

        useCase("  deck-1  ")

        assertEquals("deck-1", repository.lastSetDefaultDeckId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke rejects blank deck id`() {
        val useCase = SetDefaultDeckUseCase(FakeDefaultDeckSelectionForSetRepository())

        useCase("   ")
    }
}

private class FakeDefaultDeckSelectionForSetRepository : DefaultDeckSelectionRepository {
    var lastSetDefaultDeckId: String? = null

    override fun getDefaultDeckId(): String = ""

    override fun setDefaultDeckId(deckId: String) {
        lastSetDefaultDeckId = deckId
    }
}
