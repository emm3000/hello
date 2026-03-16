package com.emm.domain.sync

sealed interface SyncOperationPayload {
    val entityId: String
    val operationType: OperationType
}

data class DeckSyncPayload(
    override val entityId: String,
    override val operationType: OperationType,
    val name: String,
    val description: String,
) : SyncOperationPayload

data class FlashcardSyncPayload(
    override val entityId: String,
    override val operationType: OperationType,
    val deckId: String,
    val word: String,
    val meaning: String,
) : SyncOperationPayload

data class FlashcardExampleSyncPayload(
    override val entityId: String,
    override val operationType: OperationType,
    val flashcardId: String,
    val text: String,
    val translation: String,
    val type: String,
) : SyncOperationPayload

data class QuoteSyncPayload(
    override val entityId: String,
    override val operationType: OperationType,
    val phrase: String,
    val translation: String,
    val category: String,
) : SyncOperationPayload

data class ReviewEventSyncPayload(
    override val entityId: String,
    override val operationType: OperationType = OperationType.AppendEvent,
    val flashcardId: String,
    val reviewedAt: Long,
    val nextReviewAt: Long,
    val easeFactor: Double,
    val interval: Long,
    val repetitions: Long,
    val lapses: Long,
) : SyncOperationPayload
