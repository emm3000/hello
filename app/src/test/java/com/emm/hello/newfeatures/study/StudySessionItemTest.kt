package com.emm.hello.newfeatures.study

import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.generation.EvaluationMode
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.StudyCardType
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.study.StudyFlashcard
import com.emm.domain.time.Clock
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class StudySessionItemTest {

    private val clock = Clock { Instant.parse("2026-05-04T12:30:45Z") }

    @Test
    fun `flashcard without study cards falls back to a basic recognition item`() {
        val flashcard = studyFlashcard(word = "hello", translation = "hola", studyCards = emptyList())

        val items = flashcard.toStudySessionItems()

        assertThat(items).hasSize(1)
        val card = items.single().studyCard
        assertThat(card.cardType).isEqualTo(StudyCardType.Recognition)
        assertThat(card.prompt).isEqualTo("hello")
        assertThat(card.expectedAnswer).isEqualTo("hola")
        assertThat(card.evaluationMode).isEqualTo(EvaluationMode.ManualSelfCheck)
        assertThat(items.single().word).isEqualTo("hello")
        assertThat(items.single().translation).isEqualTo("hola")
    }

    @Test
    fun `flashcard with active study cards maps them and ignores the fallback`() {
        val flashcard = studyFlashcard(
            word = "hello",
            translation = "hola",
            studyCards = listOf(
                studyCard("a", StudyCardType.Recognition),
                studyCard("b", StudyCardType.Production),
            ),
        )

        val items = flashcard.toStudySessionItems()

        assertThat(items.map { it.studyCard.cardId }).containsExactly("a", "b").inOrder()
    }

    @Test
    fun `flashcard whose study cards are all inactive falls back to a basic item`() {
        val flashcard = studyFlashcard(
            word = "water",
            translation = "agua",
            studyCards = listOf(studyCard("a", StudyCardType.Recognition, isActive = false)),
        )

        val items = flashcard.toStudySessionItems()

        assertThat(items).hasSize(1)
        assertThat(items.single().studyCard.prompt).isEqualTo("water")
        assertThat(items.single().studyCard.expectedAnswer).isEqualTo("agua")
    }

    private fun studyFlashcard(
        word: String,
        translation: String,
        studyCards: List<GeneratedStudyCard>,
    ): StudyFlashcard = StudyFlashcard(
        flashcardId = word.toFlashcardId(),
        word = word,
        phonetic = "",
        meaning = "",
        translation = translation,
        review = FsrsCard.new(word.toFlashcardId(), clock),
        studyCards = studyCards,
    )

    private fun studyCard(
        id: String,
        type: StudyCardType,
        isActive: Boolean = true,
    ) = GeneratedStudyCard(
        cardId = id,
        cardType = type,
        prompt = id,
        expectedAnswer = id,
        evaluationMode = EvaluationMode.ManualSelfCheck,
        isActive = isActive,
    )
}
