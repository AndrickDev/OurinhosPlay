package com.ourinhosplay.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ourinhosplay.app.data.OptionCatalog
import com.ourinhosplay.app.data.BusinessException
import com.ourinhosplay.app.data.Court
import com.ourinhosplay.app.data.CreateMatchRepository
import com.ourinhosplay.app.data.NewMatch
import com.ourinhosplay.app.data.RoleCatalog
import com.ourinhosplay.app.data.RoleSpec
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
    /** Reserva rápida: só bloqueia a quadra (privada, 1 vaga, sem título nem opções). */
    val quick: Boolean = false,
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
    /** "Outro": a pessoa ajusta a duração de 15 em 15 minutos em vez de usar os atalhos. */
    val customDuration: Boolean = false,
    val capacity: Int = 10,
    val creatorJoins: Boolean = true,
    /** Selo "Universitário" (galera de faculdade/república). */
    val university: Boolean = false,
    /** Valor total da quadra em reais (texto digitado); vazio = sem rateio. */
    val costText: String = "",
    val pixKey: String = "",
    val isPrivate: Boolean = false,
    val level: String = "todos",
    val lookingFor: String = "",
    /** Interruptores de equipamento mexidos pela pessoa; vazio = não personalizou. */
    val options: Map<String, Boolean> = emptyMap(),
    /** 0 = jogo único; 4/8/12 = repete toda semana por N semanas. */
    val repeatWeeks: Int = 0,
    /** Vagas da função rara da modalidade (goleiro...); 0 = jogo sem funções. */
    val roleSlots: Int = 0,
    val roleReserved: Boolean = true,
    val roleReleaseHours: Int = 3,
    /** 0, 50 ou 100: quanto do rateio quem joga na função deixa de pagar. */
    val rolePerk: Int = 100,
    val check: Check = Check.Idle,
    val busy: Boolean = false,
    val error: String? = null,
    val created: Boolean = false,
    /** Id do primeiro jogo criado: a tela abre o detalhe dele em seguida. */
    val createdId: String? = null,
) {
    val startsAt: Instant get() = LocalDateTime.of(date, time).atZone(ZONE).toInstant()
    val inFuture: Boolean get() = startsAt.isAfter(Instant.now())
    val isFree: Boolean get() = (check as? Check.Done)?.a?.isFree == true
    val roleTemplate get() = RoleCatalog.forSport(sport)
    val rolesFit: Boolean get() = roleSlots == 0 || roleSlots < capacity
    val canPublish: Boolean
        get() = !busy && venueId != null && (!needsCourt || courtId != null) && sport != null &&
            (quick || title.trim().length in 2..60) && inFuture && isFree && (quick || rolesFit)
}

class CreateMatchViewModel : ViewModel() {
    private val _ui = MutableStateFlow(CreateUi())
    val ui: StateFlow<CreateUi> = _ui.asStateFlow()
    private var checkJob: Job? = null

