package com.emm.data.sync

import com.emm.data.HelloDb
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.Instant

@Suppress("LargeClass")
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
        val originDeviceId = operation.originDeviceId.ifBlank { localDeviceId }
        if (isStale(existing?.versionLamport, existing?.lastModifiedByDeviceId, operation.lamport, originDeviceId)) {
            return ApplyRemoteOperationResult.Skipped(reason = "stale_lamport")
        }

        val payload = operation.payload
        val operationType = operation.operationType.lowercase()
        val payloadName = payload.stringAny("name")
        val deferredReason = missingDeckReason(operationType = operationType, payloadName = payloadName)
        if (deferredReason != null) {
            return ApplyRemoteOperationResult.Deferred(reason = deferredReason)
        }

        val values = resolveDeckValues(
            existing = existing,
            payload = payload,
            payloadName = payloadName,
            operationType = operationType,
            operationCreatedAt = parseIsoToEpoch(operation.createdAt) ?: Instant.now().toEpochMilli(),
        )

        deckQueries.insert(
            id = operation.entityId,
            name = values.name,
            description = values.description,
            createdAt = values.createdAt,
            updatedAt = values.updatedAt,
            deletedAt = values.deletedAt,
            originDeviceId = existing?.originDeviceId ?: originDeviceId,
            lastModifiedByDeviceId = originDeviceId,
            versionLamport = operation.lamport,
        )

        return ApplyRemoteOperationResult.Applied
    }

    private fun applyFlashcard(operation: RemoteSyncOperation, localDeviceId: String): ApplyRemoteOperationResult {
        val existing = flashcardQueries.findById(operation.entityId).executeAsOneOrNull()
        val originDeviceId = operation.originDeviceId.ifBlank { localDeviceId }
        if (isStale(existing?.versionLamport, existing?.lastModifiedByDeviceId, operation.lamport, originDeviceId)) {
            return ApplyRemoteOperationResult.Skipped(reason = "stale_lamport")
        }

        val payload = operation.payload
        val operationType = operation.operationType.lowercase()
        val seed = extractFlashcardSeed(payload, existing)

        val deferredReason = missingFlashcardReason(
            deckId = seed.deckId,
            operationType = operationType,
            word = seed.word,
            meaning = seed.meaning,
        )
        if (deferredReason != null) {
            return ApplyRemoteOperationResult.Deferred(reason = deferredReason)
        }

        val values = resolveFlashcardValues(
            existing = existing,
            payload = payload,
            operationType = operationType,
            word = seed.word,
            meaning = seed.meaning,
            operationCreatedAt = parseIsoToEpoch(operation.createdAt) ?: Instant.now().toEpochMilli(),
        )

        upsertResolvedFlashcard(
            entityId = operation.entityId,
            deckId = checkNotNull(seed.deckId),
            values = values,
            originDeviceId = existing?.originDeviceId ?: originDeviceId,
            lastModifiedByDeviceId = originDeviceId,
            versionLamport = operation.lamport,
        )

        return ApplyRemoteOperationResult.Applied
    }

    private fun extractFlashcardSeed(
        payload: JsonObject,
        existing: com.emm.data.Flashcard?,
    ): FlashcardSeed {
        return FlashcardSeed(
            deckId = payload.stringAny("deckId", "deck_id") ?: existing?.deckId,
            word = payload.stringAny("word"),
            meaning = payload.stringAny("meaning"),
        )
    }

    private fun upsertResolvedFlashcard(
        entityId: String,
        deckId: String,
        values: FlashcardValues,
        originDeviceId: String,
        lastModifiedByDeviceId: String,
        versionLamport: Long,
    ) {
        flashcardQueries.create(
            id = entityId,
            deckId = deckId,
            word = values.word,
            meaning = values.meaning,
            translation = values.translation,
            phonetic = values.phonetic,
            partOfSpeech = values.partOfSpeech,
            type = values.type,
            note = values.note,
            register = values.register,
            levelBand = values.levelBand,
            domain = values.domain,
            lemma = values.lemma,
            whyUseful = values.whyUseful,
            usagePattern = values.usagePattern,
            irregularFormsJson = values.irregularFormsJson,
            collocationsJson = values.collocationsJson,
            commonMistake = values.commonMistake,
            confusableWithJson = values.confusableWithJson,
            clozeSentence = values.clozeSentence,
            sourceContext = values.sourceContext,
            warningsJson = values.warningsJson,
            studyCardsJson = values.studyCardsJson,
            qualityChecksJson = values.qualityChecksJson,
            createdAt = values.createdAt,
            updatedAt = values.updatedAt,
            deletedAt = values.deletedAt,
            originDeviceId = originDeviceId,
            lastModifiedByDeviceId = lastModifiedByDeviceId,
            versionLamport = versionLamport,
        )
    }

    private fun applyFlashcardExample(
        operation: RemoteSyncOperation,
        localDeviceId: String,
    ): ApplyRemoteOperationResult {
        val existing = flashcardExampleQueries.findById(operation.entityId).executeAsOneOrNull()
        val originDeviceId = operation.originDeviceId.ifBlank { localDeviceId }
        if (isStale(existing?.versionLamport, existing?.lastModifiedByDeviceId, operation.lamport, originDeviceId)) {
            return ApplyRemoteOperationResult.Skipped(reason = "stale_lamport")
        }

        val payload = operation.payload
        val operationType = operation.operationType.lowercase()
        val flashcardId = payload.stringAny("flashcardId", "flashcard_id") ?: existing?.flashcardId
        val text = payload.stringAny("text")
        val translation = payload.stringAny("translation")
        val type = payload.stringAny("type")

        val deferredReason = missingFlashcardExampleReason(
            flashcardId = flashcardId,
            operationType = operationType,
            text = text,
            translation = translation,
            type = type,
        )
        if (deferredReason != null) {
            return ApplyRemoteOperationResult.Deferred(reason = deferredReason)
        }

        val values = resolveFlashcardExampleValues(
            existing = existing,
            payload = payload,
            operationType = operationType,
            text = text,
            translation = translation,
            type = type,
            operationCreatedAt = parseIsoToEpoch(operation.createdAt) ?: Instant.now().toEpochMilli(),
        )

        flashcardExampleQueries.insert(
            id = operation.entityId,
            flashcardId = checkNotNull(flashcardId), // guaranteed non-null by missingFlashcardExampleReason guard above
            text = values.text,
            translation = values.translation,
            type = values.type,
            createdAt = values.createdAt,
            updatedAt = values.updatedAt,
            deletedAt = values.deletedAt,
            originDeviceId = existing?.originDeviceId ?: originDeviceId,
            lastModifiedByDeviceId = originDeviceId,
            versionLamport = operation.lamport,
        )

        return ApplyRemoteOperationResult.Applied
    }

    private fun applyQuote(operation: RemoteSyncOperation, localDeviceId: String): ApplyRemoteOperationResult {
        val existing = quotesQueries.findById(operation.entityId).executeAsOneOrNull()
        val originDeviceId = operation.originDeviceId.ifBlank { localDeviceId }
        if (isStale(existing?.versionLamport, existing?.lastModifiedByDeviceId, operation.lamport, originDeviceId)) {
            return ApplyRemoteOperationResult.Skipped(reason = "stale_lamport")
        }

        val payload = operation.payload
        val operationType = operation.operationType.lowercase()
        val phrase = payload.stringAny("phrase")
        val translation = payload.stringAny("translation")
        val category = payload.stringAny("category")

        val deferredReason = missingQuoteReason(
            operationType = operationType,
            phrase = phrase,
            translation = translation,
            category = category,
        )
        if (deferredReason != null) {
            return ApplyRemoteOperationResult.Deferred(reason = deferredReason)
        }

        val values = resolveQuoteValues(
            existing = existing,
            payload = payload,
            operationType = operationType,
            phrase = phrase,
            translation = translation,
            category = category,
            operationCreatedAt = parseIsoToEpoch(operation.createdAt) ?: Instant.now().toEpochMilli(),
        )

        quotesQueries.insert(
            id = operation.entityId,
            title = values.title,
            phrase = values.phrase,
            description = values.description,
            translation = values.translation,
            example = values.example,
            context = values.context,
            pronunciation = values.pronunciation,
            formality = values.formality,
            tags = values.tags,
            category = values.category,
            createdAt = values.createdAt,
            updatedAt = values.updatedAt,
            deletedAt = values.deletedAt,
            originDeviceId = existing?.originDeviceId ?: originDeviceId,
            lastModifiedByDeviceId = originDeviceId,
            versionLamport = operation.lamport,
        )

        return ApplyRemoteOperationResult.Applied
    }

    private fun applyReviewEvent(operation: RemoteSyncOperation, localDeviceId: String): ApplyRemoteOperationResult {
        val eventId = operation.entityId
        val payload = operation.payload
        val validation = validateReviewEvent(payload = payload, operationCreatedAt = operation.createdAt)
        val reviewEvent = (validation as? ValidationResult.Ok)?.value
        val earlyResult = when {
            localFirstQueries.findReviewEventById(eventId).executeAsOneOrNull() != null -> {
                ApplyRemoteOperationResult.Skipped(reason = "review_event_exists")
            }
            validation is ValidationResult.Error -> {
                ApplyRemoteOperationResult.Deferred(reason = validation.reason)
            }
            reviewEvent != null && flashcardQueries.findById(reviewEvent.flashcardId).executeAsOneOrNull() == null -> {
                ApplyRemoteOperationResult.Deferred(reason = "missing_parent_flashcard")
            }
            else -> null
        }
        if (earlyResult != null) {
            return earlyResult
        }
        val values = checkNotNull(reviewEvent)

        localFirstQueries.insertReviewEvent(
            eventId = eventId,
            flashcardId = values.flashcardId,
            grade = payload.stringAny("grade") ?: "review",
            reviewedAt = values.reviewedAt,
            nextReviewAt = values.nextReviewAt,
            easeFactor = values.easeFactor,
            interval = values.interval,
            repetitions = values.repetitions,
            lapses = values.lapses,
            originDeviceId = operation.originDeviceId.ifBlank { localDeviceId },
            versionLamport = operation.lamport,
            createdAt = values.createdAt,
        )

        val existingProjection = localFirstQueries
            .findReviewProjectionByFlashcardId(values.flashcardId)
            .executeAsOneOrNull()
        if (existingProjection == null || values.reviewedAt >= existingProjection.lastReviewedAt) {
            localFirstQueries.upsertReviewProjection(
                flashcardId = values.flashcardId,
                lastReviewedAt = values.reviewedAt,
                nextReviewAt = values.nextReviewAt,
                easeFactor = values.easeFactor,
                interval = values.interval,
                repetitions = values.repetitions,
                lapses = values.lapses,
                sourceEventId = eventId,
                updatedAt = values.createdAt,
            )
        }

        return ApplyRemoteOperationResult.Applied
    }

    private fun missingDeckReason(operationType: String, payloadName: String?): String? = when {
        operationType != "delete" && payloadName.isNullOrBlank() -> "missing_required_field:name"
        else -> null
    }

    private fun missingFlashcardReason(
        deckId: String?,
        operationType: String,
        word: String?,
        meaning: String?,
    ): String? = when {
        deckId.isNullOrBlank() -> "missing_required_field:deck_id"
        operationType != "delete" && (word.isNullOrBlank() || meaning.isNullOrBlank()) -> {
            "missing_required_field:word_or_meaning"
        }
        deckQueries.findById(deckId).executeAsOneOrNull() == null -> "missing_parent_deck"
        else -> null
    }

    private fun missingFlashcardExampleReason(
        flashcardId: String?,
        operationType: String,
        text: String?,
        translation: String?,
        type: String?,
    ): String? = when {
        flashcardId.isNullOrBlank() -> "missing_required_field:flashcard_id"
        flashcardQueries.findById(flashcardId).executeAsOneOrNull() == null -> "missing_parent_flashcard"
        operationType != "delete" && listOf(text, translation, type).any { it.isNullOrBlank() } -> {
            "missing_required_field:example_payload"
        }
        else -> null
    }

    private fun missingQuoteReason(
        operationType: String,
        phrase: String?,
        translation: String?,
        category: String?,
    ): String? = when {
        operationType != "delete" && listOf(phrase, translation, category).any { it.isNullOrBlank() } -> {
            "missing_required_field:quote_payload"
        }
        else -> null
    }

    private fun resolveDeckValues(
        existing: com.emm.data.Deck?,
        payload: JsonObject,
        payloadName: String?,
        operationType: String,
        operationCreatedAt: Long,
    ): DeckValues {
        val payloadCreatedAt = payload.epochAny("createdAt", "created_at")
        val payloadUpdatedAt = payload.epochAny("updatedAt", "updated_at")
        val payloadDeletedAt = payload.epochAny("deletedAt", "deleted_at")

        val createdAt = existing?.createdAt ?: payloadCreatedAt ?: operationCreatedAt
        val updatedAt = payloadUpdatedAt ?: operationCreatedAt
        val deletedAt = if (operationType == "delete") payloadDeletedAt ?: operationCreatedAt else payloadDeletedAt
        val name = if (operationType == "delete") {
            existing?.name ?: payloadName ?: "[deleted]"
        } else {
            payloadName.orEmpty()
        }
        val description = payload.stringAny("description") ?: existing?.description

        return DeckValues(
            name = name,
            description = description,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )
    }

    private fun resolveFlashcardValues(
        existing: com.emm.data.Flashcard?,
        payload: JsonObject,
        operationType: String,
        word: String?,
        meaning: String?,
        operationCreatedAt: Long,
    ): FlashcardValues {
        val createdAt = existing?.createdAt
            ?: payload.epochAny("createdAt", "created_at")
            ?: operationCreatedAt
        val updatedAt = payload.epochAny("updatedAt", "updated_at") ?: operationCreatedAt
        val deletedAt = resolveDeletedAt(payload, operationType, updatedAt)
        val richFields = resolveFlashcardRichFields(existing, payload)

        return FlashcardValues(
            word = resolveDeletedOrIncoming(operationType, existing?.word, word),
            meaning = resolveDeletedOrIncoming(operationType, existing?.meaning, meaning),
            translation = payload.stringAny("translation") ?: existing?.translation,
            phonetic = payload.stringAny("phonetic") ?: existing?.phonetic,
            partOfSpeech = payload.stringAny("partOfSpeech", "part_of_speech") ?: existing?.partOfSpeech,
            type = payload.stringAny("type") ?: existing?.type,
            note = payload.stringAny("note") ?: existing?.note,
            register = richFields.register,
            levelBand = richFields.levelBand,
            domain = richFields.domain,
            lemma = richFields.lemma,
            whyUseful = richFields.whyUseful,
            usagePattern = richFields.usagePattern,
            irregularFormsJson = richFields.irregularFormsJson,
            collocationsJson = richFields.collocationsJson,
            commonMistake = richFields.commonMistake,
            confusableWithJson = richFields.confusableWithJson,
            clozeSentence = richFields.clozeSentence,
            sourceContext = richFields.sourceContext,
            warningsJson = richFields.warningsJson,
            studyCardsJson = richFields.studyCardsJson,
            qualityChecksJson = richFields.qualityChecksJson,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )
    }

    private fun resolveFlashcardRichFields(
        existing: com.emm.data.Flashcard?,
        payload: JsonObject,
    ): FlashcardRichFields {
        val jsonFields = resolveFlashcardJsonFields(existing, payload)
        return FlashcardRichFields(
            register = payload.stringAny("register") ?: existing?.register,
            levelBand = payload.stringAny("levelBand", "level_band") ?: existing?.levelBand,
            domain = payload.stringAny("domain") ?: existing?.domain,
            lemma = payload.stringAny("lemma") ?: existing?.lemma,
            whyUseful = payload.stringAny("whyUseful", "why_useful") ?: existing?.whyUseful,
            usagePattern = payload.stringAny("usagePattern", "usage_pattern") ?: existing?.usagePattern,
            irregularFormsJson = jsonFields.irregularFormsJson,
            collocationsJson = jsonFields.collocationsJson,
            commonMistake = payload.stringAny("commonMistake", "common_mistake") ?: existing?.commonMistake,
            confusableWithJson = jsonFields.confusableWithJson,
            clozeSentence = payload.stringAny("clozeSentence", "cloze_sentence") ?: existing?.clozeSentence,
            sourceContext = payload.stringAny("sourceContext", "source_context") ?: existing?.sourceContext,
            warningsJson = jsonFields.warningsJson,
            studyCardsJson = jsonFields.studyCardsJson,
            qualityChecksJson = jsonFields.qualityChecksJson,
        )
    }

    private fun resolveFlashcardJsonFields(
        existing: com.emm.data.Flashcard?,
        payload: JsonObject,
    ): FlashcardJsonFields {
        return FlashcardJsonFields(
            irregularFormsJson = payload.stringAny("irregularFormsJson", "irregular_forms_json")
                ?: existing?.irregularFormsJson,
            collocationsJson = payload.stringAny("collocationsJson", "collocations_json")
                ?: existing?.collocationsJson,
            confusableWithJson = payload.stringAny("confusableWithJson", "confusable_with_json")
                ?: existing?.confusableWithJson,
            warningsJson = payload.stringAny("warningsJson", "warnings_json") ?: existing?.warningsJson,
            studyCardsJson = payload.stringAny("studyCardsJson", "study_cards_json") ?: existing?.studyCardsJson,
            qualityChecksJson = payload.stringAny("qualityChecksJson", "quality_checks_json")
                ?: existing?.qualityChecksJson,
        )
    }

    private fun resolveFlashcardExampleValues(
        existing: com.emm.data.FlashcardExample?,
        payload: JsonObject,
        operationType: String,
        text: String?,
        translation: String?,
        type: String?,
        operationCreatedAt: Long,
    ): FlashcardExampleValues {
        val createdAt = existing?.createdAt
            ?: payload.epochAny("createdAt", "created_at")
            ?: operationCreatedAt
        val updatedAt = payload.epochAny("updatedAt", "updated_at") ?: operationCreatedAt
        val deletedAt = if (operationType == "delete") {
            payload.epochAny("deletedAt", "deleted_at") ?: updatedAt
        } else {
            payload.epochAny("deletedAt", "deleted_at")
        }

        return FlashcardExampleValues(
            text = resolveDeletedOrIncoming(operationType, existing?.text, text),
            translation = resolveDeletedOrIncoming(operationType, existing?.translation, translation),
            type = resolveDeletedOrIncoming(operationType, existing?.type, type),
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )
    }

    private fun resolveQuoteValues(
        existing: com.emm.data.Quote?,
        payload: JsonObject,
        operationType: String,
        phrase: String?,
        translation: String?,
        category: String?,
        operationCreatedAt: Long,
    ): QuoteValues {
        val createdAt = existing?.createdAt ?: payload.epochAny("createdAt", "created_at") ?: operationCreatedAt
        val updatedAt = payload.epochAny("updatedAt", "updated_at") ?: operationCreatedAt
        val deletedAt = resolveDeletedAt(payload, operationType, updatedAt)

        return QuoteValues(
            title = payload.stringAny("title") ?: existing?.title ?: (phrase ?: "[deleted]"),
            phrase = resolveDeletedOrIncoming(operationType, existing?.phrase, phrase, "[deleted]"),
            description = payload.stringAny("description") ?: existing?.description.orEmpty(),
            translation = resolveDeletedOrIncoming(operationType, existing?.translation, translation),
            example = payload.stringAny("example") ?: existing?.example.orEmpty(),
            context = payload.stringAny("context") ?: existing?.context.orEmpty(),
            pronunciation = payload.stringAny("pronunciation") ?: existing?.pronunciation.orEmpty(),
            formality = payload.stringAny("formality") ?: existing?.formality.orEmpty(),
            tags = payload.stringAny("tags") ?: existing?.tags.orEmpty(),
            category = resolveDeletedOrIncoming(operationType, existing?.category, category),
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )
    }

    private fun validateReviewEvent(
        payload: JsonObject,
        operationCreatedAt: String,
    ): ValidationResult<ReviewEventValues> {
        val flashcardId = payload.stringAny("flashcardId", "flashcard_id")
        val reviewedAt = payload.epochAny("reviewedAt", "reviewed_at")
        val nextReviewAt = payload.epochAny("nextReviewAt", "next_review_at")
        val easeFactor = payload.doubleAny("easeFactor", "ease_factor")
        val interval = payload.longAny("interval", "interval_days")
        val repetitions = payload.longAny("repetitions")
        val lapses = payload.longAny("lapses")
        val missingReason = listOf(
            flashcardId to "missing_required_field:flashcard_id",
            reviewedAt to "missing_required_field:reviewed_at",
            nextReviewAt to "missing_required_field:next_review_at",
            easeFactor to "missing_required_field:ease_factor",
            interval to "missing_required_field:interval",
            repetitions to "missing_required_field:repetitions",
            lapses to "missing_required_field:lapses",
        ).firstOrNull { (value, _) -> value == null }?.second
        if (missingReason != null) {
            return ValidationResult.Error(reason = missingReason)
        }
        val createdAt = payload.epochAny("createdAt", "created_at")
            ?: parseIsoToEpoch(operationCreatedAt)
            ?: Instant.now().toEpochMilli()

        return ValidationResult.Ok(
            ReviewEventValues(
                flashcardId = checkNotNull(flashcardId), // guaranteed non-null by missingReason check above
                reviewedAt = reviewedAt ?: 0L,
                nextReviewAt = nextReviewAt ?: 0L,
                easeFactor = easeFactor ?: 0.0,
                interval = interval ?: 0L,
                repetitions = repetitions ?: 0L,
                lapses = lapses ?: 0L,
                createdAt = createdAt,
            )
        )
    }

    private fun resolveDeletedAt(payload: JsonObject, operationType: String, updatedAt: Long): Long? {
        return if (operationType == "delete") {
            payload.epochAny("deletedAt", "deleted_at") ?: updatedAt
        } else {
            payload.epochAny("deletedAt", "deleted_at")
        }
    }

    private fun resolveDeletedOrIncoming(
        operationType: String,
        existingValue: String?,
        incomingValue: String?,
        deleteFallback: String = "",
    ): String {
        return if (operationType == "delete") {
            existingValue ?: incomingValue ?: deleteFallback
        } else {
            incomingValue.orEmpty()
        }
    }

    private fun isStale(
        existingLamport: Long?,
        existingLastModifiedByDeviceId: String?,
        incomingLamport: Long,
        incomingOriginDeviceId: String,
    ): Boolean {
        if (existingLamport == null) return false
        return when {
            incomingLamport != existingLamport -> incomingLamport < existingLamport
            else -> incomingOriginDeviceId <= existingLastModifiedByDeviceId.orEmpty()
        }
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
}

