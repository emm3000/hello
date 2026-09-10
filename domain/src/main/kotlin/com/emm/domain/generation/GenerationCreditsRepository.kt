package com.emm.domain.generation

import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface GenerationCreditsRepository {

    fun observe(): Flow<GenerationCredits?>

    fun record(remaining: Int, observedAt: Instant)
}
