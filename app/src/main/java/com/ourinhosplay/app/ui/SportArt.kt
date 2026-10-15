package com.ourinhosplay.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.ourinhosplay.app.data.Sport
import kotlin.math.cos
import kotlin.math.sin

/**
 * Arte da modalidade: o "quadro tático" do esporte — piso e linhas de quadra/pista em tom sobre
 * tom, sangrando para fora do cartão, com a bola (renderizada com volume e sombra) como único
 * elemento sólido. Decorativa: fica fora da leitura do TalkBack.
 */
@Composable
fun SportArt(
    sport: Sport?,
    modifier: Modifier = Modifier,
    ink: Color = Palette.Noite,
    paper: Color = Palette.Giz,
) {
    Canvas(modifier.clearAndSetSemantics { }) {
        val line = ink.copy(alpha = 0.24f)
        val floor = ink.copy(alpha = 0.09f)
        if (sport == null) {
            val r = minOf(size.width, size.height) * 0.38f
            ball(Offset(size.width / 2f, size.height * 0.46f), r, BallKind.Soccer)
            return@Canvas
        }
        // Área da arte: ancorada à direita, sangrando para fora.
        val s = size.height * 1.1f
        val c = Offset(size.width - size.height * 0.52f, size.height * 0.5f)
        val w = s * 0.022f

        // brilho suave atrás da bola
        drawCircle(
            Brush.radialGradient(
                listOf(paper.copy(alpha = 0.30f), Color.Transparent),
                center = c, radius = s * 0.62f,
            ),
            radius = s * 0.62f, center = c,
        )

        when (sport) {
            Sport.Futebol, Sport.Futsal -> {
                drawRect(floor, Offset(0f, 0f), size)
                circle(c, s * 0.34f, line, w)
                drawLine(line, Offset(c.x, c.y - s * 0.62f), Offset(c.x, c.y + s * 0.62f), w)
                drawRect(line, Offset(c.x - s * 0.95f, c.y - s * 0.30f), Size(s * 0.36f, s * 0.60f), style = Stroke(w))
                drawArc(line, -53f, 106f, false, Offset(c.x - s * 0.80f, c.y - s * 0.14f), Size(s * 0.28f, s * 0.28f), style = Stroke(w))
                ball(c, s * 0.16f, BallKind.Soccer)
            }
            Sport.Futevolei -> {
                // areia, rede e bola de futebol no ar
                drawRect(floor, Offset(0f, c.y + s * 0.18f), Size(size.width, size.height))
                var x = c.x - s * 0.9f
                while (x < c.x + s * 0.9f) {
                    drawCircle(line, w * 0.45f, Offset(x, c.y + s * 0.30f + (x % (s * 0.13f)) * 0.9f))
                    drawCircle(line, w * 0.45f, Offset(x + s * 0.045f, c.y + s * 0.48f + (x % (s * 0.09f))))
                    x += s * 0.09f
                }
                val top = c.y - s * 0.02f
                val bottom = c.y + s * 0.22f
                val left = c.x - s * 0.55f
                val right = c.x + s * 0.55f
                drawLine(line, Offset(left, top - s * 0.04f), Offset(left, c.y + s * 0.30f), w * 1.6f)
                drawLine(line, Offset(right, top - s * 0.04f), Offset(right, c.y + s * 0.30f), w * 1.6f)
                var gx = left
                while (gx <= right) { drawLine(line, Offset(gx, top), Offset(gx, bottom), w * 0.55f); gx += s * 0.055f }
                var gy = top
                while (gy <= bottom) { drawLine(line, Offset(left, gy), Offset(right, gy), w * 0.55f); gy += s * 0.055f }
                drawLine(line, Offset(left, top), Offset(right, top), w * 1.4f)
                ball(Offset(c.x - s * 0.04f, c.y - s * 0.34f), s * 0.15f, BallKind.Soccer)
            }
            Sport.Volei, Sport.Biribol -> {
                drawRect(floor, Offset(0f, 0f), size)
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
                ball(Offset(c.x + s * 0.05f, c.y - s * 0.30f), s * 0.17f, BallKind.Volley)
            }
            Sport.Basquete -> {
                // piso de madeira: tábuas
                drawRect(floor, Offset(0f, 0f), size)
                var py = 0f
                while (py < size.height) { drawLine(floor, Offset(0f, py), Offset(size.width, py), w * 0.5f); py += s * 0.10f }
                drawArc(line, 90f, 180f, false, Offset(c.x - s * 0.70f, c.y - s * 0.52f), Size(s * 1.04f, s * 1.04f), style = Stroke(w))
                circle(Offset(c.x - s * 0.18f, c.y), s * 0.18f, line, w)
                drawRect(line, Offset(c.x - s * 0.18f, c.y - s * 0.18f), Size(s * 0.60f, s * 0.36f), style = Stroke(w))
                circle(Offset(c.x + s * 0.42f, c.y), s * 0.05f, line, w * 1.2f)
                ball(Offset(c.x + s * 0.02f, c.y - s * 0.05f), s * 0.17f, BallKind.Basket)
            }
            Sport.Tenis, Sport.Padel, Sport.BeachTennis -> {
                val l = c.x - s * 0.80f
                val t = c.y - s * 0.42f
                drawRect(floor, Offset(l, t), Size(s * 1.25f, s * 0.84f))
                drawRect(line, Offset(l, t), Size(s * 1.25f, s * 0.84f), style = Stroke(w))
                drawLine(line, Offset(l, c.y), Offset(l + s * 1.25f, c.y), w)
                drawLine(line, Offset(l + s * 0.40f, t), Offset(l + s * 0.40f, t + s * 0.84f), w)
                val rc = Offset(c.x + s * 0.18f, c.y - s * 0.02f)
                val rw = s * 0.24f
                val rh = s * 0.30f
                drawOval(line, Offset(rc.x - rw, rc.y - rh), Size(rw * 2, rh * 2), style = Stroke(w * 1.8f))
                clipPath(Path().apply { addOval(Rect(Offset(rc.x - rw, rc.y - rh), Size(rw * 2, rh * 2))) }) {
                    var k = -rw
                    while (k <= rw) { drawLine(line, Offset(rc.x + k, rc.y - rh), Offset(rc.x + k, rc.y + rh), w * 0.5f); k += rw / 4f }
                    var m = -rh
                    while (m <= rh) { drawLine(line, Offset(rc.x - rw, rc.y + m), Offset(rc.x + rw, rc.y + m), w * 0.5f); m += rh / 4f }
                }
                drawLine(line, Offset(rc.x, rc.y + rh), Offset(rc.x, rc.y + rh + s * 0.30f), w * 2.6f, cap = StrokeCap.Round)
                ball(Offset(c.x - s * 0.18f, c.y - s * 0.30f), s * 0.095f, BallKind.Tennis)
            }
            Sport.Natacao -> {
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
                val g = Offset(c.x - s * 0.22f, c.y - s * 0.30f)
                circle(Offset(g.x - s * 0.10f, g.y), s * 0.085f, paper, w * 2.2f)
                circle(Offset(g.x + s * 0.10f, g.y), s * 0.085f, paper, w * 2.2f)
                drawLine(paper, Offset(g.x - s * 0.015f, g.y), Offset(g.x + s * 0.015f, g.y), w * 2.2f)
            }
            Sport.Corrida -> {
                for (k in 0..4) {
                    val r = s * (0.30f + 0.11f * k)
                    drawArc(line, 180f, 180f, false, Offset(c.x - s * 0.15f - r, c.y - r + s * 0.30f), Size(r * 2, r * 2), style = Stroke(w))
                }
                drawLine(line, Offset(c.x - s * 0.15f - s * 0.30f, c.y + s * 0.30f), Offset(c.x - s * 0.15f - s * 0.30f, c.y + s * 0.62f), w)
                drawCircle(paper, s * 0.055f, Offset(c.x - s * 0.15f + s * 0.30f * cos(-0.9f), c.y + s * 0.30f + s * 0.30f * sin(-0.9f)))
            }
            Sport.Ciclismo -> {
                val r = s * 0.40f
                circle(c, r, line, w * 1.6f)
                circle(c, r * 0.12f, line, w)
                for (k in 0 until 12) {
                    val a = Math.toRadians(30.0 * k)
                    drawLine(line, c, Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat()), w * 0.6f)
                }
                circle(Offset(c.x - s * 0.62f, c.y + s * 0.18f), s * 0.20f, paper, w * 2.2f)
            }
            Sport.Caminhada -> {
                val path = Path().apply {
                    moveTo(c.x - s * 0.9f, c.y + s * 0.45f)
                    cubicTo(c.x - s * 0.5f, c.y + s * 0.55f, c.x - s * 0.5f, c.y - s * 0.05f, c.x - s * 0.1f, c.y)
                    cubicTo(c.x + s * 0.3f, c.y + s * 0.05f, c.x + s * 0.1f, c.y - s * 0.50f, c.x + s * 0.55f, c.y - s * 0.45f)
                }
                drawPath(path, line, style = Stroke(w * 1.4f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 3f, w * 4f))))
                circle(Offset(c.x + s * 0.55f, c.y - s * 0.45f), s * 0.07f, paper, w * 2.2f)
                drawCircle(paper, s * 0.025f, Offset(c.x + s * 0.55f, c.y - s * 0.45f))
            }
        }
    }
}

