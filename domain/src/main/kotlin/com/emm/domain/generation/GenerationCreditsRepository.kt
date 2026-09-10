package com.emm.domain.generation

import kotlinx.coroutines.flow.Flow

interface GenerationCreditsRepository {

    fun observe(): Flow<GenerationCredits?>

    suspend fun record(credits: GenerationCredits)
}
