package com.emm.domain.flashcard

import com.emm.domain.validation.IssueCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FlashcardGenerationContextSentencePolicyTest {

    private val policy = FlashcardGenerationContextSentencePolicy()

    @Test
    fun `contextSentenceWarningOrNull returns warning when context sentence too short`() {
        val warning = policy.contextSentenceWarningOrNull(
            contextSentence = "I got it",
            wordCount = 3,
        )

        assertNotNull(warning)
        assertEquals(IssueCode.ContextSentenceTooShort, warning.code)
        assertEquals("contextSentence", warning.field)
    }

    @Test
    fun `contextSentenceWarningOrNull returns null when sentence has enough words`() {
        val warning = policy.contextSentenceWarningOrNull(
            contextSentence = "I got it yesterday",
            wordCount = 4,
        )

        assertNull(warning)
    }

    @Test
    fun `contextSentenceWarningOrNull returns null when context sentence blank`() {
        val warning = policy.contextSentenceWarningOrNull(
            contextSentence = "",
            wordCount = 0,
        )

        assertNull(warning)
    }
}
