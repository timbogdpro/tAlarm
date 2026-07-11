package com.tim.loudalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arms all enabled alarms after reboot, app update, or a system time/zone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.rescheduleAll(context)
    }
}
