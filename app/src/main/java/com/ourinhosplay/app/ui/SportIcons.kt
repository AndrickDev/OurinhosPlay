package com.ourinhosplay.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Futevôlei: bola de futebol sobre uma rede de malha (o ícone de vôlei do Material já serve a outras modalidades). */
val FutevoleiIcon: ImageVector by lazy {
    ImageVector.Builder("Futevolei", 24.dp, 24.dp, 24f, 24f).apply {
        // bola com o pentágono vazado (fica parecendo bola de futebol)
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(12f, 2f)
            arcTo(4.6f, 4.6f, 0f, true, true, 12f, 11.2f)
            arcTo(4.6f, 4.6f, 0f, true, true, 12f, 2f)
            close()
            moveTo(12f, 4.8f); lineTo(13.7f, 6.0f); lineTo(13.1f, 8.0f)
            lineTo(10.9f, 8.0f); lineTo(10.3f, 6.0f); close()
        }
        // rede: postes, fita superior e malha em losangos
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.3f, strokeLineCap = StrokeCap.Round) {
            moveTo(3f, 13f); lineTo(3f, 22f)
            moveTo(21f, 13f); lineTo(21f, 22f)
            moveTo(3f, 13f); lineTo(21f, 13f)
            moveTo(3f, 17f); lineTo(7f, 13f)
            moveTo(3f, 21f); lineTo(11f, 13f)
            moveTo(7f, 21f); lineTo(15f, 13f)
            moveTo(11f, 21f); lineTo(19f, 13f)
            moveTo(15f, 21f); lineTo(21f, 15f)
            moveTo(21f, 17f); lineTo(17f, 13f)
            moveTo(21f, 21f); lineTo(13f, 13f)
            moveTo(17f, 21f); lineTo(9f, 13f)
            moveTo(13f, 21f); lineTo(5f, 13f)
            moveTo(9f, 21f); lineTo(3f, 15f)
        }
    }.build()
}

private val Ink = SolidColor(Color.Black)

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, true, false, cx + r, cy)
    arcTo(r, r, 0f, true, false, cx - r, cy)
    close()
}

private fun icon(name: String, build: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(build).build()

/** Futsal: gol com rede e a bola em frente. */
val FutsalIcon: ImageVector by lazy {
    icon("Futsal") {
        path(stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round) {
            moveTo(3f, 14f); lineTo(3f, 4f); lineTo(15f, 4f); lineTo(15f, 14f)
            moveTo(7f, 4f); lineTo(7f, 14f)
            moveTo(11f, 4f); lineTo(11f, 14f)
            moveTo(3f, 9f); lineTo(15f, 9f)
        }
        path(fill = Ink, pathFillType = PathFillType.EvenOdd) {
            circle(17f, 17.5f, 4.2f)
            moveTo(17f, 15.9f); lineTo(18.5f, 17f); lineTo(17.9f, 18.8f); lineTo(16.1f, 18.8f); lineTo(15.5f, 17f); close()
        }
    }
}

/** Biribol: bola de vôlei sobre a água (o jogo é na piscina). */
val BiribolIcon: ImageVector by lazy {
    icon("Biribol") {
        path(stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round) {
            circle(12f, 8f, 5.5f)
            moveTo(12f, 2.5f); lineTo(12f, 13.5f)
            moveTo(6.8f, 6f); quadTo(12f, 9.5f, 17.2f, 6f)
            moveTo(2f, 17.5f); quadTo(5f, 15f, 8f, 17.5f); quadTo(11f, 20f, 14f, 17.5f); quadTo(17f, 15f, 20f, 17.5f)
            moveTo(2f, 21.5f); quadTo(5f, 19f, 8f, 21.5f); quadTo(11f, 24f, 14f, 21.5f); quadTo(17f, 19f, 20f, 21.5f)
        }
    }
}

/** Beach tennis: raquete de palheta sem cordas, bolinha e a linha da areia. */
val BeachTennisIcon: ImageVector by lazy {
    icon("BeachTennis") {
        path(stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round) {
            moveTo(4.5f, 9f)
            arcTo(5.5f, 6.5f, 0f, true, false, 15.5f, 9f)
            arcTo(5.5f, 6.5f, 0f, true, false, 4.5f, 9f)
            close()
            moveTo(10f, 15.5f); lineTo(10f, 19.5f)
            moveTo(2.5f, 22f); lineTo(21.5f, 22f)
        }
        path(fill = Ink) {
            circle(8f, 7f, 0.9f); circle(12f, 7f, 0.9f); circle(10f, 10f, 0.9f); circle(8f, 12.3f, 0.9f); circle(12f, 12.3f, 0.9f)
            circle(18.5f, 6f, 2.2f)
        }
    }
}

/** Padel: pá em gota, cheia de furos. */
val PadelIcon: ImageVector by lazy {
    icon("Padel") {
        path(fill = Ink, pathFillType = PathFillType.EvenOdd) {
            moveTo(12f, 2f)
            curveTo(18f, 2f, 19f, 7f, 16f, 11f)
            lineTo(13.2f, 15f); lineTo(13.2f, 21.5f); lineTo(10.8f, 21.5f); lineTo(10.8f, 15f)
            lineTo(8f, 11f)
            curveTo(5f, 7f, 6f, 2f, 12f, 2f)
            close()
            circle(10f, 5.5f, 0.9f); circle(14f, 5.5f, 0.9f)
            circle(12f, 8.2f, 0.9f)
            circle(9.5f, 10.4f, 0.9f); circle(14.5f, 10.4f, 0.9f)
        }
    }
}

/** Tênis de mesa: raquete redonda de cabo curto e a bolinha. */
val TenisDeMesaIcon: ImageVector by lazy {
    icon("TenisDeMesa") {
        path(fill = Ink) {
            circle(10f, 10f, 6.5f)
            moveTo(8.8f, 15.5f); lineTo(8.8f, 21.5f); lineTo(11.2f, 21.5f); lineTo(11.2f, 15.5f); close()
            circle(19.5f, 5.5f, 2.2f)
        }
    }
}
