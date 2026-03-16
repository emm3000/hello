package com.emm.domain.sync

import kotlinx.coroutines.flow.Flow

interface PendingOperationsRepository {
    fun observeHasPendingOperations(): Flow<Boolean>
}
