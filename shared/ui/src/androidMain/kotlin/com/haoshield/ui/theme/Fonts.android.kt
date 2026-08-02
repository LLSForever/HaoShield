package com.haoshield.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.haoshield.ui.R

/**
 * The bundled families, loaded from this module's font resources.
 *
 * Variable fonts: the requested [FontWeight] maps to the font's wght axis on API 26+ (our minSdk),
 * so one file per family covers every weight the app asks for.
 */

private fun notoSerif(weight: FontWeight) = Font(resId = R.font.noto_serif_variable, weight = weight)

private fun inter(weight: FontWeight) = Font(resId = R.font.inter_variable, weight = weight)

actual val NotoSerif: FontFamily = FontFamily(
    notoSerif(FontWeight.Normal),
    notoSerif(FontWeight.Medium),
)

actual val NotoSerifHao: FontFamily = FontFamily(Font(resId = R.font.noto_serif_hao))

actual val Inter: FontFamily = FontFamily(
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
)
