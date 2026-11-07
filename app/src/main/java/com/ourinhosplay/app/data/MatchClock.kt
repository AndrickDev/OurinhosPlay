package com.ourinhosplay.app.data

/** Como a partida é dividida: tempos, intervalo entre eles e aviso de hidratação. */
data class ClockConfig(
    val periods: Int = 2,
    val periodMin: Int = 20,
    val breakMin: Int = 5,
    /** A cada quantos minutos de bola rolando avisa para beber água. 0 = sem aviso. */
    val hydrationMin: Int = 0,
) {
    fun normalized(): ClockConfig {
        val p = periodMin.coerceIn(1, 90)
        return ClockConfig(
            periods = periods.coerceIn(1, 4),
            periodMin = p,
            breakMin = breakMin.coerceIn(0, 30),
            hydrationMin = if (hydrationMin <= 0 || hydrationMin >= p) 0 else hydrationMin,
        )
    }

    companion object {
        /** Sugestão a partir da duração do jogo: dois tempos que cabem no horário. */
        fun suggest(durationMin: Int): ClockConfig {
            val brk = if (durationMin >= 60) 5 else 0
            val per = ((durationMin - brk) / 2 / 5 * 5).coerceIn(5, 45) // múltiplo de 5, como o seletor
            return ClockConfig(periods = 2, periodMin = per, breakMin = brk)
        }
    }
}

enum class ClockPhase { Playing, Break, Finished }

enum class ClockEvent { Hydration, PeriodEnd, PeriodStart, Finished }

/**
 * Estado do cronômetro, guardado como instantes (não como contador): sobrevive a atrasos do
 * relógio e a ticks perdidos com a tela bloqueada. [runningSince] nulo = pausado.
 */
data class ClockState(
    val config: ClockConfig,
    val period: Int = 1,
    val phase: ClockPhase = ClockPhase.Playing,
    val runningSince: Long? = null,
    val accMs: Long = 0,
    val hydrationsDone: Int = 0,
) {
    val paused: Boolean get() = runningSince == null && phase != ClockPhase.Finished

    fun elapsed(now: Long): Long = accMs + (runningSince?.let { now - it } ?: 0)

    val phaseMs: Long
        get() = when (phase) {
            ClockPhase.Playing -> config.periodMin * 60_000L
            ClockPhase.Break -> config.breakMin * 60_000L
            ClockPhase.Finished -> 0
        }

    fun remaining(now: Long): Long = (phaseMs - elapsed(now)).coerceAtLeast(0)
}

object MatchClock {
    fun start(config: ClockConfig, now: Long) = ClockState(config.normalized(), runningSince = now)

    fun pause(s: ClockState, now: Long): ClockState =
        if (s.runningSince == null) s else s.copy(accMs = s.elapsed(now), runningSince = null)

    fun resume(s: ClockState, now: Long): ClockState =
        if (s.runningSince != null || s.phase == ClockPhase.Finished) s else s.copy(runningSince = now)

    /** Encerra a fase atual agora (tempo acabou antes, ou pular o intervalo). */
    fun skip(s: ClockState, now: Long): Pair<ClockState, List<ClockEvent>> {
        if (s.phase == ClockPhase.Finished) return s to emptyList()
        val forced = s.copy(accMs = s.phaseMs, runningSince = now, hydrationsDone = Int.MAX_VALUE)
        val (next, ev) = advance(forced, now)
        // quem pulou estando pausado continua pausado na fase seguinte
        return (if (s.runningSince == null) pause(next, now) else next) to ev
    }

    /** Aplica o tempo passado: dispara avisos e troca de fase quantas vezes for preciso. */
    fun advance(s0: ClockState, now: Long): Pair<ClockState, List<ClockEvent>> {
        var s = s0
        val events = mutableListOf<ClockEvent>()
        while (s.runningSince != null && s.phase != ClockPhase.Finished) {
            val el = s.elapsed(now)
            if (s.phase == ClockPhase.Playing && s.config.hydrationMin > 0) {
                val hydMs = s.config.hydrationMin * 60_000L
                val due = (minOf(el, s.phaseMs - 1) / hydMs).toInt()
                if (due > s.hydrationsDone) {
                    events += ClockEvent.Hydration
                    s = s.copy(hydrationsDone = due)
                }
            }
            if (el < s.phaseMs) break
            val overflow = el - s.phaseMs
            val at = now - overflow // instante exato em que a fase acabou
            s = when {
                s.phase == ClockPhase.Playing && s.period >= s.config.periods -> {
                    events += ClockEvent.Finished
                    s.copy(phase = ClockPhase.Finished, runningSince = null, accMs = 0)
                }
                s.phase == ClockPhase.Playing && s.config.breakMin > 0 -> {
                    events += ClockEvent.PeriodEnd
                    s.copy(phase = ClockPhase.Break, runningSince = at, accMs = 0, hydrationsDone = 0)
                }
                else -> {
                    if (s.phase == ClockPhase.Playing) events += ClockEvent.PeriodEnd
                    events += ClockEvent.PeriodStart
                    s.copy(phase = ClockPhase.Playing, period = s.period + 1, runningSince = at, accMs = 0, hydrationsDone = 0)
                }
            }
        }
        return s to events
    }

    /** "1º tempo", "Intervalo", "Fim de jogo". */
    fun label(s: ClockState): String = when (s.phase) {
        ClockPhase.Playing -> if (s.config.periods == 1) "Tempo único" else "${s.period}º tempo"
        ClockPhase.Break -> "Intervalo"
        ClockPhase.Finished -> "Fim de jogo"
    }

    fun mmss(ms: Long): String {
        val t = (ms + 999) / 1000 // arredonda para cima: 00:00 só quando acabou mesmo
        return "%02d:%02d".format(t / 60, t % 60)
    }
}
