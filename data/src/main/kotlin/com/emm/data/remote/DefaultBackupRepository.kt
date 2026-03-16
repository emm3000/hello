package com.emm.data.remote

import com.emm.domain.backup.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultBackupRepository(
    private val dataStore: DataStore,
) : BackupRepository {

    override suspend fun execute(force: Boolean) = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            dataStore.markDate()
            dataStore.saveSuccess(
                if (force) {
                    "Local-first mode: backup force requested but legacy backup service is disabled"
                } else {
                    "Local-first mode: backup disabled; sync is handled by Supabase RPC pipeline"
                }
            )
            Unit
        }
    }
}
