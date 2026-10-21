package com.ourinhosplay.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke

/** Estado vazio: quadra vista de cima, só as linhas, com um marcador laranja no centro (onde o jogo vai acontecer). */
@Composable
fun EmptyCourt(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val line = Palette.Tribuna
        val sw = 2.5.dp.toPx()
        val stroke = Stroke(width = sw)
        val pad = sw
        val w = size.width - 2 * pad
        val h = size.height - 2 * pad
        val corner = CornerRadius(14.dp.toPx())
        drawRoundRect(Palette.Arquibancada, Offset(pad, pad), Size(w, h), corner)
        drawRoundRect(line, Offset(pad, pad), Size(w, h), corner, style = stroke)
        // linha do meio e círculo central
        drawLine(line, Offset(size.width / 2, pad), Offset(size.width / 2, size.height - pad), sw)
        drawCircle(line, radius = h * 0.24f, center = center, style = stroke)
        // áreas
        val boxW = w * 0.15f
        val boxH = h * 0.5f
        drawRect(line, Offset(pad, center.y - boxH / 2), Size(boxW, boxH), style = stroke)
        drawRect(line, Offset(size.width - pad - boxW, center.y - boxH / 2), Size(boxW, boxH), style = stroke)
        // marcador do jogo: brilho suave + ponto
        drawCircle(
            Brush.radialGradient(listOf(Palette.Pista.copy(alpha = 0.45f), Palette.Pista.copy(alpha = 0f)), center, h * 0.32f),
            radius = h * 0.32f, center = center,
        )
        drawCircle(Palette.Pista, radius = 6.dp.toPx(), center = center)
    }
}
