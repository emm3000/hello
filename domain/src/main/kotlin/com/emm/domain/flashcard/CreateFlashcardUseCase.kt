package com.emm.domain.flashcard

import com.emm.domain.validation.DomainValidationException
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue

class CreateFlashcardUseCase(
    private val writeRepository: FlashcardWriteRepository,
    private val readRepository: FlashcardReadRepository,
    private val validateGeneratedLearningNoteUseCase: ValidateGeneratedLearningNoteUseCase,
    private val isExactDuplicateGeneratedNoteUseCase: IsExactDuplicateGeneratedNoteUseCase,
) {

    suspend operator fun invoke(
        deckId: String,
        learningNote: GeneratedLearningNote,
    ): Flashcard {
        val normalizedExpression = learningNote.expression.toExpression()
        val normalizedMeaning = learningNote.intendedMeaningEs.toIntendedMeaningEs()
        val normalizedDefinition = learningNote.simpleDefinitionEn.toDefinitionEn()

        val validation = validateGeneratedLearningNoteUseCase(learningNote)
        if (!validation.isValid) {
            throw DomainValidationException(validation.errors)
        }

        val isDuplicate = isExactDuplicateGeneratedNoteUseCase(deckId = deckId, note = learningNote)
        if (isDuplicate) {
            throw DomainValidationException(
                issues = listOf(
                    ValidationIssue.Error(
                        code = IssueCode.DuplicateExactCardInDeck,
                        field = "deckId",
                    )
                )
            )
        }

        val input = learningNote.toCreateFlashcardInput(
            deckId = deckId,
            expression = normalizedExpression,
            intendedMeaningEs = normalizedMeaning,
            definitionEn = normalizedDefinition,
        )

        val flashcardId: String = writeRepository.create(input)

        writeRepository.upsertExamples(learningNote.toExamples(), flashcardId)

        return readRepository.fetchById(flashcardId)
    }

    private fun GeneratedLearningNote.toCreateFlashcardInput(
        deckId: String,
        expression: Expression,
        intendedMeaningEs: IntendedMeaningEs,
        definitionEn: DefinitionEn,
    ): CreateFlashcardInput {
        return CreateFlashcardInput(
            deckId = deckId,
            word = expression.value,
            meaning = definitionEn.value,
            translation = intendedMeaningEs.value,
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
