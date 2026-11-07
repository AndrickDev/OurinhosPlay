package com.ourinhosplay.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TeamsTest {
    private fun p(id: String, role: String? = null) = DrawPlayer(id, "P$id", role)

    @Test fun placar_nao_fica_negativo_nem_passa_de_99() {
        assertEquals(Score(0, 0), Score().add(0, -1))
        assertEquals(Score(1, 0), Score().add(0, 1))
        assertEquals(Score(0, 2), Score().add(1, 1).add(1, 1))
        assertEquals(Score(99, 0), Score(99, 0).add(0, 1))
        assertEquals("2 × 1", Score(2, 1).label)
    }

    @Test fun sorteio_divide_igual_e_nao_perde_ninguem() {
        val players = (1..11).map { p("$it") }
        val teams = TeamDraw.draw(players, 3, Random(1))
        assertEquals(3, teams.size)
        assertEquals(players.map { it.id }.toSet(), teams.flatten().map { it.id }.toSet())
        assertEquals(11, teams.sumOf { it.size })
        assertTrue(teams.maxOf { it.size } - teams.minOf { it.size } <= 1)
    }

    @Test fun sorteio_espalha_goleiros_um_por_time() {
        val players = listOf(p("g1", "goleiro"), p("g2", "goleiro")) + (1..8).map { p("$it") }
        repeat(20) { seed ->
            val teams = TeamDraw.draw(players, 2, Random(seed))
            teams.forEach { t -> assertEquals(1, t.count { it.role == "goleiro" }) }
        }
    }

    @Test fun sorteio_com_funcoes_continua_equilibrado() {
        val tres = listOf(p("g1", "goleiro"), p("g2", "goleiro"), p("g3", "goleiro")) + (1..7).map { p("$it") }
        repeat(20) { seed ->
            val t = TeamDraw.draw(tres, 2, Random(seed))
            assertEquals(5, t[0].size); assertEquals(5, t[1].size)
            val um = TeamDraw.draw(listOf(p("g", "goleiro")) + (1..8).map { p("$it") }, 3, Random(seed))
            assertTrue(um.all { it.size == 3 })
        }
    }

    @Test fun sobra_nao_vai_sempre_para_o_time_a() {
        val players = (1..11).map { p("$it") }
        val tamanhosA = (0 until 30).map { TeamDraw.draw(players, 3, Random(it))[0].size }.toSet()
        assertEquals(setOf(3, 4), tamanhosA)
    }

    @Test fun sorteio_limita_numero_de_times() {
        val players = (1..3).map { p("$it") }
        assertEquals(2, TeamDraw.draw(players, 1, Random(0)).size)
        assertEquals(3, TeamDraw.draw(players, 6, Random(0)).size)
        assertTrue(TeamDraw.draw(emptyList(), 2, Random(0)).all { it.isEmpty() })
    }

    @Test fun texto_para_compartilhar() {
        val t = TeamDraw.shareText(listOf(listOf(p("1"), p("2", "goleiro")), listOf(p("3"))))
        assertEquals("Time A: P1, P2 (Goleiro)\nTime B: P3", t)
    }
}
