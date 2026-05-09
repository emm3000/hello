package com.emm.data.export

import android.content.ContentResolver
import android.net.Uri
import com.emm.data.HelloDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Streams database records in batches of 1000 via LIMIT/OFFSET pagination,
 * writing directly to a user-chosen SAF output URI as UTF-8 JSON.
 *
 * Exports as a single JSON object per spec format:
 * { "schemaVersion": 1, "exportedAt": 123, "decks": [...], ... }
 */
class ExportBackupDataSource(
    private val db: HelloDb,
    private val contentResolver: ContentResolver,
) : BackupExporter {
    private companion object {
        const val BATCH_SIZE = 1000L
        const val SUPPORTED_VERSION = 1
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun export(outputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val outputStream = contentResolver.openOutputStream(outputUri)
                ?: throw ExportException("Cannot open output URI for writing")

            outputStream.bufferedWriter().use { writer ->
                exportAll(writer)
            }
        }
    }

    private fun exportAll(writer: java.io.Writer) {
        val exportedAt = System.currentTimeMillis()

        writer.write("{\n")
        writer.write("  \"schemaVersion\": $SUPPORTED_VERSION,\n")
        writer.write("  \"exportedAt\": $exportedAt,\n")

        exportDecks(writer)
        writer.write(",\n")
        exportFlashcards(writer)
        writer.write(",\n")
        exportExamples(writer)
        writer.write(",\n")
        exportTags(writer)
        writer.write(",\n")
        exportDeckTags(writer)
        writer.write(",\n")
        exportReviewEvents(writer)
        writer.write(",\n")
        exportReviewProjections(writer)

        writer.write("\n}\n")
    }

    private fun exportDecks(writer: java.io.Writer) {
        writer.write("  \"decks\": [\n")
        var offset = 0L
        var isFirst = true
        while (true) {
            val rows = db.exportQueries.allDecksPaged(BATCH_SIZE, offset).executeAsList()
            if (rows.isEmpty()) break
            rows.forEach { row ->
                if (!isFirst) writer.write(",\n")
                isFirst = false
                val dto = DeckDto(
                    id = row.id,
                    name = row.name,
                    description = row.description,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt,
                    deletedAt = row.deletedAt,
                )
                writer.write("    ")
                writer.write(json.encodeToString(dto))
            }
            offset += BATCH_SIZE
        }
        writer.write("\n  ]")
    }

    private fun exportFlashcards(writer: java.io.Writer) {
        writer.write("  \"flashcards\": [\n")
        var offset = 0L
        var isFirst = true
        while (true) {
            val rows = db.exportQueries.allFlashcardsPaged(BATCH_SIZE, offset).executeAsList()
            if (rows.isEmpty()) break
            rows.forEach { row ->
                if (!isFirst) writer.write(",\n")
                isFirst = false
                val dto = FlashcardDto(
                    id = row.id,
                    deckId = row.deckId,
                    word = row.word,
                    meaning = row.meaning,
                    translation = row.translation,
                    phonetic = row.phonetic,
                    partOfSpeech = row.partOfSpeech,
                    type = row.type,
                    note = row.note,
                    register = row.register,
                    levelBand = row.levelBand,
                    domain = row.domain,
                    lemma = row.lemma,
                    whyUseful = row.whyUseful,
                    usagePattern = row.usagePattern,
                    irregularFormsJson = row.irregularFormsJson,
                    collocationsJson = row.collocationsJson,
                    commonMistake = row.commonMistake,
                    confusableWithJson = row.confusableWithJson,
                    clozeSentence = row.clozeSentence,
                    sourceContext = row.sourceContext,
                    warningsJson = row.warningsJson,
                    studyCardsJson = row.studyCardsJson,
                    qualityChecksJson = row.qualityChecksJson,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt,
                    deletedAt = row.deletedAt,
                )
                writer.write("    ")
                writer.write(json.encodeToString(dto))
            }
            offset += BATCH_SIZE
        }
        writer.write("\n  ]")
    }

    private fun exportExamples(writer: java.io.Writer) {
        writer.write("  \"examples\": [\n")
        var offset = 0L
        var isFirst = true
        while (true) {
            val rows = db.exportQueries.allExamplesPaged(BATCH_SIZE, offset).executeAsList()
            if (rows.isEmpty()) break
            rows.forEach { row ->
                if (!isFirst) writer.write(",\n")
                isFirst = false
                val dto = FlashcardExampleDto(
                    id = row.id,
                    flashcardId = row.flashcardId,
                    text = row.text,
                    translation = row.translation,
                    type = row.type,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt,
                    deletedAt = row.deletedAt,
                )
                writer.write("    ")
                writer.write(json.encodeToString(dto))
            }
            offset += BATCH_SIZE
        }
        writer.write("\n  ]")
    }

    private fun exportTags(writer: java.io.Writer) {
        writer.write("  \"tags\": [\n")
        var offset = 0L
        var isFirst = true
        while (true) {
            val rows = db.exportQueries.allTagsPaged(BATCH_SIZE, offset).executeAsList()
            if (rows.isEmpty()) break
            rows.forEach { row ->
                if (!isFirst) writer.write(",\n")
                isFirst = false
                val dto = TagDto(
                    id = row.id,
                    name = row.name,
                    createdAt = row.createdAt,
                    deletedAt = row.deletedAt,
                )
                writer.write("    ")
                writer.write(json.encodeToString(dto))
            }
            offset += BATCH_SIZE
        }
        writer.write("\n  ]")
    }

    private fun exportDeckTags(writer: java.io.Writer) {
        writer.write("  \"deckTags\": [\n")
        var offset = 0L
        var isFirst = true
        while (true) {
            val rows = db.exportQueries.allDeckTagsPaged(BATCH_SIZE, offset).executeAsList()
            if (rows.isEmpty()) break
            rows.forEach { row ->
                if (!isFirst) writer.write(",\n")
                isFirst = false
                val dto = DeckTagDto(
                    tagId = row.tagId,
                    deckId = row.deckId,
                    createdAt = row.createdAt,
                )
                writer.write("    ")
                writer.write(json.encodeToString(dto))
            }
            offset += BATCH_SIZE
        }
        writer.write("\n  ]")
    }

    private fun exportReviewEvents(writer: java.io.Writer) {
        writer.write("  \"reviewEvents\": [\n")
        var offset = 0L
        var isFirst = true
        while (true) {
            val rows = db.exportQueries.allReviewEventsPaged(BATCH_SIZE, offset).executeAsList()
            if (rows.isEmpty()) break
            rows.forEach { row ->
                if (!isFirst) writer.write(",\n")
                isFirst = false
                val dto = ReviewEventDto(
                    eventId = row.eventId,
                    flashcardId = row.flashcardId,
                    grade = row.grade,
                    reviewedAt = row.reviewedAt,
                    nextReviewAt = row.nextReviewAt,
                    easeFactor = row.easeFactor,
                    interval = row.interval,
                    repetitions = row.repetitions,
                    lapses = row.lapses,
                    createdAt = row.createdAt,
                )
                writer.write("    ")
                writer.write(json.encodeToString(dto))
            }
            offset += BATCH_SIZE
        }
        writer.write("\n  ]")
    }

    private fun exportReviewProjections(writer: java.io.Writer) {
        writer.write("  \"reviewProjections\": [\n")
        var offset = 0L
        var isFirst = true
        while (true) {
            val rows = db.exportQueries.allReviewProjectionsPaged(BATCH_SIZE, offset).executeAsList()
            if (rows.isEmpty()) break
            rows.forEach { row ->
                if (!isFirst) writer.write(",\n")
                isFirst = false
                val dto = ReviewProjectionDto(
                    flashcardId = row.flashcardId,
                    lastReviewedAt = row.lastReviewedAt,
                    nextReviewAt = row.nextReviewAt,
                    easeFactor = row.easeFactor,
                    interval = row.interval,
                    repetitions = row.repetitions,
                    lapses = row.lapses,
                    sourceEventId = row.sourceEventId,
                    updatedAt = row.updatedAt,
                )
                writer.write("    ")
                writer.write(json.encodeToString(dto))
            }
            offset += BATCH_SIZE
        }
        writer.write("\n  ]")
    }
}
