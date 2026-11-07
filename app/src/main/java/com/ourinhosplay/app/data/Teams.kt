package com.ourinhosplay.app.data

import kotlin.random.Random

/** Placar simples de dois times, só no aparelho de quem controla o cronômetro. */
data class Score(val a: Int = 0, val b: Int = 0) {
    fun add(team: Int, delta: Int): Score =
        if (team == 0) copy(a = (a + delta).coerceIn(0, 99)) else copy(b = (b + delta).coerceIn(0, 99))

    val label: String get() = "$a × $b"
}

data class DrawPlayer(val id: String, val name: String, val role: String? = null)

/**
 * Sorteio de times na hora do jogo. Quem tem função (goleiro, levantador) é espalhado primeiro,
 * um por time; o resto é embaralhado e vai sempre para o time menor. Não há nível por jogador,
 * então "equilibrado" aqui é tamanho igual e funções divididas.
 */
object TeamDraw {
    fun draw(players: List<DrawPlayer>, teams: Int, rnd: Random = Random.Default): List<List<DrawPlayer>> {
        val n = teams.coerceIn(2, maxOf(2, players.size))
        val out = List(n) { mutableListOf<DrawPlayer>() }
        val (withRole, rest) = players.partition { it.role != null }
        val ordered = withRole.groupBy { it.role }.values.flatMap { it.shuffled(rnd) } + rest.shuffled(rnd)
        val start = rnd.nextInt(n)
        ordered.forEachIndexed { i, p ->
            if (i < withRole.size) {
                out[(start + i) % n] += p
            } else {
                val min = out.minOf { it.size }
                out.filter { it.size == min }.random(rnd) += p
            }
        }
        return out
    }

    fun teamName(i: Int): String = "Time " + ('A' + i)

    fun shareText(teams: List<List<DrawPlayer>>): String =
        teams.mapIndexed { i, t ->
            teamName(i) + ": " + t.joinToString(", ") { it.name + (it.role?.let { r -> " (" + RoleCatalog.label(r) + ")" } ?: "") }
        }.joinToString("\n")
}
