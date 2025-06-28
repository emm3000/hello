package com.emm.domain.backup

interface BackupRepository {

    suspend fun execute(): Result<Unit>
}