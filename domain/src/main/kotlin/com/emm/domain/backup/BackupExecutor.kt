package com.emm.domain.backup

class BackupExecutor(private val backupRepository: BackupRepository) {

    suspend fun execute(force: Boolean): Result<Unit> {
        return backupRepository.execute(force)
    }
}
