package com.emm.hello.newfeatures.card

import com.emm.domain.flashcard.DefinitionEn
import com.emm.domain.flashcard.FlashcardGenerationInput
import com.emm.domain.flashcard.FlashcardInputType
import com.emm.domain.flashcard.IntendedMeaningEs
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.LearningDomain
import com.emm.domain.flashcard.LearningGoal
import com.emm.domain.generation.LevelBand
import com.emm.domain.generation.RegenerableNoteField
import com.emm.domain.generation.RegisterPreference

internal fun NewCardUiState.toGenerationInput(): FlashcardGenerationInput {
    return FlashcardGenerationInput(
        inputType = word.inferInputType(),
        userText = word,
        intendedMeaningEs = intendedMeaningEs,
        contextSentence = contextSentence,
        learningGoal = LearningGoal.Both,
        levelBand = difficulty.toLevelBand(),
        register = RegisterPreference.Neutral,
        domain = LearningDomain.DailyLife,
    )
}

internal fun EditableLearningNoteField.toRegenerableFieldOrNull(): RegenerableNoteField? {
    return when (this) {
        EditableLearningNoteField.WhyUseful -> RegenerableNoteField.WhyUseful
        EditableLearningNoteField.UsagePattern -> RegenerableNoteField.UsagePattern
        EditableLearningNoteField.CommonMistake -> RegenerableNoteField.CommonMistake
        else -> null
    }
}

internal fun GeneratedLearningNote.withEditedField(
    field: EditableLearningNoteField,
    value: String,
): GeneratedLearningNote {
    return when (field) {
        EditableLearningNoteField.IntendedMeaningEs -> copy(intendedMeaningEs = IntendedMeaningEs.from(value))
        EditableLearningNoteField.SimpleDefinitionEn -> copy(simpleDefinitionEn = DefinitionEn.from(value))
        EditableLearningNoteField.WhyUseful -> copy(whyUseful = value)
        EditableLearningNoteField.ExampleSentence -> copy(exampleSentence = value)
        EditableLearningNoteField.ExampleTranslation -> copy(exampleTranslation = value)
        EditableLearningNoteField.UsagePattern -> copy(usagePattern = value)
        EditableLearningNoteField.CommonMistake -> copy(commonMistake = value)
        EditableLearningNoteField.ClozeSentence -> copy(clozeSentence = value)
    }
}

private fun String.inferInputType(): FlashcardInputType {
    val trimmed: String = trim()
    return when {
        trimmed.contains("?") || trimmed.contains(".") || trimmed.contains("!") -> FlashcardInputType.Sentence
        trimmed.contains(" ") -> FlashcardInputType.Phrase
        else -> FlashcardInputType.Word
    }
}

private fun String.toLevelBand(): LevelBand {
    return when (lowercase()) {
        "intermedio" -> LevelBand.B1_B2
        "avanzado" -> LevelBand.C1_PLUS
        else -> LevelBand.A1_A2
    }
}
