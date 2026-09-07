package com.emm.data.flashcard

import com.emm.domain.flashcard.FlashcardGenerationInput
import com.emm.domain.flashcard.FlashcardInputType
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptTest {

    @Test
    fun `buildLearningNotePrompt without previous issues omits the feedback section`() {
        val input = FlashcardGenerationInput(inputType = FlashcardInputType.Word, userText = "borrow")

        val prompt = Prompt.buildLearningNotePrompt(input)

        assertFalse(prompt.contains("rejected by these deterministic checks"))
    }

    @Test
    fun `buildLearningNotePrompt with previous issues appends the feedback section`() {
        val input = FlashcardGenerationInput(
            inputType = FlashcardInputType.Word,
            userText = "give up",
            previousIssues = listOf(ValidationIssue.Error(IssueCode.MissingUsagePattern, "usage_pattern")),
        )

        val prompt = Prompt.buildLearningNotePrompt(input)

        assertTrue(prompt.contains("If the input is too ambiguous or unusable"))
        val expectedSuffix = "\n\n" +
            "Your previous answer for this exact input was rejected by these deterministic checks:\n" +
            "- missing_usage_pattern (field: usage_pattern)\n" +
            "Fix every listed field and return the complete JSON again."
        assertTrue(prompt.endsWith(expectedSuffix))
    }
}
