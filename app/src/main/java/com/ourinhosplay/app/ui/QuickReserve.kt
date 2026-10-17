package com.ourinhosplay.app.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ourinhosplay.app.data.Court
import com.ourinhosplay.app.data.Venue

/** A porta de entrada do "+": reserva direta ou jogo com a galera. */
@Composable
fun CreateEntryScreen(onQuick: () -> Unit, onSocial: () -> Unit, onClose: () -> Unit) {
    GlassBackdrop {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("O que você quer fazer?", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onClose) { Text("Fechar") }
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                EntryCard(
                    icon = Icons.Rounded.Bolt,
                    tint = Palette.Bola,
                    title = "Só reservar",
                    text = "Escolhe quadra e horário e pronto. Fica privado, sem lista, sem chat. Abre vagas depois, se quiser.",
                    onClick = onQuick,
                )
                EntryCard(
                    icon = Icons.Rounded.Groups,
                    tint = Palette.Pista,
                    title = "Jogo com a galera",
                    text = "Abre vagas, reserva goleiro, divide a quadra e chama o pessoal.",
                    onClick = onSocial,
                )
            }
        }
    }
}

@Composable
private fun EntryCard(icon: ImageVector, tint: Color, title: String, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .card()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(40.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val PREFS = "quick_reserve"

/**
 * Reserva rápida: modalidade, quadra e horário. Abre já na última quadra usada;
 * cria um jogo privado de 1 vaga (só bloqueia a quadra).
 */
@Composable
fun QuickReserveScreen(
    venues: List<Venue>,
    courts: List<Court>,
    onClose: () -> Unit,
    onCreated: () -> Unit,
    vm: CreateMatchViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.setQuick(true) }
    // Começa na última quadra usada (assim que o catálogo de locais chegar).
    LaunchedEffect(venues.size) {
        if (ui.venueId == null && venues.isNotEmpty()) {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            vm.applyLast(p.getString("sport", null), p.getString("venue", null), p.getString("court", null), venues, courts)
        }
    }
    if (ui.created) {
        LaunchedEffect(Unit) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("sport", ui.sport?.key).putString("venue", ui.venueId).putString("court", ui.courtId).apply()
            Sfx.fire(context, Sound.Fanfare)
            onCreated()
        }
    }

    val accent = ui.sport?.color ?: Palette.Bola
    GlassBackdrop() {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Só reservar", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Privado. Só você e a quadra.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onClose) { Text("Cancelar") }
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StepCard("1", "Modalidade", accent) { SportPicker(ui.sport) { vm.chooseSport(it, venues, courts) } }
                StepCard("2", "Onde", accent) { WherePicker(ui, vm, venues, courts) }
                StepCard("3", "Quando", accent) {
                    WhenPicker(ui, vm, { pickDate = true }, { pickTime = true })
                    RepeatChips(ui.repeatWeeks, vm::setRepeat)
                }
            }
            PublishBar(ui, accent, if (ui.repeatWeeks > 0) "Reservar ${ui.repeatWeeks} semanas" else "Reservar quadra", vm::publish)
        }
    }
    DateTimeDialogs(ui, vm, pickDate, pickTime, { pickDate = false }, { pickTime = false })
}
