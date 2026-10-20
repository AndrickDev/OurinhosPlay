package com.ourinhosplay.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.ourinhosplay.app.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material.icons.rounded.SportsVolleyball
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ourinhosplay.app.data.MatchCard
import com.ourinhosplay.app.data.Sport
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val Sport.color: Color
    get() = when (this) {
        Sport.Futebol -> Palette.Grama
        Sport.Futevolei -> Palette.Limao
        Sport.Volei -> Palette.Sol
        Sport.Basquete -> Palette.Madeira
        Sport.BeachTennis -> Palette.Mar
        Sport.Futsal -> Palette.Teal
        Sport.Tenis -> Palette.Lima
        Sport.Padel -> Palette.Lavanda
        Sport.Natacao -> Palette.Azul
        Sport.Biribol -> Palette.Rosa
        Sport.Corrida -> Palette.Coral
        Sport.Ciclismo -> Palette.Roxo
        Sport.Caminhada -> Palette.Areia
        Sport.TenisDeMesa -> Palette.Fucsia
    }

val Sport.icon: ImageVector
    get() = when (this) {
        Sport.Futebol -> Icons.Rounded.SportsSoccer
        Sport.Futevolei -> Icons.Rounded.SportsVolleyball
        Sport.Volei -> Icons.Rounded.SportsVolleyball
        Sport.Basquete -> Icons.Rounded.SportsBasketball
        Sport.BeachTennis -> Icons.Rounded.SportsTennis
        Sport.Futsal -> Icons.Rounded.SportsSoccer
        Sport.Tenis -> Icons.Rounded.SportsTennis
        Sport.Padel -> Icons.Rounded.SportsTennis
        Sport.Natacao -> Icons.Rounded.Pool
        Sport.Biribol -> Icons.Rounded.SportsVolleyball
        Sport.Corrida -> Icons.AutoMirrored.Rounded.DirectionsRun
        Sport.Ciclismo -> Icons.Rounded.DirectionsBike
        Sport.Caminhada -> Icons.AutoMirrored.Rounded.DirectionsWalk
        Sport.TenisDeMesa -> Icons.Rounded.SportsTennis
    }

fun localTime(iso: String): ZonedDateTime? =
    runCatching { OffsetDateTime.parse(iso).toInstant().atZone(ZoneId.systemDefault()) }.getOrNull()

private val HOUR_FMT = DateTimeFormatter.ofPattern("HH:mm")
private val SHORT_DAY_FMT = DateTimeFormatter.ofPattern("EEE dd", Locale("pt", "BR"))
private val LONG_DAY_FMT = DateTimeFormatter.ofPattern("EEEE, dd/MM", Locale("pt", "BR"))

fun hourText(iso: String): String = localTime(iso)?.format(HOUR_FMT) ?: "--:--"

/** "Hoje", "Amanhã" ou "Sábado, 17/10": rótulo dos grupos do feed. */
fun dayGroupLabel(iso: String): String {
    val d = localTime(iso)?.toLocalDate() ?: return "Em breve"
    val today = LocalDate.now(ZoneId.systemDefault())
    return when (d) {
        today -> "Hoje"
        today.plusDays(1) -> "Amanhã"
        else -> d.format(LONG_DAY_FMT).replaceFirstChar { it.uppercase() }
    }
}

private fun shortDay(iso: String): String {
    val d = localTime(iso)?.toLocalDate() ?: return ""
    val today = LocalDate.now(ZoneId.systemDefault())
    return when (d) {
        today -> "hoje"
        today.plusDays(1) -> "amanhã"
        else -> d.format(SHORT_DAY_FMT).replace(".", "")
    }
}

/** Vagas como pílulas acesas: leitura imediata de "quanto falta pra fechar o time". */
@Composable
fun SlotBar(taken: Int, capacity: Int, color: Color, modifier: Modifier = Modifier) {
    val slots = capacity.coerceIn(1, 20)
    val lit = if (capacity <= 20) taken.coerceAtMost(slots) else (taken * 20f / capacity).toInt()
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(slots) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (i < lit) color else Palette.Tribuna),
            )
        }
    }
}

/** Foto da modalidade (uso livre, ver docs/CREDITOS-FOTOS.md). */
val Sport.photo: Int
    get() = when (this) {
        Sport.Futebol -> R.drawable.sport_futebol
        Sport.Futevolei -> R.drawable.sport_futevolei
        Sport.Volei -> R.drawable.sport_volei
        Sport.Basquete -> R.drawable.sport_basquete
        Sport.BeachTennis -> R.drawable.sport_beach_tennis
        Sport.Futsal -> R.drawable.sport_futsal
        Sport.Tenis -> R.drawable.sport_tenis
        Sport.Padel -> R.drawable.sport_padel
        Sport.Natacao -> R.drawable.sport_natacao
        Sport.Biribol -> R.drawable.sport_biribol
        Sport.Corrida -> R.drawable.sport_corrida
        Sport.Ciclismo -> R.drawable.sport_ciclismo
        Sport.Caminhada -> R.drawable.sport_caminhada
        Sport.TenisDeMesa -> R.drawable.sport_tenis_de_mesa
    }

