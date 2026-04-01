package com.emm.data.sync

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.data.HelloDb
import com.emm.domain.sync.PendingOperationsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class DefaultPendingOperationsRepository(
    private val db: HelloDb,
) : PendingOperationsRepository {

    private val localFirstQueries = db.localFirstQueries

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeHasPendingOperations(): Flow<Boolean> {
        return localFirstQueries
            .selectLocalAccountState()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .flatMapLatest { accountState ->
                val appAccountId = accountState?.appAccountId?.takeIf(String::isNotBlank)
                    ?: return@flatMapLatest flowOf(false)
                localFirstQueries
                    .countRetryableOperations(appAccountId, maxRetries = DrainOutbox.MAX_RETRY_COUNT)
                    .asFlow()
                    .mapToOne(Dispatchers.IO)
                    .map { count -> count > 0 }
            }
            .distinctUntilChanged()
    }
}
