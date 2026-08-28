package com.emm.hello.newfeatures.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.pageBackground
import com.emm.hello.core.theme.schibsted
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonVariant

@Composable
fun OnboardingScreen(
    onIntent: (OnboardingUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = pageBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = MaterialTheme.spacing.screenGutter),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.onboarding_headline),
                style = MaterialTheme.typography.displaySmall,
                color = ink,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = schibsted),
                color = inkMuted,
            )

            Spacer(modifier = Modifier.weight(1f))

            HButton(
                text = stringResource(R.string.onboarding_cta_start),
                onClick = { onIntent(OnboardingUiIntent.StartClicked) },
                variant = HButtonVariant.Primary,
                full = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@PreviewLightDark
@Composable
private fun OnboardingScreenPreview() {
    HelloTheme {
        OnboardingScreen(onIntent = {})
    }
}
