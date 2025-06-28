package com.emm.domain.backup

class BackupExecutor(private val backupRepository: BackupRepository) {

    suspend fun execute(): Result<Unit> {
        return backupRepository.execute()
    }
}