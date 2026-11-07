package com.ourinhosplay.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GearTest {
    private val joao = GearClaim("colete", "u1", "João Silva")

    @Test fun futebol_temBolaEColete() =
        assertEquals(listOf("bola", "colete"), OptionCatalog.forSport(Sport.Futebol).map { it.key })

    @Test fun foraDaLista_soVeOQueOCriadorMarcou() {
        val rows = GearRules.rows(Sport.Futebol, mapOf("bola" to true), emptyList(), "u9", member = false)
        assertEquals(listOf("bola"), rows.map { it.def.key })
    }

    @Test fun naLista_veTodosOsItensDaModalidade() {
        val rows = GearRules.rows(Sport.Futebol, emptyMap(), emptyList(), "u9", member = true)
        assertEquals(listOf("bola", "colete"), rows.map { it.def.key })
    }

    @Test fun textoMostraPrimeiroNomeDeQuemLeva() {
        val r = GearRules.rows(Sport.Futebol, emptyMap(), listOf(joao), "u9", member = true).first { it.def.key == "colete" }
        assertEquals("Colete · João leva", r.text)
        assertFalse(r.mine)
    }

    @Test fun textoQuandoEuLevo() {
        val r = GearRules.rows(Sport.Futebol, emptyMap(), listOf(joao), "u1", member = true).first { it.def.key == "colete" }
        assertEquals("Colete · você leva", r.text)
    }

    @Test fun textoSemResponsavel() {
        val rows = GearRules.rows(Sport.Futebol, mapOf("bola" to false), emptyList(), "u9", member = true)
        assertEquals(listOf("Sem bola", "Colete"), rows.map { it.text })
    }

    @Test fun naoSoltaItemDeOutraPessoa() {
        val r = GearRules.rows(Sport.Futebol, emptyMap(), listOf(joao), "u9", member = true).first { it.def.key == "colete" }
        assertFalse(GearRules.canToggle(r, member = true, ended = false))
    }

    @Test fun assumeLivreOuSoltaOProprio() {
        val rows = GearRules.rows(Sport.Futebol, emptyMap(), listOf(joao), "u1", member = true)
        assertTrue(rows.all { GearRules.canToggle(it, member = true, ended = false) })
    }

    @Test fun jogoEncerradoOuForaDaLista_naoToca() {
        val r = GearRules.rows(Sport.Futebol, emptyMap(), emptyList(), "u9", member = true).first()
        assertFalse(GearRules.canToggle(r, member = true, ended = true))
        assertFalse(GearRules.canToggle(r, member = false, ended = false))
    }
}

class MatchTagsTest {
    @Test fun jogar_de_novo_separa_selo_do_equipamento() {
        val o = mapOf("bola" to true, "universitario" to true)
        assertEquals(mapOf("bola" to true), MatchTags.gearOnly(o))
        assertEquals(true, MatchTags.isUniversity(o))
    }

    @Test fun semNada_naoMandaOpcoes() =
        assertEquals(emptyMap<String, Boolean>(), MatchTags.build(Sport.Futebol, emptyMap(), university = false))

    @Test fun universitario_vaiSozinho() =
        assertEquals(mapOf("universitario" to true), MatchTags.build(Sport.Futebol, emptyMap(), university = true))

    @Test fun interruptorTocado_completaModalidadeEMantemSelo() =
        assertEquals(
            mapOf("bola" to true, "colete" to false, "universitario" to true),
            MatchTags.build(Sport.Futebol, mapOf("bola" to true), university = true),
        )

    @Test fun seloNaoViraEquipamento() {
        val opts = mapOf("universitario" to true, "bola" to true)
        assertTrue(MatchTags.isUniversity(opts))
        assertEquals(listOf("bola"), OptionCatalog.resolve(Sport.Futebol, opts).map { it.first.key })
    }
}
