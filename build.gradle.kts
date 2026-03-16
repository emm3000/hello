// Top-level build file for common configuration options across all sub-projects and modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.google.crashlytics) apply false
    alias(libs.plugins.android.library) apply false
    id("com.google.devtools.ksp") version libs.versions.kspVersion apply false
    id("androidx.room") version libs.versions.roomRuntime apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.detekt)
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        val moduleConfig = file("$rootDir/config/detekt/${project.name}.yml")
        config.setFrom(
            files(
                "$rootDir/config/detekt/detekt.yml",
                moduleConfig.takeIf { it.exists() }
            )
        )
        baseline = file("$projectDir/detekt-baseline.xml")
        buildUponDefaultConfig = true
        parallel = true
        basePath = rootDir.absolutePath
    }


    dependencies {
        detektPlugins(rootProject.libs.detekt.formatting)
        detektPlugins(rootProject.libs.detekt.compose.rules)
    }
}

detekt {
    toolVersion = libs.versions.detekt.get()
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    parallel = true
    basePath = rootDir.absolutePath
}
