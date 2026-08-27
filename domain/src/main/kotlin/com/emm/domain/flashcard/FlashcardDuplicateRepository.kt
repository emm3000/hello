package com.emm.domain.flashcard

import com.emm.domain.ids.DeckId

interface FlashcardDuplicateRepository {

    suspend fun existsExactDuplicate(key: ExactDuplicateKey): Boolean

    suspend fun existsExpressionInDeck(deckId: DeckId, expression: Expression): Boolean
}
