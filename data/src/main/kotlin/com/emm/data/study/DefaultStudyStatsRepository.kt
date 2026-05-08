package com.emm.data.study

import com.emm.data.HelloDb
import com.emm.domain.study.StudyStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class DefaultStudyStatsRepository(
    private val db: HelloDb,
) : StudyStatsRepository {

    private val localFirstQueries = db.localFirstQueries

    override suspend fun countDistinctCardsStudiedToday(): Int = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val startOfDay = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val startOfNextDay = startOfDay.plus(1, ChronoUnit.DAYS)

        localFirstQueries.countDistinctCardsStudiedInRange(
            startMillis = startOfDay.toEpochMilli(),
            endMillis = startOfNextDay.toEpochMilli(),
        ).executeAsOne().toInt()
    }

    override suspend fun countCardsDueToday(): Int = withContext(Dispatchers.IO) {
        val nowMillis = Instant.now().toEpochMilli()
        localFirstQueries.countCardsDueBy(nowMillis = nowMillis)
            .executeAsOne()
            .toInt()
    }

    override suspend fun countCardsDueThisWeek(): Int = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val endOfWeek = now.plus(7, ChronoUnit.DAYS)
        localFirstQueries.countCardsDueInRange(
            nowMillis = now.toEpochMilli(),
            endMillis = endOfWeek.toEpochMilli(),
        ).executeAsOne().toInt()
    }

    override suspend fun findDistinctReviewDatesDescending(): List<Long> = withContext(Dispatchers.IO) {
        localFirstQueries.distinctReviewDatesDescending()
            .executeAsList()
    }
}
