package com.emm.hello.features

import android.content.Context
import android.content.SharedPreferences

class DataStore(context: Context) {

    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences("Random", Context.MODE_PRIVATE)
    }

    private val editor: SharedPreferences.Editor
        get() = preferences.edit()

    val isFirstLaunch: Boolean
        get() = preferences.getBoolean(FIRST_TIME, true)

    var uri: String?
        get() {
            val value: String? = preferences.getString(URI_FOR_FILE, "")
            if (value.isNullOrBlank()) throw IllegalStateException()
            return value
        }
        set(value) {
            editor.putString(URI_FOR_FILE, value).apply()
        }

    fun setFirstLaunchCompleted() {
        editor.putBoolean(FIRST_TIME, false).apply()
    }

    companion object {

        private const val FIRST_TIME = "FIRST_TIME"
        private const val URI_FOR_FILE = "URI_FOR_FILE"
    }
}