package com.tim.loudalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.util.Calendar

/** Schedules/cancels alarms with the system AlarmManager. */
object AlarmScheduler {

    const val ACTION_FIRE = "com.tim.loudalarm.action.FIRE"
    const val EXTRA_ALARM_ID = "alarmId"

    private const val PI_FLAGS =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun rescheduleAll(context: Context) {
        AlarmStore(context).getAll().forEach { alarm ->
            if (alarm.enabled) schedule(context, alarm) else cancel(context, alarm.id)
        }
    }

    fun schedule(context: Context, alarm: Alarm) {
        setAt(context, alarm.id, nextTriggerMillis(alarm))
    }

    fun scheduleSnooze(context: Context, alarmId: Int, minutes: Int) {
        setAt(context, alarmId, System.currentTimeMillis() + minutes * 60_000L)
    }

    private fun setAt(context: Context, alarmId: Int, triggerAt: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val firePi = firePendingIntent(context, alarmId)
        val showPi = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PI_FLAGS
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showPi), firePi)
        } else {
            // No exact-alarm permission yet: still fire, just possibly a bit late in Doze.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, firePi)
        }
    }

    fun cancel(context: Context, alarmId: Int) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(firePendingIntent(context, alarmId))
    }

    private fun firePendingIntent(context: Context, alarmId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            data = Uri.parse("loudalarm://alarm/$alarmId")   // makes the PendingIntent unique per alarm
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(context, alarmId, intent, PI_FLAGS)
    }

    /** Next fire time in epoch millis for this alarm's time and repeat-days. */
    fun nextTriggerMillis(alarm: Alarm): Long {
        val now = Calendar.getInstance()
        val base = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (alarm.days.isEmpty()) {
            if (base.timeInMillis <= now.timeInMillis) base.add(Calendar.DAY_OF_YEAR, 1)
            return base.timeInMillis
        }
        for (i in 0..7) {
            val c = base.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, i)
            if (c.timeInMillis > now.timeInMillis && alarm.days.contains(c.get(Calendar.DAY_OF_WEEK))) {
                return c.timeInMillis
            }
        }
        return base.timeInMillis
    }
}
