package com.haoshield.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.haoshield.ui.theme.HaoTheme

/**
 * The app's one text-input treatment: card shape, ink cursor, stone resting border.
 *
 * Every field in the app takes prose — an intention, a reflection, a note to pass a boundary — so
 * sentence capitalisation is the default rather than something each call site remembers.
 */
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
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
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
