package com.emm.domain.study

data class StudySession(
    val cards: List<StudyFlashcard>,
    val heldBackNewCards: Int,
) {

    init {
        require(heldBackNewCards >= 0) { "heldBackNewCards must be non-negative, was $heldBackNewCards." }
    }
}
