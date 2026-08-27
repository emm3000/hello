package com.emm.hello.newfeatures.hoy

import com.emm.domain.study.DashboardStats
import com.emm.domain.study.NextDueBatch
import com.emm.hello.core.mvi.MviState

private const val SECONDS_PER_CARD = 15
private const val SECONDS_PER_MINUTE = 60

data class HoyUiState(
    val isLoading: Boolean = true,
    val stats: DashboardStats? = null,
) : MviState {

    val cardsDueToday: Int
        get() = stats?.cardsDueToday ?: 0

    val hasSessionReady: Boolean
        get() = cardsDueToday > 0

    val nextDue: NextDueBatch?
        get() = stats?.nextDue

    val estimatedSessionMinutes: Int
        get() {
            if (cardsDueToday <= 0) return 0
            val seconds: Int = cardsDueToday * SECONDS_PER_CARD
            return maxOf(1, (seconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE)
        }
}
