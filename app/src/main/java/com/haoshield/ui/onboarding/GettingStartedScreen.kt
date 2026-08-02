package com.haoshield.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.haoshield.ui.components.HaoPrimaryButton
import com.haoshield.ui.components.HaoSecondaryButton
import com.haoshield.ui.theme.HaoTheme

/**
 * The bridge from the philosophy to the practice. Shown once after the intro so a new person is
 * pointed at the physical Shield ritual — the heart of the app — rather than dropped at Home.
 */
@Composable
fun GettingStartedScreen(
    onMakeShield: () -> Unit,
    onBeginLightly: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = HaoTheme.spacing.screenH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "好", style = HaoTheme.type.glyphSmall, color = HaoTheme.colors.ink)

        Text(
            text = "Two ways to begin",
            modifier = Modifier.padding(top = HaoTheme.spacing.lg),
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "The heart of Hǎo Shield is a small object you make yourself and keep close — " +
                "tap it to enter protected time. You can make one now, or begin lightly and make " +
                "your Shield whenever you're ready.",
            modifier = Modifier.padding(top = HaoTheme.spacing.md),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(HaoTheme.spacing.xl))

        HaoPrimaryButton(
            text = "Make your Hǎo Shield",
            onClick = onMakeShield,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(HaoTheme.spacing.md))
        HaoSecondaryButton(
            text = "Begin lightly for now",
            onClick = onBeginLightly,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Hǎo Shield will ask for a couple of permissions the first time you begin.",
            modifier = Modifier.padding(top = HaoTheme.spacing.lg),
            style = HaoTheme.type.caption,
            color = HaoTheme.colors.inkFaint,
            textAlign = TextAlign.Center,
        )
    }
}
