package com.emm.domain.study

import java.time.Instant

data class NextDueBatch(
    val at: Instant,
    val cardCount: Int,
    val daysFromToday: Int,
) {
    init {
        require(cardCount > 0) { "A next due batch must hold at least one card, was $cardCount" }
        require(daysFromToday >= 0) { "A next due batch is never in the past, was $daysFromToday" }
    }
}
