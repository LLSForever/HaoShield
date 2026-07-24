package com.haoshield.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.haoshield.ui.theme.HaoTheme

/** The app's one text-input treatment: card shape, ink cursor, stone resting border. */
@Composable
fun HaoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    minLines: Int = 1,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        placeholder = placeholder?.let {
            {
                Text(
                    text = it,
                    style = HaoTheme.type.body,
                    color = HaoTheme.colors.inkFaint,
                )
            }
        },
        textStyle = HaoTheme.type.body,
        shape = HaoTheme.shapes.card,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HaoTheme.colors.ink,
            unfocusedBorderColor = HaoTheme.colors.stone,
            cursorColor = HaoTheme.colors.ink,
            focusedTextColor = HaoTheme.colors.ink,
            unfocusedTextColor = HaoTheme.colors.ink,
        ),
        minLines = minLines,
    )
}
