package com.emm.domain.suggestion

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestedWordFilterTest {

    @Test
    fun `usable drops a candidate whose accented form is already captured`() = runTest {
        val filter = SuggestedWordFilter(FakeFlashcardRepository(words = listOf("Café")))
        val candidates: List<SuggestedWord> = listOf(
            SuggestedWord("cafe", "cafe"),
            SuggestedWord("order", "pedir"),
        )

        val result: List<SuggestedWord> = filter.usable(candidates)

        assertEquals(listOf(SuggestedWord("order", "pedir")), result)
    }

    @Test
    fun `usable drops a candidate captured as a card older than the recent words window`() = runTest {
        val newerCards: List<String> = List(SuggestedWordFilter.RECENT_WORDS_LIMIT) { index -> "newer$index" }
        val filter = SuggestedWordFilter(FakeFlashcardRepository(words = newerCards + "umbrella"))
        val candidates: List<SuggestedWord> = listOf(
            SuggestedWord("umbrella", "paraguas"),
            SuggestedWord("order", "pedir"),
        )

        val result: List<SuggestedWord> = filter.usable(candidates)

        assertEquals(listOf(SuggestedWord("order", "pedir")), result)
    }

    @Test
    fun `usable drops a candidate equal to a shown word after search normalization`() = runTest {
        val filter = SuggestedWordFilter(FakeFlashcardRepository(words = emptyList()))
        val candidates: List<SuggestedWord> = listOf(
            SuggestedWord("resume", "currículum"),
            SuggestedWord("order", "pedir"),
        )

        val result: List<SuggestedWord> = filter.usable(candidates, shownWords = listOf("Résumé"))

        assertEquals(listOf(SuggestedWord("order", "pedir")), result)
    }

    @Test
    fun `usable keeps the first of two candidates differing only by accent and case`() = runTest {
        val filter = SuggestedWordFilter(FakeFlashcardRepository(words = emptyList()))
        val candidates: List<SuggestedWord> = listOf(
            SuggestedWord("Cliché", "cliche"),
            SuggestedWord("cliche", "topico"),
        )

        val result: List<SuggestedWord> = filter.usable(candidates)

        assertEquals(listOf(SuggestedWord("Cliché", "cliche")), result)
    }

    @Test
    fun `usable drops a candidate with a blank word`() = runTest {
        val filter = SuggestedWordFilter(FakeFlashcardRepository(words = emptyList()))
        val candidates: List<SuggestedWord> = listOf(
            SuggestedWord("   ", "pedir"),
            SuggestedWord("table", "mesa"),
        )

        val result: List<SuggestedWord> = filter.usable(candidates)

        assertEquals(listOf(SuggestedWord("table", "mesa")), result)
    }

    @Test
    fun `usable drops a candidate with a blank translation`() = runTest {
        val filter = SuggestedWordFilter(FakeFlashcardRepository(words = emptyList()))
        val candidates: List<SuggestedWord> = listOf(
            SuggestedWord("order", "   "),
            SuggestedWord("table", "mesa"),
        )

        val result: List<SuggestedWord> = filter.usable(candidates)

        assertEquals(listOf(SuggestedWord("table", "mesa")), result)
    }

    @Test
    fun `usable preserves the original order of the surviving candidates`() = runTest {
        val filter = SuggestedWordFilter(FakeFlashcardRepository(words = emptyList()))
        val candidates: List<SuggestedWord> = listOf(
            SuggestedWord("third", "tercero"),
            SuggestedWord("first", "primero"),
            SuggestedWord("second", "segundo"),
        )

        val result: List<SuggestedWord> = filter.usable(candidates)

        assertEquals(candidates, result)
    }
}