    fun setQuick(v: Boolean) = _ui.update { it.copy(quick = v) }

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
            val sport = Sport.from(m.sport)
            it.copy(
                venueId = m.venueId,
                courtId = m.courtId,
                needsCourt = m.courtId != null,
                sport = sport,
                title = m.title,
                description = m.description.orEmpty(),
                capacity = m.capacity,
                roleSlots = RoleCatalog.forSport(sport)?.defaultSlots ?: 0,
            )
        }
        recheck()
    }

    /** Reserva rápida: começa na última quadra usada, se ainda existir. */
    fun applyLast(sportKey: String?, venueId: String?, courtId: String?, venues: List<Venue>, courts: List<Court>) {
        val sport = sportKey?.let { Sport.from(it) } ?: return
        val venue = venues.firstOrNull { it.id == venueId } ?: return
        val court = courts.firstOrNull { it.id == courtId && it.venueId == venue.id }
        _ui.update {
            it.copy(
                sport = sport,
                venueId = venue.id,
                courtId = court?.id,
                needsCourt = courts.any { c -> c.venueId == venue.id },
            )
        }
        recheck()
    }

    fun setSport(s: Sport) = _ui.update { it.copy(sport = s) }

    /** Escolhe a modalidade e limpa o local/quadra que não oferecem ela. */
    fun chooseSport(s: Sport, venues: List<Venue>, courts: List<Court>) {
        val cur = _ui.value.venueId?.let { id -> venues.firstOrNull { it.id == id } }
        val keep = cur != null && venueOffers(cur, courts, s)
        val slots = RoleCatalog.forSport(s)?.defaultSlots ?: 0
        _ui.update {
            if (keep) it.copy(sport = s, roleSlots = slots)
            else it.copy(sport = s, roleSlots = slots, venueId = null, courtId = null, needsCourt = false, check = Check.Idle)
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
    fun setUniversity(v: Boolean) = _ui.update { it.copy(university = v) }
    fun setCost(t: String) = _ui.update { it.copy(costText = t.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(8)) }
    fun setPix(t: String) = _ui.update { it.copy(pixKey = t.take(80)) }
    fun setPrivate(v: Boolean) = _ui.update { it.copy(isPrivate = v) }
    fun setLevel(v: String) = _ui.update { it.copy(level = v) }
    fun setOption(key: String, v: Boolean) = _ui.update { it.copy(options = it.options + (key to v)) }
    fun setLookingFor(t: String) = _ui.update { it.copy(lookingFor = t.take(60)) }
    fun setRepeat(weeks: Int) = _ui.update { it.copy(repeatWeeks = weeks) }
    fun setCapacity(n: Int) = _ui.update { it.copy(capacity = n.coerceIn(1, 40)) }
    fun setRoleSlots(n: Int) = _ui.update { it.copy(roleSlots = n.coerceIn(0, 4)) }
    fun setRoleReserved(v: Boolean) = _ui.update { it.copy(roleReserved = v) }
    fun setRoleRelease(h: Int) = _ui.update { it.copy(roleReleaseHours = h) }
    fun setRolePerk(p: Int) = _ui.update { it.copy(rolePerk = p) }

    fun setDate(d: LocalDate) { _ui.update { it.copy(date = d) }; recheck() }
    fun setTime(t: LocalTime) { _ui.update { it.copy(time = t) }; recheck() }
    /** Atalho (1h, 1h30, 2h): sai do modo "Outro". */
    fun setDuration(m: Int) { _ui.update { it.copy(durationMin = m.coerceIn(30, 360), customDuration = false) }; recheck() }

    /** Ajuste fino: mantém o modo "Outro", de 30 min a 6 h. */
    fun adjustDuration(deltaMin: Int) {
        _ui.update { it.copy(durationMin = (it.durationMin + deltaMin).coerceIn(30, 360), customDuration = true) }
        recheck()
    }

    fun startCustomDuration() = _ui.update { it.copy(customDuration = true) }

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
            val total = if (s.repeatWeeks > 0) s.repeatWeeks else 1
            val cents = if (s.quick) null else s.costText.trim().replace(',', '.').toDoubleOrNull()?.let { Math.round(it * 100).toInt() }
            val template = s.roleTemplate
            val roles = if (!s.quick && template != null && s.roleSlots > 0) {
                listOf(RoleSpec(template.key, s.roleSlots, s.roleReserved, s.roleReleaseHours, s.rolePerk))
            } else emptyList()
            var done = 0
            var firstId: String? = null
            var failure: Throwable? = null
            for (i in 0 until total) {
                val r = runCatching {
                    CreateMatchRepository.create(
                        NewMatch(
                            venueId = s.venueId!!,
                            sport = s.sport!!,
                            title = if (s.quick) "Reserva · ${s.sport.label}" else s.title.trim(),
                            description = if (s.quick) "" else s.description,
                            startsAt = s.startsAt.plus(java.time.Duration.ofDays(7L * i)),
                            durationMin = s.durationMin,
                            capacity = if (s.quick) 1 else s.capacity,
                            creatorJoins = if (s.quick) true else s.creatorJoins,
                            courtId = s.courtId,
                            costCents = cents,
                            pixKey = if (s.quick) "" else s.pixKey.trim(),
                            isPrivate = s.quick || s.isPrivate,
                            level = if (s.quick) "todos" else s.level,
                            lookingFor = if (s.quick) "" else s.lookingFor.trim(),
                            roles = roles,
                            // Mexeu em algum interruptor: os demais da modalidade contam como "não".
                            options = if (s.quick) emptyMap()
                            else com.ourinhosplay.app.data.MatchTags.build(s.sport, s.options, s.university),
                        ),
                    )
                }
                if (r.isSuccess) { done++; firstId = firstId ?: r.getOrNull() } else { failure = r.exceptionOrNull(); break }
            }
            _ui.update {
                when {
                    done == total -> it.copy(busy = false, created = true, createdId = firstId)
                    done > 0 -> it.copy(
                        busy = false,
                        created = true,
                        createdId = firstId,
                        error = null,
                    ).also { _ -> lastPartial = "Criamos $done de $total jogos. O próximo bateu em outro horário." }
                    else -> {
                        val msg = (failure as? BusinessException)?.message ?: "Sem conexão. Tente de novo."
                        it.copy(busy = false, error = msg)
                    }
                }
            }
            if (!_ui.value.created) recheck()
        }
    }

    /** Aviso para mostrar depois que um jogo recorrente foi criado só em parte. */
    var lastPartial: String? = null
        private set
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateMatchScreen(
    venues: List<Venue>,
    courts: List<Court>,
    onClose: () -> Unit,
    onCreated: (String?) -> Unit,
    vm: CreateMatchViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsState()
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(ui.description.isNotBlank()) }
    var moreOptions by remember { mutableStateOf(false) }

    if (ui.created) {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.runtime.LaunchedEffect(Unit) {
            Sfx.fire(ctx, Sound.Fanfare)
            onCreated(ui.createdId)
        }
    }

    val accent = ui.sport?.color ?: MaterialTheme.colorScheme.primary

    GlassBackdrop() {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Jogo com a galera", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onClose) { Text("Cancelar") }
            }

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StepCard("1", "O que vão jogar?", accent) { SportPicker(ui.sport) { vm.chooseSport(it, venues, courts) } }
                StepCard("2", "Onde", accent) { WherePicker(ui, vm, venues, courts) }
                StepCard("3", "Quando", accent) { WhenPicker(ui, vm, { pickDate = true }, { pickTime = true }) }

                StepCard("4", "Detalhes", accent) {
                    OutlinedTextField(
                        value = ui.title,
                        onValueChange = vm::setTitle,
                        label = { Text("Nome do jogo (ex.: Pelada de quinta)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                            modifier = Modifier.size(48.dp).semantics { contentDescription = "Menos uma vaga" },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                        Text(
                            "${ui.capacity}",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(48.dp),
                        )
                        OutlinedButton(
                            onClick = { vm.setCapacity(ui.capacity + 1) },
                            modifier = Modifier.size(48.dp).semantics { contentDescription = "Mais uma vaga" },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Eu também vou jogar", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        androidx.compose.material3.Switch(checked = ui.creatorJoins, onCheckedChange = vm::setCreatorJoins)
                    }
                }

                // Função rara da modalidade (goleiro, levantador): vaga reservada + benefício.
                ui.roleTemplate?.let { t -> RolesCard(ui, vm, t, accent) }

                // Tudo que é opcional fica recolhido: quem não abre, não vê.
                StepCard("＋", "Mais opções", accent, onHeaderClick = { moreOptions = !moreOptions }, expanded = moreOptions) {
                    if (moreOptions) {
                        val defs = OptionCatalog.forSport(ui.sport)
                        if (defs.isNotEmpty()) {
                            Text("O que vai ter", style = MaterialTheme.typography.titleSmall)
                            defs.forEach { d -> OptionSwitch(d.label, ui.options[d.key] ?: false, { vm.setOption(d.key, it) }) }
                        }
                        Text("Público", style = MaterialTheme.typography.titleSmall)
                        OptionSwitch("Universitário (faculdade, república)", ui.university, vm::setUniversity)
                        Text("Nível", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("todos" to "Todos", "iniciante" to "Novato", "intermediario" to "Médio", "avancado" to "Craque").forEach { (key, label) ->
                                FilterChip(
                                    selected = ui.level == key,
                                    onClick = { vm.setLevel(key) },
                                    colors = appChipColors(),
                                    label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        OutlinedTextField(
                            value = ui.lookingFor,
                            onValueChange = vm::setLookingFor,
                            label = { Text("Procurando (ex.: mais 2 jogadores)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = ui.costText,
                            onValueChange = vm::setCost,
                            label = { Text("Valor da quadra (R$) para dividir") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (ui.costText.isNotBlank()) {
                            OutlinedTextField(
                                value = ui.pixKey,
                                onValueChange = vm::setPix,
                                label = { Text("Sua chave Pix (só quem entra no jogo vê)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Só com código", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Não aparece no feed. Entra quem tem o código do jogo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            androidx.compose.material3.Switch(checked = ui.isPrivate, onCheckedChange = vm::setPrivate)
                        }
                        RepeatChips(ui.repeatWeeks, vm::setRepeat)
                        if (showDescription) {
                            OutlinedTextField(
                                value = ui.description,
                                onValueChange = vm::setDescription,
                                label = { Text("Descrição") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            TextButton(onClick = { showDescription = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                                Text("+ Adicionar descrição")
                            }
                        }
                    } else {
                        Text(
                            "Nível, rateio com Pix, jogo só com código, repetir toda semana.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            PublishBar(ui, accent, "Publicar jogo", vm::publish)
        }
    }

    DateTimeDialogs(ui, vm, pickDate, pickTime, { pickDate = false }, { pickTime = false })
}

/** "Repetir toda semana" (0 = jogo único). */
@Composable
internal fun RepeatChips(selected: Int, onPick: (Int) -> Unit) {
    Text("Repetir", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(0 to "Não", 4 to "4 sem", 8 to "8 sem", 12 to "12 sem").forEach { (n, label) ->
            FilterChip(
                selected = selected == n,
                onClick = { onPick(n) },
                colors = appChipColors(),
                label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (selected > 0) {
        Text(
            "Cria um jogo por semana, no mesmo dia e horário, por $selected semanas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Rodapé de vidro com o botão principal e o erro, se houver. */
@Composable
internal fun PublishBar(ui: CreateUi, accent: Color, label: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = onClick,
            enabled = ui.canPublish,
            shape = RoundedCornerShape(18.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = Palette.Noite,
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        ) {
            if (ui.busy) CircularProgressIndicator(Modifier.size(20.dp), color = Palette.Noite) else Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Vaga reservada para a função rara, com benefício no rateio e liberação automática. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RolesCard(ui: CreateUi, vm: CreateMatchViewModel, t: com.ourinhosplay.app.data.RoleTemplate, accent: Color) {
    val on = ui.roleSlots > 0
    StepCard("★", "Vaga de ${t.label.lowercase()}", accent) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (on) "${ui.roleSlots} ${if (ui.roleSlots == 1) t.label.lowercase() else t.labelPlural.lowercase()} garantidos" else "Sem função reservada",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "Ninguém ocupa essa vaga com outra função.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.Switch(
                checked = on,
                onCheckedChange = { vm.setRoleSlots(if (it) t.defaultSlots else 0) },
            )
        }
        if (on) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Quantos", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = { vm.setRoleSlots(ui.roleSlots - 1) },
                    modifier = Modifier.size(44.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("−") }
                Text("${ui.roleSlots}", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.width(44.dp))
                OutlinedButton(
                    onClick = { vm.setRoleSlots(ui.roleSlots + 1) },
                    modifier = Modifier.size(44.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("+") }
            }
            Text("Quem joga de ${t.label.lowercase()} paga", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(100 to "Nada", 50 to "Metade", 0 to "Tudo").forEach { (pct, label) ->
                    FilterChip(
                        selected = ui.rolePerk == pct,
                        onClick = { vm.setRolePerk(pct) },
                        colors = appChipColors(),
                        label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text("Se ninguém pegar, libera para todos", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(1 to "1 h antes", 3 to "3 h antes", 6 to "6 h antes", 24 to "1 dia").forEach { (h, label) ->
                    FilterChip(
                        selected = ui.roleReleaseHours == h,
                        onClick = { vm.setRoleRelease(h) },
                        colors = appChipColors(),
                        label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (!ui.rolesFit) {
                Text(
                    "Aumente as vagas do jogo: precisa sobrar lugar para os outros jogadores.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** Escolha da modalidade: um cartão com a foto do esporte. Compartilhado entre criar jogo e reserva rápida. */
@Composable
internal fun SportPicker(selected: Sport?, onPick: (Sport) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Sport.entries.forEach { s ->
            val on = selected == s
            val shape = RoundedCornerShape(18.dp)
            androidx.compose.foundation.layout.Box(
                Modifier
                    .width(118.dp)
                    .height(96.dp)
                    .clip(shape)
                    .border(if (on) 2.dp else 1.dp, if (on) s.color else Glass.Hairline, shape)
                    .clickable { onPick(s) }
                    .semantics { contentDescription = s.label + if (on) ", selecionado" else "" },
            ) {
                SportPhoto(s, Modifier.matchParentSize())
                // quanto mais escuro, menos a foto compete com o texto; o selecionado fica mais claro
                androidx.compose.foundation.layout.Box(
                    Modifier.matchParentSize().background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0.0f to Palette.Noite.copy(alpha = if (on) 0.05f else 0.35f),
                            1.0f to Palette.Noite.copy(alpha = if (on) 0.80f else 0.92f),
                        ),
                    ),
                )
                androidx.compose.foundation.layout.Row(
                    Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    androidx.compose.material3.Icon(
                        s.icon, contentDescription = null,
                        tint = if (on) s.color else Palette.Giz,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        s.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Palette.Giz,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Local e quadra, filtrados pela modalidade escolhida. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WherePicker(ui: CreateUi, vm: CreateMatchViewModel, venues: List<Venue>, courts: List<Court>) {
    val venueCourts = courts.filter { it.venueId == ui.venueId && (ui.sport == null || ui.sport!!.key in it.sports) }
    val chosenCourt = venueCourts.firstOrNull { it.id == ui.courtId }
    val accent = ui.sport?.color ?: MaterialTheme.colorScheme.primary
    val sport = ui.sport
    val shown = venues.filter { v -> sport != null && venueOffers(v, courts, sport) }
    when {
        sport == null -> Text("Escolha a modalidade para ver os locais.", style = MaterialTheme.typography.bodyMedium)
        shown.isEmpty() -> Text("Nenhum local cadastrado para ${sport.label} ainda.", style = MaterialTheme.typography.bodyMedium)
    }
    shown.forEach { v ->
        val on = ui.venueId == v.id
        val cs = courts.filter { it.venueId == v.id && sport != null && sport.key in it.sports }
        val price = when {
            cs.isEmpty() -> if (v.kind == "public") "Pública" else "Particular"
            cs.all { it.isFree } -> "Grátis"
            else -> cs.mapNotNull { it.pricePerHourCents }.minOrNull()
                ?.let { "a partir de R$ %d,%02d/h".format(it / 100, it % 100) } ?: "Paga"
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(if (on) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                .border(1.dp, if (on) accent else Color.White.copy(alpha = 0.10f), MaterialTheme.shapes.medium)
                .clickable { vm.setVenue(v, courts) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(v.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    price + if (cs.size > 1) " · ${cs.size} quadras" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (on) Text("✓", color = accent, style = MaterialTheme.typography.titleMedium)
        }
    }
    if (venueCourts.size > 1) {
        Text("Quadra", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
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
    } else if (chosenCourt != null) {
        Text(
            chosenCourt.name + " · " + chosenCourt.priceLabel + (chosenCourt.audience?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Data, hora, duração e a checagem de choque de horário. */
@Composable
internal fun WhenPicker(ui: CreateUi, vm: CreateMatchViewModel, onPickDate: () -> Unit, onPickTime: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onPickDate,
            modifier = Modifier.weight(1.4f).heightIn(min = 48.dp),
        ) { Text(ui.date.format(DATE_FMT), maxLines = 1) }
        OutlinedButton(
            onClick = onPickTime,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        ) { Text(ui.time.format(TIME_FMT)) }
    }
    Text("Duração", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(60, 90, 120).forEach { m ->
            FilterChip(
                selected = !ui.customDuration && ui.durationMin == m,
                onClick = { vm.setDuration(m) },
                colors = appChipColors(),
                label = { Text(formatDuration(m), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1) },
                modifier = Modifier.weight(1f),
            )
        }
        FilterChip(
            selected = ui.customDuration,
            onClick = vm::startCustomDuration,
            colors = appChipColors(),
            label = { Text("Outro", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1) },
            modifier = Modifier.weight(1f),
        )
    }
    if (ui.customDuration) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            listOf(-60 to "−1h", -15 to "−15").forEach { (d, label) ->
                OutlinedButton(
                    onClick = { vm.adjustDuration(d) },
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp).semantics { contentDescription = "Diminuir ${-d} minutos" },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text(label) }
            }
            Text(
                formatDuration(ui.durationMin),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1.5f),
            )
            listOf(15 to "+15", 60 to "+1h").forEach { (d, label) ->
                OutlinedButton(
                    onClick = { vm.adjustDuration(d) },
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp).semantics { contentDescription = "Aumentar $d minutos" },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text(label) }
            }
        }
    }
    val endsAt = LocalDateTime.of(ui.date, ui.time).plusMinutes(ui.durationMin.toLong())
    Text(
        "Termina às " + endsAt.toLocalTime().format(TIME_FMT) + if (endsAt.toLocalDate() != ui.date) " (dia seguinte)" else "",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    AvailabilityBanner(ui, onSuggestion = vm::applySuggestion)
}

/** Os seletores de data e hora (diálogos do Material). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimeDialogs(
    ui: CreateUi,
    vm: CreateMatchViewModel,
    showDate: Boolean,
    showTime: Boolean,
    onCloseDate: () -> Unit,
    onCloseTime: () -> Unit,
) {
    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = ui.date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = onCloseDate,
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.setDate(Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate())
                    }
                    onCloseDate()
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = onCloseDate) { Text("Cancelar") } },
        ) { DatePicker(state) }
    }

    if (showTime) {
        val state = rememberTimePickerState(initialHour = ui.time.hour, initialMinute = ui.time.minute, is24Hour = true)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onCloseTime,
            confirmButton = {
                TextButton(onClick = {
                    vm.setTime(LocalTime.of(state.hour, state.minute))
                    onCloseTime()
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = onCloseTime) { Text("Cancelar") } },
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
            Text("✓ Horário livre", color = Palette.Grama, style = MaterialTheme.typography.titleMedium)
        } else {
            val who = if (c.a.reason == "block") "bloqueada pela administração" else "ocupada por outro jogo"
            val until = c.a.busyUntil?.let { hourLabel(it) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Quadra $who" + (until?.let { " até $it" } ?: "") + ".",
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

/** Cartão de vidro com número do passo e título. */
@Composable
internal fun StepCard(
    step: String,
    title: String,
    accent: Color = Palette.Pista,
    onHeaderClick: (() -> Unit)? = null,
    expanded: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .card()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().let { if (onHeaderClick != null) it.clickable(onClick = onHeaderClick) else it },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            androidx.compose.foundation.layout.Box(
                Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape).background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(step, style = MaterialTheme.typography.labelLarge, color = Palette.Noite)
            }
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (onHeaderClick != null) {
                Text(if (expanded) "Recolher" else "Abrir", style = MaterialTheme.typography.labelMedium, color = accent)
            }
        }
        content()
    }
}
