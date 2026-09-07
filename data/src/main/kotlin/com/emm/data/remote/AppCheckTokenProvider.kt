package com.emm.data.remote

interface AppCheckTokenProvider {

    suspend fun token(): String
}
