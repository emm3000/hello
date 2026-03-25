package com.emm.domain.flashcard

data class GeneratedNoteQualityCheck(
    val code: GeneratedNoteQualityCode,
    val passed: Boolean,
    val message: String,
)

enum class GeneratedNoteQualityCode {
    SingleMeaning,
    NaturalExample,
    ExampleSupportsMeaning,
    NonAmbiguousAnswers,
    RequiredFieldsPresent,
    ClearCardFocus,
    NoteCardAlignment,
}
