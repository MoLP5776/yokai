package com.yokai.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.yokai.metadata.AppTheme
import com.yokai.metadata.ThemeMode

private val DefaultDark = darkColorScheme(
    primary = Color(0xFFE85D4F),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF70C1B3),
    tertiary = Color(0xFFF5C542),
    background = Color(0xFF181716),
    onBackground = Color(0xFFF1EFEB),
    surface = Color(0xFF242220),
    onSurface = Color(0xFFF1EFEB),
    surfaceVariant = Color(0xFF36322F),
    onSurfaceVariant = Color(0xFFD5D0C8),
)

private val DefaultLight = lightColorScheme(
    primary = Color(0xFFC8442F),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF3D8C7D),
    tertiary = Color(0xFFB8860B),
    background = Color(0xFFFBF8F3),
    onBackground = Color(0xFF1E1B18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1B18),
    surfaceVariant = Color(0xFFEDE6DC),
    onSurfaceVariant = Color(0xFF4A453E),
)

private val CatppuccinDark = darkColorScheme(
    primary = Color(0xFFCBA6F7),
    onPrimary = Color(0xFF1E1E2E),
    secondary = Color(0xFF89B4FA),
    tertiary = Color(0xFFA6E3A1),
    background = Color(0xFF1E1E2E),
    onBackground = Color(0xFFCDD6F4),
    surface = Color(0xFF313244),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF45475A),
    onSurfaceVariant = Color(0xFFBAC2DE),
)

private val CatppuccinLight = lightColorScheme(
    primary = Color(0xFF8839EF),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF1E66F5),
    tertiary = Color(0xFF40A02B),
    background = Color(0xFFEFF1F5),
    onBackground = Color(0xFF4C4F69),
    surface = Color(0xFFE6E9EF),
    onSurface = Color(0xFF4C4F69),
    surfaceVariant = Color(0xFFCCD0DA),
    onSurfaceVariant = Color(0xFF6C6F85),
)

private val NordDark = darkColorScheme(
    primary = Color(0xFF88C0D0),
    onPrimary = Color(0xFF2E3440),
    secondary = Color(0xFF81A1C1),
    tertiary = Color(0xFFA3BE8C),
    background = Color(0xFF2E3440),
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF3B4252),
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF434C5E),
    onSurfaceVariant = Color(0xFFD8DEE9),
)

private val NordLight = lightColorScheme(
    primary = Color(0xFF5E81AC),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF88C0D0),
    tertiary = Color(0xFF8FBCBB),
    background = Color(0xFFECEFF4),
    onBackground = Color(0xFF2E3440),
    surface = Color(0xFFE5E9F0),
    onSurface = Color(0xFF2E3440),
    surfaceVariant = Color(0xFFD8DEE9),
    onSurfaceVariant = Color(0xFF4C566A),
)

private val DraculaDark = darkColorScheme(
    primary = Color(0xFFBD93F9),
    onPrimary = Color(0xFF282A36),
    secondary = Color(0xFFFF79C6),
    tertiary = Color(0xFF50FA7B),
    background = Color(0xFF282A36),
    onBackground = Color(0xFFF8F8F2),
    surface = Color(0xFF343746),
    onSurface = Color(0xFFF8F8F2),
    surfaceVariant = Color(0xFF44475A),
    onSurfaceVariant = Color(0xFFC7C8CB),
)

private val DraculaLight = lightColorScheme(
    primary = Color(0xFF7B4FCB),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFD6336C),
    tertiary = Color(0xFF2C9A4A),
    background = Color(0xFFF8F8F2),
    onBackground = Color(0xFF282A36),
    surface = Color(0xFFECECEA),
    onSurface = Color(0xFF282A36),
    surfaceVariant = Color(0xFFDDDCE5),
    onSurfaceVariant = Color(0xFF44475A),
)

private val GruvboxDark = darkColorScheme(
    primary = Color(0xFFFE8019),
    onPrimary = Color(0xFF282828),
    secondary = Color(0xFFB8BB26),
    tertiary = Color(0xFFFABD2F),
    background = Color(0xFF282828),
    onBackground = Color(0xFFEBDBB2),
    surface = Color(0xFF3C3836),
    onSurface = Color(0xFFEBDBB2),
    surfaceVariant = Color(0xFF504945),
    onSurfaceVariant = Color(0xFFD5C4A1),
)

