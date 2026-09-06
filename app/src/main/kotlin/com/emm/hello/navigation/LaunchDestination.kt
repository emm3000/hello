package com.emm.hello.navigation

enum class LaunchDestination(val extraValue: String) {
    StudyDue("study_due");

    companion object {
        const val EXTRA_NAME: String = "com.emm.hello.extra.LAUNCH_DESTINATION"

        fun fromExtraValue(value: String?): LaunchDestination? =
            entries.firstOrNull { destination: LaunchDestination -> destination.extraValue == value }
    }
}