private sealed interface ValidationResult<out T> {
    data class Ok<T>(val value: T) : ValidationResult<T>
    data class Error(val reason: String) : ValidationResult<Nothing>
}

private data class DeckValues(
    val name: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

private data class FlashcardValues(
    val word: String,
    val meaning: String,
    val translation: String?,
    val phonetic: String?,
    val partOfSpeech: String?,
    val type: String?,
    val note: String?,
    val register: String?,
    val levelBand: String?,
    val domain: String?,
    val lemma: String?,
    val whyUseful: String?,
    val usagePattern: String?,
    val irregularFormsJson: String?,
    val collocationsJson: String?,
    val commonMistake: String?,
    val confusableWithJson: String?,
    val clozeSentence: String?,
    val sourceContext: String?,
    val warningsJson: String?,
    val studyCardsJson: String?,
    val qualityChecksJson: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

private data class FlashcardSeed(
    val deckId: String?,
    val word: String?,
    val meaning: String?,
)

private data class FlashcardRichFields(
    val register: String?,
    val levelBand: String?,
    val domain: String?,
    val lemma: String?,
    val whyUseful: String?,
    val usagePattern: String?,
    val irregularFormsJson: String?,
    val collocationsJson: String?,
    val commonMistake: String?,
    val confusableWithJson: String?,
    val clozeSentence: String?,
    val sourceContext: String?,
    val warningsJson: String?,
    val studyCardsJson: String?,
    val qualityChecksJson: String?,
)

private data class FlashcardJsonFields(
    val irregularFormsJson: String?,
    val collocationsJson: String?,
    val confusableWithJson: String?,
    val warningsJson: String?,
    val studyCardsJson: String?,
    val qualityChecksJson: String?,
)

private data class FlashcardExampleValues(
    val text: String,
    val translation: String,
    val type: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

private data class QuoteValues(
    val title: String,
    val phrase: String,
    val description: String,
    val translation: String,
    val example: String,
    val context: String,
    val pronunciation: String,
    val formality: String,
    val tags: String,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

private data class ReviewEventValues(
    val flashcardId: String,
    val reviewedAt: Long,
    val nextReviewAt: Long,
    val easeFactor: Double,
    val interval: Long,
    val repetitions: Long,
    val lapses: Long,
    val createdAt: Long,
)

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
