package app.mypass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ThemeMode {
    System,
    Light,
    Dark,
}

@Immutable
data class MpPalette(
    val background: Color,
    val surface: Color,
    val surfaceLow: Color,
    val surfaceContainer: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val border: Color,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val onPrimaryContainer: Color,
    val primaryFixed: Color,
    val onPrimaryFixed: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val secondaryFixed: Color,
    val onSecondaryFixed: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val tertiaryFixed: Color,
    val onTertiaryFixed: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val success: Color,
    val successContainer: Color,
    val accent: Color,
    val accentDeep: Color,
    val accentSoft: Color,
    val brandMid: Color,
    val brandSoft: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,
    val surfaceTint: Color,
)

val LightPalette = MpPalette(
    background = Color(0xFFF6F9FF),
    surface = Color(0xFFFFFFFF),
    surfaceLow = Color(0xFFEDF4FF),
    surfaceContainer = Color(0xFFE4EFFD),
    surfaceHigh = Color(0xFFDFE9F7),
    surfaceHighest = Color(0xFFD9E3F1),
    onSurface = Color(0xFF121C26),
    onSurfaceVariant = Color(0xFF414844),
    outline = Color(0xFF717973),
    outlineVariant = Color(0xFFC1C8C2),
    border = Color(0xFFD0D7E2),
    primary = Color(0xFF012D1D),
    primaryContainer = Color(0xFF1B4332),
    onPrimary = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFF86AF99),
    primaryFixed = Color(0xFFC1ECD4),
    onPrimaryFixed = Color(0xFF002114),
    secondary = Color(0xFF9B4500),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFD8A42),
    onSecondaryContainer = Color(0xFF682C00),
    secondaryFixed = Color(0xFFFFDBCA),
    onSecondaryFixed = Color(0xFF331200),
    tertiary = Color(0xFF032A39),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF1E4050),
    onTertiaryContainer = Color(0xFF8AABBE),
    tertiaryFixed = Color(0xFFC5E7FB),
    onTertiaryFixed = Color(0xFF001E2B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    success = Color(0xFF1B6B3A),
    successContainer = Color(0xFFD7F5DF),
    accent = Color(0xFFFD8A42),
    accentDeep = Color(0xFF9B4500),
    accentSoft = Color(0xFFFFF2E8),
    brandMid = Color(0xFF0D4B36),
    brandSoft = Color(0xFFE8F5EE),
    inverseSurface = Color(0xFF27313C),
    inverseOnSurface = Color(0xFFE8F2FF),
    inversePrimary = Color(0xFFA5D0B9),
    surfaceTint = Color(0xFF3F6653),
)

val DarkPalette = MpPalette(
    background = Color(0xFF08120E),
    surface = Color(0xFF101C16),
    surfaceLow = Color(0xFF15241C),
    surfaceContainer = Color(0xFF1A2C22),
    surfaceHigh = Color(0xFF20352A),
    surfaceHighest = Color(0xFF274031),
    onSurface = Color(0xFFE3F1E8),
    onSurfaceVariant = Color(0xFFB1C6B8),
    outline = Color(0xFF829B8B),
    outlineVariant = Color(0xFF334C3D),
    border = Color(0xFF2B4537),
    primary = Color(0xFF8FDCB2),
    primaryContainer = Color(0xFF2B6248),
    onPrimary = Color(0xFF00210F),
    onPrimaryContainer = Color(0xFFC1ECD4),
    primaryFixed = Color(0xFF1B4332),
    onPrimaryFixed = Color(0xFFD8F7E5),
    secondary = Color(0xFFFFB68E),
    onSecondary = Color(0xFF4A1E00),
    secondaryContainer = Color(0xFF7A3300),
    onSecondaryContainer = Color(0xFFFFDBCA),
    secondaryFixed = Color(0xFF54280B),
    onSecondaryFixed = Color(0xFFFFDBCA),
    tertiary = Color(0xFFAACBDF),
    onTertiary = Color(0xFF002B3B),
    tertiaryContainer = Color(0xFF1E4050),
    onTertiaryContainer = Color(0xFFC5E7FB),
    tertiaryFixed = Color(0xFF16323F),
    onTertiaryFixed = Color(0xFFC5E7FB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5C1414),
    onErrorContainer = Color(0xFFFFDAD6),
    success = Color(0xFF8FDCB2),
    successContainer = Color(0xFF0D3B26),
    accent = Color(0xFFFF9A5A),
    accentDeep = Color(0xFFFFB68E),
    accentSoft = Color(0xFF3A1C0B),
    brandMid = Color(0xFF3B8F68),
    brandSoft = Color(0xFF16281F),
    inverseSurface = Color(0xFFE5F2E9),
    inverseOnSurface = Color(0xFF121C26),
    inversePrimary = Color(0xFF012D1D),
    surfaceTint = Color(0xFF8FDCB2),
)

