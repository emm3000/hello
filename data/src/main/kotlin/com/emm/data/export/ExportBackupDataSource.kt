package com.emm.data.export

import android.content.ContentResolver
import android.net.Uri
import com.emm.data.HelloDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.Writer

class ExportBackupDataSource(
    private val db: HelloDb,
    private val contentResolver: ContentResolver,
) : BackupExporter {
    private companion object {
        const val BATCH_SIZE = 1000L

        // Schema v2 (FSRS-6): ReviewEventDto.rating + ReviewProjectionDto.state/stability/difficulty.
        const val SUPPORTED_VERSION = 2
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

            outputStream.bufferedWriter().use { writer -> exportAll(writer) }
        }
    }

    @Suppress("LongMethod")
    private fun exportAll(writer: Writer) {
        val exportedAt = System.currentTimeMillis()

        writer.write("{\n")
        writer.write("  \"schemaVersion\": $SUPPORTED_VERSION,\n")
        writer.write("  \"exportedAt\": $exportedAt,\n")

        exportEntities(writer, "decks") { limit, offset ->
            db.exportQueries.allDecksPaged(limit, offset).executeAsList().map {
                json.encodeToString(
                    DeckDto(
                        id = it.id,
                        name = it.name,
                        description = it.description,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                        deletedAt = it.deletedAt,
                    )
                )
            }
        }
        writer.write(",\n")
        exportEntities(writer, "flashcards") { limit, offset ->
            db.exportQueries.allFlashcardsPaged(limit, offset).executeAsList().map {
                json.encodeToString(
                    FlashcardDto(
                        id = it.id,
                        deckId = it.deckId,
                        word = it.word,
                        meaning = it.meaning,
                        translation = it.translation,
                        phonetic = it.phonetic,
                        partOfSpeech = it.partOfSpeech,
                        type = it.type,
                        note = it.note,
                        register = it.register,
                        levelBand = it.levelBand,
                        domain = it.domain,
                        lemma = it.lemma,
                        whyUseful = it.whyUseful,
                        usagePattern = it.usagePattern,
                        irregularFormsJson = it.irregularFormsJson,
                        collocationsJson = it.collocationsJson,
                        commonMistake = it.commonMistake,
                        confusableWithJson = it.confusableWithJson,
                        clozeSentence = it.clozeSentence,
                        sourceContext = it.sourceContext,
                        warningsJson = it.warningsJson,
                        studyCardsJson = it.studyCardsJson,
                        qualityChecksJson = it.qualityChecksJson,
                        enrichmentStatus = it.enrichmentStatus,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                        deletedAt = it.deletedAt,
                    )
                )
            }
        }
        writer.write(",\n")
        exportEntities(writer, "examples") { limit, offset ->
            db.exportQueries.allExamplesPaged(limit, offset).executeAsList().map {
                json.encodeToString(
                    FlashcardExampleDto(
                        id = it.id,
                        flashcardId = it.flashcardId,
                        text = it.text,
                        translation = it.translation,
                        type = it.type,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                        deletedAt = it.deletedAt,
                    )
                )
            }
        }
        writer.write(",\n")
        exportEntities(writer, "tags") { limit, offset ->
            db.exportQueries.allTagsPaged(limit, offset).executeAsList().map {
                json.encodeToString(
                    TagDto(
                        id = it.id,
                        name = it.name,
                        createdAt = it.createdAt,
                        deletedAt = it.deletedAt,
                    )
                )
            }
        }
        writer.write(",\n")
        exportEntities(writer, "deckTags") { limit, offset ->
            db.exportQueries.allDeckTagsPaged(limit, offset).executeAsList().map {
                json.encodeToString(
                    DeckTagDto(
                        tagId = it.tagId,
                        deckId = it.deckId,
                        createdAt = it.createdAt,
                    )
                )
            }
        }
        writer.write(",\n")
        exportEntities(writer, "reviewEvents") { limit, offset ->
            db.exportQueries.allReviewEventsPaged(limit, offset).executeAsList().map {
                json.encodeToString(
                    ReviewEventDto(
                        eventId = it.eventId,
                        flashcardId = it.flashcardId,
                        grade = it.grade,
                        reviewedAt = it.reviewedAt,
                        nextReviewAt = it.nextReviewAt,
                        easeFactor = it.easeFactor,
                        interval = it.interval,
                        repetitions = it.repetitions,
                        lapses = it.lapses,
                        createdAt = it.createdAt,
                        rating = it.rating,
                    )
                )
            }
        }
        writer.write(",\n")
        exportEntities(writer, "reviewProjections") { limit, offset ->
            db.exportQueries.allReviewProjectionsPaged(limit, offset).executeAsList().map {
                json.encodeToString(
                    ReviewProjectionDto(
                        flashcardId = it.flashcardId,
                        lastReviewedAt = it.lastReviewedAt,
                        nextReviewAt = it.nextReviewAt,
                        easeFactor = it.easeFactor,
                        interval = it.interval,
                        repetitions = it.repetitions,
                        lapses = it.lapses,
                        sourceEventId = it.sourceEventId,
                        updatedAt = it.updatedAt,
                        state = it.state,
                        stability = it.stability,
                        difficulty = it.difficulty,
                        productionSince = it.productionSince,
                    )
                )
            }
        }

        writer.write("\n}\n")
    }

    private fun exportEntities(
        writer: Writer,
        key: String,
        pagedQuery: (limit: Long, offset: Long) -> List<String>,
    ) {
        writer.write("  \"$key\": [\n")
        var offset = 0L
        var isFirst = true
        while (true) {
            val serializedRows = pagedQuery(BATCH_SIZE, offset)
            if (serializedRows.isEmpty()) break
            serializedRows.forEach { serialized ->
                if (!isFirst) writer.write(",\n")
                isFirst = false
                writer.write("    ")
                writer.write(serialized)
            }
            offset += BATCH_SIZE
        }
        writer.write("\n  ]")
    }
}