private val GruvboxLight = lightColorScheme(
    primary = Color(0xFFD65D0E),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF79740E),
    tertiary = Color(0xFFB57614),
    background = Color(0xFFFBF1C7),
    onBackground = Color(0xFF3C3836),
    surface = Color(0xFFEBDBB2),
    onSurface = Color(0xFF3C3836),
    surfaceVariant = Color(0xFFD5C4A1),
    onSurfaceVariant = Color(0xFF504945),
)

private val ForestDark = darkColorScheme(
    primary = Color(0xFF6FCF97),
    onPrimary = Color(0xFF0E1F14),
    secondary = Color(0xFF4FA1A0),
    tertiary = Color(0xFFC9D96B),
    background = Color(0xFF16201A),
    onBackground = Color(0xFFE2EFE6),
    surface = Color(0xFF1F2C24),
    onSurface = Color(0xFFE2EFE6),
    surfaceVariant = Color(0xFF324035),
    onSurfaceVariant = Color(0xFFC2D2C7),
)

private val ForestLight = lightColorScheme(
    primary = Color(0xFF2E7D4F),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF357A79),
    tertiary = Color(0xFF7C8A2E),
    background = Color(0xFFF2F8F2),
    onBackground = Color(0xFF16241B),
    surface = Color(0xFFE5EFE6),
    onSurface = Color(0xFF16241B),
    surfaceVariant = Color(0xFFD2E0D4),
    onSurfaceVariant = Color(0xFF3C4A3F),
)

private val StrawberryDark = darkColorScheme(
    primary = Color(0xFFFF6B9D),
    onPrimary = Color(0xFF2B0A18),
    secondary = Color(0xFFFF8FB3),
    tertiary = Color(0xFFFFC2D1),
    background = Color(0xFF1F1417),
    onBackground = Color(0xFFF6E4EA),
    surface = Color(0xFF2B1A1F),
    onSurface = Color(0xFFF6E4EA),
    surfaceVariant = Color(0xFF45282F),
    onSurfaceVariant = Color(0xFFD9B6C0),
)

private val StrawberryLight = lightColorScheme(
    primary = Color(0xFFE0457B),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFD6739B),
    tertiary = Color(0xFFC2185B),
    background = Color(0xFFFFF1F5),
    onBackground = Color(0xFF2B1219),
    surface = Color(0xFFFCE4EC),
    onSurface = Color(0xFF2B1219),
    surfaceVariant = Color(0xFFF3CCDA),
    onSurfaceVariant = Color(0xFF5C2E3B),
)

private val MonochromeDark = darkColorScheme(
    primary = Color(0xFFD9D9D9),
    onPrimary = Color(0xFF1A1A1A),
    secondary = Color(0xFFA6A6A6),
    tertiary = Color(0xFF8C8C8C),
    background = Color(0xFF121212),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFC2C2C2),
)

private val MonochromeLight = lightColorScheme(
    primary = Color(0xFF2B2B2B),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF595959),
    tertiary = Color(0xFF808080),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFF0F0F0),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFDDDDDD),
    onSurfaceVariant = Color(0xFF4D4D4D),
)

private fun AppTheme.baseColorScheme(mode: ThemeMode): ColorScheme = when (this) {
    AppTheme.DEFAULT -> if (mode == ThemeMode.DARK) DefaultDark else DefaultLight
    AppTheme.CATPPUCCIN -> if (mode == ThemeMode.DARK) CatppuccinDark else CatppuccinLight
    AppTheme.NORD -> if (mode == ThemeMode.DARK) NordDark else NordLight
    AppTheme.DRACULA -> if (mode == ThemeMode.DARK) DraculaDark else DraculaLight
    AppTheme.GRUVBOX -> if (mode == ThemeMode.DARK) GruvboxDark else GruvboxLight
    AppTheme.FOREST -> if (mode == ThemeMode.DARK) ForestDark else ForestLight
    AppTheme.STRAWBERRY -> if (mode == ThemeMode.DARK) StrawberryDark else StrawberryLight
    AppTheme.MONOCHROME -> if (mode == ThemeMode.DARK) MonochromeDark else MonochromeLight
}

private fun ColorScheme.toPureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF121212),
)

fun AppTheme.colorScheme(mode: ThemeMode, pureBlackDarkMode: Boolean = false): ColorScheme {
    val scheme = baseColorScheme(mode)
    return if (mode == ThemeMode.DARK && pureBlackDarkMode) scheme.toPureBlack() else scheme
}