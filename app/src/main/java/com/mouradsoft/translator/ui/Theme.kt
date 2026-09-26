package com.mouradsoft.translator.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Palette {
    val Mist = Color(0xFFF0F6FA)
    val Navy = Color(0xFF152E46)
    val Muted = Color(0xFF50677A)
    val Teal = Color(0xFF087F83)
    val TealDark = Color(0xFF066669)
    val Aqua = Color(0xFFDFF2F1)
    val Shell = Color(0xFF91D5D8)
    val Yellow = Color(0xFFF2C764)
    val Cream = Color(0xFFFFF2CE)
    val Line = Color(0xFFB9CDD9)
    val Error = Color(0xFF963E36)
}
object Space {
    val xxs = 4.dp; val xs = 8.dp; val sm = 12.dp; val md = 16.dp
    val lg = 24.dp; val xl = 32.dp; val xxl = 48.dp
    val touch = 56.dp; val maxWidth = 560.dp
}
object Corners {
    val small = RoundedCornerShape(12.dp)
    val medium = RoundedCornerShape(20.dp)
    val large = RoundedCornerShape(28.dp)
}

@Composable
fun MouradsoftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.TealDark, onPrimary = Color.White, primaryContainer = Palette.Aqua,
            onPrimaryContainer = Palette.Navy, secondary = Palette.Teal, onSecondary = Color.White,
            background = Palette.Mist, onBackground = Palette.Navy,
            surface = Color.White, onSurface = Palette.Navy, surfaceVariant = Palette.Mist,
            onSurfaceVariant = Palette.Muted, outline = Palette.Line,
            error = Palette.Error, errorContainer = Color(0xFFFFEDE9), onErrorContainer = Palette.Error
        ),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.7).sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 35.sp, letterSpacing = (-0.5).sp),
            titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 29.sp),
            titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
            labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
            labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)
        ),
        shapes = Shapes(small = Corners.small, medium = Corners.medium, large = Corners.large),
        content = content
    )
}

