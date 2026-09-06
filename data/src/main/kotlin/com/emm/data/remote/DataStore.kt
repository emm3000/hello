package com.emm.data.remote

import android.content.SharedPreferences
import com.emm.domain.reminder.StudyReminderSettings

private const val KEY_DEFAULT_DECK = "DEFAULT_DECK"
private const val KEY_SEEN_ONBOARDING = "HAS_SEEN_ONBOARDING"
private const val KEY_SEEDED_STARTER_DECK = "HAS_SEEDED_STARTER_DECK"
private const val KEY_STUDY_REMINDER_ENABLED = "STUDY_REMINDER_ENABLED"
private const val KEY_STUDY_REMINDER_HOUR = "STUDY_REMINDER_HOUR"
private const val KEY_STUDY_REMINDER_MINUTE = "STUDY_REMINDER_MINUTE"

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

    var hasSeededStarterDeck: Boolean
        get() = sharedPreferences.getBoolean(KEY_SEEDED_STARTER_DECK, false)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_SEEDED_STARTER_DECK, value).apply()
        }

    var isStudyReminderEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_STUDY_REMINDER_ENABLED, true)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_STUDY_REMINDER_ENABLED, value).apply()
        }

    var studyReminderHour: Int
        get() = sharedPreferences.getInt(KEY_STUDY_REMINDER_HOUR, StudyReminderSettings.DEFAULT_TIME.hour)
        set(value) {
            sharedPreferences.edit().putInt(KEY_STUDY_REMINDER_HOUR, value).apply()
        }

    var studyReminderMinute: Int
        get() = sharedPreferences.getInt(KEY_STUDY_REMINDER_MINUTE, StudyReminderSettings.DEFAULT_TIME.minute)
        set(value) {
            sharedPreferences.edit().putInt(KEY_STUDY_REMINDER_MINUTE, value).apply()
        }
}
