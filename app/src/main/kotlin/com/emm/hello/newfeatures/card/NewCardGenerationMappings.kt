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
import com.emm.data.catalog.StaticCategories
import java.text.Normalizer

private const val DEFAULT_COMMUNICATIVE_INTENT_ID = "social_small_talk"
private const val DESCRIBE_PAST_EVENTS_INTENT_ID = "describe_past_events"
private const val TALK_ABOUT_PLANS_INTENT_ID = "talk_about_plans"
private const val MAKE_POLITE_REQUESTS_INTENT_ID = "make_polite_requests"
private const val EXPRESS_EMOTIONS_INTENT_ID = "express_emotions"
private const val ORDER_FOOD_INTENT_ID = "order_food"
private const val ASK_FOR_DIRECTIONS_INTENT_ID = "ask_for_directions"
private const val HANDLE_COMPLAINTS_INTENT_ID = "handle_complaints"
private const val WORK_INTENT_ID = "work"
private const val SOLVE_DAILY_PROBLEMS_INTENT_ID = "solve_daily_problems"
private const val STUDY_INTENT_ID = "study"

private const val TRAVEL_CATEGORY_ID = 23
private const val WORK_CATEGORY_ID = 24
private const val DAILY_PROBLEMS_CATEGORY_ID = 25

private val socialSmallTalkCategoryIds = setOf(2, 8, 16, 22)
private val describePastEventsCategoryIds = setOf(4, 12, 13)
private val talkAboutPlansCategoryIds = setOf(14, 20)
private val makePoliteRequestsCategoryIds = setOf(15, 16, 17, 18)
private val expressEmotionsCategoryIds = setOf(21, 26)
private val studyCategoryIds = setOf(27, 28, 29, 30)
private val socialDomainCategoryIds = setOf(22, 26)

