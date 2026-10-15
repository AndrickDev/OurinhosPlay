package com.ourinhosplay.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.ourinhosplay.app.data.Sport
import kotlin.math.cos
import kotlin.math.sin

/**
 * Arte da modalidade: o "quadro tático" do esporte — linhas de quadra/pista em tom sobre tom,
 * sangrando para fora do cartão, com a bola (ou o equipamento) como único elemento sólido.
 * Decorativa: fica fora da leitura do TalkBack.
 */
@Composable
fun SportArt(
    sport: Sport?,
    modifier: Modifier = Modifier,
    ink: Color = Palette.Noite,
    paper: Color = Palette.Giz,
) {
    Canvas(modifier.clearAndSetSemantics { }) {
        val line = ink.copy(alpha = 0.20f)
        val solid = paper
        if (sport == null) {
            val r = minOf(size.width, size.height) * 0.42f
            ball(Offset(size.width / 2f, size.height / 2f), r, BallKind.Soccer, ink, solid)
            return@Canvas
        }
        // Área da arte: quadrado ancorado à direita, sangrando para fora.
        val s = size.height * 1.1f
        val c = Offset(size.width - size.height * 0.52f, size.height * 0.5f)
        val w = s * 0.022f
        when (sport) {
            Sport.Futebol, Sport.Futsal -> {
                circle(c, s * 0.34f, line, w)
                drawLine(line, Offset(c.x, c.y - s * 0.62f), Offset(c.x, c.y + s * 0.62f), w)
                // grande área à esquerda
                drawRect(line, Offset(c.x - s * 0.95f, c.y - s * 0.30f), Size(s * 0.36f, s * 0.60f), style = Stroke(w))
                drawArc(line, -53f, 106f, false, Offset(c.x - s * 0.80f, c.y - s * 0.14f), Size(s * 0.28f, s * 0.28f), style = Stroke(w))
                ball(c, s * 0.16f, BallKind.Soccer, ink, solid)
            }
            Sport.Volei, Sport.Biribol -> {
                // rede: faixa quadriculada
                val top = c.y - s * 0.10f
                val bottom = c.y + s * 0.10f
                val left = c.x - s * 0.75f
                val right = c.x + s * 0.75f
                drawRect(line, Offset(left, top), Size(right - left, bottom - top), style = Stroke(w))
                var x = left
                while (x < right) { drawLine(line, Offset(x, top), Offset(x, bottom), w * 0.6f); x += s * 0.06f }
                var y = top
                while (y < bottom) { drawLine(line, Offset(left, y), Offset(right, y), w * 0.6f); y += s * 0.05f }
                drawLine(line, Offset(c.x - s * 0.5f, top - s * 0.55f), Offset(c.x - s * 0.5f, bottom + s * 0.55f), w)
                ball(Offset(c.x + s * 0.05f, c.y - s * 0.30f), s * 0.17f, BallKind.Volley, ink, solid)
            }
            Sport.Basquete -> {
                drawArc(line, 90f, 180f, false, Offset(c.x - s * 0.70f, c.y - s * 0.52f), Size(s * 1.04f, s * 1.04f), style = Stroke(w))
                circle(Offset(c.x - s * 0.18f, c.y), s * 0.18f, line, w)
                drawRect(line, Offset(c.x - s * 0.18f, c.y - s * 0.18f), Size(s * 0.60f, s * 0.36f), style = Stroke(w))
                circle(Offset(c.x + s * 0.42f, c.y), s * 0.05f, line, w * 1.2f)
                ball(Offset(c.x + s * 0.02f, c.y - s * 0.05f), s * 0.17f, BallKind.Basket, ink, solid)
            }
            Sport.Tenis, Sport.Padel, Sport.BeachTennis -> {
                // quadra: retângulo + caixas de saque
                val l = c.x - s * 0.80f
                val t = c.y - s * 0.42f
                drawRect(line, Offset(l, t), Size(s * 1.25f, s * 0.84f), style = Stroke(w))
                drawLine(line, Offset(l, c.y), Offset(l + s * 1.25f, c.y), w)
                drawLine(line, Offset(l + s * 0.40f, t), Offset(l + s * 0.40f, t + s * 0.84f), w)
                // raquete: oval com cordas
                val rc = Offset(c.x + s * 0.18f, c.y - s * 0.02f)
                val rw = s * 0.24f
                val rh = s * 0.30f
                drawOval(line, Offset(rc.x - rw, rc.y - rh), Size(rw * 2, rh * 2), style = Stroke(w * 1.5f))
                clipPath(Path().apply { addOval(Rect(Offset(rc.x - rw, rc.y - rh), Size(rw * 2, rh * 2))) }) {
                    var k = -rw
                    while (k <= rw) { drawLine(line, Offset(rc.x + k, rc.y - rh), Offset(rc.x + k, rc.y + rh), w * 0.5f); k += rw / 4f }
                    var m = -rh
                    while (m <= rh) { drawLine(line, Offset(rc.x - rw, rc.y + m), Offset(rc.x + rw, rc.y + m), w * 0.5f); m += rh / 4f }
                }
                drawLine(line, Offset(rc.x, rc.y + rh), Offset(rc.x, rc.y + rh + s * 0.30f), w * 2.4f, cap = StrokeCap.Round)
                ball(Offset(c.x - s * 0.18f, c.y - s * 0.30f), s * 0.095f, BallKind.Tennis, ink, solid)
            }
            Sport.Natacao -> {
                // raias com ondas
                for (k in -2..2) {
                    val y = c.y + k * s * 0.17f
                    val p = Path().apply {
                        moveTo(c.x - s * 0.95f, y)
                        var x = c.x - s * 0.95f
                        while (x < c.x + s * 0.7f) {
                            quadraticTo(x + s * 0.06f, y - s * 0.035f, x + s * 0.12f, y)
                            quadraticTo(x + s * 0.18f, y + s * 0.035f, x + s * 0.24f, y)
                            x += s * 0.24f
                        }
                    }
                    drawPath(p, line, style = Stroke(w, cap = StrokeCap.Round))
                }
                // óculos de natação
                val g = Offset(c.x - s * 0.22f, c.y - s * 0.30f)
                circle(Offset(g.x - s * 0.10f, g.y), s * 0.085f, solid, w * 2.2f)
                circle(Offset(g.x + s * 0.10f, g.y), s * 0.085f, solid, w * 2.2f)
                drawLine(solid, Offset(g.x - s * 0.015f, g.y), Offset(g.x + s * 0.015f, g.y), w * 2.2f)
            }
            Sport.Corrida -> {
                // pista: raias em curva
                for (k in 0..4) {
                    val r = s * (0.30f + 0.11f * k)
                    drawArc(line, 180f, 180f, false, Offset(c.x - s * 0.15f - r, c.y - r + s * 0.30f), Size(r * 2, r * 2), style = Stroke(w))
                }
                drawLine(line, Offset(c.x - s * 0.15f - s * 0.30f, c.y + s * 0.30f), Offset(c.x - s * 0.15f - s * 0.30f, c.y + s * 0.62f), w)
                // largada: bolinha sólida na primeira raia
                drawCircle(solid, s * 0.055f, Offset(c.x - s * 0.15f + s * 0.30f * cos(-0.9f), c.y + s * 0.30f + s * 0.30f * sin(-0.9f)))
            }
            Sport.Ciclismo -> {
                val r = s * 0.40f
                circle(c, r, line, w * 1.6f)
                circle(c, r * 0.12f, line, w)
                for (k in 0 until 12) {
                    val a = Math.toRadians(30.0 * k)
                    drawLine(line, c, Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat()), w * 0.6f)
                }
                circle(Offset(c.x - s * 0.62f, c.y + s * 0.18f), s * 0.20f, solid, w * 2.2f)
            }
            Sport.Caminhada -> {
                val path = Path().apply {
                    moveTo(c.x - s * 0.9f, c.y + s * 0.45f)
                    cubicTo(c.x - s * 0.5f, c.y + s * 0.55f, c.x - s * 0.5f, c.y - s * 0.05f, c.x - s * 0.1f, c.y)
                    cubicTo(c.x + s * 0.3f, c.y + s * 0.05f, c.x + s * 0.1f, c.y - s * 0.50f, c.x + s * 0.55f, c.y - s * 0.45f)
                }
                drawPath(path, line, style = Stroke(w * 1.4f, cap = StrokeCap.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(w * 3f, w * 4f))))
                // marcador de destino
                circle(Offset(c.x + s * 0.55f, c.y - s * 0.45f), s * 0.07f, solid, w * 2.2f)
                drawCircle(solid, s * 0.025f, Offset(c.x + s * 0.55f, c.y - s * 0.45f))
            }
        }
    }
}

