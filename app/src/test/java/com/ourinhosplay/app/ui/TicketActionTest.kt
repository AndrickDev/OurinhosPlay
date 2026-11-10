package com.ourinhosplay.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TicketActionTest {
    @Test fun agenda_quem_esta_na_lista_ve_voce_vai() {
        assertEquals("Você vai", ticketAction(cancelled = false, full = true, mine = true, joined = true))
    }

    @Test fun agenda_criador_que_so_organiza_ve_voce_organiza() {
        assertEquals("Você organiza", ticketAction(cancelled = false, full = false, mine = true, joined = false))
    }

    @Test fun feed_mostra_entrar_ou_lotado() {
        assertEquals("Entrar", ticketAction(cancelled = false, full = false, mine = false, joined = false))
        assertEquals("Lotado", ticketAction(cancelled = false, full = true, mine = false, joined = false))
    }

    @Test fun feed_quem_ja_esta_na_lista_ve_voce_vai_e_nao_entrar() {
        assertEquals("Você vai", ticketAction(cancelled = false, full = false, mine = false, joined = true))
        assertEquals("Você vai", ticketAction(cancelled = false, full = true, mine = false, joined = true))
    }

    @Test fun historico_mostra_encerrado_e_nao_convida_a_entrar() {
        assertEquals("Encerrado", ticketAction(cancelled = false, full = false, mine = false, joined = false, past = true))
        assertEquals("Encerrado", ticketAction(cancelled = false, full = true, mine = false, joined = true, past = true))
        assertEquals("Cancelado", ticketAction(cancelled = true, full = false, mine = false, joined = false, past = true))
    }

    @Test fun cancelado_vence_tudo() {
        assertEquals("Cancelado", ticketAction(cancelled = true, full = false, mine = true, joined = true))
    }
}
