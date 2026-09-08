package com.emm.domain.study

import java.time.Instant

interface StudyStatsRepository {
    suspend fun countDistinctCardsStudiedToday(): Int
    suspend fun countReviewsDue(now: Instant): Int
    suspend fun countNewCards(): Int
    suspend fun countCardsFirstReviewedIn(start: Instant, endExclusive: Instant): Int
    suspend fun countCardsDueThisWeek(): Int
    suspend fun countCardsDueInRange(startMillis: Long, endMillis: Long): Int
    suspend fun findNextReviewAtAfter(millis: Long): Long?
    suspend fun findReviewTimestampsDescending(): List<Long>
}
