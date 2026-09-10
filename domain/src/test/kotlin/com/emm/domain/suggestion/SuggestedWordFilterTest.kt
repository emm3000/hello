package com.emm.domain.suggestion

import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestedWordFilterTest {

    @Test
    fun `usable drops a candidate whose accented form is already captured`() {
        val candidates: List<SuggestedWord> = listOf(
            SuggestedWord("cafe", "cafe"),
            SuggestedWord("order", "pedir"),
        )

        val result: List<SuggestedWord> = SuggestedWordFilter.usable(candidates, listOf("Café"))

        assertEquals(listOf(SuggestedWord("order", "pedir")), result)
    }

    @Test
    fun `usable keeps the first of two candidates differing only by accent and case`() {
        val candidates: List<SuggestedWord> = listOf(
            SuggestedWord("Cliché", "cliche"),
            SuggestedWord("cliche", "topico"),
        )

        val result: List<SuggestedWord> = SuggestedWordFilter.usable(candidates, emptyList())

        assertEquals(listOf(SuggestedWord("Cliché", "cliche")), result)
    }

    @Test
    fun `usable drops a candidate with a blank word`() {
        val candidates: List<SuggestedWord> = listOf(
            SuggestedWord("   ", "pedir"),
            SuggestedWord("table", "mesa"),
        )

        val result: List<SuggestedWord> = SuggestedWordFilter.usable(candidates, emptyList())

        assertEquals(listOf(SuggestedWord("table", "mesa")), result)
    }

    @Test
    fun `usable drops a candidate with a blank translation`() {
        val candidates: List<SuggestedWord> = listOf(
            SuggestedWord("order", "   "),
            SuggestedWord("table", "mesa"),
        )

        val result: List<SuggestedWord> = SuggestedWordFilter.usable(candidates, emptyList())

        assertEquals(listOf(SuggestedWord("table", "mesa")), result)
    }

    @Test
    fun `usable preserves the original order of the surviving candidates`() {
        val candidates: List<SuggestedWord> = listOf(
            SuggestedWord("third", "tercero"),
            SuggestedWord("first", "primero"),
            SuggestedWord("second", "segundo"),
        )

        val result: List<SuggestedWord> = SuggestedWordFilter.usable(candidates, emptyList())

        assertEquals(candidates, result)
    }
}
