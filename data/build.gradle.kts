import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.android.library)
    kotlin("plugin.serialization") version libs.versions.kotlin
    id("app.cash.sqldelight") version libs.versions.androidDriver
}

configure<LibraryExtension> {
    namespace = "com.emm.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
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

    implementation(platform(libs.firebase.bom))
    api(libs.firebase.ai)
}

sqldelight {
    databases {
        create("HelloDb") {
            packageName.set("com.emm.data")
        }
    }
}
