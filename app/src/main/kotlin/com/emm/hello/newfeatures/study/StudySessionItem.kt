package com.emm.hello.newfeatures.study

import com.emm.domain.flashcard.EvaluationMode
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.GeneratedStudyCard
import com.emm.domain.flashcard.StudyCardType

data class StudySessionItem(
    val flashcard: Flashcard,
    val studyCard: GeneratedStudyCard,
)

internal fun Flashcard.toStudySessionItems(): List<StudySessionItem> {
    val cards = studyCards
        .filter { it.isActive }
        .ifEmpty { listOf(defaultStudyCard()) }

    return cards.map { StudySessionItem(flashcard = this, studyCard = it) }
}

private fun Flashcard.defaultStudyCard(): GeneratedStudyCard {
    return GeneratedStudyCard(
        cardId = "${id}_default",
        cardType = StudyCardType.Recognition,
        prompt = word,
        expectedAnswer = translation,
        evaluationMode = EvaluationMode.ManualSelfCheck,
        hint = meaning,
        explanation = meaning,
        sourceField = "word",
    )
}
