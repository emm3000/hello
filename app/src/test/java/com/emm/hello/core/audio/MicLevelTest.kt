package com.emm.hello.core.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MicLevelTest {

    @Test
    fun `silence normalizes to zero`() {
        assertThat(MicLevel.fromRms(-2f)).isEqualTo(0f)
    }

    @Test
    fun `a loud reading normalizes to one`() {
        assertThat(MicLevel.fromRms(10f)).isEqualTo(1f)
    }

    @Test
    fun `readings below the floor clamp to zero instead of going negative`() {
        assertThat(MicLevel.fromRms(-120f)).isEqualTo(0f)
    }

    @Test
    fun `readings above the ceiling clamp to one`() {
        assertThat(MicLevel.fromRms(60f)).isEqualTo(1f)
    }

    @Test
    fun `a mid reading lands between the bounds`() {
        val level: Float = MicLevel.fromRms(4f)

        assertThat(level).isGreaterThan(0f)
        assertThat(level).isLessThan(1f)
    }

    @Test
    fun `smoothing moves toward the target without jumping to it`() {
        val smoothed: Float = MicLevel.smooth(previous = 0f, target = 1f)

        assertThat(smoothed).isGreaterThan(0f)
        assertThat(smoothed).isLessThan(1f)
    }

    @Test
    fun `smoothing repeatedly converges on the target`() {
        var level = 0f
        repeat(50) { level = MicLevel.smooth(previous = level, target = 1f) }

        assertThat(level).isWithin(0.01f).of(1f)
    }

    @Test
    fun `smoothing a steady signal keeps it steady`() {
        assertThat(MicLevel.smooth(previous = 0.5f, target = 0.5f)).isEqualTo(0.5f)
    }
}
