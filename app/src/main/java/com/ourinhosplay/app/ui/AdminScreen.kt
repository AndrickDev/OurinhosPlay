package com.ourinhosplay.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ourinhosplay.app.data.AdminRepository
import com.ourinhosplay.app.data.AdminUser
import com.ourinhosplay.app.data.AdminUsers
import com.ourinhosplay.app.data.BusinessException
import com.ourinhosplay.app.data.Court
import com.ourinhosplay.app.data.MatchCard
import com.ourinhosplay.app.data.MatchDetailRepository
import com.ourinhosplay.app.data.MatchRepository
import com.ourinhosplay.app.data.Sport
import com.ourinhosplay.app.data.Venue
import com.ourinhosplay.app.data.VenueBlock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUi(
    val matches: Remote<MatchCard> = Remote(),
    val blocks: Remote<VenueBlock> = Remote(),
    val users: Remote<AdminUser> = Remote(),
    val busy: Boolean = false,
    val message: String? = null,
)

class AdminViewModel : ViewModel() {
    private val _ui = MutableStateFlow(AdminUi())
    val ui: StateFlow<AdminUi> = _ui.asStateFlow()

    init { refresh() }

    fun refresh() {
        _ui.update { it.copy(matches = it.matches.copy(loading = true, error = null), blocks = it.blocks.copy(loading = true, error = null)) }
        viewModelScope.launch {
            val m = runCatching { MatchRepository.openMatches(null) }
            _ui.update {
                it.copy(matches = m.fold({ d -> Remote(false, null, d) }, { e -> Remote(false, friendlyError(e)) }))
            }
        }
        viewModelScope.launch {
            val u = runCatching { AdminUsers.list() }
            _ui.update {
                it.copy(users = u.fold({ d -> Remote(false, null, d) }, { e -> Remote(false, friendlyError(e)) }))
            }
        }
        viewModelScope.launch {
            val b = runCatching { AdminRepository.blocks() }
            _ui.update {
                it.copy(blocks = b.fold({ d -> Remote(false, null, d) }, { e -> Remote(false, friendlyError(e)) }))
            }
        }
    }

    fun clearMessage() = _ui.update { it.copy(message = null) }

    /** Roda uma ação do admin, mostra o resultado e recarrega. `after` atualiza o catálogo de quadras. */
    private fun act(okText: String, after: () -> Unit, block: suspend () -> Unit) {
        _ui.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val r = runCatching { block() }
            _ui.update {
                it.copy(
                    busy = false,
                    message = r.fold(
                        { okText },
                        { e -> (e as? BusinessException)?.message ?: friendlyError(e) },
                    ),
                )
            }
            if (r.isSuccess) { refresh(); after() }
        }
    }

    fun cancelMatch(id: String) = act("Jogo cancelado.", {}) { MatchDetailRepository.cancel(id) }

    fun deleteUser(id: String) = act("Conta removida.", {}) { AdminUsers.delete(id) }

    fun removeBlock(id: String) = act("Bloqueio removido.", {}) { AdminRepository.removeBlock(id) }

    fun addBlock(venueId: String, courtId: String?, start: Instant, end: Instant, reason: String) =
        act("Horário bloqueado.", {}) { AdminRepository.addBlock(venueId, courtId, start, end, reason) }

    fun saveCourt(
        venueId: String, courtId: String?, name: String, sports: List<String>,
        isFree: Boolean, priceCents: Int?, audience: String, after: () -> Unit,
    ) = act("Quadra salva.", after) {
        AdminRepository.saveCourt(venueId, courtId, name, sports, isFree, priceCents, audience)
    }

    fun createEvent(
        venueId: String, sport: Sport, title: String, description: String, start: Instant,
        durationMin: Int, capacity: Int, routeStart: String, routeEnd: String, promoted: Boolean,
    ) = act("Evento criado.", {}) {
        AdminRepository.createEvent(venueId, sport, title, description, start, durationMin, capacity, routeStart, routeEnd, promoted)
    }

    fun setPromoted(id: String, promoted: Boolean) =
        act(if (promoted) "Evento em destaque." else "Destaque removido.", {}) { AdminRepository.setPromoted(id, promoted) }

    fun deleteCourt(id: String, after: () -> Unit) =
        act("Quadra removida.", after) { AdminRepository.deleteCourt(id) }
}

