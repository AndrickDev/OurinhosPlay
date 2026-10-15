package com.ourinhosplay.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun pentagon(cx: Float, cy: Float, r: Float, startDeg: Float = -90f): Path =
    Path().apply {
        for (k in 0 until 5) {
            val a = Math.toRadians((startDeg + k * 72f).toDouble())
            val x = cx + r * cos(a).toFloat()
            val y = cy + r * sin(a).toFloat()
            if (k == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

/** Bola de futebol: pentágono central, cinco remendos na borda e costuras. */
private fun DrawScope.drawBall(center: Offset, radius: Float, rotationDeg: Float) {
    rotate(rotationDeg, pivot = center) {
        drawCircle(Palette.Giz, radius, center)
        val seam = Palette.Noite
        // costuras do pentágono central até os remendos
        for (k in 0 until 5) {
            val a = Math.toRadians((-90.0 + k * 72.0))
            val from = Offset(center.x + radius * 0.34f * cos(a).toFloat(), center.y + radius * 0.34f * sin(a).toFloat())
            val to = Offset(center.x + radius * 0.66f * cos(a).toFloat(), center.y + radius * 0.66f * sin(a).toFloat())
            drawLine(seam, from, to, strokeWidth = radius * 0.07f)
        }
        drawPath(pentagon(center.x, center.y, radius * 0.36f), seam)
        for (k in 0 until 5) {
            val a = Math.toRadians((-90.0 + k * 72.0 + 36.0))
            val c = Offset(center.x + radius * 0.74f * cos(a).toFloat(), center.y + radius * 0.74f * sin(a).toFloat())
            drawPath(pentagon(c.x, c.y, radius * 0.19f, startDeg = Math.toDegrees(a).toFloat() + 180f), seam)
        }
        drawCircle(seam, radius, center, style = androidx.compose.ui.graphics.drawscope.Stroke(radius * 0.06f))
    }
}

/** O "P" do logo: haste + bojo em forma de play. */
private fun DrawScope.drawPlayP(topLeft: Offset, height: Float, color: Color) {
    val stemW = height * 0.2f
    drawRoundRect(
        color, topLeft, androidx.compose.ui.geometry.Size(stemW, height),
        androidx.compose.ui.geometry.CornerRadius(stemW * 0.3f),
    )
    val tri = Path().apply {
        moveTo(topLeft.x + stemW * 0.7f, topLeft.y)
        lineTo(topLeft.x + stemW + height * 0.62f, topLeft.y + height * 0.36f)
        lineTo(topLeft.x + stemW * 0.7f, topLeft.y + height * 0.72f)
        close()
    }
    drawPath(tri, color)
}

private fun ease(a: Float, b: Float, p: Float) = ((p - a) / (b - a)).coerceIn(0f, 1f)
private fun smooth(t: Float) = t * t * (3f - 2f * t)
private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/**
 * Abertura: o "O" (bola) rola engolindo "urinhos", chega no "P", "lay" some e o P
 * vira o play. Termina no monograma OP.
 */
@Composable
fun SplashScreen(durationMs: Int = 2800) {
    val measurer = rememberTextMeasurer()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMs, easing = LinearEasing)) }
    val density = LocalDensity.current
    val style = TextStyle(
        fontFamily = ScoreFont,
        fontWeight = FontWeight.ExtraBold,
        fontStyle = FontStyle.Italic,
        fontSize = 50.sp,
        color = Palette.Giz,
    )

    Box(Modifier.fillMaxSize().background(Palette.Noite)) {
        Canvas(Modifier.fillMaxSize()) {
            val p = progress.value
            val letters = "urinhos"
            val h = measurer.measure("Ag", style).size.height.toFloat()
            val d = h * 0.72f
            val gap = d * 0.1f
            val widths = letters.map { measurer.measure(it.toString(), style).size.width.toFloat() }
            val wLetters = widths.sum()
            val wP = measurer.measure("P", style).size.width.toFloat()
            val wLay = measurer.measure("lay", style).size.width.toFloat()
            val total = d + gap + wLetters + wP + wLay
            val x0 = (size.width - total) / 2f
            val cy = size.height / 2f
            val lettersX0 = x0 + d + gap
            val xP = lettersX0 + wLetters
            val xLay = xP + wP
            val ballEnd = xP - d - gap
            val markW = h * 0.2f + h * 0.62f
            val total2 = d + gap * 2 + markW
            val xf0 = (size.width - total2) / 2f

            val appear = smooth(ease(0f, 0.12f, p))
            val roll = smooth(ease(0.14f, 0.62f, p))
            val layGone = smooth(ease(0.60f, 0.74f, p))
            val merge = smooth(ease(0.74f, 0.92f, p))
            val pop = 1f + 0.07f * sin((PI * ease(0.92f, 1f, p)).toFloat())

            val ballX = if (merge > 0f) lerp(ballEnd, xf0, merge) else lerp(x0, ballEnd, roll)
            val ballCenter = Offset(ballX + d / 2f, cy)
            // posição mais à frente que a bola já chegou: letras engolidas não voltam
            val reachX = lerp(x0, ballEnd, roll) + d / 2f
            val rotation = (ballX - x0) / (d / 2f) * (180f / PI.toFloat())

            scale(pop, pivot = Offset(size.width / 2f, cy)) {
                // letras "urinhos": somem quando a bola passa por cima
                var lx = lettersX0
                letters.forEachIndexed { i, ch ->
                    val w = widths[i]
                    val eaten = ((reachX - lx) / (w + d * 0.15f)).coerceIn(0f, 1f)
                    val a = (appear * (1f - eaten)).coerceIn(0f, 1f)
                    if (a > 0.01f) {
                        drawText(
                            measurer, ch.toString(),
                            Offset(lx, cy - h / 2f - eaten * h * 0.15f),
                            style.copy(color = Palette.Giz.copy(alpha = a)),
                        )
                    }
                    lx += w
                }
                // "lay"
                val layAlpha = appear * (1f - layGone)
                if (layAlpha > 0.01f) {
                    drawText(
                        measurer, "lay",
                        Offset(xLay + layGone * h * 0.4f, cy - h / 2f),
                        style.copy(color = Palette.Pista.copy(alpha = layAlpha)),
                    )
                }
                // P do texto vira o P-play
                val px = lerp(xP, xf0 + d + gap * 2, merge)
                val pAlpha = appear * (1f - merge)
                if (pAlpha > 0.01f) {
                    drawText(measurer, "P", Offset(px, cy - h / 2f), style.copy(color = Palette.Pista.copy(alpha = pAlpha)))
                }
                if (merge > 0.01f) {
                    drawPlayP(Offset(px, cy - h * 0.34f), h * 0.68f, Palette.Pista.copy(alpha = merge))
                }
                if (appear > 0.01f) {
                    drawBall(ballCenter, d / 2f * (0.6f + 0.4f * appear), rotation)
                }
            }
        }
    }
}

/**
 * Logo vertical responsivo: "Ourinhos" (com o O de bola) em cima e "Play" embaixo.
 * `size` controla tudo: o resto proporcional.
 */
@Composable
fun AppLogo(modifier: Modifier = Modifier, fontSize: TextUnit = 44.sp) {
    val ballSize: Dp = with(LocalDensity.current) { (fontSize.toPx() * 0.78f).toDp() }
    Column(modifier, horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy((-6).dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(ballSize)) { drawBall(center, size.minDimension / 2f, -12f) }
            Text(
                "urinhos",
                fontFamily = ScoreFont,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                fontSize = fontSize,
                color = Palette.Giz,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Play",
                fontFamily = ScoreFont,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                fontSize = fontSize * 1.25f,
                color = Palette.Pista,
            )
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Palette.Pista,
                modifier = Modifier.size(ballSize * 1.1f),
            )
        }
    }
}
