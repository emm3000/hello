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
                direction = StudyDirection.RECOGNITION,
                usagePattern = "go + to + place",
                irregularForms = listOf("went", "gone"),
                partOfSpeech = "verb",
                example = "I go to school every day.",
                exampleTranslation = "Voy a la escuela todos los días.",
            )
        )
    }

    @Test
    fun `flashcard with a review past its production graduation maps to direction PRODUCTION`() {
        val review = FsrsCard.new("go".toFlashcardId(), clock).copy(productionSince = 1_000L)
        val flashcard = studyFlashcard(word = "go", studyCards = emptyList(), review = review)

        val item = flashcard.toStudySessionItem()

        assertThat(item.direction).isEqualTo(StudyDirection.PRODUCTION)
    }

    @Test
    fun `flashcard with a review never graduated to production maps to direction RECOGNITION`() {
        val review = FsrsCard.new("go".toFlashcardId(), clock)
        val flashcard = studyFlashcard(word = "go", studyCards = emptyList(), review = review)

        val item = flashcard.toStudySessionItem()

        assertThat(item.direction).isEqualTo(StudyDirection.RECOGNITION)
    }

    @Test
    fun `RECOGNITION direction reveals the word on the front and back faces`() {
        val review = FsrsCard.new("go".toFlashcardId(), clock)
        val item = studyFlashcard(word = "go", studyCards = emptyList(), review = review).toStudySessionItem()

        assertThat(item.revealsWordOn(CardFace.Front)).isTrue()
        assertThat(item.revealsWordOn(CardFace.Back)).isTrue()
    }

    @Test
    fun `PRODUCTION direction reveals the word only on the back face`() {
        val review = FsrsCard.new("go".toFlashcardId(), clock).copy(productionSince = 1_000L)
        val item = studyFlashcard(word = "go", studyCards = emptyList(), review = review).toStudySessionItem()

        assertThat(item.revealsWordOn(CardFace.Front)).isFalse()
        assertThat(item.revealsWordOn(CardFace.Back)).isTrue()
    }

    @Test
    fun `cue is the translation when the flashcard has one`() {
        val flashcard = studyFlashcard(word = "go", studyCards = emptyList())
            .copy(translation = "ir", meaning = "to move from one place to another")

        val item = flashcard.toStudySessionItem()

        assertThat(item.cue).isEqualTo("ir")
    }

    @Test
    fun `cue falls back to the meaning when the translation is blank`() {
        val flashcard = studyFlashcard(word = "go", studyCards = emptyList())
            .copy(translation = "   ", meaning = "to move from one place to another")

        val item = flashcard.toStudySessionItem()

        assertThat(item.cue).isEqualTo("to move from one place to another")
    }

    private fun studyFlashcard(
        word: String,
        studyCards: List<GeneratedStudyCard>,
        review: FsrsCard = FsrsCard.new(word.toFlashcardId(), clock),
    ): StudyFlashcard = StudyFlashcard(
        flashcardId = word.toFlashcardId(),
        word = word,
        phonetic = "",
        meaning = "",
        translation = "",
        review = review,
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
