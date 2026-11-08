package com.jacob.voicecalories.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MacroProteinColor = Color(0xFF38BDF8)  // Electric Sky Cyan
val MacroCarbsColor = Color(0xFFFBBF24)    // Radiant Gold / Amber
val MacroFatColor = Color(0xFFFB7185)      // Neon Rose / Coral
val MacroSodiumColor = Color(0xFFA78BFA)   // Electric Lavender / Violet
val MacroFiberColor = Color(0xFF34D399)    // Mint Emerald
val MacroSugarColor = Color(0xFFF472B6)    // Hot Pink
val CalorieAccentColor = Color(0xFF00E676) // Electric Neon Mint

private val SleekDarkColors = darkColorScheme(
    primary = Color(0xFF00E676),          // Electric Neon Mint
    onPrimary = Color(0xFF002910),        // Deep Obsidian Green
    primaryContainer = Color(0xFF132A1C), // Deep Emerald card tint
    onPrimaryContainer = Color(0xFF6EE7B7),
    secondary = Color(0xFF38BDF8),        // Electric Sky Cyan
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0C2434),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = Color(0xFFFBBF24),         // Warm Amber
    onTertiary = Color(0xFF451A03),
    background = Color(0xFF0A0D10),       // Deep Midnight Carbon
    onBackground = Color(0xFFF1F5F9),     // Crisp Light Slate
    surface = Color(0xFF10151B),          // Elevated Slate Obsidian
    onSurface = Color(0xFFF8FAFC),        // Pure Bright White
    surfaceVariant = Color(0xFF1A222C),   // Subtle Dark Card
    onSurfaceVariant = Color(0xFF94A3B8), // Muted Cool Slate
    outline = Color(0xFF283545),          // Sleek card border
    error = Color(0xFFF43F5E),            // Neon Rose
    onError = Color(0xFF4C0519)
)

@Composable
fun VoiceCaloriesTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SleekDarkColors,
        typography = Typography(),
        content = content
    )
}
