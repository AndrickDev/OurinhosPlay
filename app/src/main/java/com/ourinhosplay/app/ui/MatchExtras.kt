package com.ourinhosplay.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ourinhosplay.app.data.BusinessException
import com.ourinhosplay.app.data.ChatMessage
import com.ourinhosplay.app.data.ChatPhoto
import com.ourinhosplay.app.data.ChatRepository
import com.ourinhosplay.app.data.Forecast
import com.ourinhosplay.app.data.QuickPhrase
import com.ourinhosplay.app.data.WeatherRepository
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Tempo na hora do jogo (some se fora dos próximos 16 dias ou sem internet). */
@Composable
fun WeatherCard(startIso: String, ended: Boolean) {
    if (ended) return
    val start = remember(startIso) { runCatching { OffsetDateTime.parse(startIso).toInstant().toEpochMilli() }.getOrNull() }
    var forecast by remember { mutableStateOf<Forecast?>(null) }
    LaunchedEffect(start) { forecast = start?.let { WeatherRepository.forecastAt(it) } }
    val f = forecast ?: return
    Row(
        Modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
            .semantics { contentDescription = "Previsão: ${f.label}, ${f.tempC} graus, ${f.rainPercent}% de chance de chuva" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(f.emoji, style = MaterialTheme.typography.headlineMedium)
        Column(Modifier.weight(1f)) {
            Text("${f.tempC}° · ${f.label}", style = MaterialTheme.typography.titleMedium)
            Text(
                if (f.rainy) "Chuva provável (${f.rainPercent}%). Combine com a galera." else "Chance de chuva: ${f.rainPercent}%",
                style = MaterialTheme.typography.bodySmall,
                color = if (f.rainy) Palette.Alerta else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val MAX_BASE64 = 300_000

/** Reduz para no máximo 720 px e comprime em JPEG até caber no limite do servidor. */
private fun encodePhoto(context: android.content.Context, uri: Uri): String? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1440) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val raw = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
    val ratio = 720f / maxOf(raw.width, raw.height)
    val bmp = if (ratio < 1f) Bitmap.createScaledBitmap(raw, (raw.width * ratio).toInt(), (raw.height * ratio).toInt(), true) else raw
    var quality = 75
    while (quality >= 35) {
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        if (b64.length <= MAX_BASE64) return b64
        quality -= 10
    }
    return null
}

private fun decodePhoto(b64: String): Bitmap? = runCatching {
    val bytes = Base64.decode(b64, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}.getOrNull()

/** Chat rápido: frases prontas + 1 foto por pessoa. Some 6 h depois do jogo (regra no banco). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatSection(matchId: String, color: Color, onToast: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var photos by remember { mutableStateOf<List<ChatPhoto>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(matchId, reload) {
        runCatching { ChatRepository.messages(matchId) }.onSuccess { messages = it }
        runCatching { ChatRepository.photos(matchId) }.onSuccess { photos = it }
    }

    fun run(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            runCatching { block() }
                .onSuccess { reload++ }
                .onFailure { e -> onToast((e as? BusinessException)?.message ?: "Sem conexão. Tente de novo.") }
            busy = false
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        run {
            val b64 = withContext(Dispatchers.Default) { encodePhoto(context, uri) }
                ?: throw BusinessException("Não consegui usar essa foto")
            ChatRepository.sendPhoto(matchId, b64)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
        Text("Chat do jogo", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Frases prontas e uma foto por pessoa. Tudo some 6 horas depois do jogo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (messages.isEmpty() && photos.isEmpty()) {
            Text("Ninguém falou ainda. Toque numa frase.", style = MaterialTheme.typography.bodyMedium)
        }
        messages.takeLast(12).forEach { m ->
            val who = m.profiles?.fullName ?: "Jogador"
            val text = QuickPhrase.from(m.phrase)?.text ?: m.phrase
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(who, style = MaterialTheme.typography.labelLarge, color = color)
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (photos.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                photos.forEach { p ->
                    val bmp = remember(p.data) { decodePhoto(p.data) }
                    if (bmp != null) {
                        Column(Modifier.size(width = 150.dp, height = 190.dp)) {
                            Image(
                                bmp.asImageBitmap(),
                                contentDescription = "Foto de ${p.profiles?.fullName ?: "jogador"}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(width = 150.dp, height = 160.dp).clip(MaterialTheme.shapes.medium),
                            )
                            Text(p.profiles?.fullName ?: "Jogador", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickPhrase.entries.forEach { q ->
                AssistChip(
                    onClick = { run { ChatRepository.sendPhrase(matchId, q) } },
                    enabled = !busy,
                    label = { Text(q.text) },
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }

        OutlinedButton(
            onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("📷 Enviar foto da quadra (1 por pessoa)") }
    }
}
