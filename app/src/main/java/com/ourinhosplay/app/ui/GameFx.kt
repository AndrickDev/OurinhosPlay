package com.ourinhosplay.app.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ourinhosplay.app.MainActivity
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Apito de juiz sintetizado em tempo real: sem arquivo de áudio, sem direitos autorais. */
object Whistle {
    private const val RATE = 22050

    private val samples: ShortArray by lazy {
        // "piiii-pi": um apito longo com a bolinha vibrando (trinado) e um curto.
        fun blast(seconds: Double, baseHz: Double): DoubleArray {
            val n = (RATE * seconds).toInt()
            var phase = 0.0
            return DoubleArray(n) { i ->
                val t = i.toDouble() / RATE
                val trill = 1.0 + 0.5 * sin(2 * PI * 32.0 * t)         // bolinha batendo
                val hz = baseHz + 140.0 * sin(2 * PI * 5.0 * t)        // leve oscilação de sopro
                phase += 2 * PI * hz / RATE
                val env = minOf(1.0, t / 0.025) * minOf(1.0, (seconds - t) / 0.06)
                val breath = (Random.nextDouble() - 0.5) * 0.12
                (sin(phase) * 0.55 * trill / 1.5 + sin(phase * 2) * 0.12 + breath) * env
            }
        }
        val a = blast(0.42, 2850.0)
        val gap = DoubleArray((RATE * 0.07).toInt())
        val b = blast(0.22, 3000.0)
        val all = a + gap + b
        ShortArray(all.size) { (all[it].coerceIn(-1.0, 1.0) * 0.8 * Short.MAX_VALUE).toInt().toShort() }
    }

    /** Respeita o toque do celular: silencioso ou vibrar não emite som. */
    suspend fun play(context: Context) = withContext(Dispatchers.Default) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return@withContext
        val data = samples
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(data.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        runCatching {
            track.write(data, 0, data.size)
            track.play()
            kotlinx.coroutines.delay(data.size * 1000L / RATE + 150)
        }
        track.release()
    }
}

/** Notificação com cronômetro regressivo até o início do jogo (e progressivo depois). */
object MatchReminder {
    private const val CHANNEL = "match_countdown"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun id(matchId: String) = matchId.hashCode()

    fun show(context: Context, matchId: String, title: String, venue: String, startsAtMs: Long, endsAtMs: Long) {
        if (!canNotify(context)) return
        val now = System.currentTimeMillis()
        if (endsAtMs <= now) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Contagem para o jogo", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle("⚽ $title")
            .setContentText("$venue · começa em…")
            .setContentIntent(open)
            .setWhen(startsAtMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setTimeoutAfter(endsAtMs - now) // some sozinha quando o jogo acaba
            .build()
        nm.notify(id(matchId), n)
    }

    fun cancel(context: Context, matchId: String) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id(matchId))
    }
}
