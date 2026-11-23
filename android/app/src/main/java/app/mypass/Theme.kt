package app.mypass

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MpColors {
    val Background = Color(0xFFF6F9FF)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceLow = Color(0xFFEDF4FF)
    val SurfaceContainer = Color(0xFFE4EFFD)
    val SurfaceHigh = Color(0xFFDFE9F7)
    val SurfaceHighest = Color(0xFFD9E3F1)
    val OnSurface = Color(0xFF121C26)
    val OnSurfaceVariant = Color(0xFF414844)
    val Outline = Color(0xFF717973)
    val OutlineVariant = Color(0xFFC1C8C2)
    val Border = Color(0xFFD0D7E2)
    val Primary = Color(0xFF012D1D)
    val PrimaryContainer = Color(0xFF1B4332)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryFixed = Color(0xFFC1ECD4)
    val OnPrimaryFixed = Color(0xFF002114)
    val Secondary = Color(0xFF9B4500)
    val SecondaryContainer = Color(0xFFFD8A42)
    val OnSecondaryContainer = Color(0xFF682C00)
    val SecondaryFixed = Color(0xFFFFDBCA)
    val OnSecondaryFixed = Color(0xFF331200)
    val Tertiary = Color(0xFF032A39)
    val TertiaryFixed = Color(0xFFC5E7FB)
    val OnTertiaryFixed = Color(0xFF001E2B)
    val Error = Color(0xFFBA1A1A)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF93000A)
    val Success = Color(0xFF1B6B3A)
    val SuccessContainer = Color(0xFFD7F5DF)
    val Accent = Color(0xFFFD8A42)
    val AccentDeep = Color(0xFF9B4500)
    val AccentSoft = Color(0xFFFFF2E8)
    val BrandMid = Color(0xFF0D4B36)
    val BrandSoft = Color(0xFFE8F5EE)
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
}

private val MpColorScheme = lightColorScheme(
    primary = MpColors.Primary,
    onPrimary = MpColors.OnPrimary,
    primaryContainer = MpColors.PrimaryContainer,
    onPrimaryContainer = Color(0xFF86AF99),
    secondary = MpColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = MpColors.SecondaryContainer,
    onSecondaryContainer = MpColors.OnSecondaryContainer,
    tertiary = MpColors.Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF1E4050),
    onTertiaryContainer = Color(0xFF8AABBE),
    background = MpColors.Background,
    onBackground = MpColors.OnSurface,
    surface = MpColors.Background,
    onSurface = MpColors.OnSurface,
    surfaceVariant = MpColors.SurfaceHighest,
    onSurfaceVariant = MpColors.OnSurfaceVariant,
    outline = MpColors.Outline,
    outlineVariant = MpColors.OutlineVariant,
    error = MpColors.Error,
    onError = Color.White,
    errorContainer = MpColors.ErrorContainer,
    onErrorContainer = MpColors.OnErrorContainer,
    inverseSurface = Color(0xFF27313C),
    inverseOnSurface = Color(0xFFE8F2FF),
    inversePrimary = Color(0xFFA5D0B9),
    surfaceTint = Color(0xFF3F6653),
    scrim = Color(0xFF000000),
)

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
fun MyPassTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MpColorScheme,
        typography = MpTypography,
        shapes = MpShapes,
        content = content,
    )
}
