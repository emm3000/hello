package com.emm.domain.backup

class RunBackupUseCase(private val backupRepository: BackupRepository) {

    suspend operator fun invoke(force: Boolean): Result<Unit> {
        return backupRepository.execute(force)
    }
}
