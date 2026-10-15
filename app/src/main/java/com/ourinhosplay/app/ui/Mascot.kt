package com.ourinhosplay.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ourinhosplay.app.data.Sport
import kotlin.math.abs

/**
 * Bichinho minimalista do esporte: um animal sorteado pela `seed` (id do jogo, por exemplo),
 * com rosto variando (olhos, boca, bochecha, faixa) e o equipamento do esporte.
 * Duas cores só: `ink` (silhueta) e `paper` (detalhes).
 */
@Composable
fun Mascot(
    sport: Sport?,
    seed: String,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    ink: Color = Palette.Noite,
    paper: Color = Palette.Giz,
) {
    val h = remember(seed) { abs(seed.hashCode().toLong()).toInt() }
    val animal = h % 6
    val eyes = (h / 6) % 3
    val smile = (h / 18) % 2 == 0
    val blush = (h / 36) % 2 == 0
    val band = (h / 72) % 3 == 0

    Canvas(
        modifier
            .size(size)
            .semantics { contentDescription = "Ilustração de um bichinho jogando ${sport?.label ?: ""}".trim() },
    ) {
        val u = this.size.minDimension / 100f
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        fun tri(a: Offset, b: Offset, c: Offset) = Path().apply {
            moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close()
        }

        // corpo
        drawOval(ink, p(22f, 64f), Size(56f * u, 50f * u))

        // orelhas
        when (animal) {
            0 -> { // gato
                drawPath(tri(p(29f, 28f), p(27f, 6f), p(45f, 18f)), ink)
                drawPath(tri(p(71f, 28f), p(73f, 6f), p(55f, 18f)), ink)
            }
            1 -> { // cachorro (orelhas caídas)
                drawOval(ink, p(19f, 20f), Size(15f * u, 30f * u))
                drawOval(ink, p(66f, 20f), Size(15f * u, 30f * u))
            }
            2 -> { // urso
                drawCircle(ink, 9f * u, p(30f, 20f))
                drawCircle(ink, 9f * u, p(70f, 20f))
            }
            3 -> { // raposa
                drawPath(tri(p(27f, 30f), p(22f, 0f), p(46f, 18f)), ink)
                drawPath(tri(p(73f, 30f), p(78f, 0f), p(54f, 18f)), ink)
            }
            4 -> { // coelho
                drawOval(ink, p(35f, -4f), Size(11f * u, 34f * u))
                drawOval(ink, p(54f, -4f), Size(11f * u, 34f * u))
            }
            else -> { // coruja
                drawPath(tri(p(30f, 26f), p(33f, 6f), p(46f, 18f)), ink)
                drawPath(tri(p(70f, 26f), p(67f, 6f), p(54f, 18f)), ink)
            }
        }

        // cabeça
        drawCircle(ink, 25f * u, p(50f, 40f))

        // focinho
        if (animal != 5) {
            drawOval(paper, p(39f, 45f), Size(22f * u, 15f * u))
            drawOval(ink, p(46.5f, 47f), Size(7f * u, 5f * u))
        } else {
            drawCircle(paper, 8f * u, p(40f, 38f))
            drawCircle(paper, 8f * u, p(60f, 38f))
        }

        // olhos
        val eyeL = p(41f, 37f)
        val eyeR = p(59f, 37f)
        if (animal == 5) {
            drawCircle(ink, 3.5f * u, eyeL); drawCircle(ink, 3.5f * u, eyeR)
        } else when (eyes) {
            0 -> { drawCircle(paper, 3f * u, eyeL); drawCircle(paper, 3f * u, eyeR) }
            1 -> {
                for (c in listOf(eyeL, eyeR)) {
                    drawArc(
                        paper, 200f, 140f, false,
                        topLeft = Offset(c.x - 4f * u, c.y - 3f * u), size = Size(8f * u, 8f * u),
                        style = Stroke(2.2f * u, cap = StrokeCap.Round),
                    )
                }
            }
            else -> {
                for (c in listOf(eyeL, eyeR)) {
                    drawCircle(paper, 5f * u, c)
                    drawCircle(ink, 2.2f * u, c)
                }
            }
        }

        // boca
        if (animal != 5) {
            drawArc(
                ink, if (smile) 20f else 200f, 140f, false,
                topLeft = Offset(46f * u, 52f * u), size = Size(8f * u, 6f * u),
                style = Stroke(1.6f * u, cap = StrokeCap.Round),
            )
        }
        if (blush) {
            drawCircle(paper.copy(alpha = 0.35f), 4f * u, p(32f, 47f))
            drawCircle(paper.copy(alpha = 0.35f), 4f * u, p(68f, 47f))
        }
        if (band) {
            drawLine(paper, p(27f, 26f), p(73f, 26f), strokeWidth = 4f * u, cap = StrokeCap.Round)
        }

        drawEquipment(sport, u, ink, paper)
    }
}

