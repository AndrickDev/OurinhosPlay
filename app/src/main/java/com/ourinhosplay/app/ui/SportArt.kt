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

        val cy = c.y + s * 0.06f
        when (sport) {
            Sport.Futebol, Sport.Futsal -> {
                drawRect(floor, Offset(0f, 0f), size)
                circle(c, s * 0.34f, line, w)
                drawLine(line, Offset(c.x, c.y - s * 0.62f), Offset(c.x, c.y + s * 0.62f), w)
                drawRect(line, Offset(c.x - s * 0.95f, c.y - s * 0.30f), Size(s * 0.36f, s * 0.60f), style = Stroke(w))
                drawArc(line, -53f, 106f, false, Offset(c.x - s * 0.80f, c.y - s * 0.14f), Size(s * 0.28f, s * 0.28f), style = Stroke(w))
                ball(c, s * 0.16f, BallKind.Soccer)
            }
            Sport.Futevolei, Sport.BeachTennis -> {
                // Areia, rede baixa vista de lado e a bola/raquete no ar.
                val fy = cy + s * 0.24f
                drawRect(floor, Offset(0f, fy), Size(size.width, size.height - fy))
                drawLine(line, Offset(0f, fy), Offset(size.width, fy), w)
                var dx = 0f
                var row = 0
                while (dx < size.width) {
                    drawCircle(line, w * 0.5f, Offset(dx + (row % 3) * s * 0.03f, fy + s * 0.10f + (row % 4) * s * 0.07f))
                    dx += s * 0.075f
                    row++
                }
                val xl = c.x - s * 0.62f
                val xr = c.x + s * 0.62f
                val top = cy + s * 0.02f
                val bot = cy + s * 0.20f
                post(xl, top - s * 0.06f, fy, w * 2.6f, ink.copy(alpha = 0.40f))
                post(xr, top - s * 0.06f, fy, w * 2.6f, ink.copy(alpha = 0.40f))
                net(xl, xr, top, bot, s * 0.05f, ink.copy(alpha = 0.30f), paper)
                if (sport == Sport.Futevolei) {
                    ball(Offset(c.x + s * 0.02f, cy - s * 0.33f), s * 0.14f, BallKind.Soccer)
                } else {
                    // raquete de beach tennis (face sólida furada) e bolinha
                    val rc = Offset(c.x - s * 0.30f, cy - s * 0.24f)
                    rotate(28f, pivot = rc) {
                        val rw = s * 0.15f
                        val rh = s * 0.20f
                        drawLine(ink.copy(alpha = 0.70f), Offset(rc.x, rc.y + rh * 0.8f), Offset(rc.x, rc.y + rh + s * 0.26f), s * 0.055f, cap = StrokeCap.Round)
                        drawOval(ink.copy(alpha = 0.78f), Offset(rc.x - rw, rc.y - rh), Size(rw * 2, rh * 2))
                        for (i in -2..2) for (j in -3..3) {
                            val p = Offset(rc.x + i * rw * 0.36f, rc.y + j * rh * 0.27f)
                            if ((p.x - rc.x) * (p.x - rc.x) / (rw * 0.78f * rw * 0.78f) + (p.y - rc.y) * (p.y - rc.y) / (rh * 0.78f * rh * 0.78f) < 1f)
                                drawCircle(paper.copy(alpha = 0.55f), s * 0.016f, p)
                        }
                        drawOval(paper.copy(alpha = 0.7f), Offset(rc.x - rw, rc.y - rh), Size(rw * 2, rh * 2), style = Stroke(w * 0.9f))
                    }
                    ball(Offset(c.x + s * 0.18f, cy - s * 0.34f), s * 0.085f, BallKind.Tennis)
                }
            }
            Sport.Volei, Sport.Biribol -> {
                // Quadra coberta: piso, rede alta de lado com fita e antenas, bola na cortada.
                val fy = cy + s * 0.24f
                if (sport == Sport.Biribol) {
                    // Biribol: o mesmo jogo dentro da piscina (água no lugar do piso).
                    drawRect(ink.copy(alpha = 0.16f), Offset(0f, fy - s * 0.10f), Size(size.width, size.height))
                    for (k in 0..2) {
                        val y = fy - s * 0.04f + k * s * 0.09f
                        val p = Path().apply {
                            moveTo(0f, y)
                            var x = 0f
                            while (x < size.width) {
                                quadraticTo(x + s * 0.05f, y - s * 0.025f, x + s * 0.10f, y)
                                quadraticTo(x + s * 0.15f, y + s * 0.025f, x + s * 0.20f, y)
                                x += s * 0.20f
                            }
                        }
                        drawPath(p, paper.copy(alpha = 0.55f), style = Stroke(w * 0.9f, cap = StrokeCap.Round))
                    }
                } else {
                    drawRect(floor, Offset(0f, fy), Size(size.width, size.height - fy))
                    drawLine(line, Offset(0f, fy), Offset(size.width, fy), w)
                    drawLine(line, Offset(0f, fy + s * 0.14f), Offset(size.width, fy + s * 0.14f), w * 0.8f)
                }
                val xl = c.x - s * 0.62f
                val xr = c.x + s * 0.62f
                val top = cy - s * 0.16f
                val bot = cy + s * 0.10f
                post(xl, top - s * 0.10f, fy, w * 2.8f, ink.copy(alpha = 0.42f))
                post(xr, top - s * 0.10f, fy, w * 2.8f, ink.copy(alpha = 0.42f))
                net(xl, xr, top, bot, s * 0.05f, ink.copy(alpha = 0.30f), paper)
                // antenas
                listOf(xl + s * 0.02f, xr - s * 0.02f).forEach { ax ->
                    drawLine(paper, Offset(ax, top - s * 0.24f), Offset(ax, top), w * 1.4f, cap = StrokeCap.Round)
                    drawLine(ink.copy(alpha = 0.55f), Offset(ax, top - s * 0.18f), Offset(ax, top - s * 0.12f), w * 1.4f)
                    drawLine(ink.copy(alpha = 0.55f), Offset(ax, top - s * 0.06f), Offset(ax, top), w * 1.4f)
                }
                // trajetória pontilhada
                drawPath(
                    Path().apply {
                        moveTo(c.x - s * 0.55f, cy - s * 0.12f)
                        quadraticTo(c.x - s * 0.25f, cy - s * 0.62f, c.x + s * 0.04f, cy - s * 0.42f)
                    },
                    line, style = Stroke(w * 1.2f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 2.5f, w * 3.5f))),
                )
                ball(Offset(c.x + s * 0.10f, cy - s * 0.33f), s * 0.14f, BallKind.Volley)
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
            Sport.Tenis, Sport.Padel -> {
                // Quadra vista de lado: piso, linhas, rede baixa com poste, raquete de cordas e bola.
                val fy = cy + s * 0.24f
                drawRect(floor, Offset(0f, fy), Size(size.width, size.height - fy))
                drawLine(line, Offset(0f, fy), Offset(size.width, fy), w)
                drawLine(line, Offset(c.x - s * 0.95f, fy + s * 0.12f), Offset(c.x + s * 0.95f, fy + s * 0.12f), w * 0.8f)
                if (sport == Sport.Padel) {
                    // parede de vidro ao fundo
                    val gl = c.x - s * 0.80f
                    drawRect(line, Offset(gl, cy - s * 0.55f), Size(s * 1.6f, fy - (cy - s * 0.55f)), style = Stroke(w))
                    var gx = gl + s * 0.40f
                    while (gx < gl + s * 1.6f) { drawLine(line, Offset(gx, cy - s * 0.55f), Offset(gx, fy), w * 0.6f); gx += s * 0.40f }
                    drawLine(paper.copy(alpha = 0.35f), Offset(gl + s * 0.1f, cy - s * 0.5f), Offset(gl + s * 0.5f, cy - s * 0.1f), w * 2.4f)
                }
                val xl = c.x - s * 0.70f
                val xr = c.x + s * 0.70f
                val top = cy + s * 0.06f
                val bot = cy + s * 0.22f
                post(xl, top - s * 0.04f, fy, w * 2.4f, ink.copy(alpha = 0.40f))
                post(xr, top - s * 0.04f, fy, w * 2.4f, ink.copy(alpha = 0.40f))
                net(xl, xr, top, bot, s * 0.045f, ink.copy(alpha = 0.30f), paper)
                // raquete
                val rc = Offset(c.x + s * 0.16f, cy - s * 0.26f)
                rotate(-24f, pivot = rc) {
                    val rw = s * 0.14f
                    val rh = s * 0.18f
                    drawLine(ink.copy(alpha = 0.72f), Offset(rc.x, rc.y + rh), Offset(rc.x, rc.y + rh + s * 0.26f), s * 0.05f, cap = StrokeCap.Round)
                    drawLine(ink.copy(alpha = 0.55f), Offset(rc.x - rw * 0.35f, rc.y + rh * 0.85f), Offset(rc.x, rc.y + rh + s * 0.04f), s * 0.025f)
                    drawLine(ink.copy(alpha = 0.55f), Offset(rc.x + rw * 0.35f, rc.y + rh * 0.85f), Offset(rc.x, rc.y + rh + s * 0.04f), s * 0.025f)
                    clipPath(Path().apply { addOval(Rect(Offset(rc.x - rw, rc.y - rh), Size(rw * 2, rh * 2))) }) {
                        var k = -rw
                        while (k <= rw) { drawLine(paper.copy(alpha = 0.55f), Offset(rc.x + k, rc.y - rh), Offset(rc.x + k, rc.y + rh), w * 0.45f); k += rw / 3.5f }
                        var m = -rh
                        while (m <= rh) { drawLine(paper.copy(alpha = 0.55f), Offset(rc.x - rw, rc.y + m), Offset(rc.x + rw, rc.y + m), w * 0.45f); m += rh / 4.5f }
                    }
                    drawOval(ink.copy(alpha = 0.78f), Offset(rc.x - rw, rc.y - rh), Size(rw * 2, rh * 2), style = Stroke(s * 0.03f))
                }
                ball(Offset(c.x - s * 0.30f, cy - s * 0.34f), s * 0.075f, BallKind.Tennis)
            }
            Sport.TenisDeMesa -> {
                val bx = c.x - s * 0.34f
                val fy = cy + s * 0.40f
                drawRect(floor, Offset(0f, fy), Size(size.width, size.height - fy))
                drawLine(line, Offset(0f, fy), Offset(size.width, fy), w)
                // mesa em perspectiva
                val fl = Offset(bx - s * 0.72f, cy + s * 0.14f)
                val fr = Offset(bx + s * 0.50f, cy + s * 0.14f)
                val br = Offset(bx + s * 0.70f, cy - s * 0.06f)
                val bl = Offset(bx - s * 0.52f, cy - s * 0.06f)
                val blue = Color(0xFF2456A6)
                // pernas
                listOf(fl.x + s * 0.04f, fr.x - s * 0.04f).forEach { lx ->
                    drawLine(ink.copy(alpha = 0.50f), Offset(lx, fl.y + s * 0.04f), Offset(lx, fy), s * 0.03f, cap = StrokeCap.Round)
                }
                listOf(bl.x + s * 0.04f, br.x - s * 0.04f).forEach { lx ->
                    drawLine(ink.copy(alpha = 0.30f), Offset(lx, bl.y), Offset(lx, fy - s * 0.04f), s * 0.025f, cap = StrokeCap.Round)
                }
                // lateral frontal (espessura)
                drawPath(Path().apply {
                    moveTo(fl.x, fl.y); lineTo(fr.x, fr.y); lineTo(fr.x, fr.y + s * 0.045f); lineTo(fl.x, fl.y + s * 0.045f); close()
                }, blue.copy(alpha = 0.55f))
                // tampo
                val top = Path().apply { moveTo(fl.x, fl.y); lineTo(fr.x, fr.y); lineTo(br.x, br.y); lineTo(bl.x, bl.y); close() }
                drawPath(top, blue.copy(alpha = 0.82f))
                drawPath(top, paper.copy(alpha = 0.85f), style = Stroke(w * 1.1f))
                val mf = Offset((fl.x + fr.x) / 2f, fl.y)
                val mb = Offset((bl.x + br.x) / 2f, bl.y)
                // linha central (longitudinal) e rede (transversal)
                drawLine(paper.copy(alpha = 0.7f), Offset((fl.x + bl.x) / 2f, (fl.y + bl.y) / 2f), Offset((fr.x + br.x) / 2f, (fr.y + br.y) / 2f), w * 0.8f)
                val h = s * 0.085f
                drawPath(Path().apply {
                    moveTo(mf.x, mf.y); lineTo(mb.x, mb.y); lineTo(mb.x, mb.y - h); lineTo(mf.x, mf.y - h); close()
                }, paper.copy(alpha = 0.22f))
                var t = 0f
                while (t <= 1.001f) {
                    val x = mf.x + (mb.x - mf.x) * t
                    val y = mf.y + (mb.y - mf.y) * t
                    drawLine(paper.copy(alpha = 0.5f), Offset(x, y), Offset(x, y - h), w * 0.5f)
                    t += 0.1f
                }
                drawLine(paper, Offset(mf.x, mf.y - h), Offset(mb.x, mb.y - h), w * 1.6f, cap = StrokeCap.Round)
                // raquete: borracha vermelha, madeira e cabo
                val pc = Offset(bx + s * 0.42f, cy - s * 0.30f)
                rotate(-30f, pivot = pc) {
                    drawRoundRect(Color(0xFFB88A55), Offset(pc.x - s * 0.028f, pc.y + s * 0.10f), Size(s * 0.056f, s * 0.20f), androidx.compose.ui.geometry.CornerRadius(s * 0.025f))
                    drawCircle(Color(0xFF191919), s * 0.145f, pc)
                    drawCircle(
                        Brush.radialGradient(listOf(Color(0xFFFF6B5E), Color(0xFFC62828)), center = Offset(pc.x - s * 0.04f, pc.y - s * 0.05f), radius = s * 0.16f),
                        radius = s * 0.13f, center = pc,
                    )
                }
                // bolinha e rastro
                drawPath(
                    Path().apply { moveTo(bx - s * 0.55f, cy - s * 0.12f); quadraticTo(bx - s * 0.30f, cy - s * 0.50f, bx - s * 0.02f, cy - s * 0.30f) },
                    line, style = Stroke(w * 1.2f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 2.2f, w * 3.2f))),
                )
                ball(Offset(bx + s * 0.06f, cy - s * 0.28f), s * 0.05f, BallKind.PingPong)
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
                val bx = c.x - s * 0.20f
                val r = s * 0.30f
                val rear = Offset(bx - s * 0.45f, c.y + s * 0.10f)
                val front = Offset(bx + s * 0.45f, c.y + s * 0.10f)
                listOf(rear, front).forEach { hub ->
                    circle(hub, r, line, w * 1.8f)
                    circle(hub, r * 0.10f, line, w)
                    for (k in 0 until 10) {
                        val a = Math.toRadians(36.0 * k)
                        drawLine(line, hub, Offset(hub.x + r * cos(a).toFloat(), hub.y + r * sin(a).toFloat()), w * 0.5f)
                    }
                }
                val crank = Offset(bx - s * 0.02f, c.y + s * 0.10f)
                val seat = Offset(bx - s * 0.16f, c.y - s * 0.20f)
                val head = Offset(bx + s * 0.30f, c.y - s * 0.22f)
                val fw = w * 2.6f
                listOf(rear to crank, crank to seat, seat to rear, crank to head, seat to head, head to front).forEach { (a, b) ->
                    drawLine(paper, a, b, fw, cap = StrokeCap.Round)
                }
                drawLine(paper, head, Offset(bx + s * 0.24f, c.y - s * 0.32f), fw, cap = StrokeCap.Round)
                drawLine(paper, Offset(bx + s * 0.20f, c.y - s * 0.32f), Offset(bx + s * 0.30f, c.y - s * 0.32f), fw, cap = StrokeCap.Round)
                drawLine(paper, Offset(bx - s * 0.24f, c.y - s * 0.24f), Offset(bx - s * 0.08f, c.y - s * 0.24f), fw * 1.2f, cap = StrokeCap.Round)
            }
            Sport.Caminhada -> {
                val bx = c.x - s * 0.22f
                val path = Path().apply {
                    moveTo(bx - s * 0.9f, c.y + s * 0.45f)
                    cubicTo(bx - s * 0.5f, c.y + s * 0.55f, bx - s * 0.5f, c.y - s * 0.05f, bx - s * 0.1f, c.y)
                    cubicTo(bx + s * 0.3f, c.y + s * 0.05f, bx + s * 0.1f, c.y - s * 0.34f, bx + s * 0.55f, c.y - s * 0.28f)
                }
                drawPath(path, line, style = Stroke(w * 1.4f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 3f, w * 4f))))
                circle(Offset(bx + s * 0.55f, c.y - s * 0.28f), s * 0.07f, paper, w * 2.2f)
                drawCircle(paper, s * 0.025f, Offset(bx + s * 0.55f, c.y - s * 0.28f))
            }
        }
    }
}

