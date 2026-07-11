package com.tim.loudalarm

/**
 * Sleep-cycle math. Pure functions, no Android deps — unit-testable.
 *
 * The idea most sleep calculators use: sleep runs in ~90-minute cycles, and it's
 * easier to wake between cycles than mid-cycle. We add a fixed latency to fall
 * asleep. Waking after 5–6 full cycles (7.5–9 h) is the sweet spot for adults.
 */
object SleepCalculator {

    const val CYCLE_MIN = 90
    const val FALL_ASLEEP_MIN = 15

    /** Cycle counts we suggest, best first. 5–6 are flagged as recommended. */
    val CYCLES = listOf(6, 5, 4, 3)

    fun isRecommended(cycles: Int): Boolean = cycles in 5..6

    /** One suggested time. [totalSleepMin] is time asleep (cycles × 90), excluding latency. */
    data class Suggestion(
        val hour: Int,
        val minute: Int,
        val cycles: Int,
        val totalSleepMin: Int
    ) {
        val recommended: Boolean get() = isRecommended(cycles)
    }

    /** Go to bed at [bedHour]:[bedMin] → good times to wake up. */
    fun wakeTimesFor(bedHour: Int, bedMin: Int): List<Suggestion> =
        CYCLES.map { c ->
            val total = c * CYCLE_MIN
            at(bedHour * 60 + bedMin + FALL_ASLEEP_MIN + total, c, total)
        }

    /** Want to wake at [wakeHour]:[wakeMin] → good times to go to bed. */
    fun bedTimesFor(wakeHour: Int, wakeMin: Int): List<Suggestion> =
        CYCLES.map { c ->
            val total = c * CYCLE_MIN
            at(wakeHour * 60 + wakeMin - FALL_ASLEEP_MIN - total, c, total)
        }

    private fun at(minutesOfDay: Int, cycles: Int, total: Int): Suggestion {
        val norm = ((minutesOfDay % 1440) + 1440) % 1440   // wrap into 0..1439
        return Suggestion(norm / 60, norm % 60, cycles, total)
    }
}