/** Foto da modalidade preenchendo o espaço (recorte central). Decorativa. */
@Composable
fun SportPhoto(sport: Sport?, modifier: Modifier = Modifier) {
    if (sport == null) {
        Box(modifier.background(Palette.Arquibancada))
    } else {
        Image(
            painter = painterResource(sport.photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

/** Miniatura quadrada com a foto do esporte e o ícone da modalidade num selo de cor. */
@Composable
fun SportThumb(sport: Sport?, size: androidx.compose.ui.unit.Dp = 64.dp) {
    val color = sport?.color ?: Palette.Pista
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.28f))) {
        SportPhoto(sport, Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Palette.Noite.copy(alpha = 0.45f)))))
        if (sport != null) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(5.dp).size(size * 0.34f).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(sport.icon, contentDescription = null, tint = Palette.Noite, modifier = Modifier.size(size * 0.2f))
            }
        }
    }
}

private val OptionOn = Color(0xFF34C759)

/** Interruptor estilo iPhone: verde = sim, cinza = não. */
@Composable
fun OptionSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Palette.Tribuna.copy(alpha = 0.5f))
            .clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = null,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = OptionOn, checkedBorderColor = OptionOn,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF3A3F55), uncheckedBorderColor = Color(0xFF3A3F55),
            ),
        )
    }
}

/** Pontinhos de equipamento no cartão: bolinha verde = tem, cinza = não tem. */
@Composable
private fun OptionDots(sport: Sport?, options: Map<String, Boolean>) {
    val items = com.ourinhosplay.app.data.OptionCatalog.resolve(sport, options)
    if (items.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (def, on) ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (on) OptionOn else Color(0xFF4A5068)))
                Text(def.short, style = MaterialTheme.typography.labelSmall, color = if (on) Palette.Giz else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Cartão do jogo: capa com a foto da modalidade e o horário como placar; embaixo local, vagas, preço, equipamento e estado. */
@Composable
fun MatchTicket(m: MatchCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sport = Sport.from(m.sport)
    val color = sport?.color ?: Palette.Pista
    val cancelled = m.status == "cancelled"
    val full = m.status == "full" || m.spotsLeft == 0

    Column(
        modifier
            .fillMaxWidth()
            .alpha(if (cancelled) 0.55f else 1f)
            .card()
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(112.dp)) {
            SportPhoto(sport, Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Palette.Noite.copy(alpha = 0.15f), Palette.Noite.copy(alpha = 0.85f)))))
            if (sport != null) {
                Box(
                    Modifier.align(Alignment.TopStart).padding(12.dp).size(32.dp).clip(CircleShape).background(color),
                    contentAlignment = Alignment.Center,
                ) { Icon(sport.icon, contentDescription = sport.label, tint = Palette.Noite, modifier = Modifier.size(18.dp)) }
            }
            val tag = when {
                m.isEvent -> "Evento"
                !full && !cancelled && m.rolesOpen.isNotEmpty() ->
                    "Falta " + m.rolesOpen.joinToString(", ") { com.ourinhosplay.app.data.RoleCatalog.label(it).lowercase() }
                else -> null
            }
            if (tag != null) {
                Text(
                    tag, style = MaterialTheme.typography.labelMedium, color = Palette.Noite,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(50)).background(Palette.Bola).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Row(
                Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    hourText(m.startsAt),
                    fontFamily = ScoreFont, fontWeight = FontWeight.ExtraBold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 40.sp, color = Palette.Giz,
                )
                Text(shortDay(m.startsAt).uppercase(), style = MaterialTheme.typography.labelLarge, color = Palette.Giz, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(m.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    m.venueName + (m.courtName?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SlotBar(m.taken, m.capacity, color, Modifier.weight(1f))
                Text("${m.taken}/${m.capacity}", style = MaterialTheme.typography.labelLarge)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val price = when {
                    m.costCents != null && m.costCents > 0 && m.capacity > 0 -> "R$ " + "%.0f".format(m.costCents / 100.0 / m.capacity) + "/pessoa"
                    m.courtIsFree == true -> "Grátis"
                    else -> null
                }
                if (price != null) Text(price, style = MaterialTheme.typography.labelLarge, color = Palette.Giz)
                OptionDots(sport, m.options)
                Spacer(Modifier.weight(1f))
                val (label, bg, fg) = when {
                    cancelled -> Triple("Cancelado", Palette.Tribuna, Palette.Giz)
                    full -> Triple("Lotado", Palette.Tribuna, Palette.Giz)
                    else -> Triple("Entrar", Palette.Pista, Palette.Noite)
                }
                Text(
                    label, style = MaterialTheme.typography.labelLarge, color = fg,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}


/** Chips selecionados em laranja-pista, em vez do cinza padrão do Material. */
@Composable
fun appChipColors() = androidx.compose.material3.FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
)

/** " · 1h30" a partir de início e fim (ISO); vazio se não der para calcular. */
fun durationLabel(startIso: String, endIso: String): String {
    val s = runCatching { OffsetDateTime.parse(startIso).toInstant() }.getOrNull() ?: return ""
    val e = runCatching { OffsetDateTime.parse(endIso).toInstant() }.getOrNull() ?: return ""
    return " · " + formatDuration(java.time.Duration.between(s, e).toMinutes().toInt())
}

/** 45 -> "45 min", 60 -> "1h", 90 -> "1h30". */
fun formatDuration(min: Int): String = when {
    min < 60 -> "$min min"
    min % 60 == 0 -> "${min / 60}h"
    else -> "${min / 60}h%02d".format(min % 60)
}
