package com.ourinhosplay.app.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ourinhosplay.app.data.BusinessException
import com.ourinhosplay.app.data.MatchCard
import com.ourinhosplay.app.data.MatchDetailRepository
import com.ourinhosplay.app.data.Participant
import com.ourinhosplay.app.data.Sport
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUi(
    val loading: Boolean = true,
    val error: String? = null,
    val match: MatchCard? = null,
    val participants: List<Participant> = emptyList(),
    val me: String? = null,
    val busy: Boolean = false,
    val toast: String? = null,
    /** Sobe a cada entrada confirmada; dispara vibração e confete. */
    val celebrations: Int = 0,
    val isAdmin: Boolean = false,
    val friendIds: Set<String> = emptySet(),
    val leaves: Int = 0,
    val arrivals: Int = 0,
    /** Jogo excluído): a tela fecha. */
    val deleted: Boolean = false,
) {
    val canManage: Boolean get() = isCreator || isAdmin
    val cancelled: Boolean get() = match?.status == "cancelled"
    val isMember: Boolean get() = participants.any { it.userId == me }
    val isCreator: Boolean get() = match?.creatorId != null && match.creatorId == me
    val ended: Boolean
        get() {
            val m = match ?: return false
            if (m.status == "finished" || m.status == "cancelled") return true
            val end = runCatching { OffsetDateTime.parse(m.endsAt).toInstant() }.getOrNull() ?: return false
            return end.isBefore(Instant.now())
        }
}

class MatchDetailViewModel(private val matchId: String) : ViewModel() {
    private val _ui = MutableStateFlow(DetailUi(me = MatchDetailRepository.currentUserId()))
    val ui: StateFlow<DetailUi> = _ui.asStateFlow()

    init {
        load()
        viewModelScope.launch { runCatching { MatchDetailRepository.isAdmin() }.onSuccess { a -> _ui.update { it.copy(isAdmin = a) } } }
        viewModelScope.launch {
            runCatching { com.ourinhosplay.app.data.FriendsRepository.list() }
                .onSuccess { f -> _ui.update { it.copy(friendIds = f.map { x -> x.id }.toSet()) } }
        }
    }

    fun load() {
        _ui.update { it.copy(loading = it.match == null, error = null) }
        viewModelScope.launch {
            val r = runCatching {
                coroutineScope {
                    val d = async { MatchDetailRepository.detail(matchId) }
                    val p = async { MatchDetailRepository.participants(matchId) }
                    d.await() to p.await()
                }
            }
            _ui.update {
                r.fold(
                    onSuccess = { (m, p) ->
                        if (m == null) it.copy(loading = false, error = "Esse jogo não existe mais.")
                        else it.copy(loading = false, match = m, participants = p)
                    },
                    onFailure = { _ -> it.copy(loading = false, error = "Sem conexão. Tente de novo.") },
                )
            }
        }
    }

    fun join() = act { MatchDetailRepository.join(matchId) }

    fun leave() = act { MatchDetailRepository.leave(matchId) }

    fun arrive() {
        if (_ui.value.busy) return
        _ui.update { it.copy(busy = true, toast = null) }
        viewModelScope.launch {
            runCatching { com.ourinhosplay.app.data.CheckIn.arrive(matchId) }
                .onFailure { e ->
                    val msg = (e as? BusinessException)?.message ?: "Sem conexão. Tente de novo."
                    _ui.update { it.copy(busy = false, toast = msg) }
                }
                .onSuccess {
                    _ui.update { it.copy(busy = false, toast = "Avisamos a galera que você chegou.", arrivals = it.arrivals + 1) }
                    load()
                }
        }
    }

    fun toast(message: String) = _ui.update { it.copy(toast = message) }

    fun cancelMatch() = manage("Jogo cancelado.") { MatchDetailRepository.cancel(matchId) }

    fun deleteMatch() = manage(null) { MatchDetailRepository.delete(matchId) }

