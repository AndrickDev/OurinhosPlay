package com.ourinhosplay.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

/**
 * Linguagem visual, na linha da Apple: o conteúdo mora em superfícies sólidas e neutras; o vidro
 * (translúcido com blur) fica só nos controles flutuantes, que passam por cima do conteúdo —
 * barra de abas e barra de ação. A cor do esporte aparece em sinais pequenos, nunca no fundo.
 */
object Glass {
    val CardShape = RoundedCornerShape(20.dp)
    val BarShape = RoundedCornerShape(30.dp)

    private val Edge = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.09f)),
    )

    /** Fio de luz da borda. */
    fun edge(): Brush = Edge

    val Hairline = Color.White.copy(alpha = 0.09f)
}

/** Estado do blur: o conteúdo rolável é a "fonte"; as barras flutuantes borram essa fonte. */
val LocalHaze = compositionLocalOf<HazeState?> { null }

/**
 * Fundo da tela + fonte do blur. O fundo é grafite quase liso, com um leve clareado no topo.
 * [overlay] recebe as barras flutuantes: ficam fora da fonte (irmãs do conteúdo), senão não borram.
 * O fundo é sólido: nenhuma cor aparece atrás do conteúdo.
 */
@Composable
fun GlassBackdrop(
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val haze = remember { HazeState() }
    Box(modifier.fillMaxSize().background(Palette.Noite)) {
        Box(
            Modifier
                .fillMaxSize()
                .haze(haze)
                .background(Palette.Noite),
        ) {
            CompositionLocalProvider(LocalHaze provides haze) { content() }
        }
        CompositionLocalProvider(LocalHaze provides haze) { overlay() }
    }
}

/** Superfície de conteúdo: sólida, neutra, com fio de luz. Sem transparência. */
fun Modifier.card(shape: Shape = Glass.CardShape, color: Color = Palette.Arquibancada): Modifier = this
    .clip(shape)
    .background(color)
    .border(1.dp, Glass.Hairline, shape)

/** Barra flutuante de vidro: blur real do que passa por trás, tom grafite neutro. */
@Composable
fun Modifier.glassBar(
    shape: Shape = Glass.BarShape,
    blur: Dp = 24.dp,
): Modifier {
    val haze = LocalHaze.current
    val base = this.clip(shape)
    val glass = if (haze != null) {
        base.hazeChild(
            state = haze,
            style = HazeStyle(
                backgroundColor = Palette.Noite,
                tint = HazeTint(Palette.Noite.copy(alpha = 0.55f)),
                blurRadius = blur,
                noiseFactor = 0.02f,
            ),
        )
    } else {
        base.background(Palette.Noite.copy(alpha = 0.92f))
    }
    return glass.border(1.dp, Glass.edge(), shape)
}
