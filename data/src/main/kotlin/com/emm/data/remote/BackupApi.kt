package com.emm.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface BackupApi {

    @POST("/hello")
    suspend fun backup(@Body request: HelloDto): HelloResponse
}