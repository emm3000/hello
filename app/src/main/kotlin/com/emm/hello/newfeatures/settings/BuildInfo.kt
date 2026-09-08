package com.emm.hello.newfeatures.settings

data class BuildInfo(
    val versionName: String,
    val versionCode: Int,
    val commit: String,
) {
    val label: String get() = "$versionName ($versionCode) · $commit"
}
