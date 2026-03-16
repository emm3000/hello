@file:Suppress("CyclomaticComplexMethod", "ReturnCount")

package com.emm.data.sync

import com.emm.data.HelloDb
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.Instant

class ApplyRemoteOperation(
    private val db: HelloDb,
) {

    private val deckQueries = db.deckQueries
    private val flashcardQueries = db.flashcardQueries
    private val flashcardExampleQueries = db.flashcardExampleQueries
    private val quotesQueries = db.quotesQueries
    private val localFirstQueries = db.localFirstQueries

    operator fun invoke(
        operation: RemoteSyncOperation,
        localDeviceId: String,
    ): ApplyRemoteOperationResult {
        return when (operation.entityType.lowercase()) {
            "deck" -> applyDeck(operation, localDeviceId)
            "flashcard" -> applyFlashcard(operation, localDeviceId)
            "flashcard_example" -> applyFlashcardExample(operation, localDeviceId)
            "quote" -> applyQuote(operation, localDeviceId)
            "review_event" -> applyReviewEvent(operation, localDeviceId)
            else -> ApplyRemoteOperationResult.Skipped(
                reason = "unsupported_entity_type:${operation.entityType}"
            )
        }
    }

    private fun applyDeck(operation: RemoteSyncOperation, localDeviceId: String): ApplyRemoteOperationResult {
        val existing = deckQueries.findById(operation.entityId).executeAsOneOrNull()
        if (existing != null && operation.lamport < existing.versionLamport) {
            return ApplyRemoteOperationResult.Skipped(reason = "stale_lamport")
        }

        val payload = operation.payload
        val operationType = operation.operationType.lowercase()
        val payloadName = payload.stringAny("name")
        val payloadDescription = payload.stringAny("description")
        val payloadCreatedAt = payload.epochAny("createdAt", "created_at")
        val payloadUpdatedAt = payload.epochAny("updatedAt", "updated_at")
        val payloadDeletedAt = payload.epochAny("deletedAt", "deleted_at")
        val operationCreatedAt = parseIsoToEpoch(operation.createdAt) ?: Instant.now().toEpochMilli()

        if (operationType != "delete" && payloadName.isNullOrBlank()) {
            return ApplyRemoteOperationResult.Deferred(reason = "missing_required_field:name")
        }

        val createdAt = existing?.createdAt ?: payloadCreatedAt ?: operationCreatedAt
        val updatedAt = payloadUpdatedAt ?: operationCreatedAt
        val deletedAt = when (operationType) {
            "delete" -> payloadDeletedAt ?: operationCreatedAt
            else -> payloadDeletedAt
        }
        val name = when (operationType) {
            "delete" -> existing?.name ?: payloadName ?: "[deleted]"
            else -> payloadName.orEmpty()
        }
        val description = payloadDescription ?: existing?.description

        deckQueries.insert(
            id = operation.entityId,
            name = name,
            description = description,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            originDeviceId = existing?.originDeviceId ?: operation.originDeviceId.ifBlank { localDeviceId },
            lastModifiedByDeviceId = operation.originDeviceId.ifBlank { localDeviceId },
            versionLamport = operation.lamport,
        )
        return ApplyRemoteOperationResult.Applied
    }

    private fun applyFlashcard(operation: RemoteSyncOperation, localDeviceId: String): ApplyRemoteOperationResult {
        val existing = flashcardQueries.findById(operation.entityId).executeAsOneOrNull()
        if (existing != null && operation.lamport < existing.versionLamport) {
            return ApplyRemoteOperationResult.Skipped(reason = "stale_lamport")
        }

        val payload = operation.payload
        val operationType = operation.operationType.lowercase()
        val deckId = payload.stringAny("deckId", "deck_id") ?: existing?.deckId
        val word = payload.stringAny("word")
        val meaning = payload.stringAny("meaning")

        if (deckId.isNullOrBlank()) {
            return ApplyRemoteOperationResult.Deferred(reason = "missing_required_field:deck_id")
        }

        if (operationType != "delete" && (word.isNullOrBlank() || meaning.isNullOrBlank())) {
            return ApplyRemoteOperationResult.Deferred(reason = "missing_required_field:word_or_meaning")
        }

        if (deckQueries.findById(deckId).executeAsOneOrNull() == null) {
            return ApplyRemoteOperationResult.Deferred(reason = "missing_parent_deck")
        }

        val createdAt = existing?.createdAt
            ?: payload.epochAny("createdAt", "created_at")
            ?: parseIsoToEpoch(operation.createdAt)
            ?: Instant.now().toEpochMilli()
        val updatedAt = payload.epochAny("updatedAt", "updated_at")
            ?: parseIsoToEpoch(operation.createdAt)
            ?: Instant.now().toEpochMilli()
        val deletedAt = when (operationType) {
            "delete" -> payload.epochAny("deletedAt", "deleted_at") ?: updatedAt
            else -> payload.epochAny("deletedAt", "deleted_at")
        }

        flashcardQueries.create(
            id = operation.entityId,
            deckId = deckId,
            word = if (operationType == "delete") existing?.word ?: word.orEmpty() else word.orEmpty(),
            meaning = if (operationType == "delete") existing?.meaning ?: meaning.orEmpty() else meaning.orEmpty(),
            translation = payload.stringAny("translation") ?: existing?.translation,
            phonetic = payload.stringAny("phonetic") ?: existing?.phonetic,
            partOfSpeech = payload.stringAny("partOfSpeech", "part_of_speech") ?: existing?.partOfSpeech,
            type = payload.stringAny("type") ?: existing?.type,
            note = payload.stringAny("note") ?: existing?.note,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            originDeviceId = existing?.originDeviceId ?: operation.originDeviceId.ifBlank { localDeviceId },
            lastModifiedByDeviceId = operation.originDeviceId.ifBlank { localDeviceId },
            versionLamport = operation.lamport,
        )

        return ApplyRemoteOperationResult.Applied
    }

    private fun applyFlashcardExample(
        operation: RemoteSyncOperation,
        localDeviceId: String,
    ): ApplyRemoteOperationResult {
        val existing = flashcardExampleQueries.findById(operation.entityId).executeAsOneOrNull()
        if (existing != null && operation.lamport < existing.versionLamport) {
            return ApplyRemoteOperationResult.Skipped(reason = "stale_lamport")
        }

        val payload = operation.payload
        val operationType = operation.operationType.lowercase()
        val flashcardId = payload.stringAny("flashcardId", "flashcard_id") ?: existing?.flashcardId

        if (flashcardId.isNullOrBlank()) {
            return ApplyRemoteOperationResult.Deferred(reason = "missing_required_field:flashcard_id")
        }

        if (flashcardQueries.findById(flashcardId).executeAsOneOrNull() == null) {
            return ApplyRemoteOperationResult.Deferred(reason = "missing_parent_flashcard")
        }

        val text = payload.stringAny("text")
        val translation = payload.stringAny("translation")
        val type = payload.stringAny("type")

        if (isMissingExamplePayload(operationType, text, translation, type)) {
            return ApplyRemoteOperationResult.Deferred(reason = "missing_required_field:example_payload")
        }

        val createdAt = existing?.createdAt
            ?: payload.epochAny("createdAt", "created_at")
            ?: parseIsoToEpoch(operation.createdAt)
            ?: Instant.now().toEpochMilli()
        val updatedAt = payload.epochAny("updatedAt", "updated_at")
            ?: parseIsoToEpoch(operation.createdAt)
            ?: Instant.now().toEpochMilli()
        val deletedAt = when (operationType) {
            "delete" -> payload.epochAny("deletedAt", "deleted_at") ?: updatedAt
            else -> payload.epochAny("deletedAt", "deleted_at")
        }

        flashcardExampleQueries.insert(
            id = operation.entityId,
            flashcardId = flashcardId,
            text = if (operationType == "delete") existing?.text ?: text.orEmpty() else text.orEmpty(),
            translation = if (operationType == "delete") {
                existing?.translation ?: translation.orEmpty()
            } else {
                translation.orEmpty()
            },
            type = if (operationType == "delete") existing?.type ?: type.orEmpty() else type.orEmpty(),
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            originDeviceId = existing?.originDeviceId ?: operation.originDeviceId.ifBlank { localDeviceId },
            lastModifiedByDeviceId = operation.originDeviceId.ifBlank { localDeviceId },
            versionLamport = operation.lamport,
        )

        return ApplyRemoteOperationResult.Applied
    }

    private fun applyQuote(operation: RemoteSyncOperation, localDeviceId: String): ApplyRemoteOperationResult {
        val existing = quotesQueries.findById(operation.entityId).executeAsOneOrNull()
        if (existing != null && operation.lamport < existing.versionLamport) {
            return ApplyRemoteOperationResult.Skipped(reason = "stale_lamport")
        }

        val payload = operation.payload
        val operationType = operation.operationType.lowercase()
        val phrase = payload.stringAny("phrase")
        val translation = payload.stringAny("translation")
        val category = payload.stringAny("category")

        if (isMissingQuotePayload(operationType, phrase, translation, category)) {
            return ApplyRemoteOperationResult.Deferred(reason = "missing_required_field:quote_payload")
        }

        val now = parseIsoToEpoch(operation.createdAt) ?: Instant.now().toEpochMilli()
        val createdAt = existing?.createdAt ?: payload.epochAny("createdAt", "created_at") ?: now
        val updatedAt = payload.epochAny("updatedAt", "updated_at") ?: now
        val deletedAt = when (operationType) {
            "delete" -> payload.epochAny("deletedAt", "deleted_at") ?: updatedAt
            else -> payload.epochAny("deletedAt", "deleted_at")
        }

        quotesQueries.insert(
            id = operation.entityId,
            title = payload.stringAny("title") ?: existing?.title ?: (phrase ?: "[deleted]"),
            phrase = if (operationType == "delete") existing?.phrase ?: (phrase ?: "[deleted]") else phrase.orEmpty(),
            description = payload.stringAny("description") ?: existing?.description.orEmpty(),
            translation = if (operationType == "delete") {
                existing?.translation ?: translation.orEmpty()
            } else {
                translation.orEmpty()
            },
            example = payload.stringAny("example") ?: existing?.example.orEmpty(),
            context = payload.stringAny("context") ?: existing?.context.orEmpty(),
            pronunciation = payload.stringAny("pronunciation") ?: existing?.pronunciation.orEmpty(),
            formality = payload.stringAny("formality") ?: existing?.formality.orEmpty(),
            tags = payload.stringAny("tags") ?: existing?.tags.orEmpty(),
            category = if (operationType == "delete") existing?.category ?: category.orEmpty() else category.orEmpty(),
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            originDeviceId = existing?.originDeviceId ?: operation.originDeviceId.ifBlank { localDeviceId },
            lastModifiedByDeviceId = operation.originDeviceId.ifBlank { localDeviceId },
            versionLamport = operation.lamport,
        )

        return ApplyRemoteOperationResult.Applied
    }

    private fun applyReviewEvent(operation: RemoteSyncOperation, localDeviceId: String): ApplyRemoteOperationResult {
        val payload = operation.payload
        val eventId = operation.entityId

        if (localFirstQueries.findReviewEventById(eventId).executeAsOneOrNull() != null) {
            return ApplyRemoteOperationResult.Skipped(reason = "review_event_exists")
        }

        val flashcardId = payload.stringAny("flashcardId", "flashcard_id")
            ?: return ApplyRemoteOperationResult.Deferred(reason = "missing_required_field:flashcard_id")

        if (flashcardQueries.findById(flashcardId).executeAsOneOrNull() == null) {
            return ApplyRemoteOperationResult.Deferred(reason = "missing_parent_flashcard")
        }

        val reviewedAt = payload.epochAny("reviewedAt", "reviewed_at")
            ?: return ApplyRemoteOperationResult.Deferred(reason = "missing_required_field:reviewed_at")
        val nextReviewAt = payload.epochAny("nextReviewAt", "next_review_at")
            ?: return ApplyRemoteOperationResult.Deferred(reason = "missing_required_field:next_review_at")
        val easeFactor = payload.doubleAny("easeFactor", "ease_factor")
            ?: return ApplyRemoteOperationResult.Deferred(reason = "missing_required_field:ease_factor")
        val interval = payload.longAny("interval", "interval_days")
            ?: return ApplyRemoteOperationResult.Deferred(reason = "missing_required_field:interval")
        val repetitions = payload.longAny("repetitions")
            ?: return ApplyRemoteOperationResult.Deferred(reason = "missing_required_field:repetitions")
        val lapses = payload.longAny("lapses")
            ?: return ApplyRemoteOperationResult.Deferred(reason = "missing_required_field:lapses")

        val createdAt = payload.epochAny("createdAt", "created_at")
            ?: parseIsoToEpoch(operation.createdAt)
            ?: Instant.now().toEpochMilli()

        localFirstQueries.insertReviewEvent(
            eventId = eventId,
            flashcardId = flashcardId,
            grade = payload.stringAny("grade") ?: "review",
            reviewedAt = reviewedAt,
            nextReviewAt = nextReviewAt,
            easeFactor = easeFactor,
            interval = interval,
            repetitions = repetitions,
            lapses = lapses,
            originDeviceId = operation.originDeviceId.ifBlank { localDeviceId },
            versionLamport = operation.lamport,
            createdAt = createdAt,
        )

        val existingProjection = localFirstQueries.findReviewProjectionByFlashcardId(flashcardId).executeAsOneOrNull()
        if (existingProjection == null || reviewedAt >= existingProjection.lastReviewedAt) {
            localFirstQueries.upsertReviewProjection(
                flashcardId = flashcardId,
                lastReviewedAt = reviewedAt,
                nextReviewAt = nextReviewAt,
                easeFactor = easeFactor,
                interval = interval,
                repetitions = repetitions,
                lapses = lapses,
                sourceEventId = eventId,
                updatedAt = createdAt,
            )
        }

        return ApplyRemoteOperationResult.Applied
    }

    private fun JsonObject.stringAny(vararg keys: String): String? {
        keys.forEach { key ->
            val value = this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            if (value != null) return value
        }
        return null
    }

    private fun JsonObject.longAny(vararg keys: String): Long? {
        keys.forEach { key ->
            val element = this[key] ?: return@forEach
            val primitive = element as? JsonPrimitive ?: return@forEach
            primitive.longOrNull?.let { return it }
            primitive.contentOrNull?.toLongOrNull()?.let { return it }
        }
        return null
    }

    private fun JsonObject.doubleAny(vararg keys: String): Double? {
        keys.forEach { key ->
            val element = this[key] ?: return@forEach
            val primitive = element as? JsonPrimitive ?: return@forEach
            primitive.doubleOrNull?.let { return it }
            primitive.contentOrNull?.toDoubleOrNull()?.let { return it }
        }
        return null
    }

    private fun JsonObject.epochAny(vararg keys: String): Long? {
        longAny(*keys)?.let { return it }
        keys.forEach { key ->
            val raw = this[key]?.jsonPrimitive?.contentOrNull
            parseIsoToEpoch(raw)?.let { return it }
        }
        return null
    }

    private fun parseIsoToEpoch(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
    }

    private fun isMissingExamplePayload(
        operationType: String,
        text: String?,
        translation: String?,
        type: String?,
    ): Boolean {
        if (operationType == "delete") return false

        return listOf(text, translation, type).any { it.isNullOrBlank() }
    }

    private fun isMissingQuotePayload(
        operationType: String,
        phrase: String?,
        translation: String?,
        category: String?,
    ): Boolean {
        if (operationType == "delete") return false

        return listOf(phrase, translation, category).any { it.isNullOrBlank() }
    }
}

sealed interface ApplyRemoteOperationResult {
    val shouldAck: Boolean

    data object Applied : ApplyRemoteOperationResult {
        override val shouldAck: Boolean = true
    }

    data class Skipped(
        val reason: String,
    ) : ApplyRemoteOperationResult {
        override val shouldAck: Boolean = true
    }

    data class Deferred(
        val reason: String,
    ) : ApplyRemoteOperationResult {
        override val shouldAck: Boolean = false
    }
}
