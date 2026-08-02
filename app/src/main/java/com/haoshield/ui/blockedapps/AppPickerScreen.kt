package com.haoshield.ui.blockedapps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import com.haoshield.ui.components.HaoBackLink
import com.haoshield.ui.theme.HaoTheme

@Composable
fun AppPickerScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppPickerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.addedEvents.collect { onNavigateBack() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = HaoTheme.spacing.screenH),
    ) {
        HaoBackLink(onClick = onNavigateBack, label = "Cancel")

        Text(
            text = "Add an app",
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
            modifier = Modifier.padding(top = HaoTheme.spacing.sm, bottom = HaoTheme.spacing.md),
        )

        when {
            uiState.isLoading -> Text(
                text = "Looking through your apps…",
                style = HaoTheme.type.body,
                color = HaoTheme.colors.inkSoft,
            )
            uiState.apps.isEmpty() -> Text(
                text = "Nothing left to add.",
                style = HaoTheme.type.body,
                color = HaoTheme.colors.inkSoft,
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.apps, key = { it.packageName }) { app ->
                    Text(
                        text = app.label,
                        style = HaoTheme.type.body,
                        color = HaoTheme.colors.ink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onAdd(app.packageName) }
                            .padding(vertical = HaoTheme.spacing.md),
                    )
                }
            }
        }
    }
}
