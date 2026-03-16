package com.emm.data.remote

import android.content.SharedPreferences

class DataStore(
    private val sharedPreferences: SharedPreferences,
) {

    private val editor: SharedPreferences.Editor by lazy { sharedPreferences.edit() }

    var defaultDeck
        get() = sharedPreferences.getString("DEFAULT_DECK", "").orEmpty()
        set(value) {
            editor.putString("DEFAULT_DECK", value).apply()
        }
}
