package com.emm.data.flashcard

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.emm.data.FlashcardQueries
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalFirstWrite
import com.emm.domain.flashcard.EnrichmentBacklog
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.FlashcardEnrichmentRepository
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@LocalFirstWrite
class DefaultFlashcardEnrichmentRepository(
    db: HelloDb,
    private val ioDispatcher: CoroutineDispatcher,
) : FlashcardEnrichmentRepository {

    private val dao: FlashcardQueries = db.flashcardQueries

    override fun observeBacklog(): Flow<EnrichmentBacklog> {
        return dao
            .countsByEnrichmentStatus()
            .asFlow()
            .mapToOne(ioDispatcher)
            .map { EnrichmentBacklog(pending = it.pending.toInt(), failed = it.failed.toInt()) }
    }

    override suspend fun findIdsByStatus(status: EnrichmentStatus): List<FlashcardId> =
        withContext(ioDispatcher) {
            dao.findIdsByEnrichmentStatus(status.name).executeAsList().map(String::toFlashcardId)
        }

    override suspend fun markPending(ids: List<FlashcardId>): Unit = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext
        dao.markPendingEnrichment(updatedAt = Instant.now().toEpochMilli(), ids = ids.map(FlashcardId::value))
    }
}
