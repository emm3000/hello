import com.android.build.api.dsl.LibraryExtension
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    kotlin("plugin.serialization") version libs.versions.kotlin
    id("app.cash.sqldelight") version libs.versions.androidDriver
}

val localPropertiesFile: File = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

fun resolveProperty(key: String): String {
    return localProperties.getProperty(key) ?: keystoreProperties.getProperty(key) ?: ""
}

configure<LibraryExtension> {
    namespace = "com.emm.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SUPABASE_URL", "\"${resolveProperty("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${resolveProperty("SUPABASE_ANON_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "xmm", resolveProperty("xmm"))
        }
        debug {
            resValue("string", "xmm", resolveProperty("xmm"))
            matchingFallbacks += listOf("release")
        }

        create("staging") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.all {
            it.systemProperty("kotlinx.coroutines.debug", "off")
            it.jvmArgs("-Xmx1024m")
        }
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {

    implementation(project(":domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("app.cash.sqldelight:sqlite-driver:${libs.versions.androidDriver.get()}")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(kotlin("test"))

    implementation(libs.kotlinx.serialization.json)

    // Sqldelight
    api(libs.android.driver)
    implementation(libs.coroutines.extensions)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth.kt)
    implementation(libs.supabase.postgrest.kt)
    implementation(libs.supabase.functions.kt)
    implementation(libs.supabase.realtime.kt)
    implementation(libs.ktor.client.android)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(platform(libs.koin.bom))
    implementation(platform(libs.firebase.bom))
    api(libs.firebase.ai)
    implementation(libs.koin.android)
}

sqldelight {
    databases {
        create("HelloDb") {
            packageName.set("com.emm.data")
        }
    }
}
