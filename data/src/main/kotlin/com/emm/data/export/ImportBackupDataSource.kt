package com.emm.data.export

import android.content.ContentResolver
import android.net.Uri
import com.emm.data.HelloDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Reads a backup JSON file from a user-chosen SAF input URI,
 * validates the schema version, then executes a transactional
 * full-replace restore (clear all tables + insert all records).
 */
open class ImportBackupDataSource(
    private val db: HelloDb,
    private val contentResolver: ContentResolver,
) {
    private companion object {
        const val SUPPORTED_VERSION = 1
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    open suspend fun import(inputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
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
            insertReviewProjections(envelope.reviewProjections)
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
            )
        }
    }

    private fun insertReviewProjections(projections: List<ReviewProjectionDto>) {
        projections.forEach { proj ->
            db.localFirstQueries.upsertReviewProjection(
                flashcardId = proj.flashcardId,
                lastReviewedAt = proj.lastReviewedAt,
                nextReviewAt = proj.nextReviewAt,
                easeFactor = proj.easeFactor,
                interval = proj.interval,
                repetitions = proj.repetitions,
                lapses = proj.lapses,
                sourceEventId = proj.sourceEventId,
                updatedAt = proj.updatedAt,
            )
        }
    }

    private fun validateSchemaVersion(actualVersion: Int) {
        if (actualVersion != SUPPORTED_VERSION) {
            throw IncompatibleSchemaException(actualVersion)
        }
    }
}
