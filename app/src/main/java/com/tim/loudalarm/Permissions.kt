package com.tim.loudalarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Shared permission checks used by both the onboarding wizard (MainActivity) and
 * the alarm-list permission banner (AlarmsFragment). Kept in one place so the two
 * can't drift apart.
 */

fun Context.needsExactAlarm(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    return !(getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
}

/** True when notifications won't show at all — either the runtime permission is
 *  missing (Android 13+) or the user switched them off in Settings (any version). */
fun Context.needsNotif(): Boolean =
    !NotificationManagerCompat.from(this).areNotificationsEnabled()

/** Only Android 13+ has a runtime prompt we can fire; otherwise we must deep-link. */
fun Context.canAskNotifPermission(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

/** Deep-link straight to this app's notification settings (to switch them back on). */
fun Context.notifSettingsIntent(): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)

fun Context.needsFullScreen(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
    return !(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).canUseFullScreenIntent()
}

fun Context.needsBatteryOpt(): Boolean {
    val pm = getSystemService(PowerManager::class.java) ?: return false
    return !pm.isIgnoringBatteryOptimizations(packageName)
}

/** A settings intent that also asks Settings to scroll to and flash our app's row
 *  in list-style screens (best effort; harmlessly ignored elsewhere). */
fun Context.appSettingsIntent(action: String): Intent {
    val intent = Intent(action, Uri.parse("package:$packageName"))
    val args = Bundle().apply { putString(":settings:fragment_args_key", packageName) }
    intent.putExtra(":settings:show_fragment_args", args)
    intent.putExtra(":settings:fragment_args_key", packageName)
    return intent
}
