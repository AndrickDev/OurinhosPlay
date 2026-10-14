package com.ourinhosplay.app.data

import io.github.jan.supabase.postgrest.from
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
) {
    val spotsLeft: Int get() = (capacity - taken).coerceAtLeast(0)
}

object MatchRepository {
    private val db get() = Supabase.client

    /** Jogos futuros e abertos, do mais próximo ao mais distante; `sport` null = todos. */
    suspend fun openMatches(sport: Sport?): List<MatchCard> =
        db.from("matches_feed").select {
            filter {
                eq("status", "open")
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
