package com.ourinhosplay.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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

        if (isAdmin) {
            Button(onClick = onAdmin, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) { Text("Painel admin") }
        }
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Sair") }
    }
}
