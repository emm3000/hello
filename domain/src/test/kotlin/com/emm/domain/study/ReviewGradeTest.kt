package com.emm.domain.study

import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewGradeTest {

    @Test
    fun `toFsrsRating maps AGAIN to 1`() {
        assertEquals(1, ReviewGrade.AGAIN.toFsrsRating())
    }

    @Test
    fun `toFsrsRating maps HARD to 2`() {
        assertEquals(2, ReviewGrade.HARD.toFsrsRating())
    }

    @Test
    fun `toFsrsRating maps GOOD to 3`() {
        assertEquals(3, ReviewGrade.GOOD.toFsrsRating())
    }

    @Test
    fun `toFsrsRating maps EASY to 4`() {
        assertEquals(4, ReviewGrade.EASY.toFsrsRating())
    }
}
