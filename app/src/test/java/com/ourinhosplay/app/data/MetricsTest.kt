package com.ourinhosplay.app.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun decodifica_respostaDoServidor() {
        val raw = """{"jogadores":12,"jogadores_novos_30d":5,"jogos_total":20,"jogos_30d":9,"jogos_cancelados":2,
            "ativos_30d":7,"vagas_preenchidas_30d":31,"por_esporte":{"futsal":6,"biribol":3},
            "por_local":[{"local":"Quadra A","jogos":4}]}"""
        val m = json.decodeFromString<AdminMetrics>(raw)
        assertEquals(12, m.players)
        assertEquals(31, m.filledSpots30d)
        assertEquals(mapOf("futsal" to 6, "biribol" to 3), m.bySport)
        assertEquals("Quadra A", m.byVenue.single().venue)
    }

    @Test fun decodifica_camposAusentesViramZero() {
        val m = json.decodeFromString<AdminMetrics>("{}")
        assertEquals(0, m.players)
        assertEquals(emptyMap<String, Int>(), m.bySport)
    }

    @Test fun ocupacao_percentualDeJogosNaoCancelados() {
        assertEquals(90, AdminMetrics(matchesTotal = 20, matchesCancelled = 2).activeMatchesPct)
        assertEquals(0, AdminMetrics().activeMatchesPct)
    }
}
