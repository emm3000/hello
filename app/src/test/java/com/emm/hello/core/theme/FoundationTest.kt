package com.emm.hello.core.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FoundationTest {

    @Test
    fun `light semantic colors map named roles`() {
        val colors = lightSemanticColors()

        assertThat(colors.success).isEqualTo(lightSuccessSemanticColor)
        assertThat(colors.warning).isEqualTo(lightWarningSemanticColor)
        assertThat(colors.destructive).isEqualTo(lightDestructiveSemanticColor)
    }

    @Test
    fun `dark semantic colors map named roles`() {
        val colors = darkSemanticColors()

        assertThat(colors.success).isEqualTo(darkSuccessSemanticColor)
        assertThat(colors.warning).isEqualTo(darkWarningSemanticColor)
        assertThat(colors.destructive).isEqualTo(darkDestructiveSemanticColor)
    }

    @Test
    fun `default spacing tokens stay ordered`() {
        assertThat(defaultHelloSpacing.xs.value).isLessThan(defaultHelloSpacing.sm.value)
        assertThat(defaultHelloSpacing.sm.value).isLessThan(defaultHelloSpacing.md.value)
        assertThat(defaultHelloSpacing.md.value).isLessThan(defaultHelloSpacing.lg.value)
        assertThat(defaultHelloSpacing.lg.value).isLessThan(defaultHelloSpacing.xl.value)
        assertThat(defaultHelloSpacing.xl.value).isLessThan(defaultHelloSpacing.xxl.value)
    }

    @Test
    fun `metadata role stays readable and distinct from labelSmall`() {
        assertThat(appTypography.metadata).isEqualTo(metadataTextStyle)
        assertThat(metadataTextStyle.fontSize).isEqualTo(appTypography.labelMedium.fontSize)
        assertThat(metadataTextStyle).isNotEqualTo(appTypography.labelSmall)
    }
}
