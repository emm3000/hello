package com.emm.domain.generation

data class GeneratedNoteQualityCheck(
    val code: GeneratedNoteQualityCode,
    val passed: Boolean,
    val message: String,
)

enum class GeneratedNoteQualityCode {
    SingleMeaning,
    NaturalExample,
    NonAmbiguousAnswers,
    ClearCardFocus,
    NoteCardAlignment,
}
