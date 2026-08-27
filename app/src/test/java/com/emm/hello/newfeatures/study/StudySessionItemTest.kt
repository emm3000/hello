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
    fun `flashcard with several active study cards maps to exactly one item`() {
        val flashcard = studyFlashcard(
            word = "hello",
            studyCards = listOf(
                studyCard("a", StudyCardType.Recognition),
                studyCard("b", StudyCardType.Production),
                studyCard("c", StudyCardType.Cloze),
            ),
        )

        val item = flashcard.toStudySessionItem()

        assertThat(item.flashcardId).isEqualTo(flashcard.flashcardId)
        assertThat(item.word).isEqualTo("hello")
    }

    @Test
    fun `flashcard without study cards still maps to one item`() {
        val flashcard = studyFlashcard(word = "water", studyCards = emptyList())

        val item = flashcard.toStudySessionItem()

        assertThat(item.flashcardId).isEqualTo(flashcard.flashcardId)
        assertThat(item.word).isEqualTo("water")
    }

    @Test
    fun `item copies every rendered field and the review from the flashcard`() {
        val review = FsrsCard.new("go".toFlashcardId(), clock)
        val flashcard = StudyFlashcard(
            flashcardId = "go".toFlashcardId(),
            word = "go",
            phonetic = "/ɡəʊ/",
            meaning = "to move from one place to another",
            translation = "ir",
            review = review,
            studyCards = emptyList(),
            usagePattern = "go + to + place",
            whyUseful = "not rendered by the session",
            sourceContext = "not rendered by the session",
            irregularForms = listOf("went", "gone"),
            partOfSpeech = "verb",
            example = "I go to school every day.",
            exampleTranslation = "Voy a la escuela todos los días.",
        )

        val item = flashcard.toStudySessionItem()

        assertThat(item).isEqualTo(
            StudySessionItem(
                flashcardId = "go".toFlashcardId(),
                review = review,
                word = "go",
                phonetic = "/ɡəʊ/",
                meaning = "to move from one place to another",
                translation = "ir",
                usagePattern = "go + to + place",
                irregularForms = listOf("went", "gone"),
                partOfSpeech = "verb",
                example = "I go to school every day.",
                exampleTranslation = "Voy a la escuela todos los días.",
            )
        )
    }

    private fun studyFlashcard(
        word: String,
        studyCards: List<GeneratedStudyCard>,
    ): StudyFlashcard = StudyFlashcard(
        flashcardId = word.toFlashcardId(),
        word = word,
        phonetic = "",
        meaning = "",
        translation = "",
        review = FsrsCard.new(word.toFlashcardId(), clock),
        studyCards = studyCards,
    )

    private fun studyCard(
        id: String,
        type: StudyCardType,
    ) = GeneratedStudyCard(
        cardId = id,
        cardType = type,
        prompt = id,
        expectedAnswer = id,
        evaluationMode = EvaluationMode.ManualSelfCheck,
    )
}
