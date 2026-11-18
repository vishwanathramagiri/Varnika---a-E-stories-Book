package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Warm Parchment"),
    SEPIA("Soft Sepia"),
    DARK("Midnight Dark")
}

private val LightColorScheme = lightColorScheme(
    primary = EmeraldInk,
    onPrimary = Color.White,
    primaryContainer = EmeraldInkContainer,
    onPrimaryContainer = OnEmeraldInkContainer,
    secondary = SaffronAmber,
    onSecondary = Color.White,
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = OnSaffronContainer,
    tertiary = TerracottaClay,
    onTertiary = Color.White,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = OnTerracottaContainer,
    background = ParchmentBg,
    onBackground = InkPrimary,
    surface = ParchmentSurface,
    onSurface = InkPrimary,
    surfaceVariant = ParchmentCard,
    onSurfaceVariant = InkSecondary,
    outline = ParchmentBorder
)

private val SepiaColorScheme = lightColorScheme(
    primary = EmeraldInk,
    onPrimary = Color.White,
    primaryContainer = EmeraldInkContainer,
    onPrimaryContainer = OnEmeraldInkContainer,
    secondary = SaffronAmber,
    onSecondary = Color.White,
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = OnSaffronContainer,
    tertiary = TerracottaClay,
    onTertiary = Color.White,
    background = SepiaBg,
    onBackground = SepiaInk,
    surface = SepiaSurface,
    onSurface = SepiaInk,
    surfaceVariant = Color(0xFFFAF3E3),
    onSurfaceVariant = Color(0xFF5D4C38),
    outline = Color(0xFFD5C4A1)
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldBright,
    onPrimary = Color(0xFF06251F),
    primaryContainer = EmeraldBrightContainer,
    onPrimaryContainer = EmeraldInkContainer,
    secondary = SaffronBright,
    onSecondary = Color(0xFF331900),
    secondaryContainer = Color(0xFF52310A),
    onSecondaryContainer = SaffronContainer,
    tertiary = TerracottaBright,
    onTertiary = Color(0xFF3B0900),
    tertiaryContainer = Color(0xFF5C1E10),
    onTertiaryContainer = TerracottaContainer,
    background = MidnightBg,
    onBackground = MidnightInk,
    surface = MidnightSurface,
    onSurface = MidnightInk,
    surfaceVariant = MidnightCard,
    onSurfaceVariant = MidnightInkSecondary,
    outline = MidnightBorder
)

val VarnikaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VarnikaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.SEPIA -> SepiaColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.SYSTEM -> if (systemDark) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VarnikaTypography,
        shapes = VarnikaShapes,
        content = content
    )
}
