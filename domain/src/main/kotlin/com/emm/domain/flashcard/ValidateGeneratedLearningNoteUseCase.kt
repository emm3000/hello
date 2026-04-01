package com.emm.domain.flashcard

class ValidateGeneratedLearningNoteUseCase {

    operator fun invoke(note: GeneratedLearningNote): GeneratedLearningNoteValidation {
        val errors = mutableListOf<GeneratedLearningNoteIssue>()
        val warnings = mutableListOf<GeneratedLearningNoteIssue>()

        validateCoreFields(note, errors)
        validateNoteTypeRequirements(note, errors)
        validateCards(note, errors, warnings)
        validateQualityChecks(note, errors)

        return GeneratedLearningNoteValidation(
            errors = errors,
            warnings = warnings,
        )
    }

    private fun validateCoreFields(
        note: GeneratedLearningNote,
        errors: MutableList<GeneratedLearningNoteIssue>,
    ) {
        validateIdentityFields(note, errors)
        validateMeaningFields(note, errors)
        validateExampleFields(note, errors)
        validateCollections(note, errors)
    }

    private fun validateNoteTypeRequirements(
        note: GeneratedLearningNote,
        errors: MutableList<GeneratedLearningNoteIssue>,
    ) {
        when (note.noteType) {
            LearningNoteType.Word -> validateWordNote(note, errors)
            LearningNoteType.Phrase -> validatePhraseNote(note, errors)
            LearningNoteType.PhrasalVerb -> validatePhrasalVerbNote(note, errors)
            LearningNoteType.Idiom -> validateIdiomNote(note, errors)
            LearningNoteType.SentencePattern -> validateSentencePatternNote(note, errors)
        }
    }

    private fun validateIdentityFields(
        note: GeneratedLearningNote,
        errors: MutableList<GeneratedLearningNoteIssue>,
    ) {
        requireNonBlank(
            note.noteId,
            GeneratedLearningNoteIssueCode.MissingNoteId,
            "La nota generada debe tener un id.",
            errors,
            noteField = "noteId",
        )
        requireNonBlank(
            note.expression,
            GeneratedLearningNoteIssueCode.MissingExpression,
            "La nota generada debe tener una expresion objetivo.",
            errors,
            noteField = "expression",
        )
    }

    private fun validateMeaningFields(
        note: GeneratedLearningNote,
        errors: MutableList<GeneratedLearningNoteIssue>,
    ) {
        requireNonBlank(
            note.intendedMeaningEs,
            GeneratedLearningNoteIssueCode.MissingIntendedMeaning,
            "La nota generada debe tener un significado intencional en espanol.",
            errors,
            noteField = "intendedMeaningEs",
        )
        requireNonBlank(
            note.simpleDefinitionEn,
            GeneratedLearningNoteIssueCode.MissingDefinition,
            "La nota generada debe tener una definicion simple en ingles.",
            errors,
            noteField = "simpleDefinitionEn",
        )
        requireNonBlank(
            note.whyUseful,
            GeneratedLearningNoteIssueCode.MissingWhyUseful,
            "La nota generada debe explicar por que vale la pena estudiarla.",
            errors,
            noteField = "whyUseful",
        )
    }

    private fun validateExampleFields(
        note: GeneratedLearningNote,
        errors: MutableList<GeneratedLearningNoteIssue>,
    ) {
        requireNonBlank(
            note.exampleSentence,
            GeneratedLearningNoteIssueCode.MissingExampleSentence,
            "La nota generada debe incluir un ejemplo principal.",
            errors,
            noteField = "exampleSentence",
        )
        requireNonBlank(
            note.exampleTranslation,
            GeneratedLearningNoteIssueCode.MissingExampleTranslation,
            "La nota generada debe incluir la traduccion del ejemplo.",
            errors,
            noteField = "exampleTranslation",
        )
    }

    private fun validateCollections(
        note: GeneratedLearningNote,
        errors: MutableList<GeneratedLearningNoteIssue>,
    ) {
        if (note.cards.isEmpty()) {
            errors += issue(
                GeneratedLearningNoteIssueCode.MissingCards,
                "La nota generada debe incluir tarjetas derivadas."
            )
        }
        if (note.qualityChecks.isEmpty()) {
            errors += issue(
                GeneratedLearningNoteIssueCode.MissingQualityChecks,
                "La nota generada debe incluir verificaciones de calidad."
            )
        }
    }

