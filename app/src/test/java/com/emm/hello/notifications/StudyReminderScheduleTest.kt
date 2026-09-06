package com.emm.hello.notifications

import com.google.common.truth.Truth.assertThat
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class StudyReminderScheduleTest {

    private val now: ZonedDateTime = ZonedDateTime.of(2026, 9, 6, 10, 0, 0, 0, ZoneId.of("America/Lima"))

    @Test
    fun `target later today returns today at that time`() {
        val occurrence: ZonedDateTime = nextOccurrence(LocalTime.of(19, 0), now)

        assertThat(occurrence).isEqualTo(ZonedDateTime.of(2026, 9, 6, 19, 0, 0, 0, ZoneId.of("America/Lima")))
    }

    @Test
    fun `target earlier today rolls over to tomorrow`() {
        val occurrence: ZonedDateTime = nextOccurrence(LocalTime.of(9, 0), now)

        assertThat(occurrence).isEqualTo(ZonedDateTime.of(2026, 9, 7, 9, 0, 0, 0, ZoneId.of("America/Lima")))
    }

    @Test
    fun `target exactly now rolls over to tomorrow`() {
        val occurrence: ZonedDateTime = nextOccurrence(LocalTime.of(10, 0), now)

        assertThat(occurrence).isEqualTo(ZonedDateTime.of(2026, 9, 7, 10, 0, 0, 0, ZoneId.of("America/Lima")))
    }
}
