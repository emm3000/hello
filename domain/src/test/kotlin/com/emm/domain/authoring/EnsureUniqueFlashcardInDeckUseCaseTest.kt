package com.emm.domain.authoring

import com.emm.domain.flashcard.ExactDuplicateKey
import com.emm.domain.flashcard.FlashcardDuplicateRepository
import com.emm.domain.flashcard.Expression
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.toDeckId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class EnsureUniqueFlashcardInDeckUseCaseTest {

    @Test
    fun `invoke throws when duplicate exists`() = runTest {
        val useCase = EnsureUniqueFlashcardInDeckUseCase(
            isExactDuplicateGeneratedNoteUseCase = IsExactDuplicateGeneratedNoteUseCase(
                repository = DuplicateRepo(exists = true),
            )
        )

        assertFailsWith<com.emm.domain.validation.DomainValidationException> {
            useCase(deckId = "deck-1".toDeckId(), note = sampleWordNote())
        }
    }

    @Test
    fun `invoke completes when duplicate does not exist`() = runTest {
        val useCase = EnsureUniqueFlashcardInDeckUseCase(
            isExactDuplicateGeneratedNoteUseCase = IsExactDuplicateGeneratedNoteUseCase(
                repository = DuplicateRepo(exists = false),
            )
        )

        useCase(deckId = "deck-1".toDeckId(), note = sampleWordNote())
    }
}

private class DuplicateRepo(
    private val exists: Boolean,
) : FlashcardDuplicateRepository {
    override suspend fun existsExactDuplicate(key: ExactDuplicateKey): Boolean = exists

    override suspend fun existsExpressionInDeck(deckId: DeckId, expression: Expression): Boolean = false
}
