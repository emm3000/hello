package com.emm.domain.flashcard

import com.emm.domain.text.lowercaseRoot

class ValidateFlashcardGenerationInputUseCase {

    operator fun invoke(input: FlashcardGenerationInput): FlashcardGenerationInputValidation {
        val normalized = input.normalized()
        val errors = mutableListOf<FlashcardGenerationInputIssue>()
        val warnings = mutableListOf<FlashcardGenerationInputIssue>()
        val userText = normalized.userText
        val wordCount = wordCount(userText)

        validateRequiredUserText(userText, errors)
        validateInputTypeRules(normalized, userText, wordCount, errors, warnings)
        validateDisambiguation(normalized, wordCount, errors)
        validateContextSentence(normalized.contextSentence, warnings)

        return FlashcardGenerationInputValidation(
            normalizedInput = normalized,
            errors = errors,
            warnings = warnings,
        )
    }

    private fun validateRequiredUserText(
        userText: String,
        errors: MutableList<FlashcardGenerationInputIssue>,
    ) {
        if (userText.isBlank()) {
            errors += issue(
                code = FlashcardGenerationInputIssueCode.EmptyUserText,
                message = "Debes ingresar el texto que quieres aprender."
            )
        }
    }

    private fun validateInputTypeRules(
        input: FlashcardGenerationInput,
        userText: String,
        wordCount: Int,
        errors: MutableList<FlashcardGenerationInputIssue>,
        warnings: MutableList<FlashcardGenerationInputIssue>,
    ) {
        when (input.inputType) {
            FlashcardInputType.Word -> validateWordInput(wordCount, warnings)
            FlashcardInputType.Phrase -> validatePhraseInput(userText, wordCount, warnings)
            FlashcardInputType.Sentence -> validateSentenceInput(userText, wordCount, errors)
            FlashcardInputType.CommunicativeGoal -> {
                validateCommunicativeGoalInput(input, userText, errors, warnings)
            }
        }
    }

    private fun validateWordInput(
        wordCount: Int,
        warnings: MutableList<FlashcardGenerationInputIssue>,
    ) {
        if (wordCount > 1) {
            warnings += issue(
                code = FlashcardGenerationInputIssueCode.WordInputContainsWhitespace,
                message = "El input parece contener varias palabras; tal vez deberia tratarse como frase."
            )
        }
    }

    private fun validatePhraseInput(
        userText: String,
        wordCount: Int,
        warnings: MutableList<FlashcardGenerationInputIssue>,
    ) {
        if (userText.isNotBlank() && wordCount < 2) {
            warnings += issue(
                code = FlashcardGenerationInputIssueCode.PhraseInputTooShort,
                message = "La frase parece demasiado corta; revisa si en realidad es una palabra."
            )
        }
    }

    private fun validateSentenceInput(
        userText: String,
        wordCount: Int,
        errors: MutableList<FlashcardGenerationInputIssue>,
    ) {
        if (userText.isNotBlank() && wordCount < MIN_SENTENCE_WORD_COUNT) {
            errors += issue(
                code = FlashcardGenerationInputIssueCode.SentenceInputTooShort,
                message = "Una entrada tipo oracion necesita mas contexto para generar una nota util."
            )
        }
    }

    private fun validateCommunicativeGoalInput(
        input: FlashcardGenerationInput,
        userText: String,
        errors: MutableList<FlashcardGenerationInputIssue>,
        warnings: MutableList<FlashcardGenerationInputIssue>,
    ) {
        if (input.communicativeIntentId.isBlank()) {
            errors += issue(
                code = FlashcardGenerationInputIssueCode.MissingCommunicativeIntent,
                message = "Debes indicar una intencion comunicativa para este tipo de entrada."
            )
        }
        if (userText.isNotBlank() && userText.length < MIN_COMMUNICATIVE_GOAL_LENGTH) {
            warnings += issue(
                code = FlashcardGenerationInputIssueCode.CommunicativeGoalTooShort,
                message = "Describe mejor el objetivo comunicativo para obtener contenido mas util."
            )
        }
    }

    private fun validateDisambiguation(
        input: FlashcardGenerationInput,
        wordCount: Int,
        errors: MutableList<FlashcardGenerationInputIssue>,
    ) {
        val needsDisambiguation = requiresDisambiguation(
            inputType = input.inputType,
            normalizedUserText = input.userText.lowercaseRoot(),
            wordCount = wordCount,
        )
        val hasDisambiguation = input.intendedMeaningEs.isNotBlank() || input.contextSentence.isNotBlank()
        if (needsDisambiguation && !hasDisambiguation) {
            errors += issue(
                code = FlashcardGenerationInputIssueCode.MissingDisambiguation,
                message = "Este termino es ambiguo; agrega el significado deseado o una oracion de contexto."
            )
        }
    }

    private fun validateContextSentence(
        contextSentence: String,
        warnings: MutableList<FlashcardGenerationInputIssue>,
    ) {
        if (contextSentence.isNotBlank() && wordCount(contextSentence) < MIN_CONTEXT_WORD_COUNT) {
            warnings += issue(
                code = FlashcardGenerationInputIssueCode.ContextSentenceTooShort,
                message = "La oracion de contexto es muy corta; intenta dar mas informacion de uso real."
            )
        }
    }

    private fun requiresDisambiguation(
        inputType: FlashcardInputType,
        normalizedUserText: String,
        wordCount: Int,
    ): Boolean {
        val supportsDisambiguation = inputType != FlashcardInputType.Sentence &&
            inputType != FlashcardInputType.CommunicativeGoal
        val isKnownAmbiguousTarget = normalizedUserText in ambiguousTargets
        val isShortAmbiguousWord = inputType == FlashcardInputType.Word &&
            wordCount == 1 &&
            normalizedUserText.length <= SHORT_AMBIGUOUS_WORD_LENGTH

        return normalizedUserText.isNotBlank() &&
            supportsDisambiguation &&
            (isKnownAmbiguousTarget || isShortAmbiguousWord)
    }

    private fun wordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(WHITESPACE_REGEX).size
    }

    private fun issue(
        code: FlashcardGenerationInputIssueCode,
        message: String,
    ): FlashcardGenerationInputIssue {
        return FlashcardGenerationInputIssue(code = code, message = message)
    }

    private companion object {
        val WHITESPACE_REGEX = "\\s+".toRegex()

        const val MIN_SENTENCE_WORD_COUNT = 4
        const val MIN_CONTEXT_WORD_COUNT = 4
        const val MIN_COMMUNICATIVE_GOAL_LENGTH = 8
        const val SHORT_AMBIGUOUS_WORD_LENGTH = 3

        val ambiguousTargets = setOf(
            "get",
            "set",
            "run",
            "take",
            "make",
            "do",
            "go",
            "come",
            "put",
            "have",
            "pick up",
            "take off",
            "look up",
            "run out",
            "figure out",
            "work out",
        )
    }
}