    fun edit(title: String, description: String, capacity: Int) =
        manage("Jogo atualizado.") { MatchDetailRepository.update(matchId, title.trim(), description.trim(), capacity) }

    /** Ação de organizador/admin: sem festa; mostra aviso e recarrega (ou fecha se excluiu). */
    private fun manage(okMessage: String?, block: suspend () -> Unit) {
        if (_ui.value.busy) return
        _ui.update { it.copy(busy = true, toast = null) }
        viewModelScope.launch {
            val r = runCatching { block() }
            r.onFailure { e ->
                val msg = (e as? BusinessException)?.message ?: "Sem conexão. Tente de novo."
                _ui.update { it.copy(busy = false, toast = msg) }
            }
            r.onSuccess {
                if (okMessage == null) _ui.update { it.copy(busy = false, deleted = true) }
                else {
                    _ui.update { it.copy(busy = false, toast = okMessage) }
                    load()
                }
            }
        }
    }

    private fun act(block: suspend () -> Unit) {
        if (_ui.value.busy) return
        val joining = !_ui.value.isMember
        _ui.update { it.copy(busy = true, toast = null) }
        viewModelScope.launch {
            val r = runCatching { block() }
            r.onFailure { e ->
                val msg = (e as? BusinessException)?.message ?: "Sem conexão. Tente de novo."
                _ui.update { it.copy(busy = false, toast = msg) }
                load() // reconcilia: a vaga pode ter acabado ou a pessoa já estar dentro
            }
            r.onSuccess {
                _ui.update {
                    it.copy(
                        busy = false,
                        celebrations = if (joining) it.celebrations + 1 else it.celebrations,
                        leaves = if (joining) it.leaves else it.leaves + 1,
                    )
                }
                load()
            }
        }
    }

    fun toastShown() = _ui.update { it.copy(toast = null) }
}

@Composable
fun MatchDetailScreen(matchId: String, onBack: () -> Unit) {
    val vm: MatchDetailViewModel = viewModel(key = "match-$matchId", factory = viewModelFactory {
        initializer { MatchDetailViewModel(matchId) }
    })
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val match = ui.match
    // O ViewModel sobrevive entre aberturas: só comemora o que acontece com a tela aberta.
    val baseCelebrations = remember { ui.celebrations }
    val baseLeaves = remember { ui.leaves }
    val baseArrivals = remember { ui.arrivals }
    val celebrate = ui.celebrations - baseCelebrations
    val leftNow = ui.leaves - baseLeaves
    val arrivedNow = ui.arrivals - baseArrivals

    LaunchedEffect(ui.deleted) { if (ui.deleted) onBack() }
    LaunchedEffect(ui.toast) {
        ui.toast?.let {
            snackbar.showSnackbar(it)
            vm.toastShown()
        }
    }
    val context = LocalContext.current
    val askNotifications = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { /* a notificação é refeita pelo efeito abaixo quando a permissão muda */ }

    // "Bora lá": vibra, apita e pede permissão de notificação para o cronômetro.
    LaunchedEffect(celebrate) {
        if (celebrate > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!MatchReminder.canNotify(context) && android.os.Build.VERSION.SDK_INT >= 33) {
                askNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            Sfx.fire(context, Sound.Whistle)
        }
    }

    LaunchedEffect(leftNow) { if (leftNow > 0) Sfx.fire(context, Sound.Down) }
    LaunchedEffect(arrivedNow) { if (arrivedNow > 0) Sfx.fire(context, Sound.Chime) }

    // Quem está no jogo ganha a notificação com cronômetro; quem sai (ou jogo acabou) perde.
    val reminderKey = Triple(ui.isMember, ui.ended, match?.id)
    LaunchedEffect(reminderKey, MatchReminder.canNotify(context)) {
        val m = match ?: return@LaunchedEffect
        val start = parseMillis(m.startsAt)
        val end = parseMillis(m.endsAt)
        if (ui.isMember && !ui.ended && start != null && end != null) {
            MatchReminder.show(context, m.id, m.title, m.venueName, start, end)
        } else {
            MatchReminder.cancel(context, m.id)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { if (ui.match != null) ActionBar(ui, vm::join, vm::leave) },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                TextButton(onClick = onBack) { Text("← Voltar") }
                when {
                    ui.loading -> Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { CircularProgressIndicator() }
                    match == null -> {
                        Text(ui.error ?: "Jogo não encontrado.", color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = vm::load, modifier = Modifier.padding(top = 12.dp)) { Text("Tentar de novo") }
                    }
                    else -> {
                        Details(ui, match, onArrive = vm::arrive, onToast = vm::toast)
                        if (ui.canManage) ManageCard(ui, match, vm)
                    }
                }
            }
            Confetti(trigger = celebrate, accent = match?.let { Sport.from(it.sport)?.color } ?: Palette.Pista)
            GoalBanner(trigger = celebrate)
        }
    }
}

