package dev.rakara.app.ui

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp





private val IvoryPaper     = Color(0xFFF7F5F0)
private val SurfaceWhite   = Color(0xFFFFFFFF)
private val SurfaceMuted   = Color(0xFFEDF0F3)
private val SapphireNavy   = Color(0xFF243B5A)
private val NavyHover      = Color(0xFF192B43)
private val SilverMist     = Color(0xFFDCE5EF)
private val LavenderIris   = Color(0xFF8C83B9)
private val AntiqueGold    = Color(0xFFB89560)
private val InkCharcoal    = Color(0xFF34363B)
private val TextSecondary  = Color(0xFF777D86)
private val BorderLight    = Color(0xFFD9DDE2)
private val SuccessGreen   = Color(0xFF55765F)
private val ErrorRed       = Color(0xFFB75D5D)


private val ReaderPaper     = Color(0xFFF4F0E6)
private val ReaderInk       = Color(0xFF403D36)
private val ReaderBorder    = Color(0xFFE2DDD0)


private val DarkBg          = Color(0xFF202124)
private val DarkSurface     = Color(0xFF2A2C2E)
private val DarkText        = Color(0xFFE7E5E0)
private val DarkBlueAccent  = Color(0xFFAEBED2)
private val DarkLavender    = Color(0xFFB9ADD9)
private val DarkGold        = Color(0xFFD4B888)
private val DarkBorder      = Color(0xFF3C3F44)





data class RakaraExtraColors(
    val blueSoft: Color,
    val gold: Color,
    val success: Color,
    val danger: Color,
    val readingPaper: Color,
    val readingText: Color,
    val readingBorder: Color,
)

private val LightExtra = RakaraExtraColors(
    blueSoft = SilverMist,
    gold = AntiqueGold,
    success = SuccessGreen,
    danger = ErrorRed,
    readingPaper = ReaderPaper,
    readingText = ReaderInk,
    readingBorder = ReaderBorder,
)

private val DarkExtra = RakaraExtraColors(
    blueSoft = Color(0xFF33475F),
    gold = DarkGold,
    success = Color(0xFF7CA087),
    danger = Color(0xFFD48383),
    readingPaper = Color(0xFF1A1B1D),
    readingText = DarkText,
    readingBorder = Color(0xFF2E3033),
)

val LocalRakaraExtra = staticCompositionLocalOf { LightExtra }

val MaterialTheme.rakaraExtra: RakaraExtraColors
    @Composable get() = LocalRakaraExtra.current





private val LightScheme = lightColorScheme(
    primary = SapphireNavy,
    onPrimary = IvoryPaper,
    primaryContainer = SilverMist,
    onPrimaryContainer = SapphireNavy,
    secondary = SilverMist,
    onSecondary = InkCharcoal,
    secondaryContainer = SurfaceMuted,
    onSecondaryContainer = InkCharcoal,
    tertiary = LavenderIris,
    onTertiary = IvoryPaper,
    tertiaryContainer = Color(0xFFE6E2F2),
    onTertiaryContainer = Color(0xFF2E2846),
    background = IvoryPaper,
    onBackground = InkCharcoal,
    surface = SurfaceWhite,
    onSurface = InkCharcoal,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    outlineVariant = Color(0xFFE8EBEF),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFF7E5E5),
    onErrorContainer = Color(0xFF5C2222),
)

private val DarkScheme = darkColorScheme(
    primary = DarkBlueAccent,
    onPrimary = DarkBg,
    primaryContainer = Color(0xFF324A6B),
    onPrimaryContainer = DarkBlueAccent,
    secondary = Color(0xFF3A4055),
    onSecondary = DarkText,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = DarkText,
    tertiary = DarkLavender,
    onTertiary = DarkBg,
    tertiaryContainer = Color(0xFF3D3453),
    onTertiaryContainer = DarkLavender,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = Color(0xFF303237),
    onSurfaceVariant = Color(0xFFAEB0B4),
    outline = DarkBorder,
    outlineVariant = Color(0xFF3A3C40),
    error = Color(0xFFD48383),
    onError = DarkBg,
    errorContainer = Color(0xFF4A2A2A),
    onErrorContainer = Color(0xFFF5C0C0),
)





private val RakaraTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 48.sp,
        letterSpacing = (-0.5).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Light,
        fontSize = 28.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 1.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
    ),
)





private object NoIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance =
        object : IndicationInstance {
            override fun ContentDrawScope.drawIndication() {
                drawContent()
            }
        }
}





@Composable
fun RakaraTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalIndication provides NoIndication,
        LocalRakaraExtra provides if (dark) DarkExtra else LightExtra,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = RakaraTypography,
            content = content,
        )
    }
}
