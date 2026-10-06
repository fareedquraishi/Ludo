package com.example.ludo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.ludo.R

/** Brand palette (measured from the Euro logo and the Ludo splash screen). */
object LudoColors {
    val Navy = Color(0xFF2A3552)
    val NavyDeep = Color(0xFF1F2842)   // panels / cards
    val Gold = Color(0xFFF8D810)
    val Teal = Color(0xFF085D78)
    val EuroBlue = Color(0xFF084879)
    val Frame = Color(0xFF002D78)      // board outline
    val OnNavy = Color(0xFFF2F4FA)
    val Muted = Color(0xFFC5CCE0)
}

private val LudoDarkScheme = darkColorScheme(
    primary = LudoColors.Gold,
    onPrimary = Color(0xFF1B2440),
    secondary = LudoColors.Teal,
    onSecondary = Color.White,
    secondaryContainer = LudoColors.Teal,
    onSecondaryContainer = Color.White,
    background = LudoColors.Navy,
    onBackground = LudoColors.OnNavy,
    surface = LudoColors.NavyDeep,
    onSurface = LudoColors.OnNavy,
    surfaceVariant = LudoColors.NavyDeep,
    onSurfaceVariant = LudoColors.Muted,
    outline = LudoColors.EuroBlue,
)

@Composable
fun LudoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LudoDarkScheme, content = content)
}

/** Navy background; content is kept clear of the system bars. */
@Composable
fun BrandBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize().background(LudoColors.Navy)) {
        CompositionLocalProvider(LocalContentColor provides LudoColors.OnNavy) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) { content() }
        }
    }
}

/** The faint white Euro logo. Size and position are decided by the caller (modifier). */
@Composable
fun EuroWatermark(modifier: Modifier = Modifier, alpha: Float = 0.14f) {
    Image(
        painter = painterResource(R.drawable.euro_logo_white),
        contentDescription = null,
        alpha = alpha,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

/** Gold "LUDO" lettering with a white outline, like the splash screen. */
@Composable
fun BrandTitle(text: String = "LUDO", size: TextUnit = 56.sp) {
    val style = TextStyle(fontSize = size, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
    Box {
        Text(
            text,
            style = style.copy(drawStyle = Stroke(width = size.value * 0.3f, join = StrokeJoin.Round)),
            color = Color.White,
        )
        Text(text, style = style, color = LudoColors.Gold)
    }
}
