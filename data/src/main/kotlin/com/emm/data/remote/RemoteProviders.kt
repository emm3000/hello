package com.emm.data.remote

import android.content.Context
import android.content.SharedPreferences

fun provideSharedPreferences(applicationContext: Context): SharedPreferences {
    return applicationContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
}

private const val SHARED_PREFERENCES_NAME = "com.emm.data.preferences"
