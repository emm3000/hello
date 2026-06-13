package com.emm.data.flashcard

import com.emm.domain.flashcard.FlashcardGenerationInput
import com.emm.domain.flashcard.FlashcardGenerationRepository
import com.emm.domain.generation.GeneratedExampleDraft
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.RegenerableNoteField
import com.emm.domain.telemetry.GeminiTelemetry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class DefaultFlashcardGenerationRepository(
    private val geminiService: GeminiService,
    private val json: Json,
    private val telemetry: GeminiTelemetry = GeminiTelemetry.NoOp,
    private val ioDispatcher: CoroutineDispatcher,
) : FlashcardGenerationRepository {

    override suspend fun generateLearningNote(input: FlashcardGenerationInput): GeneratedLearningNote =
        withContext(ioDispatcher) {
            val prompt = Prompt.buildLearningNotePrompt(input)
            geminiService.processLearningNoteWithParser(prompt) { response ->
                GeneratedLearningNoteResponseParser.parse(response, json)
            }
        }

    override suspend fun regenerateNoteField(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
        field: RegenerableNoteField,
    ): String = withContext(ioDispatcher) {
        val prompt = Prompt.buildNoteFieldRegenerationPrompt(input, note, field)
        val response = geminiService.process(prompt)
        val jsonKey = when (field) {
            RegenerableNoteField.WhyUseful -> "why_useful"
            RegenerableNoteField.UsagePattern -> "usage_pattern"
            RegenerableNoteField.CommonMistake -> "common_mistake"
        }
        parseWithTelemetry(kind = "regen_field_${field.name}", rawResponse = response) {
            PartialRegenerationParser.parseField(response, json, jsonKey = jsonKey, label = field.name)
        }
    }

    override suspend fun regenerateExample(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
    ): GeneratedExampleDraft = withContext(ioDispatcher) {
        val prompt = Prompt.buildExampleRegenerationPrompt(input, note)
        val response = geminiService.process(prompt)
        parseWithTelemetry(kind = "regen_example", rawResponse = response) {
            PartialRegenerationParser.parseExample(response, json)
        }
    }

    override suspend fun regenerateClozeSentence(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
    ): String = withContext(ioDispatcher) {
        val prompt = Prompt.buildClozeRegenerationPrompt(input, note)
        val response = geminiService.process(prompt)
        parseWithTelemetry(kind = "regen_cloze", rawResponse = response) {
            PartialRegenerationParser.parseCloze(response, json)
        }
    }

    override suspend fun regenerateStudyCard(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
        cardId: String,
    ): GeneratedStudyCard = withContext(ioDispatcher) {
        val card = note.cards.firstOrNull { it.cardId == cardId }
            ?: throw IllegalArgumentException("Study card no encontrada para regenerar.")
        val prompt = Prompt.buildStudyCardRegenerationPrompt(input, note, card)
        val response = geminiService.process(prompt)
        parseWithTelemetry(kind = "regen_study_card", rawResponse = response) {
            PartialRegenerationParser.parseStudyCard(response, json)
        }
    }

    private inline fun <T> parseWithTelemetry(
        kind: String,
        rawResponse: String,
        block: () -> T,
    ): T = try {
        block()
    } catch (cause: Throwable) {
        telemetry.recordParseFailure(
            kind = kind,
            rawResponse = rawResponse.take(MAX_RAW_RESPONSE_CHARS),
            cause = cause,
        )
        throw cause
    }

    private companion object {
        const val MAX_RAW_RESPONSE_CHARS: Int = 4_000
    }
}
