package com.emm.domain.authoring

import com.emm.domain.flashcard.toDefinitionEn
import com.emm.domain.flashcard.toExpression
import com.emm.domain.flashcard.toIntendedMeaningEs
import com.emm.domain.generation.EvaluationMode
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.GeneratedNoteQualityCheck
import com.emm.domain.generation.GeneratedNoteQualityCode
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.LearningDomain
import com.emm.domain.generation.LearningNoteType
import com.emm.domain.generation.LevelBand
import com.emm.domain.generation.PartOfSpeechTag
import com.emm.domain.generation.RegisterPreference
import com.emm.domain.generation.StudyCardType
import com.emm.domain.ids.toDeckId
import kotlin.test.Test
import kotlin.test.assertEquals

class GeneratedLearningNoteMapperTest {

    private val mapper = GeneratedLearningNoteMapper()

    @Test
    fun `toCreateFlashcardInput normalizes expression meaning and definition`() {
        val input = mapper.toCreateFlashcardInput(
            deckId = "deck-1".toDeckId(),
            note = sampleWordNote().copy(
                expression = "  Borrow   ".toExpression(),
                intendedMeaningEs = "  pedir   prestado  ".toIntendedMeaningEs(),
                simpleDefinitionEn = "  to   take something   and return it later  ".toDefinitionEn(),
            ),
        )

        assertEquals("Borrow", input.word)
        assertEquals("pedir prestado", input.translation)
        assertEquals("to take something and return it later", input.meaning)
    }

    @Test
    fun `toExamples returns main example when example sentence present`() {
        val examples = mapper.toExamples(sampleWordNote())

        assertEquals(1, examples.size)
        assertEquals("learning-note-example", examples.first().exampleId)
        assertEquals("main", examples.first().type)
    }

    @Test
    fun `toExamples returns empty list when example sentence blank`() {
        val examples = mapper.toExamples(sampleWordNote().copy(exampleSentence = ""))

        assertEquals(0, examples.size)
    }

    private fun sampleWordNote(): GeneratedLearningNote {
        return GeneratedLearningNote(
            noteId = "note-1",
            noteType = LearningNoteType.Word,
            expression = "borrow".toExpression(),
            intendedMeaningEs = "pedir prestado".toIntendedMeaningEs(),
            simpleDefinitionEn = "to take something and return it later".toDefinitionEn(),
            partOfSpeech = PartOfSpeechTag.Verb,
            register = RegisterPreference.Neutral,
            levelBand = LevelBand.A1_A2,
            domain = LearningDomain.DailyLife,
            whyUseful = "Sirve para hablar de prestamos.",
            exampleSentence = "Can I borrow your pen?",
            exampleTranslation = "Puedo pedirte prestado tu lapicero?",
            cards = listOf(
                GeneratedStudyCard(
                    cardId = "card-1",
                    cardType = StudyCardType.Recognition,
                    prompt = "borrow",
                    expectedAnswer = "pedir prestado",
                    evaluationMode = EvaluationMode.FlexibleText,
                ),
                GeneratedStudyCard(
                    cardId = "card-2",
                    cardType = StudyCardType.Production,
                    prompt = "Como dices pedir prestado en ingles?",
                    expectedAnswer = "borrow",
                    evaluationMode = EvaluationMode.Exact,
                ),
            ),
            qualityChecks = listOf(
                GeneratedNoteQualityCheck(
                    code = GeneratedNoteQualityCode.SingleMeaning,
                    passed = true,
                    message = "ok",
                ),
                GeneratedNoteQualityCheck(
                    code = GeneratedNoteQualityCode.NaturalExample,
                    passed = true,
                    message = "ok",
                ),
                GeneratedNoteQualityCheck(
                    code = GeneratedNoteQualityCode.ExampleSupportsMeaning,
                    passed = true,
                    message = "ok",
                ),
                GeneratedNoteQualityCheck(
                    code = GeneratedNoteQualityCode.NonAmbiguousAnswers,
                    passed = true,
                    message = "ok",
                ),
                GeneratedNoteQualityCheck(
                    code = GeneratedNoteQualityCode.RequiredFieldsPresent,
                    passed = true,
                    message = "ok",
                ),
                GeneratedNoteQualityCheck(
                    code = GeneratedNoteQualityCode.ClearCardFocus,
                    passed = true,
                    message = "ok",
                ),
                GeneratedNoteQualityCheck(
                    code = GeneratedNoteQualityCode.NoteCardAlignment,
                    passed = true,
                    message = "ok",
                ),
            ),
        )
    }
}