@Composable
private fun Details(ui: DetailUi, m: MatchCard, onArrive: () -> Unit, onToast: (String) -> Unit) {
    val sport = Sport.from(m.sport)
    val color = sport?.color ?: Palette.Pista
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Capa do jogo: o esporte tinge a tela inteira da partida.
        Box(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(color),
        ) {
            SportArt(sport, Modifier.matchParentSize())
            Column(
                Modifier.fillMaxWidth(0.64f).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (sport != null) Icon(sport.icon, contentDescription = null, tint = Palette.Noite, modifier = Modifier.size(20.dp))
                Text(sport?.label ?: m.sport, style = MaterialTheme.typography.labelLarge, color = Palette.Noite)
            }
            Text(m.title, style = MaterialTheme.typography.headlineLarge, color = Palette.Noite)
            Text(m.venueName, style = MaterialTheme.typography.titleMedium, color = Palette.Noite)
            Text(whenFull(m.startsAt, m.endsAt), style = MaterialTheme.typography.bodyLarge, color = Palette.Noite)
            }
        }

        Countdown(m.startsAt, m.endsAt, ui.ended)
        WeatherCard(m.startsAt, ui.ended)

        val me = ui.participants.firstOrNull { it.userId == ui.me }
        val startMs = parseMillis(m.startsAt)
        val canArrive = ui.isMember && !ui.ended && me?.arrivedAt == null &&
            startMs != null && System.currentTimeMillis() >= startMs - 60 * 60 * 1000
        if (canArrive) {
            Button(onClick = onArrive, enabled = !ui.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("✅ Cheguei na quadra")
            }
        }

        if (!m.description.isNullOrBlank()) {
            Text(m.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (!m.routeStart.isNullOrBlank() && !m.routeEnd.isNullOrBlank()) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Largada: ${m.routeStart}", style = MaterialTheme.typography.bodyLarge)
                Text("Chegada: ${m.routeEnd}", style = MaterialTheme.typography.bodyLarge)
                OutlinedButton(onClick = { openRoute(ctx, m.routeStart!!, m.routeEnd!!) }) { Text("Ver trajeto no Maps") }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SlotBar(m.taken, m.capacity, color, Modifier.weight(1f))
            Text(
                when {
                    m.status == "cancelled" -> "Cancelado"
                    ui.ended -> "Encerrado"
                    else -> "${m.taken}/${m.capacity}"
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (!ui.ended) {
            val context = LocalContext.current
            OutlinedButton(
                onClick = { shareMatch(context, m) },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Chamar a galera")
            }
        }

        Text(
            "Quem vai",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp),
        )
        ui.participants.forEach { p ->
            val name = p.profiles?.fullName?.ifBlank { null } ?: "Jogador"
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        name.first().uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Palette.Noite,
                    )
                }
                Column {
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                    val tag = buildList {
                        if (p.userId == m.creatorId) add("organizador")
                        if (p.userId == ui.me) add("você")
                        if (p.userId != ui.me && p.userId in ui.friendIds) add("★ amigo")
                        if (p.arrivedAt != null) add("✓ chegou")
                    }.joinToString(" · ")
                    if (tag.isNotEmpty()) {
                        Text(tag, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (ui.isMember && !ui.cancelled) {
            ChatSection(m.id, color, onToast)
        }
    }
}

@Composable
private fun ActionBar(ui: DetailUi, onJoin: () -> Unit, onLeave: () -> Unit) {
    val m = ui.match ?: return
    val color = Sport.from(m.sport)?.color ?: Palette.Pista
    val full = m.status == "full" || m.spotsLeft == 0
    val big = Modifier.fillMaxWidth().height(60.dp)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        when {
            ui.ended -> Button(onClick = {}, enabled = false, modifier = big) {
                Text(if (ui.cancelled) "Cancelado" else "Encerrado")
            }
            ui.isMember -> OutlinedButton(onClick = onLeave, enabled = !ui.busy, modifier = big) {
                Text("Sair do jogo")
            }
            full -> Button(onClick = {}, enabled = false, modifier = big) { Text("Lotado") }
            else -> Button(
                onClick = onJoin,
                enabled = !ui.busy,
                modifier = big,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = color, contentColor = Palette.Noite),
            ) {
                Text(
                    if (ui.busy) "Entrando…" else "Bora lá!",
                    fontFamily = ScoreFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 28.sp,
                )
            }
        }
    }
}


private class Piece(
    val fromLeft: Boolean,
    val angle: Float,      // rad, direção do canhão
    val speed: Float,      // px/s (fração da largura)
    val size: Float,       // dp-ish
    val spin: Float,       // rad/s
    val flip: Float,       // rad/s do "virar" 3D
    val phase: Float,
    val color: Color,
    val shape: Int,        // 0 = tira, 1 = bolinha, 2 = estrela
)

/**
 * Confete premium: dois canhões nos cantos de baixo, peças que giram e "viram" em 3D,
 * arrasto do ar e gravidade, e some em fade. Respeita a animação desligada no sistema.
 */
@Composable
private fun Confetti(trigger: Int, accent: Color = Palette.Pista) {
    val context = LocalContext.current
    val progress = remember { Animatable(1f) }
    val palette = remember(accent) {
        listOf(accent, Palette.Pista, Palette.Bola, Palette.Giz, Palette.Sol, Palette.Mar)
    }
    val pieces = remember(trigger, palette) {
        List(90) { i ->
            val left = i % 2 == 0
            // cone de ~40° apontando para o centro e para cima
            val base = if (left) -1.1f else -(Math.PI.toFloat() - 1.1f)
            Piece(
                fromLeft = left,
                angle = base + (Random.nextFloat() - 0.5f) * 0.8f,
                speed = 0.9f + Random.nextFloat() * 1.1f,
                size = 7f + Random.nextFloat() * 9f,
                spin = (Random.nextFloat() - 0.5f) * 14f,
                flip = 4f + Random.nextFloat() * 9f,
                phase = Random.nextFloat() * 6.28f,
                color = palette.random(),
                shape = when (Random.nextInt(10)) { in 0..5 -> 0; in 6..8 -> 1; else -> 2 },
            )
        }
    }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        if (scale == 0f) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(2600, easing = LinearEasing))
    }
    if (progress.value >= 1f) return
    Canvas(Modifier.fillMaxSize()) {
        val total = 2.6f
        val t = progress.value * total
        val drag = 2.4f
        val g = size.height * 0.9f // gravidade em px/s²
        val fade = ((1f - progress.value) / 0.25f).coerceIn(0f, 1f)
        val decay = 1f - kotlin.math.exp(-drag * t)
        pieces.forEach { p ->
            val v = p.speed * size.width * 1.35f
            val vx = cos(p.angle) * v
            val vy = sin(p.angle) * v
            val x0 = if (p.fromLeft) -size.width * 0.02f else size.width * 1.02f
            val y0 = size.height * 0.98f
            val x = x0 + vx / drag * decay
            val y = y0 + g / drag * t + (vy - g / drag) / drag * decay
            val flipScale = kotlin.math.abs(cos(p.phase + p.flip * t))
            val rot = Math.toDegrees((p.spin * t).toDouble()).toFloat()
            val c = p.color.copy(alpha = fade)
            val s = p.size * density
            rotate(rot, pivot = Offset(x, y)) {
                when (p.shape) {
                    0 -> drawRect(
                        c,
                        topLeft = Offset(x - s * 0.5f, y - s * 0.25f * flipScale),
                        size = androidx.compose.ui.geometry.Size(s, s * 0.5f * flipScale + 0.5f),
                    )
                    1 -> drawCircle(c, s * 0.32f * (0.4f + 0.6f * flipScale), Offset(x, y))
                    else -> {
                        val path = androidx.compose.ui.graphics.Path()
                        for (k in 0 until 10) {
                            val r = if (k % 2 == 0) s * 0.55f else s * 0.24f
                            val a = Math.toRadians(-90.0 + 36.0 * k)
                            val px = x + r * cos(a).toFloat() * (0.4f + 0.6f * flipScale)
                            val py = y + r * sin(a).toFloat()
                            if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        path.close()
                        drawPath(path, c)
                    }
                }
            }
        }
    }
}

