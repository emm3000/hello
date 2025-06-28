package com.emm.data.remote

import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.time.LocalDateTime

class DataStore(
    private val sharedPreferences: SharedPreferences,
    private val json: Json,
) {

    private val editor: SharedPreferences.Editor by lazy { sharedPreferences.edit() }

    var checksum: String
        get() = sharedPreferences.getString(LAST_CHECKSUM, null).orEmpty()
        set(value) {
            editor.putString(LAST_CHECKSUM, value).apply()
        }

    fun markDate() {
        val now = LocalDateTime.now()
        editor.putString(DATE_KEY, now.toString()).apply()
    }

    fun saveError(error: HttpException) {
        try {
            error.response()?.errorBody()?.string()?.let {
                val decodedErrorResponse: ExceptionResponse = json.decodeFromString<ExceptionResponse>(it)
                val encodedErrorResponse: String = json.encodeToString(decodedErrorResponse)
                editor.putString(ERROR_KEY, encodedErrorResponse).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveSuccess(value: String) {
        editor.putString(SUCCESS_KEY, value).apply()
    }

    companion object {

        const val ERROR_KEY = "ERROR_KEY"
        const val SUCCESS_KEY = "SUCCESS_KEY"
        const val DATE_KEY = "DATE_KEY"
        const val LAST_CHECKSUM = "LAST_CHECKSUM"
    }
}