package com.tim.loudalarm

import android.content.Context
import org.json.JSONArray

/** Simple JSON-in-SharedPreferences persistence. Good enough for a handful of alarms. */
class AlarmStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getAll(): List<Alarm> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length())
            .map { Alarm.fromJson(arr.getJSONObject(it)) }
            .sortedWith(compareBy({ it.hour }, { it.minute }))
    }

    fun get(id: Int): Alarm? = getAll().firstOrNull { it.id == id }

    fun put(alarm: Alarm) {
        val list = getAll().filter { it.id != alarm.id }.toMutableList()
        list.add(alarm)
        save(list)
    }

    fun delete(id: Int) = save(getAll().filter { it.id != id })

    fun nextId(): Int = (getAll().maxOfOrNull { it.id } ?: 0) + 1

    private fun save(list: List<Alarm>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val PREFS = "loud_alarm_prefs"
        private const val KEY = "alarms"
    }
}
