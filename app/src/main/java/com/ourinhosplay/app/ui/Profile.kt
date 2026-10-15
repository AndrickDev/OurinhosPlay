package com.ourinhosplay.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ourinhosplay.app.data.BusinessException
import com.ourinhosplay.app.data.Friend
import com.ourinhosplay.app.data.FriendsRepository
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(displayName: String, onSignOut: () -> Unit, isAdmin: Boolean, onAdmin: () -> Unit) {
    val scope = rememberCoroutineScope()
    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var gallery by remember { mutableStateOf(false) }
    var mine by remember { mutableStateOf<List<com.ourinhosplay.app.data.MatchCard>>(emptyList()) }
    LaunchedEffect(Unit) {
        runCatching { com.ourinhosplay.app.data.MineRepository.matches() }.onSuccess { mine = it }
    }

    suspend fun reload() {
        runCatching { FriendsRepository.list() }.onSuccess { friends = it }
    }
    LaunchedEffect(Unit) { reload() }

    fun add() {
        if (busy || input.isBlank()) return
        busy = true
        message = null
        scope.launch {
            runCatching { FriendsRepository.add(input) }
                .onSuccess { input = ""; message = "Amigo adicionado."; reload() }
                .onFailure { e -> message = (e as? BusinessException)?.message ?: "Sem conexão. Tente de novo." }
            busy = false
        }
    }

    if (gallery) {
        ArtGallery(onClose = { gallery = false })
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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

        // estatísticas leves: só o que é seu, sem ranking
        val me = com.ourinhosplay.app.data.MatchDetailRepository.currentUserId()
        val now = java.time.Instant.now()
        val finished = mine.filter {
            it.status != "cancelled" &&
                runCatching { java.time.OffsetDateTime.parse(it.endsAt).toInstant().isBefore(now) }.getOrDefault(false)
        }
        val organized = mine.count { it.creatorId == me && it.status != "cancelled" }
        val favorite = finished.groupingBy { it.sport }.eachCount().maxByOrNull { it.value }
            ?.let { com.ourinhosplay.app.data.Sport.from(it.key) }
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile("${finished.size}", "jogos feitos", Modifier.weight(1f))
            StatTile("$organized", "organizados", Modifier.weight(1f))
            StatTile(favorite?.label ?: "—", "esporte favorito", Modifier.weight(1f))
        }

        Column(Modifier.fillMaxWidth().padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Amigos", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Adicione pelo nome e número (ex.: Joao#0042). Amigos aparecem com ★ nos jogos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(40) },
                    label = { Text("Nome#0000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() }),
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = ::add, enabled = !busy && input.isNotBlank()) { Text("Adicionar") }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (friends.isEmpty()) {
                Text("Nenhum amigo ainda.", style = MaterialTheme.typography.bodyMedium)
            }
            friends.forEach { f ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★ " + f.fullName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { FriendsRepository.remove(f.id) }
                            reload()
                        }
                    }) { Text("Remover") }
                }
            }
        }

        val context = androidx.compose.ui.platform.LocalContext.current
        var soundOn by remember { mutableStateOf(Sfx.isOn(context)) }
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Sons do app", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Apito, avisos e efeitos. Respeita o modo silencioso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.Switch(
                checked = soundOn,
                onCheckedChange = {
                    soundOn = it
                    Sfx.setOn(context, it)
                    if (it) Sfx.fire(context, Sound.Chime)
                },
            )
        }

        if (isAdmin) {
            Button(onClick = onAdmin, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) { Text("Painel admin") }
        }
        if (com.ourinhosplay.app.BuildConfig.SHOW_EMAIL_LOGIN) {
            OutlinedButton(onClick = { gallery = true }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("Galeria de artes (dev)")
            }
        }
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Sair") }
    }
}

/** Só para desenvolvimento (SHOW_EMAIL_LOGIN=true): confere a arte de todas as modalidades. */
@Composable
private fun ArtGallery(onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onClose) { Text("← Voltar") }
        com.ourinhosplay.app.data.Sport.entries.forEach { s ->
            Box(
                Modifier.fillMaxWidth().height(190.dp).clip(MaterialTheme.shapes.large).background(s.color),
            ) {
                SportArt(s, Modifier.matchParentSize())
                Text(s.label, style = MaterialTheme.typography.titleLarge, color = Palette.Noite, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}