private val ZONE: ZoneId get() = ZoneId.systemDefault()
private val DAY_FMT = DateTimeFormatter.ofPattern("EEE dd/MM", Locale("pt", "BR"))
private val HOUR_FMT = DateTimeFormatter.ofPattern("HH:mm")

private fun blockLabel(b: VenueBlock): String {
    val s = localTime(b.startsAt)
    val e = localTime(b.endsAt)
    if (s == null || e == null) return "—"
    val sameDay = s.toLocalDate() == e.toLocalDate()
    return if (sameDay) "${s.format(DAY_FMT)} · ${s.format(HOUR_FMT)} – ${e.format(HOUR_FMT)}"
    else "${s.format(DAY_FMT)} ${s.format(HOUR_FMT)} – ${e.format(DAY_FMT)} ${e.format(HOUR_FMT)}"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminScreen(
    venues: List<Venue>,
    courts: List<Court>,
    onCatalogChanged: () -> Unit,
    onClose: () -> Unit,
    vm: AdminViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    var confirmCancel by remember { mutableStateOf<MatchCard?>(null) }
    var editCourt by remember { mutableStateOf<Pair<Venue, Court?>?>(null) }
    var newBlock by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Court?>(null) }
    var newEvent by remember { mutableStateOf(false) }
    var confirmUser by remember { mutableStateOf<AdminUser?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Painel admin", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Fechar") }
        }
        ui.message?.let {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = vm::clearMessage) { Text("OK") }
            }
        }
        TabRow(
            selectedTabIndex = tab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            listOf("Jogos", "Eventos", "Quadras", "Bloqueios", "Pessoas").forEachIndexed { i, label ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
            }
        }

        when (tab) {
            0 -> when {
                ui.matches.loading && ui.matches.data.isEmpty() -> Loading()
                ui.matches.error != null -> Problem(ui.matches.error!!, vm::refresh)
                ui.matches.data.isEmpty() -> Empty("Nenhum jogo marcado.")
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(ui.matches.data) { m ->
                        Column {
                            MatchTicket(m, onClick = {})
                            if (m.status != "cancelled") {
                                TextButton(onClick = { confirmCancel = m }, enabled = !ui.busy) { Text("Cancelar este jogo") }
                            }
                        }
                    }
                }
            }

            1 -> Column(Modifier.fillMaxSize()) {
                Button(onClick = { newEvent = true }, modifier = Modifier.padding(16.dp)) { Text("+ Novo evento") }
                val events = ui.matches.data.filter { it.isEvent }
                if (events.isEmpty()) {
                    Empty("Nenhum evento. Crie corridas, caminhadas e torneios.")
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(events) { m ->
                            Column {
                                MatchTicket(m, onClick = {})
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Em destaque no topo", modifier = Modifier.weight(1f).padding(start = 4.dp))
                                    Switch(checked = m.promoted, onCheckedChange = { vm.setPromoted(m.id, it) }, enabled = !ui.busy)
                                }
                            }
                        }
                    }
                }
            }

            2 -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                venues.forEach { v ->
                    val mine = courts.filter { it.venueId == v.id }
                    Column(
                        Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(v.name, style = MaterialTheme.typography.titleMedium)
                        if (mine.isEmpty()) Text("Sem quadras cadastradas.", style = MaterialTheme.typography.bodySmall)
                        mine.forEach { c ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        c.priceLabel + (c.audience?.let { " · $it" } ?: "") + " · " +
                                            c.sports.mapNotNull { Sport.from(it)?.label }.joinToString(", ").ifEmpty { "sem esporte do app" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = { editCourt = v to c }) { Text("Editar") }
                                TextButton(onClick = { confirmDelete = c }) { Text("Remover") }
                            }
                        }
                        TextButton(onClick = { editCourt = v to null }) { Text("+ Adicionar quadra") }
                    }
                }
            }

            4 -> when {
                ui.users.loading && ui.users.data.isEmpty() -> Loading()
                ui.users.error != null -> Problem(ui.users.error!!, vm::refresh)
                ui.users.data.isEmpty() -> Empty("Nenhuma pessoa cadastrada.")
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ui.users.data) { u ->
                        Row(
                            Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                                .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(u.fullName + if (u.role == "admin") " · admin" else "", style = MaterialTheme.typography.titleSmall)
                                Text(u.email ?: "sem e-mail", style = MaterialTheme.typography.bodySmall)
                            }
                            if (u.role != "admin") {
                                TextButton(onClick = { confirmUser = u }, enabled = !ui.busy) { Text("Remover") }
                            }
                        }
                    }
                }
            }

            else -> Column(Modifier.fillMaxSize()) {
                Button(onClick = { newBlock = true }, modifier = Modifier.padding(16.dp)) { Text("+ Bloquear horário") }
                when {
                    ui.blocks.loading && ui.blocks.data.isEmpty() -> Loading()
                    ui.blocks.error != null -> Problem(ui.blocks.error!!, vm::refresh)
                    ui.blocks.data.isEmpty() -> Empty("Nenhum horário bloqueado.")
                    else -> LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(ui.blocks.data) { b ->
                            val venueName = venues.firstOrNull { it.id == b.venueId }?.name ?: "Local"
                            val courtName = courts.firstOrNull { it.id == b.courtId }?.name ?: "Todas as quadras"
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                                    .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("$venueName · $courtName", style = MaterialTheme.typography.titleSmall)
                                    Text(blockLabel(b), style = MaterialTheme.typography.bodyMedium)
                                    b.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                }
                                TextButton(onClick = { vm.removeBlock(b.id) }, enabled = !ui.busy) { Text("Liberar") }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmCancel?.let { m ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            title = { Text("Cancelar jogo?") },
            text = { Text("\"${m.title}\" será cancelado para todos os participantes.") },
            confirmButton = { TextButton(onClick = { vm.cancelMatch(m.id); confirmCancel = null }) { Text("Cancelar jogo") } },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text("Voltar") } },
        )
    }

    confirmUser?.let { u ->
        AlertDialog(
            onDismissRequest = { confirmUser = null },
            title = { Text("Remover conta?") },
            text = { Text("${u.fullName} (${u.email ?: "sem e-mail"}) perde o login, sai de todos os jogos e os jogos que criou são apagados. Não dá para desfazer.") },
            confirmButton = { TextButton(onClick = { vm.deleteUser(u.id); confirmUser = null }) { Text("Remover de vez") } },
            dismissButton = { TextButton(onClick = { confirmUser = null }) { Text("Voltar") } },
        )
    }

    confirmDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Remover quadra?") },
            text = { Text("\"${c.name}\" sai da lista. Quadras com jogos marcados não podem ser removidas.") },
            confirmButton = { TextButton(onClick = { vm.deleteCourt(c.id, onCatalogChanged); confirmDelete = null }) { Text("Remover") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Voltar") } },
        )
    }

    editCourt?.let { (venue, court) ->
        CourtDialog(
            venue = venue,
            court = court,
            onDismiss = { editCourt = null },
            onSave = { name, sports, free, cents, audience ->
                vm.saveCourt(venue.id, court?.id, name, sports, free, cents, audience, onCatalogChanged)
                editCourt = null
            },
        )
    }

    if (newEvent) {
        EventDialog(
            venues = venues,
            onDismiss = { newEvent = false },
            onSave = { venueId, sport, title, desc, start, dur, cap, a, b, promo ->
                vm.createEvent(venueId, sport, title, desc, start, dur, cap, a, b, promo)
                newEvent = false
            },
        )
    }

    if (newBlock) {
        BlockDialog(
            venues = venues,
            courts = courts,
            onDismiss = { newBlock = false },
            onSave = { venueId, courtId, start, end, reason ->
                vm.addBlock(venueId, courtId, start, end, reason)
                newBlock = false
            },
        )
    }
}

