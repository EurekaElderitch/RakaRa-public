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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val EinkLight = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFFAFAFA),
    primaryContainer = Color(0xFFEEEEEE),
    onPrimaryContainer = Color(0xFF111111),
    background = Color(0xFFF7F7F5),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF444444),
    outline = Color(0xFF888888),
    error = Color(0xFF222222),
    onError = Color(0xFFFFFFFF),
)

private val EinkDark = darkColorScheme(
    primary = Color(0xFFEDEDED),
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color(0xFFEDEDED),
    background = Color(0xFF0E0E0E),
    onBackground = Color(0xFFE5E5E5),
    surface = Color(0xFF151515),
    onSurface = Color(0xFFE5E5E5),
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = Color(0xFFBBBBBB),
    outline = Color(0xFF555555),
    error = Color(0xFFBBBBBB),
    onError = Color(0xFF111111),
)

private val EinkTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 40.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp),
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
    CompositionLocalProvider(LocalIndication provides NoIndication) {
        MaterialTheme(
            colorScheme = if (dark) EinkDark else EinkLight,
            typography = EinkTypography,
            content = content
        )
    }
}