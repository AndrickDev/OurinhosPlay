package com.ourinhosplay.app.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ourinhosplay.app.MainActivity
import com.ourinhosplay.app.data.ClockConfig
import com.ourinhosplay.app.data.ClockEvent
import com.ourinhosplay.app.data.ClockPhase
import com.ourinhosplay.app.data.ClockState
import com.ourinhosplay.app.data.MatchClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Cronômetro rodando agora (um por aparelho), para a tela do jogo acompanhar. */
data class RunningClock(val matchId: String, val title: String, val state: ClockState)

/**
 * Cronômetro da partida em serviço de primeiro plano: continua contando e apitando com a tela
 * bloqueada e o celular no bolso. A conta fica em [MatchClock]; aqui só tick, som e notificação.
 */
class ClockService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null
    private var wake: PowerManager.WakeLock? = null
    private var lastPhase: Pair<Int, ClockPhase>? = null
    private var lastPaused: Boolean? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val now = System.currentTimeMillis()
        when (intent?.action) {
            ACTION_START -> {
                val cfg = ClockConfig(
                    periods = intent.getIntExtra("periods", 2),
                    periodMin = intent.getIntExtra("periodMin", 20),
                    breakMin = intent.getIntExtra("breakMin", 5),
                    hydrationMin = intent.getIntExtra("hydrationMin", 0),
                )
                _running.value = RunningClock(
                    intent.getStringExtra("matchId").orEmpty(),
                    intent.getStringExtra("title").orEmpty(),
                    MatchClock.start(cfg, now),
                )
                goForeground()
                whistle()
                startLoop()
            }
            // pausado não segura o processador; retomar pega de novo
            ACTION_PAUSE -> { update { MatchClock.pause(it, now) }; releaseWake() }
            ACTION_RESUME -> { update { MatchClock.resume(it, now) }; acquireWake() }
            ACTION_SKIP -> {
                var ev = emptyList<ClockEvent>()
                _running.update { r -> r?.let { val (s, e) = MatchClock.skip(it.state, now); ev = e; it.copy(state = s) } }
                react(ev)
            }
            ACTION_STOP -> { stopAll(); return START_NOT_STICKY }
        }
        if (_running.value == null) { stopAll(); return START_NOT_STICKY }
        refreshNotification(force = true)
        return START_NOT_STICKY
    }

    private fun update(f: (ClockState) -> ClockState) {
        _running.update { r -> r?.copy(state = f(r.state)) }
    }

    private fun startLoop() {
        loop?.cancel()
        acquireWake()
        loop = scope.launch {
            while (isActive) {
                val r = _running.value ?: break
                val (s, ev) = MatchClock.advance(r.state, System.currentTimeMillis())
                // CAS: se a pessoa pausou/encerrou neste instante, recalcula no próximo tick
                if (s != r.state && !_running.compareAndSet(r, r.copy(state = s))) { delay(50); continue }
                react(ev)
                refreshNotification(force = false)
                if (s.phase == ClockPhase.Finished) {
                    // acabou: espera o apito final tocar, solta tudo e deixa só a notificação "Fim de jogo"
                    delay(5000)
                    releaseWake()
                    ServiceCompat.stopForeground(this@ClockService, ServiceCompat.STOP_FOREGROUND_DETACH)
                    stopSelf()
                    break
                }
                delay(500)
            }
        }
    }

    private fun react(events: List<ClockEvent>) {
        if (events.isEmpty()) return
        // vários eventos juntos (tick perdido): um apito só, o último é o que importa
        when (events.last()) {
            ClockEvent.Hydration -> { scope.launch { Sfx.play(this@ClockService, Sound.Chime, loud = true) }; buzz(longArrayOf(0, 200, 120, 200)) }
            ClockEvent.PeriodEnd, ClockEvent.PeriodStart -> { whistle(); buzz(longArrayOf(0, 500)) }
            ClockEvent.Finished -> { whistle(); buzz(longArrayOf(0, 500, 200, 500, 200, 800)) }
        }
    }

    private fun whistle() { scope.launch { Sfx.play(this@ClockService, Sound.Whistle, loud = true) } }

    private fun buzz(pattern: LongArray) {
        val v = if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        runCatching { v.vibrate(VibrationEffect.createWaveform(pattern, -1)) }
    }

    private fun acquireWake() {
        if (wake?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ourinhosplay:clock").apply {
            setReferenceCounted(false)
            acquire(8 * 60 * 60 * 1000L) // teto de segurança: 8 h (máximo configurável é 7,5 h)
        }
    }

    private fun goForeground() {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(), type)
    }

    /** Só reconstrói a notificação quando muda fase ou pausa (o relógio dela conta sozinho). */
    private fun refreshNotification(force: Boolean) {
        val s = _running.value?.state ?: return
        val key = s.period to s.phase
        if (!force && key == lastPhase && s.paused == lastPaused) return
        lastPhase = key
        lastPaused = s.paused
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): android.app.Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Cronômetro da partida", NotificationManager.IMPORTANCE_LOW))
        }
        val r = _running.value
        val s = r?.state
        val now = System.currentTimeMillis()
        val open = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val b = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle(r?.title?.ifBlank { null } ?: "Cronômetro")
            .setContentIntent(open)
            .setOngoing(s?.phase != ClockPhase.Finished)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        if (s != null) {
            val label = MatchClock.label(s)
            if (s.paused) {
                b.setContentText("$label · pausado em ${MatchClock.mmss(s.remaining(now))}").setShowWhen(false)
                b.addAction(0, "Retomar", action(ACTION_RESUME))
            } else if (s.phase == ClockPhase.Finished) {
                b.setContentText(label).setShowWhen(false)
            } else {
                b.setContentText(label).setWhen(now + s.remaining(now)).setShowWhen(true)
                    .setUsesChronometer(true).setChronometerCountDown(true)
                b.addAction(0, "Pausar", action(ACTION_PAUSE))
            }
            b.addAction(0, if (s.phase == ClockPhase.Finished) "Fechar" else "Encerrar", action(ACTION_STOP))
        }
        return b.build()
    }

    private fun action(a: String): PendingIntent =
        PendingIntent.getService(this, a.hashCode(), Intent(this, ClockService::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE)

    private fun stopAll() {
        loop?.cancel()
        _running.value = null
        wake?.takeIf { it.isHeld }?.release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseWake() { wake?.takeIf { it.isHeld }?.release() }

    override fun onDestroy() {
        releaseWake()
        _running.value = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "match_clock"
        private const val NOTIF_ID = 4242
        private const val ACTION_START = "clock.start"
        private const val ACTION_PAUSE = "clock.pause"
        private const val ACTION_RESUME = "clock.resume"
        private const val ACTION_SKIP = "clock.skip"
        private const val ACTION_STOP = "clock.stop"

        private val _running = MutableStateFlow<RunningClock?>(null)
        val running: StateFlow<RunningClock?> = _running.asStateFlow()

        fun start(context: Context, matchId: String, title: String, cfg: ClockConfig) {
            val i = Intent(context, ClockService::class.java).setAction(ACTION_START)
                .putExtra("matchId", matchId).putExtra("title", title)
                .putExtra("periods", cfg.periods).putExtra("periodMin", cfg.periodMin)
                .putExtra("breakMin", cfg.breakMin).putExtra("hydrationMin", cfg.hydrationMin)
            ContextCompat.startForegroundService(context, i)
        }

        private fun send(context: Context, a: String) {
            context.startService(Intent(context, ClockService::class.java).setAction(a))
        }

        fun pause(context: Context) = send(context, ACTION_PAUSE)
        fun resume(context: Context) = send(context, ACTION_RESUME)
        fun skip(context: Context) = send(context, ACTION_SKIP)
        fun stop(context: Context) = send(context, ACTION_STOP)
    }
}
