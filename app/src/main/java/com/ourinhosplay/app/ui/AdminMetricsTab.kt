package com.ourinhosplay.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ourinhosplay.app.data.AdminMetrics
import com.ourinhosplay.app.data.Sport

/** Painel de tração (migration 0017): números agregados, sem dados pessoais. */
@Composable
fun MetricsTab(metrics: AdminMetrics?, error: String?, retry: () -> Unit) {
    when {
        error != null && metrics == null -> Problem(error, retry)
        metrics == null -> Loading()
        else -> Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("${metrics.players}", "jogadores", "+${metrics.newPlayers30d} em 30 dias", Modifier.weight(1f))
                Tile("${metrics.active30d}", "ativos em 30 dias", "entraram em algum jogo", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("${metrics.matchesTotal}", "jogos criados", "${metrics.matches30d} em 30 dias", Modifier.weight(1f))
                Tile("${metrics.filledSpots30d}", "vagas preenchidas", "em 30 dias", Modifier.weight(1f))
            }
            Tile(
                "${metrics.activeMatchesPct}%", "jogos não cancelados",
                "${metrics.matchesCancelled} cancelados de ${metrics.matchesTotal}", Modifier.fillMaxWidth(),
            )

            Text("Jogos por esporte", style = MaterialTheme.typography.titleMedium)
            if (metrics.bySport.isEmpty()) Muted("Nenhum jogo ainda.")
            val top = metrics.bySport.values.maxOrNull() ?: 1
            metrics.bySport.entries.sortedByDescending { it.value }.forEach { (sport, n) ->
                BarRow(Sport.from(sport)?.label ?: sport, n, top)
            }

            Text("Locais com mais jogos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
            if (metrics.byVenue.isEmpty()) Muted("Nenhum jogo ainda.")
            val topVenue = metrics.byVenue.maxOfOrNull { it.matches } ?: 1
            metrics.byVenue.forEach { BarRow(it.venue, it.matches, topVenue) }
        }
    }
}

@Composable
private fun Tile(value: String, label: String, hint: String, modifier: Modifier) {
    Column(modifier.card().padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BarRow(label: String, value: Int, max: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.45f), maxLines = 1)
        SlotBar(value, max.coerceAtLeast(1), MaterialTheme.colorScheme.primary, Modifier.weight(0.45f))
        Text("$value", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Muted(text: String) =
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