    private fun validateWordNote(
        note: GeneratedLearningNote,
        errors: MutableList<GeneratedLearningNoteIssue>,
    ) {
        requireExpectedCard(note, StudyCardType.Recognition, errors)
        requireExpectedCard(note, StudyCardType.Production, errors)
    }

    private fun validatePhraseNote(
        note: GeneratedLearningNote,
        errors: MutableList<GeneratedLearningNoteIssue>,
    ) {
        requireUsagePattern(note, "Las frases deben incluir un patron de uso.", errors)
        requireExpectedCard(note, StudyCardType.Recognition, errors)
        requireExpectedCard(note, StudyCardType.Production, errors)
        requireExpectedCard(note, StudyCardType.Cloze, errors)
    }

    private fun validatePhrasalVerbNote(
        note: GeneratedLearningNote,
        errors: MutableList<GeneratedLearningNoteIssue>,
    ) {
        requireUsagePattern(
            note,
            "Los phrasal verbs deben incluir patron de uso o separabilidad.",
            errors,
        )
        requireExpectedCard(note, StudyCardType.Recognition, errors)
        requireExpectedCard(note, StudyCardType.Production, errors)
        requireExpectedCard(note, StudyCardType.Cloze, errors)
    }

    private fun validateIdiomNote(
        note: GeneratedLearningNote,
        errors: MutableList<GeneratedLearningNoteIssue>,
    ) {
        requireExpectedCard(note, StudyCardType.Recognition, errors)
        requireExpectedCard(note, StudyCardType.Production, errors)
    }

    private fun validateSentencePatternNote(
        note: GeneratedLearningNote,
        errors: MutableList<GeneratedLearningNoteIssue>,
    ) {
        requireUsagePattern(note, "Los sentence patterns deben incluir patron de uso.", errors)
        requireNonBlank(
            note.clozeSentence,
            GeneratedLearningNoteIssueCode.MissingClozeSentence,
            "Los sentence patterns deben incluir una oracion cloze.",
            errors,
            noteField = "clozeSentence",
        )
        requireExpectedCard(note, StudyCardType.Production, errors)
        requireExpectedCard(note, StudyCardType.Cloze, errors)
    }

    private fun requireUsagePattern(
        note: GeneratedLearningNote,
        message: String,
        errors: MutableList<GeneratedLearningNoteIssue>,
    ) {
        requireNonBlank(
            note.usagePattern,
            GeneratedLearningNoteIssueCode.MissingUsagePattern,
            message,
            errors,
            noteField = "usagePattern",
        )
    }

    private fun validateCards(
        note: GeneratedLearningNote,
        errors: MutableList<GeneratedLearningNoteIssue>,
        warnings: MutableList<GeneratedLearningNoteIssue>,
    ) {
        val activeCards = note.cards.filter { it.isActive }
        if (activeCards.isEmpty()) {
            errors += issue(
                GeneratedLearningNoteIssueCode.NoActiveCards,
                "La nota generada debe incluir al menos una tarjeta activa."
            )
        }
        if (activeCards.size > MAX_RECOMMENDED_ACTIVE_CARDS) {
            warnings += issue(
                GeneratedLearningNoteIssueCode.TooManyActiveCards,
                "La nota tiene demasiadas tarjetas activas; " +
                    "conviene mantener entre 2 y 4 para una recuperacion mas clara."
            )
        }

        val duplicatedActiveCards: Map<String, GeneratedStudyCard> = activeCards
            .groupBy { "${it.prompt.normalizeForComparison()}::${it.expectedAnswer.normalizeForComparison()}" }
            .filterValues { it.size > 1 }
            .values
            .flatten()
            .associateBy { it.cardId }

        note.cards.forEach { card ->
            validateCard(card, duplicatedActiveCards, errors, warnings)
        }
    }

