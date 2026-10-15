package com.ourinhosplay.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ourinhosplay.app.data.Sport
import kotlin.math.cos
import kotlin.math.sin

/**
 * Figurinha (sticker) do esporte: um atleta minimalista fazendo o movimento da modalidade,
 * em `ink` com contorno `paper` — como adesivo. `seed` fica na assinatura por compatibilidade.
 */
@Composable
fun Mascot(
    sport: Sport?,
    @Suppress("UNUSED_PARAMETER") seed: String = "",
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    ink: Color = Palette.Noite,
    paper: Color = Palette.Giz,
) {
    Canvas(
        modifier
            .size(size)
            .semantics { contentDescription = "Figurinha de ${sport?.label ?: "esporte"}" },
    ) {
        val u = this.size.minDimension / 100f
        val pose = poseFor(sport)
        // 1) contorno do adesivo, 2) figura
        pose.draw(this, u, ink, paper, outline = true)
        pose.draw(this, u, ink, paper, outline = false)
    }
}

private class Pose(
    val limbs: List<List<Offset>>,
    val head: Offset?,
    val headR: Float = 7f,
    val extras: DrawScope.(u: Float, ink: Color, paper: Color, outline: Boolean) -> Unit = { _, _, _, _ -> },
) {
    fun draw(scope: DrawScope, u: Float, ink: Color, paper: Color, outline: Boolean) = with(scope) {
        val color = if (outline) paper else ink
        val extra = if (outline) 5.5f * u else 0f
        limbs.forEach { pts ->
            val path = Path().apply {
                pts.forEachIndexed { i, p ->
                    if (i == 0) moveTo(p.x * u, p.y * u) else lineTo(p.x * u, p.y * u)
                }
            }
            drawPath(path, color, style = Stroke(7.5f * u + extra, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        head?.let { drawCircle(color, headR * u + extra / 2f, Offset(it.x * u, it.y * u)) }
        extras(u, ink, paper, outline)
    }
}

private fun o(x: Float, y: Float) = Offset(x, y)

private fun DrawScope.ballAt(c: Offset, r: Float, u: Float, ink: Color, paper: Color, outline: Boolean) {
    val center = Offset(c.x * u, c.y * u)
    if (outline) {
        drawCircle(paper, r * u + 2.75f * u, center)
        return
    }
    drawCircle(paper, r * u, center)
    drawCircle(ink, r * u, center, style = Stroke(1.4f * u))
}

private fun poseFor(sport: Sport?): Pose = when (sport) {
    Sport.Futebol, Sport.Futsal -> Pose(
        limbs = listOf(
            listOf(o(46f, 34f), o(50f, 58f)),                      // tronco
            listOf(o(50f, 58f), o(46f, 76f), o(44f, 94f)),         // perna de apoio
            listOf(o(50f, 58f), o(68f, 64f), o(82f, 50f)),         // perna do chute
            listOf(o(46f, 38f), o(30f, 50f), o(20f, 46f)),         // braço de trás
            listOf(o(46f, 38f), o(60f, 46f), o(66f, 40f)),         // braço da frente
        ),
        head = o(45f, 24f),
        extras = { u, ink, paper, outline ->
            ballAt(o(90f, 36f), 7f, u, ink, paper, outline)
            if (!outline) {
                // gomos da bola
                drawCircle(ink, 2.4f * u, Offset(90f * u, 36f * u))
            }
        },
    )
    Sport.Volei, Sport.Biribol -> Pose(
        limbs = listOf(
            listOf(o(44f, 40f), o(46f, 62f)),
            listOf(o(46f, 62f), o(36f, 74f), o(40f, 88f)),
            listOf(o(46f, 62f), o(58f, 72f), o(54f, 86f)),
            listOf(o(44f, 44f), o(54f, 30f), o(64f, 17f)),         // braço da cortada
            listOf(o(44f, 44f), o(30f, 38f), o(24f, 28f)),
        ),
        head = o(43f, 30f),
        extras = { u, ink, paper, outline ->
            ballAt(o(73f, 9f), 7f, u, ink, paper, outline)
            if (!outline) {
                drawArc(ink, 200f, 140f, false, Offset(66f * u, 0f), Size(14f * u, 14f * u), style = Stroke(1.3f * u))
            }
        },
    )
    Sport.Basquete -> Pose(
        limbs = listOf(
            listOf(o(40f, 36f), o(44f, 60f)),
            listOf(o(44f, 60f), o(34f, 74f), o(38f, 90f)),
            listOf(o(44f, 60f), o(58f, 70f), o(60f, 86f)),
            listOf(o(40f, 40f), o(50f, 28f), o(58f, 15f)),
            listOf(o(40f, 40f), o(54f, 36f), o(64f, 24f)),
        ),
        head = o(39f, 26f),
        extras = { u, ink, paper, outline ->
            ballAt(o(66f, 10f), 7f, u, ink, paper, outline)
            if (!outline) {
                drawLine(ink, Offset(59f * u, 10f * u), Offset(73f * u, 10f * u), strokeWidth = 1.3f * u)
                drawLine(ink, Offset(66f * u, 3f * u), Offset(66f * u, 17f * u), strokeWidth = 1.3f * u)
            }
            // aro e tabela
            val rim = if (outline) paper else ink
            val w = if (outline) 6f * u else 2.4f * u
            drawLine(rim, Offset(78f * u, 22f * u), Offset(96f * u, 22f * u), strokeWidth = w, cap = StrokeCap.Round)
            drawLine(rim, Offset(96f * u, 8f * u), Offset(96f * u, 28f * u), strokeWidth = w, cap = StrokeCap.Round)
        },
    )
    Sport.Tenis, Sport.Padel, Sport.BeachTennis -> Pose(
        limbs = listOf(
            listOf(o(42f, 34f), o(44f, 58f)),
            listOf(o(44f, 58f), o(36f, 76f), o(32f, 94f)),
            listOf(o(44f, 58f), o(56f, 76f), o(62f, 92f)),
            listOf(o(42f, 38f), o(58f, 34f), o(72f, 24f)),         // braço da raquete
            listOf(o(42f, 38f), o(30f, 30f), o(26f, 18f)),
        ),
        head = o(41f, 24f),
        extras = { u, ink, paper, outline ->
            // raquete
            val c = if (outline) paper else ink
            val w = if (outline) 6.5f * u else 2.6f * u
            drawLine(c, Offset(72f * u, 24f * u), Offset(80f * u, 14f * u), strokeWidth = w, cap = StrokeCap.Round)
            drawOval(c, Offset(76f * u, 0f), Size(18f * u, 22f * u), style = Stroke(w))
            ballAt(o(26f, 8f), 4.5f, u, ink, paper, outline)
        },
    )
    Sport.Natacao -> Pose(
        limbs = listOf(
            listOf(o(36f, 54f), o(70f, 50f)),
            listOf(o(40f, 52f), o(48f, 36f), o(62f, 33f)),         // braço no ar
            listOf(o(40f, 55f), o(30f, 66f), o(22f, 62f)),
            listOf(o(70f, 50f), o(84f, 57f), o(95f, 52f)),
            listOf(o(70f, 50f), o(84f, 43f), o(95f, 47f)),
        ),
        head = o(29f, 51f),
        headR = 6.5f,
        extras = { u, ink, paper, outline ->
            val c = if (outline) paper else ink
            val w = if (outline) 5.5f * u else 2.2f * u
            val wave = Path().apply {
                moveTo(4f * u, 74f * u)
                var x = 4f
                while (x < 94f) {
                    quadraticTo((x + 6f) * u, 68f * u, (x + 12f) * u, 74f * u)
                    x += 12f
                }
            }
            drawPath(wave, c, style = Stroke(w, cap = StrokeCap.Round))
        },
    )
    Sport.Corrida -> Pose(
        limbs = listOf(
            listOf(o(52f, 33f), o(46f, 56f)),
            listOf(o(46f, 56f), o(62f, 66f), o(52f, 86f)),
            listOf(o(46f, 56f), o(32f, 70f), o(18f, 64f)),
            listOf(o(52f, 38f), o(66f, 46f), o(76f, 38f)),
            listOf(o(52f, 38f), o(38f, 44f), o(32f, 54f)),
        ),
        head = o(55f, 23f),
        extras = { u, ink, paper, outline ->
            val c = if (outline) paper else ink
            val w = if (outline) 5.5f * u else 2f * u
            listOf(26f to 20f, 34f to 30f, 22f to 40f).forEach { (y, len) ->
                drawLine(c, Offset((12f) * u, y * u + 6f * u), Offset((12f + len) * u, y * u + 6f * u), strokeWidth = w, cap = StrokeCap.Round)
            }
        },
    )
    Sport.Ciclismo -> Pose(
        limbs = listOf(
            listOf(o(24f, 74f), o(46f, 74f), o(58f, 52f), o(40f, 52f), o(46f, 74f)),   // quadro
            listOf(o(58f, 52f), o(66f, 48f)),                                           // guidão
            listOf(o(44f, 50f), o(58f, 36f)),                                           // tronco
            listOf(o(58f, 38f), o(68f, 48f)),                                           // braço
            listOf(o(44f, 50f), o(54f, 64f), o(47f, 76f)),                              // perna
        ),
        head = o(63f, 26f),
        extras = { u, ink, paper, outline ->
            val c = if (outline) paper else ink
            val w = if (outline) 6f * u else 2.4f * u
            drawCircle(c, 14f * u, Offset(24f * u, 74f * u), style = Stroke(w))
            drawCircle(c, 14f * u, Offset(76f * u, 74f * u), style = Stroke(w))
        },
    )
    Sport.Caminhada -> Pose(
        limbs = listOf(
            listOf(o(46f, 33f), o(46f, 58f)),
            listOf(o(46f, 58f), o(56f, 76f), o(58f, 94f)),
            listOf(o(46f, 58f), o(38f, 76f), o(32f, 92f)),
            listOf(o(46f, 38f), o(58f, 50f), o(64f, 56f)),
            listOf(o(46f, 38f), o(36f, 50f), o(32f, 60f)),
            listOf(o(66f, 40f), o(70f, 96f)),                      // bastão
        ),
        head = o(46f, 23f),
    )
    null -> Pose(
        limbs = emptyList(),
        head = null,
        extras = { u, ink, paper, outline ->
            val c = Offset(50f * u, 50f * u)
            if (outline) {
                drawCircle(paper, 33f * u, c)
            } else {
                drawCircle(paper, 28f * u, c)
                drawCircle(ink, 28f * u, c, style = Stroke(2.4f * u))
                for (k in 0 until 5) {
                    val a = Math.toRadians(-90.0 + 72.0 * k)
                    drawLine(
                        ink,
                        Offset(c.x + 10f * u * cos(a).toFloat(), c.y + 10f * u * sin(a).toFloat()),
                        Offset(c.x + 28f * u * cos(a).toFloat(), c.y + 28f * u * sin(a).toFloat()),
                        strokeWidth = 2f * u,
                    )
                }
                drawCircle(ink, 10f * u, c)
            }
        },
    )
}
