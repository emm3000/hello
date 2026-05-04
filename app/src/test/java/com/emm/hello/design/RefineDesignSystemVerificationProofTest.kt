package com.emm.hello.design

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

class RefineDesignSystemVerificationProofTest {

    private val repoRoot: Path = findRepoRoot()

    @Test
    fun `proven pattern is promoted`() {
        assertThat(Files.exists(resolve("app/src/main/kotlin/com/emm/hello/core/ui/SectionBlock.kt"))).isTrue()
        assertThat(Files.exists(resolve("app/src/main/kotlin/com/emm/hello/core/ui/StatCard.kt"))).isTrue()

        val sectionBlockConsumers = listOf(
            "app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardScreen.kt",
            "app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardInputStepScreen.kt",
            "app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailScreen.kt",
        ).filter { read(it).contains("SectionBlock(") }

        assertThat(sectionBlockConsumers).hasSize(3)
        assertThat(read("app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailScreen.kt"))
            .contains("StatCard(")
    }

    @Test
    fun `weak reuse stays local`() {
        val categoryChipUsages = kotlinFilesUnder("app/src/main/kotlin")
            .filter { file -> file.toFile().readText().contains("CategoryChip(") }
            .map(::relativePath)

        assertThat(categoryChipUsages).containsExactly(
            "app/src/main/kotlin/com/emm/hello/newfeatures/card/BottomSheetDialogForPickCategory.kt"
        )
        assertThat(read("app/src/main/kotlin/com/emm/hello/newfeatures/card/BottomSheetDialogForPickCategory.kt"))
            .contains("Intencionalmente local")
        assertThat(Files.exists(resolve("app/src/main/kotlin/com/emm/hello/core/ui/CategoryChip.kt"))).isFalse()
    }

    @Test
    fun `initial rollout boundary holds`() {
        assertThat(read("app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardScreen.kt"))
            .contains("SectionBlock(")
        assertThat(read("app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardInputStepScreen.kt"))
            .contains("SectionBlock(")

        val deckDetailSource = read("app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailScreen.kt")
        assertThat(deckDetailSource).contains("SectionBlock(")
        assertThat(deckDetailSource).contains("StatCard(")

        val studySource = read("app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyScreen.kt")
        assertThat(studySource).doesNotContain("SectionBlock(")
        assertThat(studySource).doesNotContain("StatCard(")
    }

    @Test
    fun `out of scope work is rejected`() {
        val sectionBlockImports = filesContaining("import com.emm.hello.core.ui.SectionBlock")
        assertThat(sectionBlockImports).containsExactly(
            "app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardInputStepScreen.kt",
            "app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardScreen.kt",
            "app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailScreen.kt",
        )

        val statCardImports = filesContaining("import com.emm.hello.core.ui.StatCard")
        assertThat(statCardImports).containsExactly(
            "app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailScreen.kt"
        )

        val promotedPatternUsageOutsideApp = kotlinFilesUnder("data/src")
            .plus(kotlinFilesUnder("domain/src"))
            .map(::relativePath)
            .filter { path ->
                val source = read(path)
                source.contains("SectionBlock") ||
                    source.contains("StatCard") ||
                    source.contains("helloShapes") ||
                    source.contains("semanticColors")
            }

        assertThat(promotedPatternUsageOutsideApp).isEmpty()
    }

    private fun filesContaining(snippet: String): List<String> {
        return kotlinFilesUnder("app/src/main/kotlin")
            .filter { file -> file.toFile().readText().contains(snippet) }
            .map(::relativePath)
    }

    private fun kotlinFilesUnder(relativeDir: String): List<Path> {
        val directory = resolve(relativeDir)
        if (!Files.exists(directory)) return emptyList()

        Files.walk(directory).use { stream ->
            return stream
                .filter { file -> Files.isRegularFile(file) && file.toString().endsWith(".kt") }
                .sorted()
                .collect(Collectors.toList())
        }
    }

    private fun read(relativePath: String): String = resolve(relativePath).toFile().readText()

    private fun resolve(relativePath: String): Path = repoRoot.resolve(relativePath)

    private fun relativePath(path: Path): String = repoRoot.relativize(path).toString()

    private fun findRepoRoot(): Path {
        var current: Path? = Paths.get("").toAbsolutePath()
        while (current != null && !Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.parent
        }
        check(current != null) { "Could not find repo root from ${Paths.get("").toAbsolutePath()}" }
        return current
    }
}
