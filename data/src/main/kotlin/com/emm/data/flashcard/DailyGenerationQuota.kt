package com.emm.data.flashcard

import android.content.SharedPreferences
import com.emm.domain.generation.GenerationQuota
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class DailyGenerationQuota(
    private val preferences: SharedPreferences,
    private val limit: Int = DEFAULT_LIMIT,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.system(zone),
) : GenerationQuota {

    override suspend fun tryConsume(): GenerationQuota.Outcome {
        val today = LocalDate.now(clock)
        val storedDate = preferences.getString(KEY_DATE, null)
        val countToday = if (storedDate == today.toString()) preferences.getInt(KEY_COUNT, 0) else 0

        if (countToday >= limit) {
            val resetAt = today.plusDays(1).atStartOfDay(zone).toInstant()
            return GenerationQuota.Outcome.Exceeded(limit = limit, resetAt = resetAt)
        }

        preferences.edit()
            .putString(KEY_DATE, today.toString())
            .putInt(KEY_COUNT, countToday + 1)
            .apply()

        return GenerationQuota.Outcome.Allowed
    }

    companion object {
        const val DEFAULT_LIMIT: Int = 50
        const val KEY_DATE: String = "gen_quota_date"
        const val KEY_COUNT: String = "gen_quota_count"
    }
}
