package com.ourinhosplay.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Sons do app, sintetizados em tempo real: sem arquivo de áudio e sem direitos autorais. */
enum class Sound {
    /** Apito de juiz: entrar no jogo. */
    Whistle,
    /** Dois toques agudos: "cheguei", jogo criado. */
    Chime,
    /** Estalo curto: frase enviada no chat. */
    Pop,
    /** Dois toques descendo: saiu do jogo. */
    Down,
    /** Clique de câmera: foto enviada. */
    Shutter,
    /** Arpejo curto: jogo publicado. */
    Fanfare,
}

object Sfx {
    private const val RATE = 22050
    private const val PREFS = "ourinhosplay_prefs"
    private const val KEY_ON = "sound_on"

    fun isOn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ON, true)

    fun setOn(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ON, on).apply()
    }

    // ---------------------------------------------------------------- síntese

    /** Nota de sino: seno + 2º harmônico, ataque rápido e queda exponencial. */
    private fun bell(hz: Double, seconds: Double, volume: Double = 0.55): DoubleArray {
        val n = (RATE * seconds).toInt()
        return DoubleArray(n) { i ->
            val t = i.toDouble() / RATE
            val attack = minOf(1.0, t / 0.006)
            val env = attack * exp(-t * 7.0)
            (sin(2 * PI * hz * t) + 0.3 * sin(2 * PI * hz * 2 * t)) * env * volume
        }
    }

    /** Varredura de frequência (estalo/pop). */
    private fun sweep(fromHz: Double, toHz: Double, seconds: Double, volume: Double = 0.6): DoubleArray {
        val n = (RATE * seconds).toInt()
        var phase = 0.0
        return DoubleArray(n) { i ->
            val p = i.toDouble() / n
            phase += 2 * PI * (fromHz + (toHz - fromHz) * p) / RATE
            sin(phase) * volume * (1 - p) * minOf(1.0, i / (RATE * 0.003))
        }
    }

    private fun noiseBurst(seconds: Double, volume: Double = 0.5): DoubleArray {
        val n = (RATE * seconds).toInt()
        var last = 0.0
        return DoubleArray(n) { i ->
            val p = i.toDouble() / n
            last = last * 0.55 + (Random.nextDouble() - 0.5) * 0.9 * 0.45 // passa-baixa leve
            last * (1 - p) * (1 - p) * volume * 2
        }
    }

    /** Soma `b` em `a` começando em `offsetSeconds`. */
    private fun mix(a: DoubleArray, b: DoubleArray, offsetSeconds: Double): DoubleArray {
        val off = (RATE * offsetSeconds).toInt()
        val out = DoubleArray(maxOf(a.size, off + b.size))
        a.copyInto(out)
        for (i in b.indices) out[off + i] += b[i]
        return out
    }

    private fun build(sound: Sound): DoubleArray = when (sound) {
        Sound.Whistle -> DoubleArray(0) // gravação real: res/raw/apito_juiz.wav
        Sound.Chime -> mix(bell(784.0, 0.45), bell(1175.0, 0.55), 0.11)
        Sound.Pop -> sweep(720.0, 260.0, 0.09)
        Sound.Down -> mix(bell(587.0, 0.30, 0.45), bell(392.0, 0.45, 0.45), 0.12)
        Sound.Shutter -> mix(noiseBurst(0.035), noiseBurst(0.05, 0.4), 0.07)
        Sound.Fanfare -> {
            var a = bell(523.0, 0.40, 0.45)
            a = mix(a, bell(659.0, 0.40, 0.45), 0.09)
            a = mix(a, bell(784.0, 0.40, 0.45), 0.18)
            mix(a, bell(1047.0, 0.70, 0.5), 0.27)
        }
    }

    private val cache = HashMap<Sound, ShortArray>()

    private fun pcm(sound: Sound): ShortArray = synchronized(cache) {
        cache.getOrPut(sound) {
            val d = build(sound)
            ShortArray(d.size) { (d[it].coerceIn(-1.0, 1.0) * 0.85 * Short.MAX_VALUE).toInt().toShort() }
        }
    }

    private val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Default + kotlinx.coroutines.SupervisorJob())

    /** Toca sem esperar e sem morrer junto com a tela que pediu o som. */
    fun fire(context: Context, sound: Sound) {
        val app = context.applicationContext
        scope.launch { play(app, sound) }
    }

    /** Apito real (CC0, SpliceSound via Wikimedia Commons) tocado por MediaPlayer. */
    private suspend fun playRaw(context: Context, resId: Int) {
        val mp = android.media.MediaPlayer.create(context, resId) ?: return
        mp.setOnCompletionListener { it.release() }
        mp.setOnErrorListener { p, _, _ -> p.release(); true }
        mp.start()
    }

    /** Respeita a preferência do app e o toque do celular (silencioso/vibrar = sem som). */
    suspend fun play(context: Context, sound: Sound) = withContext(Dispatchers.Default) {
        if (!isOn(context)) return@withContext
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return@withContext
        if (sound == Sound.Whistle) {
            playRaw(context, com.ourinhosplay.app.R.raw.apito_juiz)
            return@withContext
        }
        val data = pcm(sound)
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
