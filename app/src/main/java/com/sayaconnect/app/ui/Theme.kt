package com.sayaconnect.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Accent = Color(0xFF8677FF)
val Sky = Color(0xFF45D8E3)
val Pink = Color(0xFFFF8FAB)
private val darkScheme = darkColorScheme(
    primary = Color(0xFFACA1FF), onPrimary = Color(0xFF130D38),
    secondary = Color(0xFF6DE2ED), background = Color(0xFF0B1222),
    surface = Color(0xFF141E31), surfaceVariant = Color(0xFF1B2B43),
    onSurface = Color(0xFFF2F4FF), onBackground = Color(0xFFF2F4FF),
    outline = Color(0xFF53627B)
)
private val lightScheme = lightColorScheme(
    primary = Color(0xFF6651DB), secondary = Color(0xFF007F95),
    background = Color(0xFFF6F6FE), surface = Color.White,
    surfaceVariant = Color(0xFFEDEAFF), onSurface = Color(0xFF111A33),
    onBackground = Color(0xFF111A33)
)

@Composable
fun SayaTheme(dark: Boolean, content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = if (dark) darkScheme else lightScheme,
    shapes = Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(26.dp)
    ), content = content
)

fun wallpaperBrush(type: String): Brush = when (type) {
    "Ocean" -> Brush.verticalGradient(listOf(Color(0xFF072E48), Color(0xFF165B83), Color(0xFF24416B)))
    "Rose" -> Brush.verticalGradient(listOf(Color(0xFF5A274B), Color(0xFF9E617F), Color(0xFF38294D)))
    "Midnight" -> Brush.verticalGradient(listOf(Color(0xFF11172A), Color(0xFF1D274A), Color(0xFF09111D)))
    "Mint" -> Brush.verticalGradient(listOf(Color(0xFF0C444E), Color(0xFF2A8A88), Color(0xFF124755)))
    else -> Brush.verticalGradient(listOf(Color(0xFF1C1F58), Color(0xFF5A348B), Color(0xFF172B56)))
}

@Composable
fun AccentPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.background(Brush.linearGradient(listOf(Color(0xFF5345CC), Color(0xFF8E55C8))),
        RoundedCornerShape(25.dp)).padding(21.dp), content = content)
