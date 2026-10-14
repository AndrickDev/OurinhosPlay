package com.ourinhosplay.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ourinhosplay.app.data.Availability
import com.ourinhosplay.app.data.BusinessException
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
    val sport: Sport? = null,
    val title: String = "",
    val description: String = "",
    // Próxima hora cheia depois de +1h; data e hora saem do mesmo instante (vira o dia junto).
    val date: LocalDate = defaultStart().toLocalDate(),
    val time: LocalTime = defaultStart().toLocalTime(),
    val durationMin: Int = 90,
    val capacity: Int = 10,
    val check: Check = Check.Idle,
    val busy: Boolean = false,
    val error: String? = null,
    val created: Boolean = false,
) {
    val startsAt: Instant get() = LocalDateTime.of(date, time).atZone(ZONE).toInstant()
    val inFuture: Boolean get() = startsAt.isAfter(Instant.now())
    val isFree: Boolean get() = (check as? Check.Done)?.a?.isFree == true
    val canPublish: Boolean
        get() = !busy && venueId != null && sport != null && title.trim().length in 2..60 && inFuture && isFree
}

class CreateMatchViewModel : ViewModel() {
    private val _ui = MutableStateFlow(CreateUi())
    val ui: StateFlow<CreateUi> = _ui.asStateFlow()
    private var checkJob: Job? = null

    fun setVenue(v: Venue) {
        _ui.update {
            val sports = v.sports.mapNotNull { Sport.from(it) }
            it.copy(venueId = v.id, sport = it.sport?.takeIf { s -> s in sports } ?: sports.singleOrNull())
        }
        recheck()
    }

    fun setSport(s: Sport) = _ui.update { it.copy(sport = s) }
    fun setTitle(t: String) = _ui.update { it.copy(title = t.take(60)) }
    fun setDescription(t: String) = _ui.update { it.copy(description = t.take(300)) }
    fun setCapacity(n: Int) = _ui.update { it.copy(capacity = n.coerceIn(2, 40)) }

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
            val result = runCatching { CreateMatchRepository.availability(venueId, s.startsAt, s.durationMin) }
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
    onClose: () -> Unit,
    onCreated: () -> Unit,
    vm: CreateMatchViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsState()
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    if (ui.created) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onCreated() }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Criar jogo", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Cancelar") }
        }

        Text("Quadra", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            venues.forEach { v ->
                FilterChip(
                    selected = ui.venueId == v.id,
                    onClick = { vm.setVenue(v) },
                    label = { Text(v.name) },
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }

        val venueSports = venues.firstOrNull { it.id == ui.venueId }?.sports?.mapNotNull { Sport.from(it) }.orEmpty()
        if (venueSports.isNotEmpty()) {
            Text("Esporte", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                venueSports.forEach { s ->
                    FilterChip(
                        selected = ui.sport == s,
                        onClick = { vm.setSport(s) },
                        label = { Text("${s.emoji} ${s.label}") },
                    )
                }
            }
        }

        OutlinedTextField(
            value = ui.title,
            onValueChange = vm::setTitle,
            label = { Text("Título (ex.: Pelada de quinta)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Quando", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pickDate = true }) { Text(ui.date.format(DATE_FMT)) }
            OutlinedButton(onClick = { pickTime = true }) { Text(ui.time.format(TIME_FMT)) }
        }

        Text("Duração", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(60, 90, 120).forEach { m ->
                FilterChip(
                    selected = ui.durationMin == m,
                    onClick = { vm.setDuration(m) },
                    label = { Text("$m min") },
                )
            }
        }

        AvailabilityBanner(ui, onSuggestion = vm::applySuggestion)

        Text("Vagas", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { vm.setCapacity(ui.capacity - 1) }) { Text("−") }
            Text("${ui.capacity}", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { vm.setCapacity(ui.capacity + 1) }) { Text("+") }
        }

        OutlinedTextField(
            value = ui.description,
            onValueChange = vm::setDescription,
            label = { Text("Descrição (opcional)") },
            modifier = Modifier.fillMaxWidth(),
        )

        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(onClick = vm::publish, enabled = ui.canPublish, modifier = Modifier.fillMaxWidth()) {
            if (ui.busy) CircularProgressIndicator(Modifier.padding(2.dp)) else Text("Publicar jogo")
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
