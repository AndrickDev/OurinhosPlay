package com.ourinhosplay.app.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private suspend fun rpcOrBusiness(fn: String, params: JsonObject, fallback: String) {
    try {
        Supabase.client.postgrest.rpc(fn, params)
    } catch (e: io.github.jan.supabase.exceptions.RestException) {
        throw BusinessException(e.message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() } ?: fallback)
    }
}

// ------------------------------------------------------------------ pessoas (admin)

@Serializable
data class AdminUser(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    val tag: Int? = null,
    val email: String? = null,
    val role: String = "user",
) {
    val fullName: String
        get() = (displayName ?: "Sem nome") + (tag?.let { "#" + it.toString().padStart(4, '0') } ?: "")
}

object AdminUsers {
    suspend fun list(): List<AdminUser> =
        Supabase.client.postgrest.rpc("admin_list_users").decodeList<AdminUser>()

    suspend fun delete(id: String) =
        rpcOrBusiness("admin_delete_user", buildJsonObject { put("p_user_id", id) }, "Não foi possível remover")
}

// ------------------------------------------------------------------ cheguei

object CheckIn {
    suspend fun arrive(matchId: String) =
        rpcOrBusiness("check_in", buildJsonObject { put("p_match_id", matchId) }, "Não foi possível avisar")
}

// ------------------------------------------------------------------ chat rápido

/** Frases prontas do chat: a chave vai para o banco, o texto fica só no app. */
enum class QuickPhrase(val key: String, val text: String) {
    Chegando("chegando", "Estou chegando 🏃"),
    Cheguei("cheguei", "Cheguei na quadra ✅"),
    Atraso5("atraso5", "Vou atrasar uns 5 min"),
    Atraso15("atraso15", "Vou atrasar uns 15 min"),
    QuemBola("quem_bola", "Quem leva a bola?"),
    EuBola("eu_bola", "Eu levo a bola ⚽"),
    Coletes("coletes", "Quem leva os coletes?"),
    Agua("agua", "Levem água 💧"),
    FaltaGente("falta_gente", "Falta gente, chama mais um!"),
    Chuva("chuva", "Vai chover, bora confirmar?"),
    BomJogo("bom_jogo", "Bom jogo, galera! 🙌"),
    Valeu("valeu", "Valeu pelo jogo!"),
    DeNovo("de_novo", "Bora marcar de novo?");

    companion object {
        fun from(key: String): QuickPhrase? = entries.firstOrNull { it.key == key }
    }
}

@Serializable
data class ChatAuthor(
    @SerialName("display_name") val displayName: String? = null,
    val tag: Int? = null,
) {
    val fullName: String
        get() = (displayName ?: "Jogador") + (tag?.let { "#" + it.toString().padStart(4, '0') } ?: "")
}

@Serializable
data class ChatMessage(
    val id: String,
    @SerialName("user_id") val userId: String,
    val phrase: String,
    @SerialName("created_at") val createdAt: String,
    val profiles: ChatAuthor? = null,
)

@Serializable
data class ChatPhoto(
    @SerialName("user_id") val userId: String,
    val data: String,
    val profiles: ChatAuthor? = null,
)

object ChatRepository {
    private val db get() = Supabase.client

    suspend fun messages(matchId: String): List<ChatMessage> =
        db.from("match_messages")
            .select(Columns.raw("id, user_id, phrase, created_at, profiles(display_name, tag)")) {
                filter { eq("match_id", matchId) }
                order("created_at", Order.ASCENDING)
            }.decodeList<ChatMessage>()

    suspend fun photos(matchId: String): List<ChatPhoto> =
        db.from("match_photos")
            .select(Columns.raw("user_id, data, profiles(display_name, tag)")) {
                filter { eq("match_id", matchId) }
            }.decodeList<ChatPhoto>()

    suspend fun sendPhrase(matchId: String, phrase: QuickPhrase) =
        rpcOrBusiness(
            "send_phrase",
            buildJsonObject { put("p_match_id", matchId); put("p_phrase", phrase.key) },
            "Não foi possível enviar",
        )

