package com.emm.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("/hello")
    suspend fun createBackup(@Body request: SyncRequest): SyncStatusResponse

    @GET("/hello")
    suspend fun fetchSync(@Query("deviceId") deviceId: String): FetchSyncResponse
}
