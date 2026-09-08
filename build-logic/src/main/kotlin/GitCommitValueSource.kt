import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations

abstract class GitCommitValueSource : ValueSource<String, ValueSourceParameters.None> {

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val shortSha: String = git("rev-parse", "--short", "HEAD")
        if (shortSha.isBlank()) return "unknown"
        val porcelain: String = git("status", "--porcelain")
        return if (porcelain.isBlank()) shortSha else "$shortSha-dirty"
    }

    private fun git(vararg arguments: String): String {
        val standard = ByteArrayOutputStream()
        val exitValue: Int = execOperations.exec {
            commandLine("git", *arguments)
            standardOutput = standard
            errorOutput = ByteArrayOutputStream()
            isIgnoreExitValue = true
        }.exitValue
        return if (exitValue == 0) standard.toString().trim() else ""
    }
}
