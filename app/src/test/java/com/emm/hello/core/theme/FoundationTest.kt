package com.emm.hello.core.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FoundationTest {

    @Test
    fun `semantic colors map named roles`() {
        val colors = helloSemanticColors()

        assertThat(colors.success).isEqualTo(successSemanticColor)
        assertThat(colors.warning).isEqualTo(warningSemanticColor)
        assertThat(colors.destructive).isEqualTo(destructiveSemanticColor)
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
