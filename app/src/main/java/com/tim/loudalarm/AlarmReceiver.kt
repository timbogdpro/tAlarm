package com.tim.loudalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Fired by AlarmManager at the alarm time. Starts playback and rolls the schedule forward. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        if (alarmId == -1) return

        // Start ringing right away via the foreground service.
        val svc = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        ContextCompat.startForegroundService(context, svc)

        // Repeating alarm -> schedule the next occurrence. One-shot -> disable it.
        val store = AlarmStore(context)
        val alarm = store.get(alarmId) ?: return
        if (alarm.days.isNotEmpty()) {
            AlarmScheduler.schedule(context, alarm)
        } else {
            store.put(alarm.copy(enabled = false))
        }
    }
}
