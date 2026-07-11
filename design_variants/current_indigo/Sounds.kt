package com.tim.loudalarm

/** The ringtones bundled in res/raw. Swap the files there to change the sounds. */
object Sounds {
    data class Sound(val id: String, val title: String, val resId: Int)

    val all: List<Sound> = listOf(
        Sound("siren", "Сирена", R.raw.siren),
        Sound("train_horn", "Гудок поезда", R.raw.train_horn),
        Sound("ship_horn", "Гудок корабля", R.raw.ship_horn),
        Sound("siren2", "Сирена (вторая)", R.raw.siren2),
        Sound("air_raid_siren", "Воздушная тревога", R.raw.air_raid_siren),
        Sound("fantastic_siren", "Космическая сирена", R.raw.fantastic_siren),
        Sound("loud_siren", "Громкая сирена", R.raw.loud_siren),
        Sound("car_horn", "Клаксон", R.raw.car_horn)
    )

    fun byId(id: String): Sound = all.firstOrNull { it.id == id } ?: all.first()
    fun indexOf(id: String): Int = all.indexOfFirst { it.id == id }.coerceAtLeast(0)
}
