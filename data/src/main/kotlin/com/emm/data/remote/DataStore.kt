package com.emm.data.remote

import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.time.LocalDateTime

class DataStore(
    private val sharedPreferences: SharedPreferences,
    private val json: Json,
) {

    private val editor: SharedPreferences.Editor by lazy { sharedPreferences.edit() }

    val lastUpdatedDate: Flow<String> = sharedPreferences.observeString(
        key = DATE_KEY,
        defaultValue = LocalDateTime.now().toString(),
    ).distinctUntilChanged()

    var defaultDeck
        get() = sharedPreferences.getString("DEFAULT_DECK", "").orEmpty()
        set(value) {
            editor.putString("DEFAULT_DECK", value).apply()
        }

    var firstInitializer: Boolean
        get() = sharedPreferences.getBoolean("FIRST_INITIALIZER", false)
        set(value) {
            editor.putBoolean("FIRST_INITIALIZER", value).apply()
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
        } catch (e: SerializationException) {
            Log.w(TAG, "Could not decode error response", e)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Unexpected error response format", e)
        }
    }

    fun saveSuccess(value: String) {
        editor.putString(SUCCESS_KEY, value).apply()
    }

    companion object {
        private const val TAG = "DataStore"

        const val ERROR_KEY = "ERROR_KEY"
        const val SUCCESS_KEY = "SUCCESS_KEY"
        const val DATE_KEY = "DATE_KEY"
    }
}

fun SharedPreferences.observeString(key: String, defaultValue: String): Flow<String> = callbackFlow {
    trySend(getString(key, defaultValue) ?: defaultValue)

    val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
        if (changedKey == key) {
            trySend(sharedPreferences.getString(key, defaultValue) ?: defaultValue)
        }
    }

    registerOnSharedPreferenceChangeListener(listener)

    awaitClose {
        unregisterOnSharedPreferenceChangeListener(listener)
    }
}
