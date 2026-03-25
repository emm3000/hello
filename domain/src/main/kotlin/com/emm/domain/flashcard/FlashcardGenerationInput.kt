package com.emm.domain.flashcard

data class FlashcardGenerationInput(
    val inputType: FlashcardInputType,
    val userText: String,
    val intendedMeaningEs: String = "",
    val contextSentence: String = "",
    val learningGoal: LearningGoal = LearningGoal.Both,
    val levelBand: LevelBand = LevelBand.A1_A2,
    val register: RegisterPreference = RegisterPreference.Neutral,
    val domain: LearningDomain = LearningDomain.DailyLife,
    val communicativeIntentId: String = "",
) {

    fun normalized(): FlashcardGenerationInput {
        return copy(
            userText = userText.normalizeWhitespace(),
            intendedMeaningEs = intendedMeaningEs.normalizeWhitespace(),
            contextSentence = contextSentence.normalizeWhitespace(),
            communicativeIntentId = communicativeIntentId.trim(),
        )
    }
}

private fun String.normalizeWhitespace(): String {
    return trim().replace("\\s+".toRegex(), " ")
}
