package com.emm.data.flashcard

import com.emm.data.HelloDb
import com.emm.domain.flashcard.ExactDuplicateKey
import com.emm.domain.flashcard.Expression
import com.emm.domain.flashcard.FlashcardDuplicateRepository
import com.emm.domain.ids.DeckId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultFlashcardDuplicateRepository(
    db: HelloDb,
) : FlashcardDuplicateRepository {

    private val queries = db.flashcardQueries

    override suspend fun existsExactDuplicate(key: ExactDuplicateKey): Boolean = withContext(Dispatchers.IO) {
        queries.existsExactDuplicate(
            deckId = key.deckId.value,
            normalizedExpression = key.expression.canonical,
            normalizedIntendedMeaningEs = key.intendedMeaningEs.canonical,
            normalizedNoteType = key.noteType.name.lowercase(Locale.ROOT),
        ).executeAsOne()
    }

    override suspend fun existsExpressionInDeck(
        deckId: DeckId,
        expression: Expression,
    ): Boolean = withContext(Dispatchers.IO) {
        queries.existsExpressionInDeck(
            deckId = deckId.value,
            normalizedExpression = expression.canonical,
        ).executeAsOne()
    }
}
