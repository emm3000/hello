package com.emm.domain.flashcard

import com.emm.domain.validation.IssueCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidateFlashcardGenerationInputUseCaseTest {

    private val useCase = ValidateFlashcardGenerationInputUseCase()

    @Test
    fun `invoke with blank user text returns error`() {
        val result = useCase(
            FlashcardGenerationInput(
                inputType = FlashcardInputType.Word,
                userText = "   ",
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == IssueCode.EmptyUserText })
    }

    @Test
    fun `invoke with ambiguous word and no disambiguation returns error`() {
        val result = useCase(
            FlashcardGenerationInput(
                inputType = FlashcardInputType.Word,
                userText = "get",
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == IssueCode.MissingDisambiguation })
    }

    @Test
    fun `invoke with ambiguous word and intended meaning is valid`() {
        val result = useCase(
            FlashcardGenerationInput(
                inputType = FlashcardInputType.Word,
                userText = "get",
                intendedMeaningEs = "obtener",
            )
        )

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `invoke with short sentence input returns error`() {
        val result = useCase(
            FlashcardGenerationInput(
                inputType = FlashcardInputType.Sentence,
                userText = "I got it",
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == IssueCode.SentenceInputTooShort })
    }

    @Test
    fun `invoke with communicative goal and missing intent returns error`() {
        val result = useCase(
            FlashcardGenerationInput(
                inputType = FlashcardInputType.CommunicativeGoal,
                userText = "pedir ayuda",
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == IssueCode.MissingCommunicativeIntent })
    }

    @Test
    fun `invoke normalizes whitespace`() {
        val result = useCase(
            FlashcardGenerationInput(
                inputType = FlashcardInputType.Word,
                userText = "  look    up  ",
                intendedMeaningEs = "  buscar  informacion ",
                contextSentence = " I   looked it up yesterday ",
            )
        )

        assertEquals("look up", result.value.userText)
        assertEquals("buscar informacion", result.value.intendedMeaningEs)
        assertEquals("I looked it up yesterday", result.value.contextSentence)
    }
}
