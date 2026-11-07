package com.ourinhosplay.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchClockTest {
    private val min = 60_000L
    private val cfg = ClockConfig(periods = 2, periodMin = 20, breakMin = 5, hydrationMin = 0)

    @Test fun conta_regressivo() {
        val s = MatchClock.start(cfg, 0)
        assertEquals(15 * min, s.remaining(5 * min))
        assertEquals("15:00", MatchClock.mmss(s.remaining(5 * min)))
    }

    @Test fun fimDoTempo_vaiParaIntervalo() {
        val (s, ev) = MatchClock.advance(MatchClock.start(cfg, 0), 20 * min + 1000)
        assertEquals(ClockPhase.Break, s.phase)
        assertEquals(listOf(ClockEvent.PeriodEnd), ev)
        assertEquals(5 * min - 1000, s.remaining(20 * min + 1000)) // o segundo que passou já conta no intervalo
    }

    @Test fun tickPerdido_atravessaVariasFases() {
        // tela bloqueada sem tick do minuto 19 ao 30: fim do 1º, intervalo inteiro e 5 min do 2º
        val (s, ev) = MatchClock.advance(MatchClock.start(cfg, 0), 30 * min)
        assertEquals(listOf(ClockEvent.PeriodEnd, ClockEvent.PeriodStart), ev)
        assertEquals(2, s.period)
        assertEquals(15 * min, s.remaining(30 * min))
    }

    @Test fun ultimoTempo_terminaJogo() {
        val (s, ev) = MatchClock.advance(MatchClock.start(cfg, 0), 45 * min)
        assertEquals(ClockPhase.Finished, s.phase)
        assertEquals(ClockEvent.Finished, ev.last())
        assertEquals("Fim de jogo", MatchClock.label(s))
    }

    @Test fun pausa_congelaTempo() {
        val p = MatchClock.pause(MatchClock.start(cfg, 0), 5 * min)
        assertTrue(p.paused)
        assertEquals(15 * min, p.remaining(60 * min))
        val (r, ev) = MatchClock.advance(MatchClock.resume(p, 60 * min), 61 * min)
        assertEquals(14 * min, r.remaining(61 * min))
        assertTrue(ev.isEmpty())
    }

    @Test fun hidratacao_avisaUmaVezPorIntervalo() {
        val c = cfg.copy(hydrationMin = 7)
        var s = MatchClock.start(c, 0)
        val all = mutableListOf<ClockEvent>()
        for (t in 0..20L * 60) { val (n, e) = MatchClock.advance(s, t * 1000); s = n; all += e }
        // 7 e 14 min: dois avisos; aos 20 acaba o tempo (aviso dos 21 não existe)
        assertEquals(listOf(ClockEvent.Hydration, ClockEvent.Hydration, ClockEvent.PeriodEnd), all)
    }

    @Test fun semIntervalo_emendaProximoTempo() {
        val (s, ev) = MatchClock.advance(MatchClock.start(cfg.copy(breakMin = 0), 0), 20 * min)
        assertEquals(listOf(ClockEvent.PeriodEnd, ClockEvent.PeriodStart), ev)
        assertEquals(ClockPhase.Playing, s.phase)
        assertEquals(2, s.period)
    }

    @Test fun pular_encerraFaseSemAvisoDeHidratacao() {
        val c = cfg.copy(hydrationMin = 5)
        val (s, ev) = MatchClock.skip(MatchClock.start(c, 0), 3 * min)
        assertEquals(ClockPhase.Break, s.phase)
        assertEquals(listOf(ClockEvent.PeriodEnd), ev)
        assertEquals(5 * min, s.remaining(3 * min))
    }

    @Test fun pularPausado_continuaPausado() {
        val p = MatchClock.pause(MatchClock.start(cfg, 0), min)
        val (s, _) = MatchClock.skip(p, 2 * min)
        assertTrue(s.paused)
        assertEquals(ClockPhase.Break, s.phase)
        assertEquals(5 * min, s.remaining(10 * min))
    }

    @Test fun configuracao_limitada() {
        val n = ClockConfig(periods = 9, periodMin = 0, breakMin = 99, hydrationMin = 5).normalized()
        assertEquals(ClockConfig(4, 1, 30, 0), n)
    }

    @Test fun sugestao_cabeNoHorario() {
        assertEquals(ClockConfig(2, 25, 5, 0), ClockConfig.suggest(60))
        assertEquals(ClockConfig(2, 45, 5, 0), ClockConfig.suggest(120))
        assertEquals(ClockConfig(2, 15, 0, 0), ClockConfig.suggest(30))
    }
}
