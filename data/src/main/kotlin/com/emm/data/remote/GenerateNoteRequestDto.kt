package com.emm.data.remote

import com.emm.domain.flashcard.FlashcardGenerationInput
import com.emm.domain.validation.ValidationIssue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerateNoteRequestDto(
    @SerialName("input_type") val inputType: String,
    @SerialName("user_text") val userText: String,
    @SerialName("learning_goal") val learningGoal: String,
    @SerialName("level_band") val levelBand: String,
    @SerialName("register") val register: String,
    @SerialName("domain") val domain: String,
    @SerialName("previous_issues") val previousIssues: List<PreviousIssueDto>,
)

@Serializable
data class PreviousIssueDto(
    @SerialName("code") val code: String,
    @SerialName("field") val field: String,
)

fun FlashcardGenerationInput.toRequestDto(): GenerateNoteRequestDto {
    val normalized: FlashcardGenerationInput = normalized()
    return GenerateNoteRequestDto(
        inputType = normalized.inputType.name,
        userText = normalized.userText,
        learningGoal = normalized.learningGoal.name,
        levelBand = normalized.levelBand.name,
        register = normalized.register.name,
        domain = normalized.domain.name,
        previousIssues = normalized.previousIssues.map { issue -> issue.toDto() },
    )
}

private fun ValidationIssue.toDto(): PreviousIssueDto = PreviousIssueDto(code = code.value, field = field)
