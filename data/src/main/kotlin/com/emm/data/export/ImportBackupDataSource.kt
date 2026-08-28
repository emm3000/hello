package com.emm.data.export

import android.content.ContentResolver
import android.net.Uri
import com.emm.data.HelloDb
import com.emm.data.flashcard.toEnrichmentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ImportBackupDataSource(
    private val db: HelloDb,
    private val contentResolver: ContentResolver,
) : BackupImporter {
    private companion object {
        const val SCHEMA_V1 = 1
        const val SCHEMA_V2 = 2
        val SUPPORTED_VERSIONS = SCHEMA_V1..SCHEMA_V2

        // Mirrors the seeding formula in 1.sqm (migration 1->2) so that a v1 backup
        // (no FSRS columns) imported onto a v2 install gets the same seeded state/stability/
        // difficulty an on-device migration would have produced from the same SM-2 fields.
        const val SEEDED_STABILITY_FLOOR = 0.001
        const val SEEDED_STABILITY_CEILING = 36500.0
        const val MIN_DIFFICULTY = 1.0
        const val MAX_DIFFICULTY = 10.0
        const val MIN_EASE_FACTOR = 1.3
        const val DIFFICULTY_SPAN = 9.0 / 1.4
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun import(inputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val inputStream = contentResolver.openInputStream(inputUri)
                ?: throw ImportException("Cannot open input URI for reading")

            val envelope = inputStream.bufferedReader().use { reader ->
                json.decodeFromString(BackupEnvelope.serializer(), reader.readText())
            }

            validateSchemaVersion(envelope.schemaVersion)
            restoreAll(envelope)
        }
    }

    private fun restoreAll(envelope: BackupEnvelope) {
        db.transaction {
            clearAllTables()
            insertDecks(envelope.decks)
            insertFlashcards(envelope.flashcards)
            insertExamples(envelope.examples)
            insertTags(envelope.tags)
            insertDeckTags(envelope.deckTags)
            insertReviewEvents(envelope.reviewEvents)
            insertReviewProjections(envelope.reviewProjections, isLegacySchema = envelope.schemaVersion < SCHEMA_V2)
        }
    }

    private fun clearAllTables() {
        db.exportQueries.deleteAllDecks()
        db.exportQueries.deleteAllFlashcards()
        db.exportQueries.deleteAllFlashcardExamples()
        db.exportQueries.deleteAllTags()
        db.exportQueries.deleteAllReviewEvents()
        db.exportQueries.deleteAllReviewProjections()
    }

    private fun insertDecks(decks: List<DeckDto>) {
        decks.forEach { deck ->
            db.deckQueries.insert(
                id = deck.id,
                name = deck.name,
                description = deck.description,
                createdAt = deck.createdAt,
                updatedAt = deck.updatedAt,
                deletedAt = deck.deletedAt,
            )
        }
    }

    private fun insertFlashcards(flashcards: List<FlashcardDto>) {
        flashcards.forEach { card ->
            db.flashcardQueries.create(
                id = card.id,
                deckId = card.deckId,
                word = card.word,
                meaning = card.meaning,
                translation = card.translation,
                phonetic = card.phonetic,
                partOfSpeech = card.partOfSpeech,
                type = card.type,
                note = card.note,
                register = card.register,
                levelBand = card.levelBand,
                domain = card.domain,
                lemma = card.lemma,
                whyUseful = card.whyUseful,
                usagePattern = card.usagePattern,
                irregularFormsJson = card.irregularFormsJson,
                collocationsJson = card.collocationsJson,
                commonMistake = card.commonMistake,
                confusableWithJson = card.confusableWithJson,
                clozeSentence = card.clozeSentence,
                sourceContext = card.sourceContext,
                warningsJson = card.warningsJson,
                studyCardsJson = card.studyCardsJson,
                qualityChecksJson = card.qualityChecksJson,
                enrichmentStatus = toEnrichmentStatus(card.enrichmentStatus).name,
                createdAt = card.createdAt,
                updatedAt = card.updatedAt,
                deletedAt = card.deletedAt,
            )
        }
    }

    private fun insertExamples(examples: List<FlashcardExampleDto>) {
        examples.forEach { ex ->
            db.flashcardExampleQueries.insert(
                id = ex.id,
                flashcardId = ex.flashcardId,
                text = ex.text,
                translation = ex.translation,
                type = ex.type,
                createdAt = ex.createdAt,
                updatedAt = ex.updatedAt,
                deletedAt = ex.deletedAt,
            )
        }
    }

    private fun insertTags(tags: List<TagDto>) {
        tags.forEach { tag ->
            db.exportQueries.insertTag(
                id = tag.id,
                name = tag.name,
                createdAt = tag.createdAt,
                deletedAt = tag.deletedAt,
            )
        }
    }

    private fun insertDeckTags(deckTags: List<DeckTagDto>) {
        deckTags.forEach { dk ->
            db.tagQueries.insertDeckTag(
                tagId = dk.tagId,
                deckId = dk.deckId,
                createdAt = dk.createdAt,
            )
        }
    }

    private fun insertReviewEvents(events: List<ReviewEventDto>) {
        events.forEach { event ->
            db.localFirstQueries.insertReviewEvent(
                eventId = event.eventId,
                flashcardId = event.flashcardId,
                grade = event.grade,
                reviewedAt = event.reviewedAt,
                nextReviewAt = event.nextReviewAt,
                easeFactor = event.easeFactor,
                interval = event.interval,
                repetitions = event.repetitions,
                lapses = event.lapses,
                createdAt = event.createdAt,
                // v1 backups lack a rating column; the DTO default (0L = legacy/unknown) applies.
                rating = event.rating,
            )
        }
    }

    private fun insertReviewProjections(projections: List<ReviewProjectionDto>, isLegacySchema: Boolean) {
        projections.forEach { proj ->
            val (state, stability, difficulty) = if (isLegacySchema) {
                seedFsrsFromLegacy(interval = proj.interval, lapses = proj.lapses, easeFactor = proj.easeFactor)
            } else {
                Triple(proj.state, proj.stability, proj.difficulty)
            }
            db.localFirstQueries.insertReviewProjectionFull(
                flashcardId = proj.flashcardId,
                lastReviewedAt = proj.lastReviewedAt,
                nextReviewAt = proj.nextReviewAt,
                easeFactor = proj.easeFactor,
                interval = proj.interval,
                repetitions = proj.repetitions,
                lapses = proj.lapses,
                sourceEventId = proj.sourceEventId,
                updatedAt = proj.updatedAt,
                state = state,
                stability = stability,
                difficulty = difficulty,
                productionSince = proj.productionSince,
            )
        }
    }

    /**
     * Seeds FSRS state/stability/difficulty from legacy SM-2 fields, mirroring the on-device
     * migration formula in `data/src/main/sqldelight/com/emm/data/1.sqm` exactly so a v1 backup
     * restored onto a v2 install ends up with the same values an in-place upgrade would have
     * produced.
     */
    private fun seedFsrsFromLegacy(interval: Long, lapses: Long, easeFactor: Double): Triple<String, Double, Double> {
        val state = when {
            interval <= 0L -> "NEW"
            interval == 1L && lapses > 0L -> "RELEARNING"
            else -> "REVIEW"
        }
        val stability = interval.toDouble().coerceIn(SEEDED_STABILITY_FLOOR, SEEDED_STABILITY_CEILING)
        val difficulty = (MAX_DIFFICULTY - (easeFactor - MIN_EASE_FACTOR) * DIFFICULTY_SPAN)
            .coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        return Triple(state, stability, difficulty)
    }

    private fun validateSchemaVersion(actualVersion: Int) {
        if (actualVersion !in SUPPORTED_VERSIONS) {
            throw IncompatibleSchemaException(actualVersion, SUPPORTED_VERSIONS)
        }
    }
}