private val LocalMpPalette = staticCompositionLocalOf { LightPalette }
private val LocalMpDark = staticCompositionLocalOf { false }

object MpTheme {
    val colors: MpPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalMpPalette.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalMpDark.current
}

/**
 * Mantiene la API existente de MpColors, pero lee la paleta activa para que
 * los composables se recompongan al cambiar entre tema claro y oscuro.
 */
object MpColors {
    val ScanInk = Color(0xFF475569)
    val BrandMark = Color(0xFFD6F3E3)

    val Background: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.surface
    val SurfaceLow: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.surfaceLow
    val SurfaceContainer: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.surfaceContainer
    val SurfaceHigh: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.surfaceHigh
    val SurfaceHighest: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.surfaceHighest
    val OnSurface: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.onSurface
    val OnSurfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.onSurfaceVariant
    val Outline: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.outline
    val OutlineVariant: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.outlineVariant
    val Border: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.border
    val Primary: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.primary
    val PrimaryContainer: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.primaryContainer
    val OnPrimary: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.onPrimary
    val PrimaryFixed: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.primaryFixed
    val OnPrimaryFixed: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.onPrimaryFixed
    val Secondary: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.secondary
    val SecondaryContainer: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.secondaryContainer
    val OnSecondaryContainer: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.onSecondaryContainer
    val SecondaryFixed: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.secondaryFixed
    val OnSecondaryFixed: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.onSecondaryFixed
    val Tertiary: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.tertiary
    val TertiaryFixed: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.tertiaryFixed
    val OnTertiaryFixed: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.onTertiaryFixed
    val Error: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.error
    val OnError: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.onError
    val ErrorContainer: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.errorContainer
    val OnErrorContainer: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.onErrorContainer
    val Success: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.success
    val SuccessContainer: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.successContainer
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.accent
    val AccentDeep: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.accentDeep
    val AccentSoft: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.accentSoft
    val BrandMid: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.brandMid
    val BrandSoft: Color @Composable @ReadOnlyComposable get() = LocalMpPalette.current.brandSoft
}

object MpGradients {
    val Brand = Brush.linearGradient(
        listOf(Color(0xFF01261A), Color(0xFF012D1D), Color(0xFF0D4B36)),
    )
    val BrandDeep = Brush.linearGradient(
        listOf(Color(0xFF001E14), Color(0xFF012D1D), Color(0xFF0A4230)),
    )
    val Warm = Brush.linearGradient(
        listOf(Color(0xFF3B1A00), Color(0xFF9B4500), Color(0xFFFD8A42)),
    )
    val Sheen = Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.18f), Color.Transparent, Color.Black.copy(alpha = 0.08f)),
    )

    val PrimaryButton: Brush
        @Composable
        @ReadOnlyComposable
        get() = if (LocalMpDark.current) {
            Brush.linearGradient(
                listOf(Color(0xFFA9E9C7), Color(0xFF8FDCB2), Color(0xFF74C69A)),
            )
        } else {
            Brand
        }
}

private fun MpPalette.colorScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onSurface,
        surface = background,
        onSurface = onSurface,
        surfaceVariant = surfaceHighest,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        inversePrimary = inversePrimary,
        surfaceTint = surfaceTint,
        scrim = Color.Black,
    )
} else {
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onSurface,
        surface = background,
        onSurface = onSurface,
        surfaceVariant = surfaceHighest,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        inversePrimary = inversePrimary,
        surfaceTint = surfaceTint,
        scrim = Color.Black,
    )
}

private val MpFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_400, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_500, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_600, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_700, FontWeight.Bold),
)

private val MpTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = MpFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = MpFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = MpFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = MpFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = MpFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = MpFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = MpFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = MpFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = MpFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = MpFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = MpFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = MpFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = MpFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.4.sp,
    ),
)

private val MpShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun MyPassTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val palette = if (dark) DarkPalette else LightPalette

    CompositionLocalProvider(
        LocalMpPalette provides palette,
        LocalMpDark provides dark,
    ) {
        MaterialTheme(
            colorScheme = palette.colorScheme(dark),
            typography = MpTypography,
            shapes = MpShapes,
            content = content,
        )
    }
}
