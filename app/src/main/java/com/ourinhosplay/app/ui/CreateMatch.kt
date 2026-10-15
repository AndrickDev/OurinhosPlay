package com.ourinhosplay.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ourinhosplay.app.data.Availability
import com.ourinhosplay.app.data.BusinessException
import com.ourinhosplay.app.data.Court
import com.ourinhosplay.app.data.CreateMatchRepository
import com.ourinhosplay.app.data.NewMatch
import com.ourinhosplay.app.data.Sport
import com.ourinhosplay.app.data.Venue
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val ZONE: ZoneId get() = ZoneId.systemDefault()

/** O local oferece a modalidade se ele (ou alguma quadra dele) a lista. */
internal fun venueOffers(v: Venue, courts: List<Court>, s: Sport): Boolean =
    s.key in v.sports || courts.any { it.venueId == v.id && s.key in it.sports }

private fun defaultStart(): LocalDateTime =
    LocalDateTime.now(ZONE).plusHours(2).withMinute(0).withSecond(0).withNano(0)

sealed interface Check {
    data object Idle : Check
    data object Loading : Check
    data class Done(val a: Availability) : Check
    data class Failed(val message: String) : Check
}

data class CreateUi(
    val venueId: String? = null,
    val courtId: String? = null,
    val needsCourt: Boolean = false,
    val sport: Sport? = null,
    val title: String = "",
    val description: String = "",
    // Próxima hora cheia depois de +1h; data e hora saem do mesmo instante (vira o dia junto).
    val date: LocalDate = defaultStart().toLocalDate(),
    val time: LocalTime = defaultStart().toLocalTime(),
    val durationMin: Int = 90,
    val capacity: Int = 10,
    val creatorJoins: Boolean = true,
    val check: Check = Check.Idle,
    val busy: Boolean = false,
    val error: String? = null,
    val created: Boolean = false,
) {
    val startsAt: Instant get() = LocalDateTime.of(date, time).atZone(ZONE).toInstant()
    val inFuture: Boolean get() = startsAt.isAfter(Instant.now())
    val isFree: Boolean get() = (check as? Check.Done)?.a?.isFree == true
    val canPublish: Boolean
        get() = !busy && venueId != null && (!needsCourt || courtId != null) && sport != null && title.trim().length in 2..60 && inFuture && isFree
}

class CreateMatchViewModel : ViewModel() {
    private val _ui = MutableStateFlow(CreateUi())
    val ui: StateFlow<CreateUi> = _ui.asStateFlow()
    private var checkJob: Job? = null

    fun setVenue(v: Venue, courts: List<Court> = emptyList()) {
        val mine = courts.filter { it.venueId == v.id }
        val only = mine.singleOrNull()
        _ui.update {
            val sports = (only?.sports ?: v.sports).mapNotNull { Sport.from(it) }
            it.copy(
                venueId = v.id,
                courtId = only?.id,
                needsCourt = mine.isNotEmpty(),
                sport = it.sport?.takeIf { s -> s in sports } ?: sports.singleOrNull(),
            )
        }
        recheck()
    }

    fun setCourt(c: Court) {
        _ui.update {
            val sports = c.sports.mapNotNull { Sport.from(it) }
            it.copy(courtId = c.id, sport = it.sport?.takeIf { s -> s in sports } ?: sports.singleOrNull())
        }
        recheck()
    }

    /** "Jogar de novo": copia quadra, esporte, título e vagas de um jogo anterior. */
    fun prefill(m: com.ourinhosplay.app.data.MatchCard) {
        _ui.update {
            it.copy(
                venueId = m.venueId,
                courtId = m.courtId,
                needsCourt = m.courtId != null,
                sport = Sport.from(m.sport),
                title = m.title,
                description = m.description.orEmpty(),
                capacity = m.capacity,
            )
        }
        recheck()
    }

    fun setSport(s: Sport) = _ui.update { it.copy(sport = s) }

    /** Escolhe a modalidade e limpa o local/quadra que não oferecem ela. */
    fun chooseSport(s: Sport, venues: List<Venue>, courts: List<Court>) {
        val cur = _ui.value.venueId?.let { id -> venues.firstOrNull { it.id == id } }
        val keep = cur != null && venueOffers(cur, courts, s)
        _ui.update {
            if (keep) it.copy(sport = s)
            else it.copy(sport = s, venueId = null, courtId = null, needsCourt = false, check = Check.Idle)
        }
        if (keep) {
            // quadra escolhida precisa aceitar a modalidade
            val courtOk = _ui.value.courtId?.let { cid -> courts.firstOrNull { it.id == cid }?.sports?.contains(s.key) } ?: true
            if (!courtOk) _ui.update { it.copy(courtId = null) }
        }
    }
    fun setTitle(t: String) = _ui.update { it.copy(title = t.take(60)) }
    fun setDescription(t: String) = _ui.update { it.copy(description = t.take(300)) }
    fun setCreatorJoins(v: Boolean) = _ui.update { it.copy(creatorJoins = v) }
    fun setCapacity(n: Int) = _ui.update { it.copy(capacity = n.coerceIn(1, 40)) }

