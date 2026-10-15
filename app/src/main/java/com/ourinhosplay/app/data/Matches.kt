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
    BeachTennis("beach_tennis", "Beach Tennis", "🎾"),
    Futsal("futsal", "Futsal", "⚽"),
    Tenis("tenis", "Tênis", "🎾"),
    Padel("padel", "Padel", "🎾"),
    Natacao("natacao", "Natação", "🏊"),
    Biribol("biribol", "Biribol", "🏐"),
    Corrida("corrida", "Corrida", "🏃"),
    Ciclismo("ciclismo", "Ciclismo", "🚴"),
    Caminhada("caminhada", "Caminhada", "🚶");

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
    @SerialName("court_id") val courtId: String? = null,
    @SerialName("court_name") val courtName: String? = null,
    @SerialName("court_is_free") val courtIsFree: Boolean? = null,
    @SerialName("court_price_cents") val courtPriceCents: Int? = null,
    @SerialName("is_event") val isEvent: Boolean = false,
    val promoted: Boolean = false,
    @SerialName("route_start") val routeStart: String? = null,
    @SerialName("route_end") val routeEnd: String? = null,
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

@Serializable
private data class MatchIdRow(@SerialName("match_id") val matchId: String)

object MineRepository {
    private val db get() = Supabase.client

    /** Jogos que criei ou em que estou confirmado (inclui passados e cancelados). */
    suspend fun matches(): List<MatchCard> {
        val uid = db.auth.currentUserOrNull()?.id ?: return emptyList()
        val ids = db.from("participants")
            .select(io.github.jan.supabase.postgrest.query.Columns.list("match_id")) { filter { eq("user_id", uid) } }
            .decodeList<MatchIdRow>().map { it.matchId }
        return db.from("matches_feed").select {
            filter {
                or {
                    eq("creator_id", uid)
                    if (ids.isNotEmpty()) isIn("id", ids)
                }
            }
            order("starts_at", Order.DESCENDING)
        }.decodeList<MatchCard>()
    }
}

object VenueRepository {
    private val db get() = Supabase.client

    suspend fun all(): List<Venue> =
        db.from("venues").select { order("name", Order.ASCENDING) }.decodeList<Venue>()
}

@Serializable
data class Court(
    val id: String,
    @SerialName("venue_id") val venueId: String,
    val name: String,
    val sports: List<String> = emptyList(),
    @SerialName("is_free") val isFree: Boolean = true,
    @SerialName("price_per_hour_cents") val pricePerHourCents: Int? = null,
    val audience: String? = null,
) {
    val priceLabel: String
        get() = if (isFree) "Grátis" else pricePerHourCents?.let { "R$ %d,%02d/h".format(it / 100, it % 100) } ?: "Pago"
}

object CourtRepository {
    private val db get() = Supabase.client

    suspend fun all(): List<Court> =
        db.from("courts").select { order("name", Order.ASCENDING) }.decodeList<Court>()
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
    val creatorJoins: Boolean = true,
    val courtId: String? = null,
)

/** Erro de negócio vindo do banco (mensagem já em português, segura para exibir). */
class BusinessException(message: String) : Exception(message)

object CreateMatchRepository {
    private val db get() = Supabase.client