private fun DrawScope.drawEquipment(sport: Sport?, u: Float, ink: Color, paper: Color) {
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    val c = p(76f, 76f)
    val r = 14f * u
    val line = 1.8f * u
    when (sport) {
        Sport.Futebol, Sport.Futsal -> {
            drawCircle(paper, r, c)
            val pent = Path().apply {
                for (k in 0 until 5) {
                    val a = Math.toRadians(-90.0 + 72.0 * k)
                    val x = c.x + 5f * u * kotlin.math.cos(a).toFloat()
                    val y = c.y + 5f * u * kotlin.math.sin(a).toFloat()
                    if (k == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(pent, ink)
            for (k in 0 until 5) {
                val a = Math.toRadians(-90.0 + 72.0 * k)
                drawLine(
                    ink,
                    Offset(c.x + 5f * u * kotlin.math.cos(a).toFloat(), c.y + 5f * u * kotlin.math.sin(a).toFloat()),
                    Offset(c.x + r * kotlin.math.cos(a).toFloat(), c.y + r * kotlin.math.sin(a).toFloat()),
                    strokeWidth = line,
                )
            }
        }
        Sport.Volei, Sport.Biribol -> {
            drawCircle(paper, r, c)
            drawArc(ink, 200f, 140f, false, Offset(c.x - r, c.y - r * 1.5f), Size(r * 2, r * 2), style = Stroke(line))
            drawArc(ink, 20f, 140f, false, Offset(c.x - r, c.y - r * 0.5f), Size(r * 2, r * 2), style = Stroke(line))
            drawLine(ink, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = line)
        }
        Sport.Basquete -> {
            drawCircle(paper, r, c)
            drawLine(ink, Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokeWidth = line)
            drawLine(ink, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = line)
            drawArc(ink, -60f, 120f, false, Offset(c.x - r * 2f, c.y - r), Size(r * 2, r * 2), style = Stroke(line))
            drawArc(ink, 120f, 120f, false, Offset(c.x, c.y - r), Size(r * 2, r * 2), style = Stroke(line))
        }
        Sport.Tenis, Sport.Padel, Sport.BeachTennis -> {
            drawOval(paper, p(63f, 56f), Size(26f * u, 32f * u), style = Stroke(2.6f * u))
            drawLine(paper, p(76f, 88f), p(76f, 108f), strokeWidth = 3.4f * u, cap = StrokeCap.Round)
            drawLine(paper, p(76f, 58f), p(76f, 86f), strokeWidth = 0.9f * u)
            drawLine(paper, p(65f, 72f), p(87f, 72f), strokeWidth = 0.9f * u)
        }
        Sport.Natacao -> {
            val l = p(41f, 37f)
            val r2 = p(59f, 37f)
            drawCircle(paper, 6.5f * u, l, style = Stroke(2.2f * u))
            drawCircle(paper, 6.5f * u, r2, style = Stroke(2.2f * u))
            drawLine(paper, p(47.5f, 37f), p(52.5f, 37f), strokeWidth = 2.2f * u)
            drawLine(paper, p(34.5f, 37f), p(26f, 33f), strokeWidth = 2.2f * u)
            drawLine(paper, p(65.5f, 37f), p(74f, 33f), strokeWidth = 2.2f * u)
        }
        Sport.Corrida -> {
            drawRoundRect(paper, p(40f, 74f), Size(20f * u, 18f * u), androidx.compose.ui.geometry.CornerRadius(2f * u))
            drawLine(ink, p(45f, 80f), p(55f, 80f), strokeWidth = line)
            drawLine(ink, p(45f, 85f), p(55f, 85f), strokeWidth = line)
        }
        Sport.Ciclismo -> {
            drawCircle(paper, r, c, style = Stroke(2.4f * u))
            for (k in 0 until 4) {
                val a = Math.toRadians(45.0 * k)
                drawLine(
                    paper,
                    Offset(c.x - r * kotlin.math.cos(a).toFloat(), c.y - r * kotlin.math.sin(a).toFloat()),
                    Offset(c.x + r * kotlin.math.cos(a).toFloat(), c.y + r * kotlin.math.sin(a).toFloat()),
                    strokeWidth = 0.9f * u,
                )
            }
        }
        Sport.Caminhada -> {
            drawLine(paper, p(80f, 52f), p(88f, 100f), strokeWidth = 3.4f * u, cap = StrokeCap.Round)
            drawLine(paper, p(80f, 52f), p(73f, 55f), strokeWidth = 3.4f * u, cap = StrokeCap.Round)
        }
        null -> Unit
    }
}
