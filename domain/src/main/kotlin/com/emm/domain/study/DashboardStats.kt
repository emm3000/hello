package com.emm.domain.study

data class DashboardStats(
    val cardsStudiedToday: Int,
    val cardsDueToday: Int,
    val currentStreak: Int,
    val cardsDueThisWeek: Int,
)
