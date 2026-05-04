package com.emm.domain.time

import java.time.Instant

fun interface Clock {
    fun now(): Instant
}

data object SystemClock : Clock {
    override fun now(): Instant = Instant.now()
}
