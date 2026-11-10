package com.arch1based.lensy.reminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arch1based.lensy.MainActivity
import com.arch1based.lensy.R
import com.arch1based.lensy.core.LensPlan
import com.arch1based.lensy.core.nextReminder
import com.arch1based.lensy.data.Prefs
import com.arch1based.lensy.data.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object Reminders {
    private const val CHANNEL = "lens_reminders"
    private const val NOTIFICATION_ID = 1

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.notif_channel),
            NotificationManager.IMPORTANCE_HIGH,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** (Re)arms the single alarm for the next reminder, or cancels it when nothing should fire. */
    fun schedule(context: Context, prefs: Prefs) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(
            context, 0, Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val plan = prefs.plan
        if (plan == null || !prefs.unlocked(LocalDate.now())) {
            alarms.cancel(intent)
            return
        }
        val at = nextReminder(plan, LocalDateTime.now(), prefs.remindAt)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent)
    }

    @SuppressLint("MissingPermission")
    fun notify(context: Context, plan: LensPlan) {
        val left = plan.daysLeft(LocalDate.now())
        if (left > plan.leadDays) return
        val text = when {
            left >= 2 -> context.getString(R.string.notif_2days)
            left == 1L -> context.getString(R.string.notif_1day)
            left == 0L -> context.getString(R.string.notif_today)
            else -> context.getString(R.string.notif_overdue, (-left).toInt())
        }
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}

private fun BroadcastReceiver.withPrefs(context: Context, block: (Prefs) -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            block(Store(context).prefs.first())
        } finally {
            pending.finish()
        }
    }
}

/** Fired by the alarm: show today's reminder, then arm the next one. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = withPrefs(context) { prefs ->
        val plan = prefs.plan
        if (plan != null && prefs.unlocked(LocalDate.now())) Reminders.notify(context, plan)
        Reminders.schedule(context, prefs)
    }
}

/** Alarms are lost on reboot and drift on clock changes, so re-arm. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = withPrefs(context) { prefs ->
        Reminders.schedule(context, prefs)
    }
}
