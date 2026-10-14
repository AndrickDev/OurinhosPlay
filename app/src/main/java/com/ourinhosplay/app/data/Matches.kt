package com.ourinhosplay.app.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.put
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class Sport(val key: String, val label: String, val emoji: String) {
    Futebol("futebol", "Futebol", "⚽"),
    Volei("volei", "Vôlei", "🏐"),
    Basquete("basquete", "Basquete", "🏀"),
    BeachTennis("beach_tennis", "Beach Tennis", "🎾");

    companion object {
        fun from(key: String): Sport? = entries.firstOrNull { it.key == key }
    }
}

@Serializable
data class Venue(
    val id: String,
    val name: String,
    val kind: String = "public",
    val address: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val sports: List<String> = emptyList(),
)

@Serializable
data class MatchCard(
    val id: String,
    @SerialName("venue_id") val venueId: String,
    @SerialName("venue_name") val venueName: String,
    val sport: String,
    val title: String,
    /** ISO-8601 com offset, como o Postgres devolve. */
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val capacity: Int,
    val taken: Int,
    val status: String,
    val description: String? = null,
    @SerialName("creator_id") val creatorId: String? = null,
) {
    val spotsLeft: Int get() = (capacity - taken).coerceAtLeast(0)
}

object MatchRepository {
    private val db get() = Supabase.client

    /** Jogos futuros e abertos, do mais próximo ao mais distante; `sport` null = todos. */
    suspend fun openMatches(sport: Sport?): List<MatchCard> =
        db.from("matches_feed").select {
            filter {
                isIn("status", listOf("open", "full"))
                gt("ends_at", java.time.Instant.now().toString())
                if (sport != null) eq("sport", sport.key)
            }
            order("starts_at", Order.ASCENDING)
        }.decodeList<MatchCard>()
}

object VenueRepository {
    private val db get() = Supabase.client

    suspend fun all(): List<Venue> =
        db.from("venues").select { order("name", Order.ASCENDING) }.decodeList<Venue>()
}

@Serializable
data class Availability(
    @SerialName("is_free") val isFree: Boolean,
    val reason: String? = null,
    @SerialName("busy_until") val busyUntil: String? = null,
    @SerialName("next_free_at") val nextFreeAt: String? = null,
)

data class NewMatch(
    val venueId: String,
    val sport: Sport,
    val title: String,
    val description: String,
    val startsAt: java.time.Instant,
    val durationMin: Int,
    val capacity: Int,
)

/** Erro de negócio vindo do banco (mensagem já em português, segura para exibir). */
class BusinessException(message: String) : Exception(message)

object CreateMatchRepository {
    private val db get() = Supabase.client

    suspend fun availability(venueId: String, startsAt: java.time.Instant, durationMin: Int): Availability =
        db.postgrest.rpc(
            "venue_availability",
            kotlinx.serialization.json.buildJsonObject {
                put("p_venue_id", venueId)
                put("p_starts_at", startsAt.toString())
                put("p_duration_min", durationMin)
            },
        ).decodeList<Availability>().first()

    suspend fun create(m: NewMatch) {
        try {
            db.postgrest.rpc(
                "create_match",
                kotlinx.serialization.json.buildJsonObject {
                    put("p_venue_id", m.venueId)
                    put("p_sport", m.sport.key)
                    put("p_title", m.title)
                    put("p_description", m.description)
                    put("p_starts_at", m.startsAt.toString())
                    put("p_duration_min", m.durationMin)
                    put("p_capacity", m.capacity)
                },
            )
        } catch (e: io.github.jan.supabase.exceptions.RestException) {
            // A 1ª linha da mensagem é o texto da exceção do Postgres (sem URL nem cabeçalhos).
            throw BusinessException(e.message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: "Não foi possível criar o jogo")
        }
    }
}

@Serializable
data class ParticipantProfile(
    @SerialName("display_name") val displayName: String? = null,
    val tag: Int? = null,
) {
    val fullName: String
        get() = displayName.orEmpty() + (tag?.let { "#" + it.toString().padStart(4, '0') } ?: "")
}

@Serializable
data class Participant(
    @SerialName("user_id") val userId: String,
    @SerialName("joined_at") val joinedAt: String,
    val profiles: ParticipantProfile? = null,
)

object MatchDetailRepository {
    private val db get() = Supabase.client

    suspend fun detail(id: String): MatchCard? =
        db.from("matches_feed").select { filter { eq("id", id) } }.decodeSingleOrNull<MatchCard>()

    suspend fun participants(id: String): List<Participant> =
        db.from("participants")
            .select(io.github.jan.supabase.postgrest.query.Columns.raw("user_id, joined_at, profiles(display_name, tag)")) {
                filter { eq("match_id", id) }
                order("joined_at", Order.ASCENDING)
            }.decodeList<Participant>()

    fun currentUserId(): String? =
        db.auth.currentUserOrNull()?.id

    suspend fun join(id: String) = call("join_match", id)
    suspend fun leave(id: String) = call("leave_match", id)

    private suspend fun call(fn: String, id: String) {
        try {
            db.postgrest.rpc(fn, kotlinx.serialization.json.buildJsonObject { put("p_match_id", id) })
        } catch (e: io.github.jan.supabase.exceptions.RestException) {
            throw BusinessException(
                e.message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Não foi possível concluir",
            )
        }
    }
}
