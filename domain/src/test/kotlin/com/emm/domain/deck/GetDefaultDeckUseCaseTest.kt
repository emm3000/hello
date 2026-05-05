package com.emm.domain.deck

import com.emm.domain.ids.DeckId
import org.junit.Assert.assertEquals
import org.junit.Test

class GetDefaultDeckUseCaseTest {

    @Test
    fun `invoke normalizes deck id from repository`() {
        val repository = FakeDefaultDeckSelectionRepository(defaultDeckId = "  deck-1  ")
        val useCase = GetDefaultDeckUseCase(repository)

        val result = useCase()

        assertEquals("deck-1", result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke rejects blank deck id from repository`() {
        val useCase = GetDefaultDeckUseCase(
            FakeDefaultDeckSelectionRepository(defaultDeckId = "   ")
        )

        useCase()
    }
}

private class FakeDefaultDeckSelectionRepository(
    private val defaultDeckId: String,
) : DefaultDeckSelectionRepository {
    override fun getDefaultDeckId(): String = defaultDeckId

    override fun setDefaultDeckId(deckId: DeckId) = Unit
}
