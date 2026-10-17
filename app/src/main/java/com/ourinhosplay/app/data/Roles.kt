package com.ourinhosplay.app.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Função rara de uma modalidade: a que costuma faltar e vale reservar vaga. */
data class RoleTemplate(val key: String, val label: String, val labelPlural: String, val defaultSlots: Int)

object RoleCatalog {
    private val goleiro = RoleTemplate("goleiro", "Goleiro", "Goleiros", 2)
    private val levantador = RoleTemplate("levantador", "Levantador", "Levantadores", 2)

    fun forSport(s: Sport?): RoleTemplate? = when (s) {
        Sport.Futebol, Sport.Futsal -> goleiro
        Sport.Volei -> levantador
        else -> null
    }

    fun label(key: String): String = when (key) {
        goleiro.key -> goleiro.label
        levantador.key -> levantador.label
        else -> key.replaceFirstChar { it.uppercase() }
    }
}

/** Configuração de uma função na criação do jogo. */
data class RoleSpec(
    val role: String,
    val slots: Int,
    val reserved: Boolean = true,
    val releaseHours: Int = 3,
    /** 0, 50 ou 100: quanto do rateio quem joga nessa função deixa de pagar. */
    val perkPct: Int = 100,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("role", role)
        put("slots", slots)
        put("reserved", reserved)
        put("release_hours", releaseHours)
        put("perk_pct", perkPct)
    }
}

internal fun List<RoleSpec>.toJsonArray(): JsonArray = JsonArray(map { it.toJson() })

/** Situação de uma função num jogo (vem de `match_roles_status`). */
@Serializable
data class RoleStatus(
    val role: String,
    val slots: Int,
    val taken: Int,
    val reserved: Boolean = false,
    /** Reservada e ainda antes do prazo de liberação. */
    val locked: Boolean = false,
    @SerialName("release_at") val releaseAt: String? = null,
    @SerialName("perk_pct") val perkPct: Int = 0,
) {
    val open: Int get() = (slots - taken).coerceAtLeast(0)
}

object RolesRepository {
    private val db get() = Supabase.client

    /** Funções do jogo; lista vazia se o jogo não usa funções ou o banco ainda não tem a migration 0015. */
    suspend fun status(matchId: String): List<RoleStatus> = try {
        db.postgrest.rpc("match_roles_status", buildJsonObject { put("p_match_id", matchId) })
            .decodeList<RoleStatus>()
    } catch (e: io.github.jan.supabase.exceptions.RestException) {
        emptyList()
    }

    /** Transforma a reserva rápida (privada, 1 vaga) em jogo aberto na lista. */
    suspend fun openReservation(matchId: String, capacity: Int) {
        try {
            db.postgrest.rpc(
                "open_reservation",
                buildJsonObject {
                    put("p_match_id", matchId)
                    put("p_capacity", capacity)
                },
            )
        } catch (e: io.github.jan.supabase.exceptions.RestException) {
            throw BusinessException(
                e.message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Não foi possível abrir as vagas",
            )
        }
    }
}

/** Valor que a pessoa paga no rateio, em fração (1 = inteiro, 0 = isento). */
fun payWeight(role: String?, roles: List<RoleStatus>): Double {
    val perk = roles.firstOrNull { it.role == role }?.perkPct ?: 0
    return 1.0 - perk / 100.0
}
