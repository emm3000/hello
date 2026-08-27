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
        // Post Ember redesign: the promoted components still live in core/ui. HSectionBlock has no
        // consumers anymore (the redesigned screens use bespoke layouts), but HStatCard is still
        // consumed by the Hoy stats row.
        assertThat(Files.exists(resolve("app/src/main/kotlin/com/emm/hello/core/ui/SectionBlock.kt"))).isTrue()
        assertThat(Files.exists(resolve("app/src/main/kotlin/com/emm/hello/core/ui/StatCard.kt"))).isTrue()

        assertThat(read("app/src/main/kotlin/com/emm/hello/newfeatures/hoy/HoyStatsSection.kt"))
            .contains("HStatCard(")
    }

    @Test
    fun `weak reuse stays local`() {
        // Phase 3.4 (Ember restyle) removed the chip pattern entirely: the category bottom sheet now
        // uses serif rows + accent check instead. Assertion now guards that no chip resurrected in
        // core/ui, and that no usage remains anywhere in app/src/main/kotlin.
        val categoryChipUsages = kotlinFilesUnder("app/src/main/kotlin")
            .filter { file -> file.toFile().readText().contains("CategoryChip(") }
            .map(::relativePath)

        assertThat(categoryChipUsages).isEmpty()
        assertThat(Files.exists(resolve("app/src/main/kotlin/com/emm/hello/core/ui/CategoryChip.kt"))).isFalse()
    }

    @Test
    fun `initial rollout boundary holds`() {
        // Post Ember redesign: StudyScreen is still the explicit boundary — the promoted components
        // must not leak into it. NewCardInputStepScreen no longer consumes
        // HSectionBlock either (Phase 2 redesigned both with bespoke layouts).
        val studySource = read("app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyScreen.kt")
        assertThat(studySource).doesNotContain("HSectionBlock(")
        assertThat(studySource).doesNotContain("HStatCard(")
    }

    @Test
    fun `out of scope work is rejected`() {
        // Post Ember redesign: HSectionBlock has no consumers (kept in core/ui as a reusable shell
        // but unused). HStatCard is consumed only by Hoy.
        val sectionBlockImports = filesContaining("import com.emm.hello.core.ui.HSectionBlock")
        assertThat(sectionBlockImports).isEmpty()

        val statCardImports = filesContaining("import com.emm.hello.core.ui.HStatCard")
        assertThat(statCardImports).containsExactly(
            "app/src/main/kotlin/com/emm/hello/newfeatures/hoy/HoyStatsSection.kt",
        )

        val promotedPatternUsageOutsideApp = kotlinFilesUnder("data/src")
            .plus(kotlinFilesUnder("domain/src"))
            .map(::relativePath)
            .filter { path ->
                val source = read(path)
                source.contains("HSectionBlock") ||
                    source.contains("HStatCard") ||
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
