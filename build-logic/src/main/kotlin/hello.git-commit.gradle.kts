import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuildConfigField
import java.io.Serializable

val gitCommit: Provider<String> = providers.of(GitCommitValueSource::class) {}

configure<ApplicationAndroidComponentsExtension> {
    onVariants { variant ->
        val buildConfigFields: MapProperty<String, BuildConfigField<out Serializable>> =
            checkNotNull(variant.buildConfigFields)
        buildConfigFields.put(
            "GIT_COMMIT",
            gitCommit.map { commit: String -> BuildConfigField("String", "\"$commit\"", null) },
        )
    }
}
