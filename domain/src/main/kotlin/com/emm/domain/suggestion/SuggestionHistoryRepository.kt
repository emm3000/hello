package com.emm.domain.suggestion

import java.time.Instant

interface SuggestionHistoryRepository {

    suspend fun fetch(): SuggestionHistory

    suspend fun deleteShownBefore(cutoff: Instant)
}
