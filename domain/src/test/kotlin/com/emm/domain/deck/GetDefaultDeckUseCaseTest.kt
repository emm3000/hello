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

    @Test
    fun `invoke returns empty when repository has blank deck id`() {
        val useCase = GetDefaultDeckUseCase(
            FakeDefaultDeckSelectionRepository(defaultDeckId = "   ")
        )

        val result = useCase()

        assertEquals("", result)
    }
}

private class FakeDefaultDeckSelectionRepository(
    private val defaultDeckId: String,
) : DefaultDeckSelectionRepository {
    override fun getDefaultDeckId(): String = defaultDeckId

    override fun setDefaultDeckId(deckId: DeckId) = Unit

    override fun clearDefaultDeckId() = Unit
}
