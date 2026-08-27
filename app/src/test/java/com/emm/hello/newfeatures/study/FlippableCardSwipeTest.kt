package com.emm.hello.newfeatures.study

import com.emm.domain.study.ReviewGrade
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FlippableCardSwipeTest {

    private val widthPx = 1000f

    @Test
    fun `left drag below the dead zone returns null`() {
        val grade = computeSwipeGrade(dragOffsetPx = -100f, widthPx = widthPx)

        assertThat(grade).isNull()
    }

    @Test
    fun `right drag below the dead zone returns null`() {
        val grade = computeSwipeGrade(dragOffsetPx = 100f, widthPx = widthPx)

        assertThat(grade).isNull()
    }

    @Test
    fun `left drag past the threshold returns AGAIN`() {
        val grade = computeSwipeGrade(dragOffsetPx = -300f, widthPx = widthPx)

        assertThat(grade).isEqualTo(ReviewGrade.AGAIN)
    }

    @Test
    fun `right drag past the threshold returns GOOD`() {
        val grade = computeSwipeGrade(dragOffsetPx = 300f, widthPx = widthPx)

        assertThat(grade).isEqualTo(ReviewGrade.GOOD)
    }

    @Test
    fun `long left drag past 0,5 still returns AGAIN, not HARD`() {
        val grade = computeSwipeGrade(dragOffsetPx = -600f, widthPx = widthPx)

        assertThat(grade).isEqualTo(ReviewGrade.AGAIN)
    }

    @Test
    fun `zero width returns null instead of grading the card`() {
        val resting = computeSwipeGrade(dragOffsetPx = 0f, widthPx = 0f)
        val dragged = computeSwipeGrade(dragOffsetPx = 300f, widthPx = 0f)

        assertThat(resting).isNull()
        assertThat(dragged).isNull()
    }

    @Test
    fun `long right drag past 0,5 still returns GOOD, not EASY`() {
        val grade = computeSwipeGrade(dragOffsetPx = 600f, widthPx = widthPx)

        assertThat(grade).isEqualTo(ReviewGrade.GOOD)
    }
}
