package com.translation.counter.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBg = Color(0xFF0F172A)
val CardBg = Color(0xFF1E293B)
val CardBorder = Color(0xFF334155)

val PrimaryCyan = Color(0xFF06B6D4)
val SecondaryTeal = Color(0xFF14B8A6)
val AccentCoral = Color(0xFFF43F5E)
val ActiveGreen = Color(0xFF10B981)

val TextWhite = Color(0xFFF8FAFC)
val TextSubtle = Color(0xFF94A3B8)
val TextKoreanYellow = Color(0xFFFDE047)

private val CustomDarkColorScheme: ColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    secondary = SecondaryTeal,
    background = DarkBg,
    surface = CardBg,
    onPrimary = Color.Black,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun CounterTranslationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CustomDarkColorScheme,
        content = content
    )
}