    private fun validateCard(
        card: GeneratedStudyCard,
        duplicatedActiveCards: Map<String, GeneratedStudyCard>,
        errors: MutableList<GeneratedLearningNoteIssue>,
        warnings: MutableList<GeneratedLearningNoteIssue>,
    ) {
        if (card.prompt.isBlank()) {
            errors += issue(
                GeneratedLearningNoteIssueCode.EmptyCardPrompt,
                "Cada tarjeta derivada debe tener un prompt.",
                cardId = card.cardId,
            )
        }
        if (card.expectedAnswer.isBlank()) {
            errors += issue(
                GeneratedLearningNoteIssueCode.EmptyCardAnswer,
                "Cada tarjeta derivada debe tener una respuesta esperada.",
                cardId = card.cardId,
            )
        }
        if (
            card.prompt.isNotBlank() &&
            card.expectedAnswer.isNotBlank() &&
            card.prompt.normalizeForComparison() == card.expectedAnswer.normalizeForComparison()
        ) {
            errors += issue(
                GeneratedLearningNoteIssueCode.CardPromptMatchesAnswer,
                "La tarjeta no genera recuperacion real porque el prompt coincide con la respuesta esperada.",
                cardId = card.cardId,
            )
        }
        if (card.isActive && duplicatedActiveCards.containsKey(card.cardId)) {
            warnings += issue(
                GeneratedLearningNoteIssueCode.DuplicateActiveCard,
                "Hay tarjetas activas redundantes con el mismo prompt y respuesta esperada.",
                cardId = card.cardId,
            )
        }
        if (card.expectedAnswer.wordCountForRecall() > MAX_RECOMMENDED_ANSWER_WORDS) {
            warnings += issue(
                GeneratedLearningNoteIssueCode.AnswerTooLongForRecall,
                "La respuesta esperada es larga; intenta que la recuperacion apunte a una expresion mas concreta.",
                cardId = card.cardId,
            )
        }
        if (!card.isActive) {
            warnings += issue(
                GeneratedLearningNoteIssueCode.InactiveCard,
                "La nota contiene una tarjeta inactiva; revisa si debe persistirse.",
                cardId = card.cardId,
            )
        }
    }

    private fun validateQualityChecks(
        note: GeneratedLearningNote,
        errors: MutableList<GeneratedLearningNoteIssue>,
    ) {
        val presentCodes = note.qualityChecks.map { it.code }.toSet()
        val hasSingleMeaningCheck = presentCodes.contains(GeneratedNoteQualityCode.SingleMeaning)
        if (!hasSingleMeaningCheck) {
            errors += issue(
                GeneratedLearningNoteIssueCode.MissingSingleMeaningQualityCheck,
                "La nota debe incluir la verificacion de significado unico."
            )
        }

        GeneratedNoteQualityCode.entries
            .filterNot(presentCodes::contains)
            .forEach { missingCode ->
                errors += issue(
                    GeneratedLearningNoteIssueCode.MissingRequiredQualityCheck,
                    "Falta el quality check requerido: ${missingCode.name}.",
                )
            }

        val failedChecks = note.qualityChecks.filterNot { it.passed }
        if (failedChecks.isNotEmpty()) {
            errors += issue(
                GeneratedLearningNoteIssueCode.FailedQualityCheck,
                "La nota no puede guardarse si algun quality check falla."
            )
        }
    }

    private fun requireExpectedCard(
        note: GeneratedLearningNote,
        cardType: StudyCardType,
        errors: MutableList<GeneratedLearningNoteIssue>,
    ) {
        val hasCard = note.cards.any { it.cardType == cardType && it.isActive }
        if (!hasCard) {
            errors += issue(
                GeneratedLearningNoteIssueCode.MissingExpectedCardType,
                "La nota ${note.noteType} debe incluir una tarjeta activa de tipo $cardType."
            )
        }
    }

    private fun requireNonBlank(
        value: String,
        code: GeneratedLearningNoteIssueCode,
        message: String,
        errors: MutableList<GeneratedLearningNoteIssue>,
        noteField: String? = null,
    ) {
        if (value.isBlank()) {
            errors += issue(code, message, noteField = noteField)
        }
    }

    private fun issue(
        code: GeneratedLearningNoteIssueCode,
        message: String,
        noteField: String? = null,
        cardId: String? = null,
    ): GeneratedLearningNoteIssue {
        return GeneratedLearningNoteIssue(
            code = code,
            message = message,
            noteField = noteField,
            cardId = cardId,
        )
    }

    private companion object {
        const val MAX_RECOMMENDED_ACTIVE_CARDS = 4
        const val MAX_RECOMMENDED_ANSWER_WORDS = 8
    }
}

private fun String.normalizeForComparison(): String {
    return trim().lowercase().replace("\\s+".toRegex(), " ")
}

private fun String.wordCountForRecall(): Int {
    if (isBlank()) return 0
    return trim().split("\\s+".toRegex()).size
}
