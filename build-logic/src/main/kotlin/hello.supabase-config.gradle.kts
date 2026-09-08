import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuildConfigField
import java.io.FileInputStream
import java.io.Serializable
import java.util.Properties

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

configure<ApplicationAndroidComponentsExtension> {
    onVariants(selector().withBuildType("debug")) { variant ->
        val buildConfigFields: MapProperty<String, BuildConfigField<out Serializable>> =
            checkNotNull(variant.buildConfigFields)
        buildConfigFields.put(
            "SUPABASE_URL",
            BuildConfigField("String", "\"$debugSupabaseUrl\"", null),
        )
        buildConfigFields.put(
            "SUPABASE_PUBLISHABLE_KEY",
            BuildConfigField("String", "\"$debugSupabasePublishableKey\"", null),
        )
    }

    onVariants(selector().withBuildType("release")) { variant ->
        val buildConfigFields: MapProperty<String, BuildConfigField<out Serializable>> =
            checkNotNull(variant.buildConfigFields)
        val variantName: String = variant.name
        val url: String = localSupabaseUrl
        val publishableKey: String = localSupabasePublishableKey
        buildConfigFields.put(
            "SUPABASE_URL",
            provider {
                require(url.isNotBlank()) {
                    "The $variantName build needs supabase.url in local.properties " +
                        "(CI provisions it from the SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY secrets)."
                }
                BuildConfigField("String", "\"$url\"", null)
            },
        )
        buildConfigFields.put(
            "SUPABASE_PUBLISHABLE_KEY",
            provider {
                require(publishableKey.isNotBlank()) {
                    "The $variantName build needs supabase.publishableKey in local.properties " +
                        "(CI provisions it from the SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY secrets)."
                }
                BuildConfigField("String", "\"$publishableKey\"", null)
            },
        )
    }
}
