package com.ourinhosplay.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.SportsScore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ourinhosplay.app.data.MatchCard
import com.ourinhosplay.app.data.Sport
import com.ourinhosplay.app.data.Venue
import java.time.Instant

private enum class Tab(val label: String, val icon: ImageVector) {
    Feed("Jogos", Icons.Rounded.SportsScore),
    Mine("Meus jogos", Icons.Rounded.EventAvailable),
    Venues("Quadras", Icons.Rounded.Place),
    Profile("Perfil", Icons.Rounded.Person),
}

@Composable
fun HomeShell(displayName: String, onSignOut: () -> Unit, vm: HomeViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(Tab.Feed.ordinal) }
    // Cada abertura usa um ViewModel novo (chave diferente), para não herdar "criado" da anterior.
    var createKey by rememberSaveable { mutableIntStateOf(0) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var openMatch by rememberSaveable { mutableStateOf<String?>(null) }
    var template by remember { mutableStateOf<MatchCard?>(null) }

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
        val createVm: CreateMatchViewModel = viewModel(key = "create-$createKey")
        LaunchedEffect(createKey) { template?.let { createVm.prefill(it) } }
        CreateMatchScreen(
            venues = venues.data,
            onClose = { creating = false; template = null },
            onCreated = {
                creating = false
                template = null
                vm.refreshFeed()
                vm.refreshMine()
            },
            vm = createVm,
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (tab == Tab.Feed.ordinal) {
                ExtendedFloatingActionButton(
                    onClick = {
                        template = null
                        createKey++
                        creating = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Criar jogo", style = MaterialTheme.typography.labelLarge) },
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t.ordinal,
                        onClick = { tab = t.ordinal },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Palette.Noite,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            when (Tab.entries[tab]) {
                Tab.Feed -> FeedScreen(vm) { openMatch = it }
                Tab.Mine -> MineScreen(vm, onOpen = { openMatch = it }, onAgain = {
                    template = it
                    createKey++
                    creating = true
                })
                Tab.Venues -> VenuesScreen(vm)
                Tab.Profile -> ProfileScreen(displayName, onSignOut)
            }
        }
    }
}

@Composable
private fun Wordmark(subtitle: String) {
    Column(Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp)) {
        Text(
            buildAnnotatedString {
                append("Ourinhos")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Play") }
            },
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FeedScreen(vm: HomeViewModel, onOpen: (String) -> Unit) {
    val ui by vm.feed.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Wordmark("Quem joga hoje em Ourinhos?")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = ui.sport == null,
                    onClick = { vm.selectSport(null) },
                    label = { Text("Todos") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                        selectedLabelColor = Palette.Noite,
                    ),
                )
            }
            items(Sport.entries) { s ->
                FilterChip(
                    selected = ui.sport == s,
                    onClick = { vm.selectSport(s) },
                    label = { Text(s.label) },
                    leadingIcon = {
                        Icon(
                            s.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (ui.sport == s) Palette.Noite else s.color,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = s.color,
                        selectedLabelColor = Palette.Noite,
                    ),
                )
            }
        }

        // Agrupa por dia: cabeçalhos viram itens da mesma lista.
        val items: List<Any> = buildList {
            var last: String? = null
            ui.matches.data.forEach { m ->
                val label = dayGroupLabel(m.startsAt)
                if (label != last) { add(label); last = label }
                add(m)
            }
        }
        RemoteList(
            state = Remote(ui.matches.loading, ui.matches.error, items),
            emptyTitle = "Nenhum jogo por aqui",
            emptyText = "Seja o primeiro: crie um jogo e chame a galera.",
            onRetry = vm::refreshFeed,
        ) { item ->
            when (item) {
                is String -> Text(
                    item,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 10.dp),
                )
                is MatchCard -> MatchTicket(item, onClick = { onOpen(item.id) })
            }
        }
    }
}

@Composable
private fun VenuesScreen(vm: HomeViewModel) {
    val state by vm.venues.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Wordmark("Onde jogar")
        RemoteList(
            state = state,
            emptyTitle = "Nenhuma quadra cadastrada",
            emptyText = "As quadras da cidade aparecem aqui.",
            onRetry = vm::refreshVenues,
            topPadding = 16.dp,
        ) { VenueRow(it) }
    }
}

@Composable
private fun <T> RemoteList(
    state: Remote<T>,
    emptyTitle: String,
    emptyText: String,
    onRetry: () -> Unit,
    topPadding: androidx.compose.ui.unit.Dp = 4.dp,
    row: @Composable (T) -> Unit,
) {
    when {
        state.loading && state.data.isEmpty() -> Centered { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        state.error != null -> Centered {
            Text("Não deu pra carregar", style = MaterialTheme.typography.headlineSmall)
            Text(
                state.error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            OutlinedButton(onClick = onRetry) { Text("Tentar de novo") }
        }
        state.data.isEmpty() -> Centered {
            Text(emptyTitle, style = MaterialTheme.typography.headlineSmall)
            Text(
                emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            TextButton(onClick = onRetry) { Text("Atualizar") }
        }
        else -> LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPadding, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { items(state.data) { row(it) } }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
private fun VenueRow(v: Venue) {
    val context = LocalContext.current
    val sports = v.sports.mapNotNull { Sport.from(it) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(v.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                if (v.kind == "public") "Pública" else "Particular",
                style = MaterialTheme.typography.labelMedium,
                color = if (v.kind == "public") Palette.Grama else Palette.Sol,
            )
        }
        Text(v.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (sports.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                sports.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(s.icon, contentDescription = null, tint = s.color, modifier = Modifier.size(18.dp))
                        Text(s.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        TextButton(onClick = { openInMaps(context, v) }, contentPadding = PaddingValues(0.dp)) {
            Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Abrir no Maps")
        }
    }
}

@Composable
private fun ProfileScreen(displayName: String, onSignOut: () -> Unit) = Centered {
    Box(
        Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            displayName.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
    Text(displayName, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
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

@Composable
private fun MineScreen(vm: HomeViewModel, onOpen: (String) -> Unit, onAgain: (MatchCard) -> Unit) {
    val state by vm.mine.collectAsState()
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { vm.refreshMine() }

    val now = Instant.now()
    fun isUpcoming(m: MatchCard): Boolean {
        if (m.status == "cancelled" || m.status == "finished") return false
        val end = runCatching { java.time.OffsetDateTime.parse(m.endsAt).toInstant() }.getOrNull() ?: return true
        return end.isAfter(now)
    }
    val upcoming = state.data.filter(::isUpcoming).sortedBy { it.startsAt }
    val history = state.data.filterNot(::isUpcoming)
    val shown = if (tabIndex == 0) upcoming else history

    Column(Modifier.fillMaxSize()) {
        Wordmark("Seus jogos")
        androidx.compose.material3.TabRow(
            selectedTabIndex = tabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            androidx.compose.material3.Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Próximos") })
            androidx.compose.material3.Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Histórico") })
        }
        RemoteList(
            state = state.copy(data = shown),
            emptyTitle = if (tabIndex == 0) "Nada marcado" else "Sem histórico ainda",
            emptyText = if (tabIndex == 0) "Entre num jogo ou crie o seu." else "Os jogos que passarem aparecem aqui.",
            onRetry = vm::refreshMine,
            topPadding = 16.dp,
        ) { m ->
            Column {
                MatchTicket(m, onClick = { onOpen(m.id) })
                if (tabIndex == 1) {
                    TextButton(onClick = { onAgain(m) }) { Text("Jogar de novo") }
                }
            }
        }
    }
}
