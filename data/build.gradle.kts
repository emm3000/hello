import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.android.library)
    kotlin("plugin.serialization") version libs.versions.kotlin
    id("app.cash.sqldelight") version libs.versions.androidDriver
}

configure<LibraryExtension> {
    namespace = "com.emm.data"
    compileSdk = 37

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
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.ktor.client.mock)
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
    implementation(libs.supabase.functions.kt)
    implementation(libs.ktor.client.core)
}

sqldelight {
    databases {
        create("HelloDb") {
            packageName.set("com.emm.data")
            schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}

val checkSqlDelightSnapshots: TaskProvider<Task> = tasks.register("checkSqlDelightSnapshots") {
    val migrationsDirectory: File = layout.projectDirectory.dir("src/main/sqldelight/com/emm/data").asFile
    val snapshotsDirectory: File = layout.projectDirectory.dir("src/main/sqldelight/databases").asFile
    inputs.dir(migrationsDirectory)
    inputs.dir(snapshotsDirectory)
    doLast {
        val migrationVersions: List<Int> = migrationsDirectory
            .listFiles { file -> file.extension == "sqm" }
            .orEmpty()
            .map { file -> file.nameWithoutExtension.toInt() }
            .sorted()
        val missingSnapshots: List<String> = migrationVersions
            .map { version -> "${version + 1}.db" }
            .filterNot { snapshot -> File(snapshotsDirectory, snapshot).exists() }
        check(missingSnapshots.isEmpty()) {
            "Missing SQLDelight schema snapshots ${missingSnapshots.joinToString()} in ${snapshotsDirectory.path}: " +
                "run ./gradlew :data:generateDebugHelloDbSchema at the commit that introduced each migration and commit the .db"
        }
    }
}

tasks.matching { task -> task.name == "verifySqlDelightMigration" }.configureEach {
    dependsOn(checkSqlDelightSnapshots)
}
