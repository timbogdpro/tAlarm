package com.tim.loudalarm

/** The ringtones bundled in res/raw. Swap the files there to change the sounds. */
object Sounds {
    data class Sound(val id: String, val titleRes: Int, val resId: Int)

    val all: List<Sound> = listOf(
        Sound("air_raid_siren", R.string.sound_air_raid, R.raw.air_raid_siren),
        Sound("loud_siren", R.string.sound_loud_siren, R.raw.loud_siren),
        Sound("fantastic_siren", R.string.sound_fantastic, R.raw.fantastic_siren),
        Sound("car_horn", R.string.sound_car_horn, R.raw.car_horn)
    )

    fun byId(id: String): Sound = all.firstOrNull { it.id == id } ?: all.first()
    fun indexOf(id: String): Int = all.indexOfFirst { it.id == id }.coerceAtLeast(0)
}
