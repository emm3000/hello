package com.emm.domain.backup

interface BackupRepository {

    suspend fun execute(force: Boolean): Result<Unit>
}