    suspend fun sendPhoto(matchId: String, base64Jpeg: String) =
        rpcOrBusiness(
            "send_photo",
            buildJsonObject { put("p_match_id", matchId); put("p_data", base64Jpeg) },
            "Não foi possível enviar a foto",
        )
}

// ------------------------------------------------------------------ amigos

@Serializable
data class Friend(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    val tag: Int? = null,
) {
    val fullName: String
        get() = (displayName ?: "Jogador") + (tag?.let { "#" + it.toString().padStart(4, '0') } ?: "")
}

object FriendsRepository {
    suspend fun list(): List<Friend> =
        Supabase.client.postgrest.rpc("my_friends").decodeList<Friend>()

    /** Aceita "Nome#0042" (ou só "Nome 42"). */
    suspend fun add(input: String) {
        val m = Regex("""^\s*(.+?)\s*#?\s*(\d{1,4})\s*$""").find(input)
            ?: throw BusinessException("Digite o nome com o número, ex.: Joao#0042")
        val name = m.groupValues[1].trimEnd('#', ' ')
        val tag = m.groupValues[2].toInt()
        rpcOrBusiness(
            "add_friend",
            buildJsonObject { put("p_name", name); put("p_tag", tag) },
            "Não foi possível adicionar",
        )
    }

    suspend fun remove(id: String) =
        rpcOrBusiness("remove_friend", buildJsonObject { put("p_friend_id", id) }, "Não foi possível remover")
}

// ------------------------------------------------------------------ tempo (Open-Meteo, sem chave)

data class Forecast(val tempC: Int, val rainPercent: Int, val code: Int) {
    val emoji: String
        get() = when (code) {
            0 -> "☀️"
            1, 2 -> "🌤️"
            3 -> "☁️"
            45, 48 -> "🌫️"
            in 51..67, in 80..82 -> "🌧️"
            in 95..99 -> "⛈️"
            else -> "🌥️"
        }
    val label: String
        get() = when (code) {
            0 -> "Céu limpo"
            1, 2 -> "Poucas nuvens"
            3 -> "Nublado"
            45, 48 -> "Neblina"
            in 51..67, in 80..82 -> "Chuva"
            in 95..99 -> "Tempestade"
            else -> "Tempo instável"
        }
    /** Chuva provável a ponto de valer um aviso. */
    val rainy: Boolean get() = rainPercent >= 50 || code in 51..67 || code in 80..99
}

object WeatherRepository {
    private val client by lazy { HttpClient(Android) }
    private val json = Json { ignoreUnknownKeys = true }

    // Centro de Ourinhos/SP.
    private const val LAT = -22.979
    private const val LON = -49.871

    /** Previsão para a hora do jogo; null se fora do alcance (16 dias) ou sem conexão. */
    suspend fun forecastAt(startMillis: Long): Forecast? = runCatching {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$LAT&longitude=$LON" +
            "&hourly=temperature_2m,precipitation_probability,weather_code" +
            "&timezone=America%2FSao_Paulo&forecast_days=16"
        val root = json.parseToJsonElement(client.get(url).bodyAsText()).jsonObject
        val h = root["hourly"]!!.jsonObject
        val times = h["time"]!!.jsonArray
        val temps = h["temperature_2m"]!!.jsonArray
        val rain = h["precipitation_probability"]!!.jsonArray
        val codes = h["weather_code"]!!.jsonArray

        val zone = java.time.ZoneId.of("America/Sao_Paulo")
        val target = java.time.Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDateTime()
            .withMinute(0).withSecond(0).withNano(0)
        val idx = (0 until times.size).firstOrNull {
            java.time.LocalDateTime.parse(times[it].jsonPrimitive.content) == target
        } ?: return@runCatching null
        Forecast(
            tempC = Math.round(temps[idx].jsonPrimitive.doubleOrNull ?: return@runCatching null).toInt(),
            rainPercent = rain[idx].jsonPrimitive.doubleOrNull?.toInt() ?: 0,
            code = codes[idx].jsonPrimitive.contentOrNull?.toIntOrNull() ?: 0,
        )
    }.getOrNull()
}
