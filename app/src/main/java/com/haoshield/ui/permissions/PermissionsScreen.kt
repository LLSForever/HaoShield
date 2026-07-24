package com.haoshield.ui.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.ui.theme.HaoTheme

@Composable
fun PermissionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PermissionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HaoTheme.spacing.screenH),
    ) {
        TextButton(
            onClick = onNavigateBack,
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
        ) {
            Text(text = "Back", style = HaoTheme.type.caption, color = HaoTheme.colors.inkSoft)
        }

        Text(
            text = "Permissions",
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
        )

        Text(
            text = "Hǎo Shield uses the accessibility service only to notice which app is in the " +
                "foreground, so it can rest a blocked app behind a calm screen while a session is " +
                "open. It reads no content, records nothing, and sends nothing off your device.",
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
            modifier = Modifier.padding(top = HaoTheme.spacing.md),
        )

        Spacer(modifier = Modifier.padding(top = HaoTheme.spacing.lg))

        PermissionRow(
            title = "Accessibility service",
            granted = uiState.isAccessibilityEnabled,
            grantedLabel = "Enabled",
            actionLabel = "Open Accessibility settings",
            onAction = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
        )

        PermissionRow(
            title = "Display over other apps",
            granted = uiState.canDrawOverlay,
            grantedLabel = "Allowed",
            actionLabel = "Open display settings",
            onAction = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            },
        )

        PermissionRow(
            title = "NFC (for Shield Mode taps)",
            granted = uiState.isNfcAvailable && uiState.isNfcEnabled,
            grantedLabel = "Ready",
            actionLabel = if (!uiState.isNfcAvailable) {
                "This device has no NFC — use a printed code"
            } else {
                "Turn on NFC in settings"
            },
            actionEnabled = uiState.isNfcAvailable,
            onAction = {
                context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            },
        )

        Spacer(modifier = Modifier.padding(top = HaoTheme.spacing.lg))

        Text(
            text = "On Android 13 and later, if the accessibility toggle looks greyed out, open " +
                "this screen's App info, tap the ⋮ menu, and choose \"Allow restricted settings\" " +
                "first.",
            style = HaoTheme.type.caption,
            color = HaoTheme.colors.inkFaint,
            modifier = Modifier.padding(bottom = HaoTheme.spacing.xxl),
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    grantedLabel: String,
    actionLabel: String,
    onAction: () -> Unit,
    actionEnabled: Boolean = true,
) {
    val stoneColor = HaoTheme.colors.stone

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = HaoTheme.spacing.sm)) {
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (granted) {
                Text(
                    text = "✓",
                    style = HaoTheme.type.heading,
                    color = HaoTheme.colors.ink,
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .size(10.dp)
                        .drawBehind {
                            drawCircle(
                                color = stoneColor,
                                radius = size.minDimension / 2f,
                                style = Stroke(1.5.dp.toPx()),
                            )
                        },
                )
            }
        }

        Spacer(modifier = Modifier.width(HaoTheme.spacing.md))

        Column {
            Text(
                text = title,
                style = HaoTheme.type.heading,
                color = HaoTheme.colors.ink,
            )
            if (granted) {
                Text(
                    text = grantedLabel,
                    modifier = Modifier.padding(top = HaoTheme.spacing.xs),
                    style = HaoTheme.type.label,
                    color = HaoTheme.colors.ink,
                )
            } else if (actionEnabled) {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.padding(top = HaoTheme.spacing.xs),
                ) {
                    Text(
                        text = actionLabel,
                        style = HaoTheme.type.label,
                        color = HaoTheme.colors.ink,
                    )
                }
            } else {
                Text(
                    text = actionLabel,
                    modifier = Modifier.padding(top = HaoTheme.spacing.xs),
                    style = HaoTheme.type.caption,
                    color = HaoTheme.colors.inkSoft,
                )
            }
        }
    }
}
