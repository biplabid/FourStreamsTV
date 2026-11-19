package com.streamcast.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object StreamCastColors {
    val Background = Color(0xFF121316)
    val SurfaceLow = Color(0xFF1B1B1F)
    val SurfaceSecondary = Color(0xFF1F1F23)
    val CardSurface = Color(0xFF292A2D)
    val SurfaceHighest = Color(0xFF343538)
    val SurfaceBright = Color(0xFF38393C)

    val Outline = Color(0xFF484555)
    val OutlineVariant = Color(0xFF938EA1)

    val PrimaryViolet = Color(0xFF7657FF)
    val PrimaryContainer = Color(0xFF7657FF)
    val OnPrimaryContainer = Color(0xFFFFFDFF)
    val PrimaryFixedDim = Color(0xFFC9BEFF)

    val SecondaryCyan = Color(0xFF4AD7F6)
    val SecondaryContainer = Color(0xFF00B5D4)
    val OnSecondaryContainer = Color(0xFF00424E)

    val LiveGreen = Color(0xFF4AE08D)
    val TertiaryContainer = Color(0xFF00874D)
    val OnTertiaryContainer = Color(0xFFFBFFF8)

    val TextPrimary = Color(0xFFE3E2E6)
    val TextSecondary = Color(0xFFC9C4D8)

    val Error = Color(0xFFFFB4AB)
    val ErrorContainer = Color(0xFF93000A)
    val OnErrorContainer = Color(0xFFFFDAD6)
}

private val StreamCastColorScheme = darkColorScheme(
    background = StreamCastColors.Background,
    onBackground = StreamCastColors.TextPrimary,
    surface = StreamCastColors.Background,
    onSurface = StreamCastColors.TextPrimary,
    surfaceVariant = StreamCastColors.CardSurface,
    onSurfaceVariant = StreamCastColors.TextSecondary,
    primary = StreamCastColors.PrimaryViolet,
    onPrimary = Color.White,
    primaryContainer = StreamCastColors.PrimaryContainer,
    onPrimaryContainer = StreamCastColors.OnPrimaryContainer,
    secondary = StreamCastColors.SecondaryCyan,
    onSecondary = Color(0xFF003640),
    secondaryContainer = StreamCastColors.SecondaryContainer,
    onSecondaryContainer = StreamCastColors.OnSecondaryContainer,
    tertiary = StreamCastColors.LiveGreen,
    onTertiary = Color(0xFF00391D),
    tertiaryContainer = StreamCastColors.TertiaryContainer,
    onTertiaryContainer = StreamCastColors.OnTertiaryContainer,
    outline = StreamCastColors.Outline,
    outlineVariant = StreamCastColors.OutlineVariant,
    error = StreamCastColors.Error,
    errorContainer = StreamCastColors.ErrorContainer,
    onErrorContainer = StreamCastColors.OnErrorContainer
)

@Composable
fun StreamCastTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StreamCastColorScheme,
        content = content
    )
}
