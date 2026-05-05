package com.emm.domain.flashcard

import com.emm.domain.validation.IssueCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FlashcardGenerationDisambiguationPolicyTest {

    private val policy = FlashcardGenerationDisambiguationPolicy()

    @Test
    fun `missingDisambiguationIssueOrNull returns issue for ambiguous word without context`() {
        val issue = policy.missingDisambiguationIssueOrNull(
            input = FlashcardGenerationInput(
                inputType = FlashcardInputType.Word,
                userText = "get",
            ),
            wordCount = 1,
        )

        assertNotNull(issue)
        assertEquals(IssueCode.MissingDisambiguation, issue.code)
        assertEquals("disambiguation", issue.field)
    }

    @Test
    fun `missingDisambiguationIssueOrNull returns null when intended meaning exists`() {
        val issue = policy.missingDisambiguationIssueOrNull(
            input = FlashcardGenerationInput(
                inputType = FlashcardInputType.Word,
                userText = "get",
                intendedMeaningEs = "obtener",
            ),
            wordCount = 1,
        )

        assertNull(issue)
    }

    @Test
    fun `missingDisambiguationIssueOrNull returns null for sentence input`() {
        val issue = policy.missingDisambiguationIssueOrNull(
            input = FlashcardGenerationInput(
                inputType = FlashcardInputType.Sentence,
                userText = "I get it now",
            ),
            wordCount = 4,
        )

        assertNull(issue)
    }
}