    suspend fun availability(venueId: String, startsAt: java.time.Instant, durationMin: Int, courtId: String? = null): Availability =
        db.postgrest.rpc(
            "venue_availability",
            kotlinx.serialization.json.buildJsonObject {
                put("p_venue_id", venueId)
                put("p_starts_at", startsAt.toString())
                put("p_duration_min", durationMin)
                courtId?.let { put("p_court_id", it) }
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
                    put("p_creator_joins", m.creatorJoins)
                    m.courtId?.let { put("p_court_id", it) }
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
    @SerialName("arrived_at") val arrivedAt: String? = null,
    val profiles: ParticipantProfile? = null,
)

object MatchDetailRepository {
    private val db get() = Supabase.client

    suspend fun detail(id: String): MatchCard? =
        db.from("matches_feed").select { filter { eq("id", id) } }.decodeSingleOrNull<MatchCard>()

    suspend fun participants(id: String): List<Participant> =
        db.from("participants")
            .select(io.github.jan.supabase.postgrest.query.Columns.raw("user_id, joined_at, arrived_at, profiles(display_name, tag)")) {
                filter { eq("match_id", id) }
                order("joined_at", Order.ASCENDING)
            }.decodeList<Participant>()

    fun currentUserId(): String? =
        db.auth.currentUserOrNull()?.id

    suspend fun isAdmin(): Boolean = ProfileRepository.current()?.role == "admin"

    suspend fun cancel(id: String) = call("cancel_match", id)
    suspend fun delete(id: String) = call("delete_match", id)

    suspend fun update(id: String, title: String, description: String, capacity: Int) {
        try {
            db.postgrest.rpc(
                "update_match",
                kotlinx.serialization.json.buildJsonObject {
                    put("p_match_id", id)
                    put("p_title", title)
                    put("p_description", description)
                    put("p_capacity", capacity)
                },
            )
        } catch (e: io.github.jan.supabase.exceptions.RestException) {
            throw BusinessException(
                e.message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Não foi possível salvar",
            )
        }
    }

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

@Serializable
data class VenueBlock(
    val id: String,
    @SerialName("venue_id") val venueId: String,
    @SerialName("court_id") val courtId: String? = null,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val reason: String? = null,
)

/** Operações do painel admin. O banco confere `is_admin()` em toda chamada. */
object AdminRepository {
    private val db get() = Supabase.client

    suspend fun blocks(): List<VenueBlock> =
        db.from("venue_blocks").select {
            filter { gt("ends_at", java.time.Instant.now().toString()) }
            order("starts_at", Order.ASCENDING)
        }.decodeList<VenueBlock>()

    suspend fun addBlock(
        venueId: String,
        courtId: String?,
        startsAt: java.time.Instant,
        endsAt: java.time.Instant,
        reason: String,
    ) = rpc("admin_add_block") {
        put("p_venue_id", venueId)
        courtId?.let { put("p_court_id", it) }
        put("p_starts_at", startsAt.toString())
        put("p_ends_at", endsAt.toString())
        put("p_reason", reason)
    }

    suspend fun removeBlock(id: String) = rpc("admin_remove_block") { put("p_block_id", id) }

    suspend fun saveCourt(
        venueId: String,
        courtId: String?,
        name: String,
        sports: List<String>,
        isFree: Boolean,
        priceCents: Int?,
        audience: String,
    ) = rpc("admin_save_court") {
        put("p_venue_id", venueId)
        courtId?.let { put("p_court_id", it) }
        put("p_name", name)
        put("p_sports", kotlinx.serialization.json.JsonArray(sports.map { kotlinx.serialization.json.JsonPrimitive(it) }))
        put("p_is_free", isFree)
        priceCents?.let { put("p_price_cents", it) }
        put("p_audience", audience)
    }

    suspend fun deleteCourt(id: String) = rpc("admin_delete_court") { put("p_court_id", id) }

    suspend fun createEvent(
        venueId: String,
        sport: Sport,
        title: String,
        description: String,
        startsAt: java.time.Instant,
        durationMin: Int,
        capacity: Int,
        routeStart: String,
        routeEnd: String,
        promoted: Boolean,
    ) = rpc("admin_create_event") {
        put("p_venue_id", venueId)
        put("p_sport", sport.key)
        put("p_title", title)
        put("p_description", description)
        put("p_starts_at", startsAt.toString())
        put("p_duration_min", durationMin)
        put("p_capacity", capacity)
        put("p_route_start", routeStart)
        put("p_route_end", routeEnd)
        put("p_promoted", promoted)
    }

    suspend fun setPromoted(matchId: String, promoted: Boolean) = rpc("admin_set_promoted") {
        put("p_match_id", matchId)
        put("p_promoted", promoted)
    }

    private suspend fun rpc(fn: String, build: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) {
        try {
            db.postgrest.rpc(fn, kotlinx.serialization.json.buildJsonObject(build))
        } catch (e: io.github.jan.supabase.exceptions.RestException) {
            throw BusinessException(
                e.message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Não foi possível concluir",
            )
        }
    }
}
