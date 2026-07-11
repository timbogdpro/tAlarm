package com.tim.loudalarm

import org.json.JSONArray
import org.json.JSONObject

/** How the user must turn a ringing alarm off. */
enum class DismissMode(val storageKey: String, val titleRes: Int) {
    BUTTON("button", R.string.dm_button),
    MATH("math", R.string.dm_math),
    SHAKE("shake", R.string.dm_shake);

    companion object {
        fun from(key: String?): DismissMode =
            entries.firstOrNull { it.storageKey == key } ?: BUTTON
    }
}

/** Which kind of problem the MATH dismiss shows. */
enum class MathType(val storageKey: String, val titleRes: Int) {
    ADD_SUB("addsub", R.string.math_addsub),   // single-digit + / −
    MULT("mult", R.string.math_mult);          // multiplication

    companion object {
        fun from(key: String?): MathType =
            entries.firstOrNull { it.storageKey == key } ?: ADD_SUB
    }
}

/**
 * A single alarm. [days] holds java.util.Calendar day constants (SUNDAY=1 .. SATURDAY=7).
 * An empty [days] set means a one-shot alarm that fires at the next occurrence and then disables.
 */
data class Alarm(
    val id: Int,
    var hour: Int = 7,
    var minute: Int = 0,
    var days: Set<Int> = emptySet(),
    var enabled: Boolean = true,
    var label: String = "",
    var soundId: String = Sounds.all.first().id,
    var dismissMode: DismissMode = DismissMode.BUTTON,
    var volumePercent: Int = 100,   // share of the alarm stream's max volume
    var boostDb: Int = 60,          // extra LoudnessEnhancer gain in dB (0..60); default = loudest
    var vibrate: Boolean = true,
    var snoozeMinutes: Int = 5,
    var mathType: MathType = MathType.ADD_SUB,  // kind of problem for the MATH dismiss
    var mathDigits: Int = 1,        // digits per operand (1 = single-digit problems)
    var shakeCount: Int = 15        // shakes required to dismiss
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("hour", hour)
        put("minute", minute)
        put("days", JSONArray(days.toList()))
        put("enabled", enabled)
        put("label", label)
        put("soundId", soundId)
        put("dismissMode", dismissMode.storageKey)
        put("volumePercent", volumePercent)
        put("boostDb", boostDb)
        put("vibrate", vibrate)
        put("snoozeMinutes", snoozeMinutes)
        put("mathType", mathType.storageKey)
        put("mathDigits", mathDigits)
        put("shakeCount", shakeCount)
    }

    companion object {
        fun fromJson(o: JSONObject): Alarm {
            val daysArr = o.optJSONArray("days") ?: JSONArray()
            val days = (0 until daysArr.length()).map { daysArr.getInt(it) }.toSet()
            return Alarm(
                id = o.getInt("id"),
                hour = o.optInt("hour", 7),
                minute = o.optInt("minute", 0),
                days = days,
                enabled = o.optBoolean("enabled", true),
                label = o.optString("label", ""),
                soundId = o.optString("soundId", Sounds.all.first().id),
                dismissMode = DismissMode.from(o.optString("dismissMode")),
                volumePercent = o.optInt("volumePercent", 100),
                boostDb = o.optInt("boostDb", 60),
                vibrate = o.optBoolean("vibrate", true),
                snoozeMinutes = o.optInt("snoozeMinutes", 5),
                mathType = MathType.from(o.optString("mathType")),
                mathDigits = o.optInt("mathDigits", 1),
                shakeCount = o.optInt("shakeCount", 15)
            )
        }
    }
}
