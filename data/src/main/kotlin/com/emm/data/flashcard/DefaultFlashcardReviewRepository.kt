package com.emm.data.flashcard

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.localfirst.LocalFirstWrite
import com.emm.data.localfirst.OperationLogWriter
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.sync.OperationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

@LocalFirstWrite
class DefaultFlashcardReviewRepository(
    private val db: HelloDb,
    private val operationLogWriter: OperationLogWriter,
    private val localDeviceIdentityProvider: LocalDeviceIdentityProvider,
) : FlashcardReviewRepository {

    private val localFirstQueries = db.localFirstQueries

    override fun all(): Flow<List<FlashcardReview>> {
        return localFirstQueries
            .allReviewProjections()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { projections -> projections.map { it.toDomainFromProjection() } }
    }

    override suspend fun update(flashcardReview: FlashcardReview) = withContext(Dispatchers.IO) {
        val now = Instant.now().toEpochMilli()
        val eventId = UUID.randomUUID().toString()
        val deviceId = localDeviceIdentityProvider.getOrCreateDeviceId()

        db.transaction {
            val payloadJson = buildJsonObject {
                put("entityId", eventId)
                put("operationType", OperationType.AppendEvent.name)
                put("flashcardId", flashcardReview.flashcardId)
                put("reviewedAt", flashcardReview.lastReviewedAt)
                put("nextReviewAt", flashcardReview.nextReviewAt)
                put("easeFactor", flashcardReview.easeFactor)
                put("interval", flashcardReview.interval)
                put("repetitions", flashcardReview.repetitions)
                put("lapses", flashcardReview.lapses)
            }.toString()
            val lamport = operationLogWriter.appendOperation(
                entityType = "review_event",
                entityId = eventId,
                operationType = OperationType.AppendEvent,
                payloadJson = payloadJson,
                originDeviceId = deviceId,
                createdAt = now,
            )
            localFirstQueries.insertReviewEvent(
                eventId = eventId,
                flashcardId = flashcardReview.flashcardId,
                grade = "review",
                reviewedAt = flashcardReview.lastReviewedAt,
                nextReviewAt = flashcardReview.nextReviewAt,
                easeFactor = flashcardReview.easeFactor,
                interval = flashcardReview.interval,
                repetitions = flashcardReview.repetitions,
                lapses = flashcardReview.lapses,
                originDeviceId = deviceId,
                versionLamport = lamport,
                createdAt = now,
            )
            localFirstQueries.upsertReviewProjection(
                flashcardId = flashcardReview.flashcardId,
                lastReviewedAt = flashcardReview.lastReviewedAt,
                nextReviewAt = flashcardReview.nextReviewAt,
                easeFactor = flashcardReview.easeFactor,
                interval = flashcardReview.interval,
                repetitions = flashcardReview.repetitions,
                lapses = flashcardReview.lapses,
                sourceEventId = eventId,
                updatedAt = now,
            )
        }
        Unit
    }
}
