import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuildConfigField
import java.io.FileInputStream
import java.io.Serializable
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

val localPropertiesFile: File = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val localSupabaseUrl: String = localProperties.getProperty("supabase.url").orEmpty()
val localSupabasePublishableKey: String = localProperties.getProperty("supabase.publishableKey").orEmpty()

val debugSupabaseUrl: String = localSupabaseUrl.ifEmpty { "http://127.0.0.1:54321" }
val debugSupabasePublishableKey: String = localSupabasePublishableKey
    .ifEmpty { "sb_publishable_ACJWlzQHlZjBrEguHvfOxg_3BJgxAaH" }

fun quoted(value: String): String = "\"$value\""

fun requireSupabaseProperty(value: String, propertyName: String, variantName: String): String {
    require(value.isNotBlank()) {
        "The $variantName build needs $propertyName in local.properties (CI provisions it from the SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY secrets)."
    }
    return quoted(value)
}

val releaseVersionCode: Int = providers.environmentVariable("VERSION_CODE").orNull?.toIntOrNull() ?: 1

configure<ApplicationExtension> {
    namespace = "com.emm.hello"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.emm.hello"
        minSdk = 26
        targetSdk = 37
        versionCode = releaseVersionCode
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            buildConfigField("Boolean", "USE_CANNED_AI", "false")
            buildConfigField("String", "SUPABASE_URL", quoted(localSupabaseUrl))
            buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", quoted(localSupabasePublishableKey))
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            buildConfigField("Boolean", "USE_CANNED_AI", "true")
            buildConfigField("String", "SUPABASE_URL", quoted(debugSupabaseUrl))
            buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", quoted(debugSupabasePublishableKey))
            manifestPlaceholders["usesCleartextTraffic"] = "true"
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

    lint {
        checkDependencies = true
    }
}

configure<ApplicationAndroidComponentsExtension> {
    onVariants(selector().withBuildType("release")) { variant ->
        val buildConfigFields: MapProperty<String, BuildConfigField<out Serializable>> =
            checkNotNull(variant.buildConfigFields)
        buildConfigFields.put(
            "SUPABASE_URL",
            provider {
                BuildConfigField(
                    "String",
                    requireSupabaseProperty(localSupabaseUrl, "supabase.url", variant.name),
                    null,
                )
            },
        )
        buildConfigFields.put(
            "SUPABASE_PUBLISHABLE_KEY",
            provider {
                BuildConfigField(
                    "String",
                    requireSupabaseProperty(
                        localSupabasePublishableKey,
                        "supabase.publishableKey",
                        variant.name,
                    ),
                    null,
                )
            },
        )
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
    testImplementation(platform(libs.koin.bom))
    testImplementation(libs.koin.test)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coroutines.extensions)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth.kt)
    implementation(libs.supabase.functions.kt)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.kotlinx.coroutines.play.services)
}
