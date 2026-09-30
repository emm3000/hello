package com.emm.data.flashcard

import com.emm.data.FlashcardQueries
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalFirstWrite
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.FlashcardEnrichmentRepository
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@LocalFirstWrite
class DefaultFlashcardEnrichmentRepository(
    db: HelloDb,
    private val ioDispatcher: CoroutineDispatcher,
) : FlashcardEnrichmentRepository {

    private val dao: FlashcardQueries = db.flashcardQueries

    override suspend fun findIdsByStatus(status: EnrichmentStatus): List<FlashcardId> =
        withContext(ioDispatcher) {
            dao.findIdsByEnrichmentStatus(status.name).executeAsList().map(String::toFlashcardId)
        }

    override suspend fun markPending(ids: List<FlashcardId>): Unit = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext
        dao.markPendingEnrichment(updatedAt = Instant.now().toEpochMilli(), ids = ids.map(FlashcardId::value))
    }
}