private val FULL_DAY = DateTimeFormatter.ofPattern("EEEE, dd/MM", Locale("pt", "BR"))
private val HOUR = DateTimeFormatter.ofPattern("HH:mm")

private fun whenFull(startIso: String, endIso: String): String {
    val z = ZoneId.systemDefault()
    val s = runCatching { OffsetDateTime.parse(startIso).toInstant().atZone(z) }.getOrNull() ?: return startIso
    val e = runCatching { OffsetDateTime.parse(endIso).toInstant().atZone(z) }.getOrNull()
    return s.format(FULL_DAY).replaceFirstChar { it.uppercase() } + " · " + s.format(HOUR) +
        (e?.let { " às " + it.format(HOUR) } ?: "")
}

private fun parseMillis(iso: String): Long? =
    runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrNull()

/** Contagem ao vivo: "Começa em 02:14:35" / "Rolando agora" (some quando encerra). */
@Composable
private fun Countdown(startIso: String, endIso: String, ended: Boolean) {
    if (ended) return
    val start = parseMillis(startIso) ?: return
    val end = parseMillis(endIso) ?: return
    var now by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    val label = when {
        now >= end -> return
        now >= start -> "🔥 Rolando agora"
        else -> {
            val s = (start - now) / 1000
            val d = s / 86400
            val h = (s % 86400) / 3600
            val m = (s % 3600) / 60
            val sec = s % 60
            "⏱ Começa em " + (if (d > 0) "${d}d " else "") + "%02d:%02d:%02d".format(h, m, sec)
        }
    }
    Text(
        label,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = Palette.Bola,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** "BORA LÁ!!!" que estoura na tela e some junto do confete. */
@Composable
private fun GoalBanner(trigger: Int) {
    val context = LocalContext.current
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        val animationsOff =
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        if (animationsOff) return@LaunchedEffect
        scale.snapTo(0.3f)
        alpha.snapTo(1f)
        scale.animateTo(1.15f, tween(350))
        kotlinx.coroutines.delay(700)
        alpha.animateTo(0f, tween(450))
    }
    if (alpha.value <= 0f) return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "BORA LÁ!!!",
            fontFamily = ScoreFont,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            fontSize = 58.sp,
            color = Palette.Bola,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = -5f
                this.alpha = alpha.value
            },
        )
    }
}