private enum class BallKind { Soccer, Volley, Basket, Tennis }

private fun DrawScope.circle(center: Offset, r: Float, color: Color, width: Float) =
    drawCircle(color, r, center, style = Stroke(width))

/**
 * Bola com volume: cor e costuras próprias de cada modalidade, luz vinda do alto à esquerda,
 * borda escurecida e sombra no chão.
 */
private fun DrawScope.ball(c: Offset, r: Float, kind: BallKind) {
    // sombra no chão
    val sc = Offset(c.x + r * 0.18f, c.y + r * 1.08f)
    scale(1f, 0.22f, pivot = sc) {
        drawCircle(
            Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.40f), Color.Transparent), center = sc, radius = r * 1.2f),
            radius = r * 1.2f, center = sc,
        )
    }

    val base: Color
    val seam: Color
    when (kind) {
        BallKind.Soccer -> { base = Color(0xFFF5F5F1); seam = Color(0xFF14161E) }
        BallKind.Volley -> { base = Color(0xFFF3EBD0); seam = Color(0xFF1F3F86) }
        BallKind.Basket -> { base = Color(0xFFE4701F); seam = Color(0xFF2B1608) }
        BallKind.Tennis -> { base = Color(0xFFD3E63A); seam = Color(0xFFF7F8EE) }
    }
    drawCircle(base, r, c)

    val w = r * 0.07f
    clipPath(Path().apply { addOval(Rect(c, r)) }) {
        when (kind) {
            BallKind.Soccer -> {
                val pent = Path()
                for (k in 0 until 5) {
                    val a = Math.toRadians(-90.0 + 72.0 * k)
                    val x = c.x + r * 0.33f * cos(a).toFloat()
                    val y = c.y + r * 0.33f * sin(a).toFloat()
                    if (k == 0) pent.moveTo(x, y) else pent.lineTo(x, y)
                }
                pent.close()
                drawPath(pent, seam)
                for (k in 0 until 5) {
                    val a = Math.toRadians(-90.0 + 72.0 * k)
                    val from = Offset(c.x + r * 0.33f * cos(a).toFloat(), c.y + r * 0.33f * sin(a).toFloat())
                    val to = Offset(c.x + r * 0.64f * cos(a).toFloat(), c.y + r * 0.64f * sin(a).toFloat())
                    drawLine(seam, from, to, w * 1.3f)
                    // pentágono do contorno, cortado pela borda
                    val oc = Offset(c.x + r * 0.92f * cos(a).toFloat(), c.y + r * 0.92f * sin(a).toFloat())
                    val op = Path()
                    for (j in 0 until 5) {
                        val b = a + Math.PI + j * 2 * Math.PI / 5
                        val x = oc.x + r * 0.30f * cos(b).toFloat()
                        val y = oc.y + r * 0.30f * sin(b).toFloat()
                        if (j == 0) op.moveTo(x, y) else op.lineTo(x, y)
                    }
                    op.close()
                    drawPath(op, seam)
                    // costuras entre os pentágonos do contorno
                    val n = Math.toRadians(-90.0 + 72.0 * k + 36.0)
                    drawLine(
                        seam,
                        Offset(c.x + r * 0.62f * cos(n).toFloat(), c.y + r * 0.62f * sin(n).toFloat()),
                        Offset(c.x + r * 1.0f * cos(n).toFloat(), c.y + r * 1.0f * sin(n).toFloat()),
                        w * 1.2f,
                    )
                }
            }
            BallKind.Volley -> {
                val bands = listOf(0f to Color(0xFF2F5DA8), 120f to Color(0xFFF2C230), 240f to Color(0xFF2F5DA8))
                bands.forEach { (deg, col) ->
                    rotate(deg, pivot = c) {
                        drawArc(col, 215f, 110f, false, Offset(c.x - r * 1.9f, c.y - r * 1.9f - r * 0.1f), Size(r * 3.8f, r * 3.8f), style = Stroke(r * 0.42f))
                    }
                }
                bands.forEach { (deg, _) ->
                    rotate(deg, pivot = c) {
                        val top = Offset(c.x - r * 1.9f, c.y - r * 1.9f - r * 0.1f)
                        drawArc(seam, 215f, 110f, false, Offset(top.x, top.y - r * 0.21f), Size(r * 3.8f, r * 3.8f), style = Stroke(w))
                        drawArc(seam, 215f, 110f, false, Offset(top.x, top.y + r * 0.21f), Size(r * 3.8f, r * 3.8f), style = Stroke(w))
                    }
                }
            }
            BallKind.Basket -> {
                drawLine(seam, Offset(c.x, c.y - r), Offset(c.x, c.y + r), w * 1.5f)
                drawLine(seam, Offset(c.x - r, c.y), Offset(c.x + r, c.y), w * 1.5f)
                drawArc(seam, -52f, 104f, false, Offset(c.x - r * 2.15f, c.y - r * 1.05f), Size(r * 2.1f, r * 2.1f), style = Stroke(w * 1.5f))
                drawArc(seam, 128f, 104f, false, Offset(c.x + r * 0.05f, c.y - r * 1.05f), Size(r * 2.1f, r * 2.1f), style = Stroke(w * 1.5f))
                // textura de gomos
                for (i in -3..3) {
                    for (j in -3..3) {
                        val p = Offset(c.x + i * r * 0.27f, c.y + j * r * 0.27f)
                        drawCircle(Color.Black.copy(alpha = 0.10f), r * 0.025f, p)
                    }
                }
            }
            BallKind.Tennis -> {
                drawArc(seam, -48f, 96f, false, Offset(c.x - r * 1.95f, c.y - r * 0.95f), Size(r * 1.9f, r * 1.9f), style = Stroke(w * 1.9f))
                drawArc(seam, 132f, 96f, false, Offset(c.x + r * 0.05f, c.y - r * 0.95f), Size(r * 1.9f, r * 1.9f), style = Stroke(w * 1.9f))
            }
        }
        // volume: luz do alto à esquerda + borda escura
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.50f), Color.Transparent),
                center = Offset(c.x - r * 0.38f, c.y - r * 0.42f), radius = r * 0.85f,
            ),
            radius = r, center = c,
        )
        drawCircle(
            Brush.radialGradient(
                0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.42f),
                center = Offset(c.x - r * 0.15f, c.y - r * 0.18f), radius = r * 1.25f,
            ),
            radius = r, center = c,
        )
    }
}
