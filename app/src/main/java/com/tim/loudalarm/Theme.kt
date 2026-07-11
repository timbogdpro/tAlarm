package com.tim.loudalarm

import android.app.Activity
import android.content.Context

/**
 * Selectable accent colour scheme. Neutrals (Paper) and light/dark come from the
 * base theme automatically; this just swaps the accent via a ThemeOverlay.
 */
enum class ColorScheme(val storageKey: String, val overlay: Int, val titleRes: Int) {
    GREEN("green", R.style.ThemeOverlay_LoudAlarm_Green, R.string.scheme_green),
    AMBER("amber", R.style.ThemeOverlay_LoudAlarm_Amber, R.string.scheme_amber),
    TEAL("teal", R.style.ThemeOverlay_LoudAlarm_Teal, R.string.scheme_teal),
    PLUM("plum", R.style.ThemeOverlay_LoudAlarm_Plum, R.string.scheme_plum);

    companion object {
        private const val PREFS = "loud_alarm_prefs"
        private const val KEY = "colorScheme"

        fun current(ctx: Context): ColorScheme {
            val key = ctx.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, GREEN.storageKey)
            return entries.firstOrNull { it.storageKey == key } ?: GREEN
        }

        fun save(ctx: Context, scheme: ColorScheme) {
            ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, scheme.storageKey).apply()
        }
    }
}

/** Apply the saved accent overlay. Call in onCreate before inflating the layout. */
fun Activity.applyColorScheme() {
    theme.applyStyle(ColorScheme.current(this).overlay, true)
}
