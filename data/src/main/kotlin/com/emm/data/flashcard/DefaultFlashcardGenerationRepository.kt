package com.emm.data.flashcard

import com.emm.domain.flashcard.FlashcardGenerationInput
import com.emm.domain.flashcard.FlashcardGenerationRepository
import com.emm.domain.generation.GeneratedLearningNote
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class DefaultFlashcardGenerationRepository(
    private val geminiService: GeminiService,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher,
) : FlashcardGenerationRepository {

    override suspend fun generateLearningNote(input: FlashcardGenerationInput): GeneratedLearningNote =
        withContext(ioDispatcher) {
            val prompt = Prompt.buildLearningNotePrompt(input)
            geminiService.processLearningNoteWithParser(prompt) { response ->
                GeneratedLearningNoteResponseParser.parse(response, json)
            }
        }
}
