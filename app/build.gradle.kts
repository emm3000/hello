import com.android.build.api.dsl.ApplicationExtension
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.google.crashlytics)
    kotlin("plugin.serialization") version libs.versions.kotlin
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// Temporary runtime gate for the stacked local-only rollout.
// Default stays false so regular builds keep remote startup enabled.
val localOnlyMode: Boolean = providers.gradleProperty("hello.localOnlyMode")
    .orNull
    ?.toBooleanStrictOrNull()
    ?: false

configure<ApplicationExtension> {
    namespace = "com.emm.hello"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.emm.hello"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("Boolean", "LOCAL_ONLY_MODE", localOnlyMode.toString())
    }

    signingConfigs {
        create("config") {
            keyAlias = keystoreProperties.getProperty("keyAlias") ?: ""
            keyPassword = keystoreProperties.getProperty("keyPassword") ?: ""
            storeFile = file(keystoreProperties.getProperty("storeFile") ?: "keystore.p12")
            storePassword = keystoreProperties.getProperty("storePassword") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs["config"]
            buildConfigField("Boolean", "SHOW_SYNC_DEBUG_PANEL", "false")
            buildConfigField("Boolean", "USE_CANNED_AI", "false")
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            signingConfig = signingConfigs["config"]
            buildConfigField("Boolean", "SHOW_SYNC_DEBUG_PANEL", "true")
            buildConfigField("Boolean", "USE_CANNED_AI", "true")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            matchingFallbacks += listOf("release")
        }

        create("staging") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs["config"]
            buildConfigField("Boolean", "SHOW_SYNC_DEBUG_PANEL", "true")
            buildConfigField("Boolean", "USE_CANNED_AI", "false")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.FlowPreview",
        )
    }
}

dependencies {

    implementation(project(":data"))
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui.text.google.fonts)
    // Brings Theme.Material3.* XML parent themes for the app launcher/system chrome.
    // Compose owns runtime styling; this is only needed for splash + status bar defaults.
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    androidTestImplementation(platform(libs.koin.bom))
    androidTestImplementation(libs.koin.test)
    androidTestImplementation(libs.koin.test.junit4)
    androidTestImplementation(libs.mockk.android)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coroutines.extensions)
}
