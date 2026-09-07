package com.emm.hello.core.audio

private const val SILENCE_DB = -2f
private const val LOUD_DB = 10f
private const val SMOOTHING_FACTOR = 0.3f

object MicLevel {

    fun fromRms(rmsdB: Float): Float {
        return ((rmsdB - SILENCE_DB) / (LOUD_DB - SILENCE_DB)).coerceIn(0f, 1f)
    }

    fun smooth(previous: Float, target: Float): Float {
        return previous + (target - previous) * SMOOTHING_FACTOR
    }
}
