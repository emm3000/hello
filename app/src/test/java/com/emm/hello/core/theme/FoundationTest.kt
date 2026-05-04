package com.emm.hello.core.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FoundationTest {

    @Test
    fun `light semantic colors map named roles`() {
        val colors = lightSemanticColors()

        assertThat(colors.success).isEqualTo(LightSuccessSemanticColor)
        assertThat(colors.warning).isEqualTo(LightWarningSemanticColor)
        assertThat(colors.destructive).isEqualTo(LightDestructiveSemanticColor)
    }

    @Test
    fun `dark semantic colors map named roles`() {
        val colors = darkSemanticColors()

        assertThat(colors.success).isEqualTo(DarkSuccessSemanticColor)
        assertThat(colors.warning).isEqualTo(DarkWarningSemanticColor)
        assertThat(colors.destructive).isEqualTo(DarkDestructiveSemanticColor)
    }

    @Test
    fun `default spacing tokens stay ordered`() {
        assertThat(DefaultHelloSpacing.xs.value).isLessThan(DefaultHelloSpacing.sm.value)
        assertThat(DefaultHelloSpacing.sm.value).isLessThan(DefaultHelloSpacing.md.value)
        assertThat(DefaultHelloSpacing.md.value).isLessThan(DefaultHelloSpacing.lg.value)
        assertThat(DefaultHelloSpacing.lg.value).isLessThan(DefaultHelloSpacing.xl.value)
        assertThat(DefaultHelloSpacing.xl.value).isLessThan(DefaultHelloSpacing.xxl.value)
    }

    @Test
    fun `metadata role stays readable and distinct from labelSmall`() {
        assertThat(appTypography.metadata).isEqualTo(metadataTextStyle)
        assertThat(metadataTextStyle.fontSize).isEqualTo(appTypography.labelMedium.fontSize)
        assertThat(metadataTextStyle).isNotEqualTo(appTypography.labelSmall)
    }
}