/** Editar / cancelar / excluir: só para o organizador e para admin. */
@Composable
private fun ManageCard(ui: DetailUi, m: MatchCard, vm: MatchDetailViewModel) {
    var edit by remember { androidx.compose.runtime.mutableStateOf(false) }
    var confirmCancel by remember { androidx.compose.runtime.mutableStateOf(false) }
    var confirmDelete by remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.material3.Card(
        Modifier.fillMaxWidth().padding(top = 24.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (ui.isCreator) "Você organiza esse jogo" else "Administração",
                style = MaterialTheme.typography.titleMedium,
            )
            if (!ui.cancelled && !ui.ended) {
                OutlinedButton(onClick = { edit = true }, enabled = !ui.busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Editar")
                }
                OutlinedButton(onClick = { confirmCancel = true }, enabled = !ui.busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar jogo")
                }
            }
            if (ui.isAdmin) {
                TextButton(onClick = { confirmDelete = true }, enabled = !ui.busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Excluir de vez (admin)", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmCancel) ConfirmDialog(
        title = "Cancelar esse jogo?",
        text = "Quem confirmou verá que o jogo foi cancelado.",
        confirm = "Cancelar jogo",
        dismiss = "Voltar",
        onConfirm = { confirmCancel = false; vm.cancelMatch() },
        onDismiss = { confirmCancel = false },
    )
    if (confirmDelete) ConfirmDialog(
        title = "Excluir de vez?",
        text = "O jogo e a lista de confirmados somem. Não dá para desfazer.",
        confirm = "Excluir",
        dismiss = "Voltar",
        onConfirm = { confirmDelete = false; vm.deleteMatch() },
        onDismiss = { confirmDelete = false },
    )
    if (edit) EditDialog(
        m = m,
        taken = m.taken,
        onSave = { t, d, c -> edit = false; vm.edit(t, d, c) },
        onDismiss = { edit = false },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    dismiss: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismiss) } },
    )
}

