package com.emm.domain.study

interface StudyStatsRepository {
    suspend fun countDistinctCardsStudiedToday(): Int
    suspend fun countCardsDueToday(): Int
    suspend fun countCardsDueThisWeek(): Int
    suspend fun findDistinctReviewDatesDescending(): List<Long>
}
