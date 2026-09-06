package com.emm.domain.connectivity

import kotlinx.coroutines.flow.Flow

interface ConnectivityRepository {
    fun observeOnline(): Flow<Boolean>
}