@Composable
private fun EditDialog(m: MatchCard, taken: Int, onSave: (String, String, Int) -> Unit, onDismiss: () -> Unit) {
    var title by remember { androidx.compose.runtime.mutableStateOf(m.title) }
    var description by remember { androidx.compose.runtime.mutableStateOf(m.description.orEmpty()) }
    var capacity by remember { androidx.compose.runtime.mutableIntStateOf(m.capacity) }
    val min = maxOf(1, taken)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar jogo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(60) },
                    label = { Text("Título") },
                    singleLine = true,
                )
                androidx.compose.material3.OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(300) },
                    label = { Text("Descrição") },
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Vagas")
                    OutlinedButton(onClick = { capacity = (capacity - 1).coerceAtLeast(min) }) { Text("−") }
                    Text("$capacity", style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = { capacity = (capacity + 1).coerceAtMost(40) }) { Text("+") }
                }
                Text("Quadra e horário não mudam aqui: para isso, cancele e crie outro jogo.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, description, capacity) }, enabled = title.trim().length >= 2) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Abre a folha de compartilhar do Android (WhatsApp etc.) com o convite pronto. */
private fun shareMatch(context: android.content.Context, m: MatchCard) {
    val sport = Sport.from(m.sport)
    val left = m.spotsLeft
    val text = buildString {
        append("${sport?.emoji.orEmpty()} ${m.title}\n")
        append("📍 ${m.venueName}\n")
        append("🕒 ${whenFull(m.startsAt, m.endsAt)}\n")
        append(if (left > 0) "Faltam $left vagas. Bora lá!" else "Lotado, mas fica de olho.")
        append("\n\nBaixe o OurinhosPlay e confirme sua presença.")
    }
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    context.startActivity(android.content.Intent.createChooser(send, "Chamar a galera"))
}

/** Abre o trajeto largada -> chegada no Google Maps (ou no navegador). */
private fun openRoute(context: android.content.Context, start: String, end: String) {
    val url = "https://www.google.com/maps/dir/?api=1&origin=" + android.net.Uri.encode(start) +
        "&destination=" + android.net.Uri.encode(end) + "&travelmode=walking"
    runCatching {
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    }
}
