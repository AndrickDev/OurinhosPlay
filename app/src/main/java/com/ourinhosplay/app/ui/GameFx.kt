package com.ourinhosplay.app.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ourinhosplay.app.MainActivity

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

/** Dispara a notificação "seu jogo começa em 1 hora". */
class ReminderReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!MatchReminder.canNotify(context)) return
        val title = intent.getStringExtra("title") ?: return
        val venue = intent.getStringExtra("venue").orEmpty()
        val matchId = intent.getStringExtra("matchId") ?: return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel("match_reminder", "Lembrete do jogo", NotificationManager.IMPORTANCE_HIGH),
            )
        }
        val open = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, "match_reminder")
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle("⏱ Seu jogo começa em 1 hora")
            .setContentText("$title · $venue")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        nm.notify(("rem" + matchId).hashCode(), n)
    }
}

/** Lembrete local 1 h antes do jogo (alarme do sistema; não precisa de servidor). */
object Reminders {
    private fun pending(context: Context, matchId: String, title: String, venue: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, ("rem" + matchId).hashCode(),
            Intent(context, ReminderReceiver::class.java)
                .putExtra("matchId", matchId).putExtra("title", title).putExtra("venue", venue),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun schedule(context: Context, matchId: String, title: String, venue: String, startMs: Long) {
        val at = startMs - 60 * 60 * 1000L
        if (at <= System.currentTimeMillis()) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        runCatching { am.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, at, pending(context, matchId, title, venue)) }
    }

    fun cancel(context: Context, matchId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        runCatching { am.cancel(pending(context, matchId, "", "")) }
    }
}
