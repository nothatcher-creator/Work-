package com.nothatcher.sproutbook

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SproutBackground = Color(0xFF07130F)
val SproutSurface = Color(0xFF0F2119)
val SproutSurface2 = Color(0xFF153125)
val SproutPrimary = Color(0xFFD7B98E)
val SproutGreen = Color(0xFF77A981)
val SproutText = Color(0xFFF2F5F0)
val SproutMuted = Color(0xFFAEBAB1)
val SproutDanger = Color(0xFFE69A9A)

private val SproutColors = darkColorScheme(
    primary = SproutPrimary,
    onPrimary = Color(0xFF231B10),
    secondary = SproutGreen,
    onSecondary = Color(0xFF07130F),
    background = SproutBackground,
    onBackground = SproutText,
    surface = SproutSurface,
    onSurface = SproutText,
    surfaceVariant = SproutSurface2,
    onSurfaceVariant = SproutMuted,
    outline = Color(0xFF53705C),
    error = SproutDanger
)

@Composable
fun SproutBookTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SproutColors,
        content = content
    )
}
