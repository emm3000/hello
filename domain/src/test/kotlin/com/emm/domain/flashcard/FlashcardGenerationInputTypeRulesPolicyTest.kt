package com.emm.domain.flashcard

import com.emm.domain.validation.IssueCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlashcardGenerationInputTypeRulesPolicyTest {

    private val policy = FlashcardGenerationInputTypeRulesPolicy()

    @Test
    fun `validate returns word warning for whitespace`() {
        val result = policy.validate(
            input = FlashcardGenerationInput(
                inputType = FlashcardInputType.Word,
                userText = "pick up",
            ),
            userText = "pick up",
            wordCount = 2,
        )

        assertTrue(result.errors.isEmpty())
        assertEquals(IssueCode.WordInputContainsWhitespace, result.warnings.single().code)
    }

    @Test
    fun `validate returns sentence error when too short`() {
        val result = policy.validate(
            input = FlashcardGenerationInput(
                inputType = FlashcardInputType.Sentence,
                userText = "I got it",
            ),
            userText = "I got it",
            wordCount = 3,
        )

        assertEquals(IssueCode.SentenceInputTooShort, result.errors.single().code)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `validate returns communicative intent error when missing`() {
        val result = policy.validate(
            input = FlashcardGenerationInput(
                inputType = FlashcardInputType.CommunicativeGoal,
                userText = "pedir ayuda",
                communicativeIntentId = "",
            ),
            userText = "pedir ayuda",
            wordCount = 2,
        )

        assertEquals(IssueCode.MissingCommunicativeIntent, result.errors.single().code)
    }
}
