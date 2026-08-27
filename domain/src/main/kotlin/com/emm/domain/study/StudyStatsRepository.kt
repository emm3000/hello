package com.emm.domain.study

interface StudyStatsRepository {
    suspend fun countDistinctCardsStudiedToday(): Int
    suspend fun countCardsDueToday(): Int
    suspend fun countCardsDueThisWeek(): Int
    suspend fun countCardsDueInRange(startMillis: Long, endMillis: Long): Int
    suspend fun findNextReviewAtAfter(millis: Long): Long?
    suspend fun findDistinctReviewDatesDescending(): List<Long>
}