    fun setDate(d: LocalDate) { _ui.update { it.copy(date = d) }; recheck() }
    fun setTime(t: LocalTime) { _ui.update { it.copy(time = t) }; recheck() }
    fun setDuration(m: Int) { _ui.update { it.copy(durationMin = m) }; recheck() }

    /** Aplica a sugestão "próximo horário livre" devolvida pelo servidor. */
    fun applySuggestion(iso: String) {
        val z = runCatching { Instant.parse(iso) }.getOrNull()
            ?: runCatching { java.time.OffsetDateTime.parse(iso).toInstant() }.getOrNull() ?: return
        val l = z.atZone(ZONE)
        _ui.update { it.copy(date = l.toLocalDate(), time = l.toLocalTime().withSecond(0).withNano(0)) }
        recheck()
    }

    private fun recheck() {
        checkJob?.cancel()
        val s = _ui.value
        val venueId = s.venueId ?: return
        if (!s.inFuture) {
            _ui.update { it.copy(check = Check.Failed("Escolha um horário no futuro.")) }
            return
        }
        _ui.update { it.copy(check = Check.Loading) }
        checkJob = viewModelScope.launch {
            delay(300) // evita uma consulta por clique ao mexer em data/hora
            val result = runCatching { CreateMatchRepository.availability(venueId, s.startsAt, s.durationMin, s.courtId) }
            _ui.update {
                it.copy(
                    check = result.fold(
                        onSuccess = { a -> Check.Done(a) },
                        onFailure = { Check.Failed("Não consegui checar a quadra. Tente de novo.") },
                    ),
                )
            }
        }
    }

