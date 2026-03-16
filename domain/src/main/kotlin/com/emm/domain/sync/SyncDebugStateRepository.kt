package com.emm.domain.sync

import kotlinx.coroutines.flow.Flow

interface SyncDebugStateRepository {
    fun observe(): Flow<SyncDebugState>
}