@Composable
private fun Loading() = Column(
    Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
) { CircularProgressIndicator() }

@Composable
private fun Empty(text: String) = Column(
    Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
) { Text(text, style = MaterialTheme.typography.bodyLarge) }

@Composable
private fun Problem(text: String, retry: () -> Unit) = Column(
    Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
    OutlinedButton(onClick = retry, modifier = Modifier.padding(top = 12.dp)) { Text("Tentar de novo") }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourtDialog(
    venue: Venue,
    court: Court?,
    onDismiss: () -> Unit,
    onSave: (String, List<String>, Boolean, Int?, String) -> Unit,
) {
    var name by remember { mutableStateOf(court?.name.orEmpty()) }
    var free by remember { mutableStateOf(court?.isFree ?: true) }
    var price by remember { mutableStateOf(court?.pricePerHourCents?.let { "%d,%02d".format(it / 100, it % 100) }.orEmpty()) }
    var audience by remember { mutableStateOf(court?.audience.orEmpty()) }
    var sports by remember { mutableStateOf(court?.sports.orEmpty().toSet()) }

    // "70", "70,5" e "70,50" viram centavos; vazio ou inválido = null
    val cents: Int? = price.trim().replace(',', '.').toDoubleOrNull()?.let { Math.round(it * 100).toInt() }?.takeIf { it >= 0 }
    val valid = name.trim().isNotEmpty() && (free || cents != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (court == null) "Nova quadra em ${venue.name}" else "Editar quadra") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(60) }, label = { Text("Nome (ex.: Quadra 2)") }, singleLine = true)
                Text("Esportes", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sport.entries.forEach { s ->
                        FilterChip(
                            selected = s.key in sports,
                            onClick = { sports = if (s.key in sports) sports - s.key else sports + s.key },
                            label = { Text(s.label) },
                            colors = appChipColors(),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Uso gratuito", modifier = Modifier.weight(1f))
                    Switch(checked = free, onCheckedChange = { free = it })
                }
                if (!free) {
                    OutlinedTextField(
                        price, { price = it.take(8) },
                        label = { Text("Preço por hora (R$)") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                OutlinedTextField(
                    audience, { audience = it.take(60) },
                    label = { Text("Quem pode usar (opcional)") }, singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), sports.toList(), free, if (free) null else cents, audience.trim()) }, enabled = valid) {
                Text("Salvar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Voltar") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BlockDialog(
    venues: List<Venue>,
    courts: List<Court>,
    onDismiss: () -> Unit,
    onSave: (String, String?, Instant, Instant, String) -> Unit,
) {
    var venueId by remember { mutableStateOf<String?>(null) }
    var courtId by remember { mutableStateOf<String?>(null) }
    var date by remember { mutableStateOf(LocalDate.now(ZONE)) }
    var start by remember { mutableStateOf(LocalTime.of(18, 0)) }
    var end by remember { mutableStateOf(LocalTime.of(20, 0)) }
    var reason by remember { mutableStateOf("") }
    var pick by remember { mutableStateOf<String?>(null) } // "date" | "start" | "end"

    val startInstant = date.atTime(start).atZone(ZONE).toInstant()
    val endInstant = date.atTime(end).atZone(ZONE).toInstant()
    val valid = venueId != null && endInstant.isAfter(startInstant)
    val venueCourts = courts.filter { it.venueId == venueId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bloquear horário") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Local", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    venues.forEach { v ->
                        FilterChip(
                            selected = venueId == v.id,
                            onClick = { venueId = v.id; courtId = null },
                            label = { Text(v.name) },
                            colors = appChipColors(),
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                if (venueCourts.size > 1) {
                    Text("Quadra", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = courtId == null, onClick = { courtId = null }, label = { Text("Todas") }, colors = appChipColors())
                        venueCourts.forEach { c ->
                            FilterChip(selected = courtId == c.id, onClick = { courtId = c.id }, label = { Text(c.name) }, colors = appChipColors())
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pick = "date" }) { Text(date.format(DateTimeFormatter.ofPattern("EEE dd/MM", Locale("pt", "BR")))) }
                    OutlinedButton(onClick = { pick = "start" }) { Text("das " + start.format(HOUR_FMT)) }
                    OutlinedButton(onClick = { pick = "end" }) { Text("às " + end.format(HOUR_FMT)) }
                }
                if (!endInstant.isAfter(startInstant)) {
                    Text("O fim precisa ser depois do início.", color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    reason, { reason = it.take(80) },
                    label = { Text("Motivo (opcional)") }, singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(venueId!!, courtId, startInstant, endInstant, reason.trim()) }, enabled = valid) {
                Text("Bloquear")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Voltar") } },
    )

    when (pick) {
        "date" -> {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
            )
            DatePickerDialog(
                onDismissRequest = { pick = null },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let {
                            date = Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        }
                        pick = null
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { pick = null }) { Text("Voltar") } },
            ) { DatePicker(state) }
        }
        "start", "end" -> {
            val isStart = pick == "start"
            val base = if (isStart) start else end
            val state = rememberTimePickerState(initialHour = base.hour, initialMinute = base.minute, is24Hour = true)
            AlertDialog(
                onDismissRequest = { pick = null },
                confirmButton = {
                    TextButton(onClick = {
                        val t = LocalTime.of(state.hour, state.minute)
                        if (isStart) start = t else end = t
                        pick = null
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { pick = null }) { Text("Voltar") } },
                text = { TimePicker(state) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EventDialog(
    venues: List<Venue>,
    onDismiss: () -> Unit,
    onSave: (String, Sport, String, String, Instant, Int, Int, String, String, Boolean) -> Unit,
) {
    var venueId by remember { mutableStateOf<String?>(null) }
    var sport by remember { mutableStateOf(Sport.Corrida) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var routeStart by remember { mutableStateOf("") }
    var routeEnd by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("100") }
    var hours by remember { mutableStateOf("2") }
    var promoted by remember { mutableStateOf(true) }
    var date by remember { mutableStateOf(LocalDate.now(ZONE).plusDays(7)) }
    var time by remember { mutableStateOf(LocalTime.of(7, 0)) }
    var pick by remember { mutableStateOf<String?>(null) }

    val cap = capacity.toIntOrNull()
    val dur = hours.toIntOrNull()
    val start = date.atTime(time).atZone(ZONE).toInstant()
    val valid = venueId != null && title.trim().length in 2..60 &&
        cap != null && cap in 1..500 && dur != null && dur in 1..12 && start.isAfter(Instant.now())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo evento") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it.take(60) }, label = { Text("Nome do evento") }, singleLine = true)
                Text("Modalidade", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sport.entries.forEach { s ->
                        FilterChip(selected = sport == s, onClick = { sport = s }, label = { Text(s.label) }, colors = appChipColors())
                    }
                }
                Text("Local de encontro", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    venues.forEach { v ->
                        FilterChip(selected = venueId == v.id, onClick = { venueId = v.id }, label = { Text(v.name) }, colors = appChipColors())
                    }
                }
                OutlinedTextField(routeStart, { routeStart = it.take(120) }, label = { Text("Largada (endereço ou lugar)") })
                OutlinedTextField(routeEnd, { routeEnd = it.take(120) }, label = { Text("Chegada (endereço ou lugar)") })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pick = "date" }) { Text(date.format(DateTimeFormatter.ofPattern("EEE dd/MM", Locale("pt", "BR")))) }
                    OutlinedButton(onClick = { pick = "time" }) { Text(time.format(HOUR_FMT)) }
                }
                OutlinedTextField(
                    hours, { hours = it.filter(Char::isDigit).take(2) }, label = { Text("Duração (horas)") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    capacity, { capacity = it.filter(Char::isDigit).take(3) }, label = { Text("Vagas (até 500)") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(description, { description = it.take(300) }, label = { Text("Descrição (opcional)") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Destacar no topo do feed", modifier = Modifier.weight(1f))
                    Switch(checked = promoted, onCheckedChange = { promoted = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(venueId!!, sport, title.trim(), description.trim(), start, dur!! * 60, cap!!, routeStart.trim(), routeEnd.trim(), promoted)
                },
                enabled = valid,
            ) { Text("Criar evento") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Voltar") } },
    )

    when (pick) {
        "date" -> {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
            )
            DatePickerDialog(
                onDismissRequest = { pick = null },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let {
                            date = Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        }
                        pick = null
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { pick = null }) { Text("Voltar") } },
            ) { DatePicker(state) }
        }
        "time" -> {
            val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
            AlertDialog(
                onDismissRequest = { pick = null },
                confirmButton = {
                    TextButton(onClick = { time = LocalTime.of(state.hour, state.minute); pick = null }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { pick = null }) { Text("Voltar") } },
                text = { TimePicker(state) },
            )
        }
    }
}
