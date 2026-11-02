package com.emm.domain.dailywords

import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyWordsSettingsTest {

    @Test
    fun `default settings are enabled at nine in the morning on every day of the week`() {
        val expected = DailyWordsSettings(
            isEnabled = true,
            time = LocalTime.of(9, 0),
            days = DayOfWeek.entries.toSet(),
        )

        assertEquals(expected, DailyWordsSettings.DEFAULT)
    }
}
