package com.ourinhosplay.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ourinhosplay.app.data.MatchCard
import com.ourinhosplay.app.data.Sport
import com.ourinhosplay.app.data.Venue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Tab(val label: String, val glyph: String) {
    Feed("Jogos", "⚽"),
    Mine("Meus jogos", "📋"),
    Venues("Quadras", "📍"),
    Profile("Perfil", "👤"),
}

@Composable
fun HomeShell(displayName: String, onSignOut: () -> Unit, vm: HomeViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(Tab.Feed.ordinal) }
    // Cada abertura usa um ViewModel novo (chave diferente), para não herdar "criado" da anterior.
    var createKey by rememberSaveable { mutableIntStateOf(0) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var openMatch by rememberSaveable { mutableStateOf<String?>(null) }

    // Voltar do sistema fecha detalhe/criação em vez de sair do app.
    androidx.activity.compose.BackHandler(enabled = creating || openMatch != null) {
        if (creating) creating = false else openMatch = null
    }

    openMatch?.let { id ->
        MatchDetailScreen(matchId = id, onBack = { openMatch = null; vm.refreshFeed() })
        return
    }
    val venues by vm.venues.collectAsState()

    if (creating) {
        CreateMatchScreen(
            venues = venues.data,
            onClose = { creating = false },
            onCreated = {
                creating = false
                vm.refreshFeed()
            },
            vm = viewModel(key = "create-$createKey"),
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            if (tab == Tab.Feed.ordinal) {
                ExtendedFloatingActionButton(
                    onClick = {
                        createKey++
                        creating = true
                    },
                ) { Text("＋ Criar jogo") }
            }
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t.ordinal,
                        onClick = { tab = t.ordinal },
                        icon = { Text(t.glyph) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            when (Tab.entries[tab]) {
                Tab.Feed -> FeedScreen(vm) { openMatch = it }
                Tab.Mine -> Placeholder("Meus jogos chega na Sprint 5.")
                Tab.Venues -> VenuesScreen(vm)
                Tab.Profile -> ProfileScreen(displayName, onSignOut)
            }
        }
    }
}

@Composable
private fun FeedScreen(vm: HomeViewModel, onOpen: (String) -> Unit) {
    val ui by vm.feed.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Text(
            "Jogos abertos",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = ui.sport == null,
                    onClick = { vm.selectSport(null) },
                    label = { Text("Todos") },
                )
            }
            items(Sport.entries) { s ->
                FilterChip(
                    selected = ui.sport == s,
                    onClick = { vm.selectSport(s) },
                    label = { Text("${s.emoji} ${s.label}") },
                )
            }
        }
        RemoteList(
            state = ui.matches,
            emptyText = "Nenhum jogo ainda. Crie o primeiro!",
            onRetry = vm::refreshFeed,
        ) { MatchRow(it) { onOpen(it.id) } }
    }
}

@Composable
private fun VenuesScreen(vm: HomeViewModel) {
    val state by vm.venues.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Text(
            "Quadras",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
        )
        RemoteList(
            state = state,
            emptyText = "Nenhuma quadra cadastrada.",
            onRetry = vm::refreshVenues,
        ) { VenueRow(it) }
    }
}

@Composable
private fun <T> RemoteList(
    state: Remote<T>,
    emptyText: String,
    onRetry: () -> Unit,
    row: @Composable (T) -> Unit,
) {
    when {
        state.loading && state.data.isEmpty() -> Centered { CircularProgressIndicator() }
        state.error != null -> Centered {
            Text("Não deu pra carregar.", style = MaterialTheme.typography.titleMedium)
            Text(state.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            OutlinedButton(onClick = onRetry) { Text("Tentar de novo") }
        }
        state.data.isEmpty() -> Centered {
            Text(emptyText, style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = onRetry) { Text("Atualizar") }
        }
        else -> LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { items(state.data) { row(it) } }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
private fun Placeholder(text: String) = Centered {
    Text(text, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun MatchRow(m: MatchCard, onClick: () -> Unit) {
    val sport = Sport.from(m.sport)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${sport?.emoji.orEmpty()} ${sport?.label ?: m.sport}",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(m.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${m.venueName} · ${whenLabel(m.startsAt)}", style = MaterialTheme.typography.bodyMedium)
            Text(
                if (m.spotsLeft == 0) "Lotado" else "${m.taken}/${m.capacity} confirmados",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun VenueRow(v: Venue) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(v.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(v.address, style = MaterialTheme.typography.bodyMedium)
            val sports = v.sports.mapNotNull { Sport.from(it) }.joinToString("  ") { "${it.emoji} ${it.label}" }
            if (sports.isNotEmpty()) Text(sports, style = MaterialTheme.typography.bodySmall)
            Row { TextButton(onClick = { openInMaps(context, v) }) { Text("Abrir no Maps") } }
        }
    }
}

@Composable
private fun ProfileScreen(displayName: String, onSignOut: () -> Unit) = Centered {
    Text(displayName, style = MaterialTheme.typography.headlineMedium)
    OutlinedButton(onClick = onSignOut, modifier = Modifier.padding(top = 24.dp)) { Text("Sair") }
}

private fun openInMaps(context: Context, v: Venue) {
    val query = if (v.lat != null && v.lng != null) "${v.lat},${v.lng}(${v.name})" else "${v.name}, ${v.address}"
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query)))
    try {
        context.startActivity(geo)
    } catch (_: ActivityNotFoundException) {
        val web = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
        context.startActivity(Intent(Intent.ACTION_VIEW, web))
    }
}

private val DAY_FMT = DateTimeFormatter.ofPattern("EEE dd/MM", Locale("pt", "BR"))
private val HOUR_FMT = DateTimeFormatter.ofPattern("HH:mm")

/** "Hoje 20:00", "Amanhã 19:30" ou "qua 16/10 20:00". */
private fun whenLabel(iso: String): String {
    val zoned = runCatching { Instant.parse(iso) }.getOrNull()
        ?: runCatching { java.time.OffsetDateTime.parse(iso).toInstant() }.getOrNull()
        ?: return iso
    val local = zoned.atZone(ZoneId.systemDefault())
    val today = LocalDate.now(ZoneId.systemDefault())
    val day = when (local.toLocalDate()) {
        today -> "Hoje"
        today.plusDays(1) -> "Amanhã"
        else -> local.format(DAY_FMT)
    }
    return "$day ${local.format(HOUR_FMT)}"
}
