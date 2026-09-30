package com.emm.data.library

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.FlashcardQueries
import com.emm.data.HelloDb
import com.emm.data.LibraryFlashcards
import com.emm.data.flashcard.enrichmentFailureOf
import com.emm.data.flashcard.toEnrichmentStatus
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.library.LibraryFlashcard
import com.emm.domain.library.LibraryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultLibraryRepository(
    db: HelloDb,
    private val ioDispatcher: CoroutineDispatcher,
) : LibraryRepository {

    private val dao: FlashcardQueries = db.flashcardQueries

    override fun observeLibrary(): Flow<List<LibraryFlashcard>> = dao
        .libraryFlashcards()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { rows -> rows.map(LibraryFlashcards::toLibraryFlashcard) }
}

private fun LibraryFlashcards.toLibraryFlashcard(): LibraryFlashcard {
    val status: EnrichmentStatus = toEnrichmentStatus(enrichmentStatus)
    return LibraryFlashcard(
        id = id.toFlashcardId(),
        deckId = deckId.toDeckId(),
        deckName = deckName,
        word = word,
        translation = translation.orEmpty(),
        meaning = meaning,
        enrichmentStatus = status,
        enrichmentFailure = enrichmentFailureOf(status, enrichmentFailureCode, enrichmentFailureReason),
        nextReviewAt = nextReviewAt,
    )
}