    fun publish() {
        val s = _ui.value
        if (!s.canPublish) return
        _ui.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val r = runCatching {
                CreateMatchRepository.create(
                    NewMatch(
                        venueId = s.venueId!!,
                        sport = s.sport!!,
                        title = s.title.trim(),
                        description = s.description,
                        startsAt = s.startsAt,
                        durationMin = s.durationMin,
                        capacity = s.capacity,
                        creatorJoins = s.creatorJoins,
                        courtId = s.courtId,
                    ),
                )
            }
            _ui.update {
                r.fold(
                    onSuccess = { _ -> it.copy(busy = false, created = true) },
                    onFailure = { e ->
                        val msg = (e as? BusinessException)?.message ?: "Sem conexão. Tente de novo."
                        // Perdeu a corrida para outro usuário: reconsulta para sugerir outro horário.
                        it.copy(busy = false, error = msg)
                    },
                )
            }
            if (!_ui.value.created) recheck()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateMatchScreen(
    venues: List<Venue>,
    courts: List<Court>,
    onClose: () -> Unit,
    onCreated: () -> Unit,
    vm: CreateMatchViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsState()
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(ui.description.isNotBlank()) }

    if (ui.created) {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.runtime.LaunchedEffect(Unit) {
            Sfx.fire(ctx, Sound.Fanfare)
            onCreated()
        }
    }

    val venueCourts = courts.filter { it.venueId == ui.venueId }
    val chosenCourt = venueCourts.firstOrNull { it.id == ui.courtId }
    val venueSports = (chosenCourt?.sports ?: venues.firstOrNull { it.id == ui.venueId }?.sports.orEmpty())
        .mapNotNull { Sport.from(it) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Criar jogo", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Cancelar") }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section("O que vão jogar?") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Sport.entries.forEach { s ->
                        val on = ui.sport == s
                        androidx.compose.foundation.layout.Column(
                            Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .background(if (on) s.color else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { vm.chooseSport(s, venues, courts) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .semantics { contentDescription = s.label + if (on) ", selecionado" else "" },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            androidx.compose.material3.Icon(
                                s.icon, contentDescription = null,
                                tint = if (on) Palette.Noite else s.color,
                                modifier = Modifier.size(30.dp),
                            )
                            Text(
                                s.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (on) Palette.Noite else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            Section("Onde") {
                val shown = venues.filter { v -> ui.sport != null && venueOffers(v, courts, ui.sport!!) }
                if (ui.sport == null) {
                    Text("Escolha a modalidade para ver os locais.", style = MaterialTheme.typography.bodyMedium)
                } else if (shown.isEmpty()) {
                    Text("Nenhum local cadastrado para ${ui.sport!!.label} ainda.", style = MaterialTheme.typography.bodyMedium)
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    shown.forEach { v ->
                        FilterChip(
                            selected = ui.venueId == v.id,
                            onClick = { vm.setVenue(v, courts) },
                            colors = appChipColors(),
                            label = { Text(v.name) },
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
            }

            if (venueCourts.size > 1) {
                Section("Quadra") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        venueCourts.forEach { c ->
                            FilterChip(
                                selected = ui.courtId == c.id,
                                onClick = { vm.setCourt(c) },
                                colors = appChipColors(),
                                label = { Text(c.name + " · " + c.priceLabel) },
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    }
                }
            } else if (chosenCourt != null) {
                Text(
                    chosenCourt.name + " · " + chosenCourt.priceLabel + (chosenCourt.audience?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = ui.title,
                onValueChange = vm::setTitle,
                label = { Text("Título (ex.: Pelada de quinta)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Section("Quando") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = { pickDate = true }) { Text(ui.date.format(DATE_FMT)) }
                    OutlinedButton(onClick = { pickTime = true }) { Text(ui.time.format(TIME_FMT)) }
                    listOf(60, 90, 120).forEach { m ->
                        FilterChip(
                            selected = ui.durationMin == m,
                            onClick = { vm.setDuration(m) },
                            colors = appChipColors(),
                            label = { Text("$m min") },
                        )
                    }
                }
                AvailabilityBanner(ui, onSuggestion = vm::applySuggestion)
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Vagas", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (ui.creatorJoins) "Você ocupa uma delas." else "Você só organiza.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = { vm.setCapacity(ui.capacity - 1) },
                    modifier = Modifier.semantics { contentDescription = "Menos uma vaga" },
                ) { Text("−") }
                Text("${ui.capacity}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 12.dp))
                OutlinedButton(
                    onClick = { vm.setCapacity(ui.capacity + 1) },
                    modifier = Modifier.semantics { contentDescription = "Mais uma vaga" },
                ) { Text("+") }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Eu também vou jogar", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(checked = ui.creatorJoins, onCheckedChange = vm::setCreatorJoins)
            }

            if (showDescription) {
                OutlinedTextField(
                    value = ui.description,
                    onValueChange = vm::setDescription,
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                TextButton(onClick = { showDescription = true }) { Text("+ Adicionar descrição") }
            }
        }

        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = vm::publish,
                enabled = ui.canPublish,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                if (ui.busy) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Publicar jogo")
            }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = ui.date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.setDate(Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate())
                    }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancelar") } },
        ) { DatePicker(state) }
    }

    if (pickTime) {
        val state = rememberTimePickerState(initialHour = ui.time.hour, initialMinute = ui.time.minute, is24Hour = true)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.setTime(LocalTime.of(state.hour, state.minute))
                    pickTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Cancelar") } },
            text = { TimePicker(state) },
        )
    }
}

@Composable
private fun AvailabilityBanner(ui: CreateUi, onSuggestion: (String) -> Unit) {
    when (val c = ui.check) {
        Check.Idle -> Text("Escolha a quadra para ver se o horário está livre.", style = MaterialTheme.typography.bodyMedium)
        Check.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(Modifier.padding(2.dp))
            Text("Checando a quadra…")
        }
        is Check.Failed -> Text(c.message, color = MaterialTheme.colorScheme.error)
        is Check.Done -> if (c.a.isFree) {
            Text("✅ Horário livre", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        } else {
            val who = if (c.a.reason == "block") "bloqueada pela administração" else "ocupada por outro jogo"
            val until = c.a.busyUntil?.let { hourLabel(it) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "⛔ Quadra $who" + (until?.let { " até $it" } ?: "") + ".",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium,
                )
                c.a.nextFreeAt?.let { next ->
                    OutlinedButton(onClick = { onSuggestion(next) }) { Text("Usar próximo livre: ${dayHourLabel(next)}") }
                }
            }
        }
    }
}

private val DATE_FMT = DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", Locale("pt", "BR"))
private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_HOUR_FMT = DateTimeFormatter.ofPattern("EEE dd/MM HH:mm", Locale("pt", "BR"))

private fun parseInstant(iso: String): Instant? =
    runCatching { Instant.parse(iso) }.getOrNull()
        ?: runCatching { java.time.OffsetDateTime.parse(iso).toInstant() }.getOrNull()

private fun hourLabel(iso: String): String? = parseInstant(iso)?.atZone(ZONE)?.format(TIME_FMT)
private fun dayHourLabel(iso: String): String = parseInstant(iso)?.atZone(ZONE)?.format(DAY_HOUR_FMT) ?: iso

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}
