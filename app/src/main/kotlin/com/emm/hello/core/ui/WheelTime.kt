package com.emm.hello.core.ui

import java.time.LocalTime

private const val MINUTE_STEP: Int = 5
private const val MINUTES_PER_HOUR: Int = 60
private const val MINUTES_PER_DAY: Int = 24 * MINUTES_PER_HOUR
private const val WHEEL_CYCLES: Int = 1000

internal class WheelRange(val rowCount: Int, val step: Int) {

    val virtualCount: Int = rowCount * WHEEL_CYCLES

    val maxValue: Int = (rowCount - 1) * step

    fun valueAt(index: Int): Int = index.mod(rowCount) * step

    fun startIndexFor(value: Int): Int = virtualCount / 2 + rowFor(value)

    fun nearestIndexFor(value: Int, fromIndex: Int): Int {
        val forward: Int = (rowFor(value) - fromIndex).mod(rowCount)
        val delta: Int = if (forward > rowCount / 2) forward - rowCount else forward
        return fromIndex + delta
    }

    private fun rowFor(value: Int): Int = (value / step).mod(rowCount)
}

internal val hourWheelRange: WheelRange = WheelRange(rowCount = 24, step = 1)
internal val minuteWheelRange: WheelRange = WheelRange(rowCount = MINUTES_PER_HOUR / MINUTE_STEP, step = MINUTE_STEP)

internal fun LocalTime.roundedToMinuteStep(): LocalTime {
    val minuteOfDay: Int = hour * MINUTES_PER_HOUR + minute
    val rounded: Int = (minuteOfDay + MINUTE_STEP / 2) / MINUTE_STEP * MINUTE_STEP
    val wrapped: Int = rounded.mod(MINUTES_PER_DAY)
    return LocalTime.of(wrapped / MINUTES_PER_HOUR, wrapped % MINUTES_PER_HOUR)
}
