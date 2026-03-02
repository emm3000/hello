import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    id("com.google.devtools.ksp")
    kotlin("plugin.serialization") version libs.versions.kotlin
    id("app.cash.sqldelight") version libs.versions.androidDriver
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.emm.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "BASE_URL", "\"${keystoreProperties.getProperty("BASE_URL") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "xmm", keystoreProperties.getProperty("xmm") ?: "")
        }
        debug {
            resValue("string", "xmm", keystoreProperties.getProperty("xmm") ?: "")
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

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }
}

dependencies {

    implementation(project(":domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(kotlin("test"))

    api(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    implementation(libs.kotlinx.serialization.json)

    // Sqldelight
    api(libs.android.driver)
    implementation(libs.coroutines.extensions)

    debugImplementation(libs.library)
    releaseImplementation(libs.library.no.op)
    "stagingImplementation"(libs.library)

    // retrofit
    api(libs.retrofit)
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

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