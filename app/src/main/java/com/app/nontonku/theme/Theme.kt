package com.app.nontonku.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Identitas NontonKu: oranye hangat di atas putih.
val Brand = Color(0xFFF97316)
val BrandDark = Color(0xFFEA580C)
val Ink = Color(0xFF16181D)
val Paper = Color(0xFFFFFFFF)
val Mist = Color(0xFFF5F5F4)
val Stone = Color(0xFFE7E5E4)
val Slate = Color(0xFF5F6672)

private val LightColors = lightColorScheme(
    primary = Brand, onPrimary = Paper,
    secondary = BrandDark, onSecondary = Paper,
    background = Paper, onBackground = Ink,
    surface = Paper, onSurface = Ink,
    surfaceVariant = Mist, onSurfaceVariant = Slate,
    outline = Stone,
    error = Color(0xFFD93251),
)

@Composable
fun NontonKuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
