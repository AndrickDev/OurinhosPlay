package com.ourinhosplay.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ourinhosplay.app.BuildConfig
import com.ourinhosplay.app.auth.AuthUi

@Composable
fun LoadingScreen() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { CircularProgressIndicator() }
}

@Composable
private fun Messages(ui: AuthUi) {
    ui.error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
    }
    ui.info?.let {
        Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun LoginScreen(
    ui: AuthUi,
    onGoogle: () -> Unit,
    onEmailSignIn: (String, String) -> Unit,
    onEmailSignUp: (String, String) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppLogo(fontSize = 54.sp)
        Text(
            "Marque o jogo, chame a galera.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
        )
        Button(
            onClick = onGoogle,
            enabled = !ui.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text("Entrar com Google") }

        if (BuildConfig.DEBUG) {
            Text(
                "Só em build debug",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 32.dp),
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("E-mail") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Senha") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedButton(
                onClick = { onEmailSignIn(email, password) },
                enabled = !ui.busy && email.isNotBlank() && password.length >= 6,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Entrar com e-mail") }
            TextButton(
                onClick = { onEmailSignUp(email, password) },
                enabled = !ui.busy && email.isNotBlank() && password.length >= 6,
            ) { Text("Criar conta com e-mail") }
        }

        if (ui.busy) CircularProgressIndicator(Modifier.padding(top = 16.dp))
        Messages(ui)
    }
}

@Composable
fun NameScreen(ui: AuthUi, onSave: (String) -> Unit, onSignOut: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val ready = !ui.busy && name.trim().length >= 2
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppLogo(fontSize = 32.sp)
        Text(
            "Como a galera te chama?",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 32.dp),
        )
        Text(
            "Seu nome aparece nos jogos, com um número só seu (ex.: Joao#0042). Depois de salvo, ele não muda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 30) name = it },
            label = { Text("Nome de exibição") },
            supportingText = { Text("${name.trim().length}/30 · mínimo 2 letras") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words,
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { if (ready) onSave(name) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onSave(name) },
            enabled = ready,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).heightIn(min = 52.dp),
        ) { Text("Continuar") }
        if (ui.busy) CircularProgressIndicator(Modifier.padding(top = 16.dp))
        TextButton(onClick = onSignOut, enabled = !ui.busy) { Text("Sair") }
        Messages(ui)
    }
}
