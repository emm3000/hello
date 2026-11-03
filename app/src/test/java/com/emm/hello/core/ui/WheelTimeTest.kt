package com.emm.hello.core.ui

import com.google.common.truth.Truth.assertThat
import java.time.LocalTime
import org.junit.Test

class WheelTimeTest {

    @Test
    fun `a time on a step stays where it is`() {
        assertThat(LocalTime.of(10, 0).roundedToMinuteStep()).isEqualTo(LocalTime.of(10, 0))
    }

    @Test
    fun `a minute below the midpoint rounds down`() {
        assertThat(LocalTime.of(9, 2).roundedToMinuteStep()).isEqualTo(LocalTime.of(9, 0))
        assertThat(LocalTime.of(9, 57).roundedToMinuteStep()).isEqualTo(LocalTime.of(9, 55))
    }

    @Test
    fun `a minute at or past the midpoint rounds up`() {
        assertThat(LocalTime.of(9, 3).roundedToMinuteStep()).isEqualTo(LocalTime.of(9, 5))
    }

    @Test
    fun `seconds do not push a minute over the midpoint`() {
        assertThat(LocalTime.of(9, 2, 59, 999).roundedToMinuteStep()).isEqualTo(LocalTime.of(9, 0))
    }

    @Test
    fun `rounding up carries into the hour`() {
        assertThat(LocalTime.of(9, 58).roundedToMinuteStep()).isEqualTo(LocalTime.of(10, 0))
    }

    @Test
    fun `rounding up past the last hour wraps to midnight`() {
        assertThat(LocalTime.of(23, 58).roundedToMinuteStep()).isEqualTo(LocalTime.MIDNIGHT)
    }

    @Test
    fun `the hour wheel wraps from 23 back to 0 in both directions`() {
        assertThat(hourWheelRange.valueAt(23)).isEqualTo(23)
        assertThat(hourWheelRange.valueAt(24)).isEqualTo(0)
        assertThat(hourWheelRange.valueAt(-1)).isEqualTo(23)
    }

    @Test
    fun `the minute wheel counts in steps of five and wraps from 55 back to 0`() {
        assertThat(minuteWheelRange.valueAt(1)).isEqualTo(5)
        assertThat(minuteWheelRange.valueAt(11)).isEqualTo(55)
        assertThat(minuteWheelRange.valueAt(12)).isEqualTo(0)
        assertThat(minuteWheelRange.valueAt(-1)).isEqualTo(55)
    }

    @Test
    fun `the start index shows the value in the middle of the virtual list`() {
        val start: Int = minuteWheelRange.startIndexFor(30)

        assertThat(minuteWheelRange.valueAt(start)).isEqualTo(30)
        assertThat(start - minuteWheelRange.virtualCount / 2).isIn(0 until minuteWheelRange.rowCount)
    }

    @Test
    fun `moving to a value takes the shortest path across the wrap`() {
        val atHour23: Int = hourWheelRange.startIndexFor(23)
        val atHour0: Int = hourWheelRange.startIndexFor(0)

        assertThat(hourWheelRange.nearestIndexFor(0, fromIndex = atHour23)).isEqualTo(atHour23 + 1)
        assertThat(hourWheelRange.nearestIndexFor(23, fromIndex = atHour0)).isEqualTo(atHour0 - 1)
    }

    @Test
    fun `moving to a nearby value stays on the same cycle`() {
        val atMinute10: Int = minuteWheelRange.startIndexFor(10)

        assertThat(minuteWheelRange.nearestIndexFor(25, fromIndex = atMinute10)).isEqualTo(atMinute10 + 3)
        assertThat(minuteWheelRange.nearestIndexFor(10, fromIndex = atMinute10)).isEqualTo(atMinute10)
    }
}