internal fun NewCardUiState.toGenerationInput(): FlashcardGenerationInput {
    return when (typeView) {
        TypeView.WordOrPhase -> FlashcardGenerationInput(
            inputType = word.inferInputType(),
            userText = word,
            intendedMeaningEs = intendedMeaningEs,
            contextSentence = contextSentence,
            learningGoal = LearningGoal.Both,
            levelBand = difficulty.toLevelBand(),
            register = RegisterPreference.Neutral,
            domain = LearningDomain.DailyLife,
        )

        TypeView.WithCategories -> FlashcardGenerationInput(
            inputType = FlashcardInputType.CommunicativeGoal,
            userText = category.toGenerationPrompt(),
            learningGoal = LearningGoal.Both,
            levelBand = difficulty.toLevelBand(),
            register = RegisterPreference.Neutral,
            domain = category.toLearningDomain(),
            communicativeIntentId = category.toCommunicativeIntentId(),
        )

        TypeView.WithAiHelp -> FlashcardGenerationInput(
            inputType = FlashcardInputType.CommunicativeGoal,
            userText = aiRequest,
            learningGoal = LearningGoal.Both,
            levelBand = difficulty.toLevelBand(),
            register = RegisterPreference.Neutral,
            domain = aiRequest.toLearningDomain(),
            communicativeIntentId = aiRequest.toCommunicativeIntentId(),
        )
    }
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
    val trimmed = trim()
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

private fun StaticCategories.toGenerationPrompt(): String {
    return "Create a high-value English learning note for the category: $name"
}

private fun StaticCategories.toCommunicativeIntentId(): String {
    return when (id) {
        in socialSmallTalkCategoryIds -> DEFAULT_COMMUNICATIVE_INTENT_ID
        in describePastEventsCategoryIds -> DESCRIBE_PAST_EVENTS_INTENT_ID
        in talkAboutPlansCategoryIds -> TALK_ABOUT_PLANS_INTENT_ID
        in makePoliteRequestsCategoryIds -> MAKE_POLITE_REQUESTS_INTENT_ID
        in expressEmotionsCategoryIds -> EXPRESS_EMOTIONS_INTENT_ID
        TRAVEL_CATEGORY_ID -> ASK_FOR_DIRECTIONS_INTENT_ID
        WORK_CATEGORY_ID -> WORK_INTENT_ID
        DAILY_PROBLEMS_CATEGORY_ID -> SOLVE_DAILY_PROBLEMS_INTENT_ID
        in studyCategoryIds -> STUDY_INTENT_ID
        else -> DEFAULT_COMMUNICATIVE_INTENT_ID
    }
}

private fun StaticCategories.toLearningDomain(): LearningDomain {
    return when (id) {
        TRAVEL_CATEGORY_ID -> LearningDomain.Travel
        WORK_CATEGORY_ID -> LearningDomain.Work
        in studyCategoryIds -> LearningDomain.Study
        in socialDomainCategoryIds -> LearningDomain.Social
        else -> LearningDomain.DailyLife
    }
}

private fun String.toCommunicativeIntentId(): String {
    val normalized = normalizeForInference()
    return when {
        normalized.hasAnyKeyword(
            "restaurante",
            "restaurant",
            "comida",
            "food",
            "menu",
            "cafeteria",
            "cafe",
            "delivery",
            "ordenar",
            "pedir",
            "order",
        ) -> ORDER_FOOD_INTENT_ID
        normalized.hasAnyKeyword(
            "aeropuerto",
            "airport",
            "vuelo",
            "flight",
            "hotel",
            "taxi",
            "check in",
            "boarding",
            "gate",
            "travel",
            "viaje",
        ) -> ASK_FOR_DIRECTIONS_INTENT_ID
        normalized.hasAnyKeyword(
            "trabajo",
            "work",
            "oficina",
            "office",
            "negocio",
            "business",
            "reunion",
            "meeting",
            "entrevista",
            "interview",
            "email",
            "correo",
        ) -> WORK_INTENT_ID
        normalized.hasAnyKeyword(
            "estudio",
            "study",
            "clase",
            "class",
            "escuela",
            "school",
            "universidad",
            "university",
            "examen",
            "exam",
        ) -> STUDY_INTENT_ID
        normalized.hasAnyKeyword(
            "queja",
            "complaint",
            "reclamo",
            "refund",
            "devolucion",
            "customer service",
            "servicio al cliente",
        ) -> HANDLE_COMPLAINTS_INTENT_ID
        normalized.hasAnyKeyword(
            "ayuda",
            "help",
            "please",
            "favor",
            "request",
            "solicitar",
            "pedir ayuda",
        ) -> MAKE_POLITE_REQUESTS_INTENT_ID
        normalized.hasAnyKeyword(
            "emocion",
            "emotion",
            "sentimiento",
            "feeling",
            "feliz",
            "happy",
            "triste",
            "sad",
            "frustrado",
            "angry",
        ) -> EXPRESS_EMOTIONS_INTENT_ID
        normalized.hasAnyKeyword(
            "pasado",
            "past",
            "ayer",
            "experiencia",
            "experience",
            "historia",
            "story",
        ) -> DESCRIBE_PAST_EVENTS_INTENT_ID
        normalized.hasAnyKeyword(
            "planes",
            "future",
            "futuro",
            "mañana",
            "tomorrow",
            "plan",
            "going to",
            "next week",
        ) -> TALK_ABOUT_PLANS_INTENT_ID
        else -> DEFAULT_COMMUNICATIVE_INTENT_ID
    }
}

private fun String.toLearningDomain(): LearningDomain {
    val normalized = normalizeForInference()
    return when {
        normalized.hasAnyKeyword(
            "aeropuerto",
            "airport",
            "hotel",
            "taxi",
            "travel",
            "viaje",
            "vacaciones",
            "vacation",
            "boarding",
            "flight",
        ) -> LearningDomain.Travel
        normalized.hasAnyKeyword(
            "trabajo",
            "work",
            "oficina",
            "office",
            "negocio",
            "business",
            "cliente",
            "client",
        ) -> LearningDomain.Work
        normalized.hasAnyKeyword(
            "amigos",
            "friends",
            "social",
            "party",
            "cita",
            "date",
            "conversation",
            "conversacion",
        ) -> LearningDomain.Social
        normalized.hasAnyKeyword(
            "estudio",
            "study",
            "escuela",
            "school",
            "universidad",
            "university",
            "exam",
            "examen",
        ) -> LearningDomain.Study
        else -> LearningDomain.DailyLife
    }
}

private fun String.normalizeForInference(): String {
    return Normalizer
        .normalize(trim().lowercase(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
}

private fun String.hasAnyKeyword(vararg keywords: String): Boolean {
    return keywords.any { keyword -> contains(keyword) }
}
