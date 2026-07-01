package com.emm.data.remote

import android.content.SharedPreferences

private const val KEY_DEFAULT_DECK = "DEFAULT_DECK"
private const val KEY_SEEN_ONBOARDING = "HAS_SEEN_ONBOARDING"
private const val KEY_SEEN_GRADE_HINT = "HAS_SEEN_GRADE_HINT"
private const val KEY_SEEDED_STARTER_DECK = "HAS_SEEDED_STARTER_DECK"

class DataStore(
    private val sharedPreferences: SharedPreferences,
) {

    var defaultDeck
        get() = sharedPreferences.getString(KEY_DEFAULT_DECK, "").orEmpty()
        set(value) {
            sharedPreferences.edit().putString(KEY_DEFAULT_DECK, value).apply()
        }

    fun clearDefaultDeck() {
        sharedPreferences.edit().remove(KEY_DEFAULT_DECK).apply()
    }

    var hasSeenOnboarding: Boolean
        get() = sharedPreferences.getBoolean(KEY_SEEN_ONBOARDING, false)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_SEEN_ONBOARDING, value).apply()
        }

    var hasSeenGradeHint: Boolean
        get() = sharedPreferences.getBoolean(KEY_SEEN_GRADE_HINT, false)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_SEEN_GRADE_HINT, value).apply()
        }

    var hasSeededStarterDeck: Boolean
        get() = sharedPreferences.getBoolean(KEY_SEEDED_STARTER_DECK, false)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_SEEDED_STARTER_DECK, value).apply()
        }
}