private enum class BallKind { Soccer, Volley, Basket, Tennis, PingPong }

private fun DrawScope.post(x: Float, top: Float, bottom: Float, w: Float, color: Color) =
    drawLine(color, Offset(x, top), Offset(x, bottom), w, cap = StrokeCap.Round)

/** Rede vista de lado: malha, fita branca no topo. */
private fun DrawScope.net(xl: Float, xr: Float, top: Float, bottom: Float, step: Float, mesh: Color, band: Color) {
    val lw = maxOf(1f, step * 0.06f)
    var x = xl
    while (x <= xr + 0.1f) { drawLine(mesh, Offset(x, top), Offset(x, bottom), lw); x += step }
    var y = top
    while (y <= bottom + 0.1f) { drawLine(mesh, Offset(xl, y), Offset(xr, y), lw); y += step }
    drawRoundRect(band, Offset(xl, top - step * 0.5f), Size(xr - xl, step * 0.9f), androidx.compose.ui.geometry.CornerRadius(step * 0.4f))
    drawLine(band.copy(alpha = 0.7f), Offset(xl, bottom), Offset(xr, bottom), lw * 2.2f)
}

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
        BallKind.PingPong -> { base = Color(0xFFFFF6E3); seam = Color(0xFFD9CDB2) }
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
                }
            }
            BallKind.Volley -> {
                val bands = listOf(0f to Color(0xFF2F5DA8), 120f to Color(0xFFF2C230), 240f to Color(0xFF2F5DA8))
                bands.forEach { (deg, col) ->
                    rotate(deg, pivot = c) {
                        val band = Path().apply {
                            moveTo(c.x - r * 1.2f, c.y - r * 0.15f)
                            quadraticTo(c.x, c.y - r * 1.15f, c.x + r * 1.2f, c.y - r * 0.15f)
                        }
                        drawPath(band, col, style = Stroke(r * 0.46f))
                        val edgeUp = Path().apply {
                            moveTo(c.x - r * 1.2f, c.y - r * 0.38f)
                            quadraticTo(c.x, c.y - r * 1.38f, c.x + r * 1.2f, c.y - r * 0.38f)
                        }
                        val edgeDown = Path().apply {
                            moveTo(c.x - r * 1.2f, c.y + r * 0.08f)
                            quadraticTo(c.x, c.y - r * 0.92f, c.x + r * 1.2f, c.y + r * 0.08f)
                        }
                        drawPath(edgeUp, seam, style = Stroke(w * 0.9f))
                        drawPath(edgeDown, seam, style = Stroke(w * 0.9f))
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
            BallKind.PingPong -> Unit
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
