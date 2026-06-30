package com.emm.data.flashcard

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.HelloDb
import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.ids.DeckId
import com.emm.domain.study.StudyFlashcard
import com.emm.domain.study.StudySessionRepository
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class DefaultStudySessionRepository(
    private val db: HelloDb,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher,
) : StudySessionRepository {

    private val dao = db.flashcardQueries

    override suspend fun sessionToday(deckId: DeckId): List<StudyFlashcard> = withContext(ioDispatcher) {
        val flashcardsToReviewByDeck = dao.flashcardsToReviewByDeck(
            deckId = deckId.value,
            now = Instant.now().toEpochMilli(),
        ).executeAsList()

        flashcardsToReviewByDeck.map {
            val card: FsrsCard = mapFsrsCard(it)
            toStudyFlashcard(it, card, json)
        }
    }

    override suspend fun sessionTodayAllDecks(): List<StudyFlashcard> = withContext(ioDispatcher) {
        val flashcardsToReviewAllDecks = dao.flashcardsToReviewAllDecks(
            now = Instant.now().toEpochMilli(),
        ).executeAsList()

        flashcardsToReviewAllDecks.map {
            val card: FsrsCard = mapFsrsCard(it)
            toStudyFlashcard(it, card, json)
        }
    }

    override fun flashcardWithReview(deckId: DeckId): Flow<List<StudyFlashcard>> {
        return dao.flashcardsWithReview(deckId.value).asFlow()
            .mapToList(ioDispatcher)
            .map { list ->
                list.map {
                    // mapFsrsCard reuses the same null-guarded mapping as the due-card queries:
                    // never-reviewed cards (no projection row) resolve to a fresh FsrsCard.new(...).
                    val card: FsrsCard = mapFsrsCard(it)
                    toStudyFlashcard(it, card, json)
                }
            }
    }
}
