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
        )
        requireNonBlank(
            note.expression,
            GeneratedLearningNoteIssueCode.MissingExpression,
            "La nota generada debe tener una expresion objetivo.",
            errors,
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
        )
        requireNonBlank(
            note.simpleDefinitionEn,
            GeneratedLearningNoteIssueCode.MissingDefinition,
            "La nota generada debe tener una definicion simple en ingles.",
            errors,
        )
        requireNonBlank(
            note.whyUseful,
            GeneratedLearningNoteIssueCode.MissingWhyUseful,
            "La nota generada debe explicar por que vale la pena estudiarla.",
            errors,
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
        )
        requireNonBlank(
            note.exampleTranslation,
            GeneratedLearningNoteIssueCode.MissingExampleTranslation,
            "La nota generada debe incluir la traduccion del ejemplo.",
            errors,
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
        )
    }

    private fun validateCards(
        note: GeneratedLearningNote,
        errors: MutableList<GeneratedLearningNoteIssue>,
        warnings: MutableList<GeneratedLearningNoteIssue>,
    ) {
        note.cards.forEach { card ->
            if (card.prompt.isBlank()) {
                errors += issue(
                    GeneratedLearningNoteIssueCode.EmptyCardPrompt,
                    "Cada tarjeta derivada debe tener un prompt."
                )
            }
            if (card.expectedAnswer.isBlank()) {
                errors += issue(
                    GeneratedLearningNoteIssueCode.EmptyCardAnswer,
                    "Cada tarjeta derivada debe tener una respuesta esperada."
                )
            }
            if (!card.isActive) {
                warnings += issue(
                    GeneratedLearningNoteIssueCode.InactiveCard,
                    "La nota contiene una tarjeta inactiva; revisa si debe persistirse."
                )
            }
        }
    }

    private fun validateQualityChecks(
        note: GeneratedLearningNote,
        errors: MutableList<GeneratedLearningNoteIssue>,
    ) {
        val hasSingleMeaningCheck = note.qualityChecks.any {
            it.code == GeneratedNoteQualityCode.SingleMeaning
        }
        if (!hasSingleMeaningCheck) {
            errors += issue(
                GeneratedLearningNoteIssueCode.MissingSingleMeaningQualityCheck,
                "La nota debe incluir la verificacion de significado unico."
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
    ) {
        if (value.isBlank()) {
            errors += issue(code, message)
        }
    }

    private fun issue(
        code: GeneratedLearningNoteIssueCode,
        message: String,
    ): GeneratedLearningNoteIssue {
        return GeneratedLearningNoteIssue(code = code, message = message)
    }
}