private enum class BallKind { Soccer, Volley, Basket, Tennis }

private fun DrawScope.circle(center: Offset, r: Float, color: Color, width: Float) =
    drawCircle(color, r, center, style = Stroke(width))

/** Bola sólida (papel) com o desenho de cada modalidade em tinta. */
private fun DrawScope.ball(c: Offset, r: Float, kind: BallKind, ink: Color, paper: Color) {
    val w = r * 0.085f
    drawCircle(paper, r, c)
    clipPath(Path().apply { addOval(Rect(c, r)) }) {
        when (kind) {
            BallKind.Soccer -> {
                val pent = Path()
                for (k in 0 until 5) {
                    val a = Math.toRadians(-90.0 + 72.0 * k)
                    val x = c.x + r * 0.34f * cos(a).toFloat()
                    val y = c.y + r * 0.34f * sin(a).toFloat()
                    if (k == 0) pent.moveTo(x, y) else pent.lineTo(x, y)
                }
                pent.close()
                drawPath(pent, ink)
                for (k in 0 until 5) {
                    val a = Math.toRadians(-90.0 + 72.0 * k)
                    val from = Offset(c.x + r * 0.34f * cos(a).toFloat(), c.y + r * 0.34f * sin(a).toFloat())
                    val to = Offset(c.x + r * 1.0f * cos(a).toFloat(), c.y + r * 1.0f * sin(a).toFloat())
                    drawLine(ink, from, to, w * 1.4f)
                    val b = Math.toRadians(-90.0 + 72.0 * k + 36.0)
                    drawCircle(ink, r * 0.17f, Offset(c.x + r * 0.95f * cos(b).toFloat(), c.y + r * 0.95f * sin(b).toFloat()))
                }
            }
            BallKind.Volley -> {
                for (k in 0 until 3) {
                    val a = Math.toRadians(90.0 * k + 20.0).toFloat()
                    drawArc(
                        ink, Math.toDegrees(a.toDouble()).toFloat(), 110f, false,
                        Offset(c.x - r * 1.5f + r * 1.0f * cos(a), c.y - r * 1.5f + r * 1.0f * sin(a)),
                        Size(r * 3f, r * 3f), style = Stroke(w * 1.3f),
                    )
                }
                drawLine(ink, Offset(c.x - r, c.y - r * 0.15f), Offset(c.x + r, c.y + r * 0.15f), w * 1.3f)
            }
            BallKind.Basket -> {
                drawLine(ink, Offset(c.x, c.y - r), Offset(c.x, c.y + r), w * 1.4f)
                drawLine(ink, Offset(c.x - r, c.y), Offset(c.x + r, c.y), w * 1.4f)
                drawArc(ink, -50f, 100f, false, Offset(c.x - r * 2.1f, c.y - r), Size(r * 2f, r * 2f), style = Stroke(w * 1.4f))
                drawArc(ink, 130f, 100f, false, Offset(c.x + r * 0.1f, c.y - r), Size(r * 2f, r * 2f), style = Stroke(w * 1.4f))
            }
            BallKind.Tennis -> {
                drawArc(ink, -40f, 80f, false, Offset(c.x - r * 1.9f, c.y - r * 0.9f), Size(r * 1.8f, r * 1.8f), style = Stroke(w * 1.6f))
                drawArc(ink, 140f, 80f, false, Offset(c.x + r * 0.1f, c.y - r * 0.9f), Size(r * 1.8f, r * 1.8f), style = Stroke(w * 1.6f))
            }
        }
    }
    drawCircle(ink, r, c, style = Stroke(w))
}
