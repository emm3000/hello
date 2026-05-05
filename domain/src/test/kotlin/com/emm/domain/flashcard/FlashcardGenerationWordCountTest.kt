package com.emm.domain.flashcard

import kotlin.test.Test
import kotlin.test.assertEquals

class FlashcardGenerationWordCountTest {

    @Test
    fun `wordCountNormalized returns zero for blank text`() {
        assertEquals(0, "   ".wordCountNormalized())
    }

    @Test
    fun `wordCountNormalized collapses multiple spaces`() {
        assertEquals(3, "  i   got   it  ".wordCountNormalized())
    }

    @Test
    fun `wordCountNormalized handles single word`() {
        assertEquals(1, "borrow".wordCountNormalized())
    }
}
