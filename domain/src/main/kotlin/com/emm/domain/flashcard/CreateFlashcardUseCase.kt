package com.emm.domain.flashcard

class CreateFlashcardUseCase(
    private val writeRepository: FlashcardWriteRepository,
    private val readRepository: FlashcardReadRepository,
) {

    suspend operator fun invoke(
        deckId: String,
        flashcard: FlashcardGenerated,
    ): Flashcard {
        val input = flashcard.toCreateFlashcardInput(deckId)

        val flashcardId: String = writeRepository.create(input)

        writeRepository.upsertExamples(flashcard.examples, flashcardId)

        return readRepository.fetchById(flashcardId)
    }

    suspend operator fun invoke(
        deckId: String,
        learningNote: GeneratedLearningNote,
    ): Flashcard {
        val input = learningNote.toCreateFlashcardInput(deckId)

        val flashcardId: String = writeRepository.create(input)

        writeRepository.upsertExamples(learningNote.toExamples(), flashcardId)

        return readRepository.fetchById(flashcardId)
    }

    private fun FlashcardGenerated.toCreateFlashcardInput(deckId: String): CreateFlashcardInput {
        return CreateFlashcardInput(
            deckId = deckId,
            word = word,
            meaning = meaning,
            translation = translation,
            phonetic = phonetics,
            partOfSpeech = partOfSpeech,
            type = type,
            note = notes,
        )
    }

    private fun GeneratedLearningNote.toCreateFlashcardInput(deckId: String): CreateFlashcardInput {
        return CreateFlashcardInput(
            deckId = deckId,
            word = expression,
            meaning = simpleDefinitionEn,
            translation = intendedMeaningEs,
            phonetic = ipa,
            partOfSpeech = partOfSpeech.name,
            type = noteType.name,
            note = buildNoteSummary(),
            register = register.name,
            levelBand = levelBand.name,
            domain = domain.name,
            lemma = lemma,
            whyUseful = whyUseful,
            usagePattern = usagePattern,
            irregularForms = irregularForms,
            collocations = collocations,
            commonMistake = commonMistake,
            confusableWith = confusableWith,
            clozeSentence = clozeSentence,
            sourceContext = sourceContext,
            warnings = warnings,
            studyCards = cards,
            qualityChecks = qualityChecks,
        )
    }

    private fun GeneratedLearningNote.toExamples(): List<Example> {
        return buildList {
            if (exampleSentence.isNotBlank()) {
                add(
                    Example(
                        exampleId = "learning-note-example",
                        text = exampleSentence,
                        translation = exampleTranslation,
                        type = "main",
                    )
                )
            }
        }
    }

    private fun GeneratedLearningNote.buildNoteSummary(): String {
        return listOfNotNull(
            whyUseful.takeIf { it.isNotBlank() },
            usagePattern.takeIf { it.isNotBlank() },
            commonMistake.takeIf { it.isNotBlank() },
        ).joinToString(separator = " | ")
    }
}
