package com.ourinhosplay.app.ui

import androidx.compose.foundation.background
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
    }

val Sport.icon: ImageVector
    get() = when (this) {
        Sport.Futebol -> Icons.Rounded.SportsSoccer
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
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (i < lit) color else Palette.Tribuna),
            )
        }
    }
}

/** O card-ingresso: horário gigante na cor do esporte, título e vagas. */
@Composable
fun MatchTicket(m: MatchCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sport = Sport.from(m.sport)
    val color = sport?.color ?: Palette.Pista
    val cancelled = m.status == "cancelled"
    val full = m.status == "full" || m.spotsLeft == 0

    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .alpha(if (cancelled) 0.55f else 1f)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        Column(
            Modifier.widthIn(min = 96.dp).fillMaxHeight().background(color).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(hourText(m.startsAt), fontFamily = ScoreFont, fontWeight = FontWeight.ExtraBold,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 32.sp, color = Palette.Noite)
            Text(shortDay(m.startsAt), style = MaterialTheme.typography.labelMedium, color = Palette.Noite)
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (sport != null) Icon(sport.icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Text(sport?.label ?: m.sport, style = MaterialTheme.typography.labelMedium, color = color)
                if (m.isEvent) {
                    Text(
                        "EVENTO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.Noite,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Palette.Bola).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(m.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(m.venueName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SlotBar(m.taken, m.capacity, color, Modifier.weight(1f))
                Text(
                    when {
                        cancelled -> "Cancelado"
                        full -> "Lotado"
                        else -> "${m.taken}/${m.capacity}"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (full && !cancelled) Palette.Bola else MaterialTheme.colorScheme.onSurface,
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
