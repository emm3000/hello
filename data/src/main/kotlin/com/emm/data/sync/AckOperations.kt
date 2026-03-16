package com.emm.data.sync

class AckOperations(
    private val remote: SupabaseSyncRemoteDataSource,
) {

    suspend operator fun invoke(opIds: List<String>): Int {
        if (opIds.isEmpty()) return 0
        return remote.ack(opIds)
    }
}
