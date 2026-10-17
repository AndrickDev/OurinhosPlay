package com.ourinhosplay.app.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.ourinhosplay.app.R

/**
 * Identidade: cores vêm das superfícies do esporte (pista, grama, areia, mar, madeira),
 * sobre um azul-noite de estádio. Cada esporte tinge o próprio card e botão.
 */
object Palette {
    val Noite = Color(0xFF0A0E1F)       // fundo (azul-noite, sólido)
    val Arquibancada = Color(0xFF121833) // superfícies (cartões)
    val Tribuna = Color(0xFF1D2548)      // superfícies elevadas / bordas / vagas vazias
    val Giz = Color(0xFFF4F6FB)          // texto principal
    val Neblina = Color(0xFF98A1C4)      // texto secundário
    val Pista = Color(0xFFFF5B1F)        // primária (pista de corrida)
    val Bola = Color(0xFFE6FF3D)         // destaque raro: ao vivo, GOOOL
    val Alerta = Color(0xFFFF4D6D)

    val Grama = Color(0xFF2BD965)        // futebol
    val Sol = Color(0xFFFFC928)          // vôlei
    val Limao = Color(0xFFFFE45C)        // futevôlei
    val Madeira = Color(0xFFFF9A3D)      // basquete
    val Mar = Color(0xFF22D3EE)          // beach tennis
    val Teal = Color(0xFF34D1BF)         // futsal
    val Lima = Color(0xFFA3E635)         // tênis
    val Lavanda = Color(0xFF8C9EFF)      // padel
    val Azul = Color(0xFF4DA3FF)         // natação
    val Rosa = Color(0xFFF472B6)         // biribol
    val Coral = Color(0xFFFF7A8A)        // corrida
    val Roxo = Color(0xFFB388FF)         // ciclismo
    val Areia = Color(0xFFD9C7A0)        // caminhada
    val Fucsia = Color(0xFFE879F9)       // tênis de mesa
}

private val Display = FontFamily(
    Font(R.font.barlow_condensed_extrabold_italic, FontWeight.ExtraBold, FontStyle.Italic),
    Font(R.font.barlow_condensed_bold_italic, FontWeight.Bold, FontStyle.Italic),
)

private val Body = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
)

/** Fonte do placar (horários, números, títulos de seção). */
val ScoreFont: FontFamily = Display

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, fontSize = 56.sp, lineHeight = 56.sp),
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, fontSize = 44.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val Colors: ColorScheme = darkColorScheme(
    primary = Palette.Pista,
    onPrimary = Color.White,
    primaryContainer = Palette.Tribuna,
    onPrimaryContainer = Palette.Giz,
    secondary = Palette.Bola,
    onSecondary = Palette.Noite,
    background = Palette.Noite,
    onBackground = Palette.Giz,
    surface = Palette.Noite,
    onSurface = Palette.Giz,
    surfaceVariant = Palette.Arquibancada,
    onSurfaceVariant = Palette.Neblina,
    surfaceContainer = Palette.Arquibancada,
    surfaceContainerHigh = Palette.Tribuna,
    outline = Palette.Tribuna,
    outlineVariant = Palette.Tribuna,
    error = Palette.Alerta,
    onError = Color.White,
    inverseSurface = Palette.Giz,
    inverseOnSurface = Palette.Noite,
)

/** Um tema só: escuro, como placar de estádio à noite. */
@Composable
fun OurinhosPlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
