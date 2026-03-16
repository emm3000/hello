package com.emm.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.emm.data.BuildConfig

fun provideSharedPreferences(applicationContext: Context): SharedPreferences {
    return applicationContext.getSharedPreferences(BuildConfig.LIBRARY_PACKAGE_NAME, Context.MODE_PRIVATE)
}
