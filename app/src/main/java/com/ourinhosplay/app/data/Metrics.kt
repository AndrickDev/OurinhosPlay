package com.ourinhosplay.app.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VenueCount(
    @SerialName("local") val venue: String,
    @SerialName("jogos") val matches: Int,
)

/** Contagens agregadas de `admin_metrics()` (migration 0017). Sem dados pessoais. */
@Serializable
data class AdminMetrics(
    @SerialName("jogadores") val players: Int = 0,
    @SerialName("jogadores_novos_30d") val newPlayers30d: Int = 0,
    @SerialName("jogos_total") val matchesTotal: Int = 0,
    @SerialName("jogos_30d") val matches30d: Int = 0,
    @SerialName("jogos_cancelados") val matchesCancelled: Int = 0,
    @SerialName("ativos_30d") val active30d: Int = 0,
    @SerialName("vagas_preenchidas_30d") val filledSpots30d: Int = 0,
    @SerialName("por_esporte") val bySport: Map<String, Int> = emptyMap(),
    @SerialName("por_local") val byVenue: List<VenueCount> = emptyList(),
) {
    /** % de jogos que não foram cancelados. */
    val activeMatchesPct: Int
        get() = if (matchesTotal == 0) 0 else (matchesTotal - matchesCancelled) * 100 / matchesTotal
}

object MetricsRepository {
    suspend fun load(): AdminMetrics =
        Supabase.client.postgrest.rpc("admin_metrics").decodeAs<AdminMetrics>()
}
