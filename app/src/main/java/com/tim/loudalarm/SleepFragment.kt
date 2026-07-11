package com.tim.loudalarm

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.color.MaterialColors
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.tim.loudalarm.databinding.FragmentSleepBinding
import com.tim.loudalarm.databinding.ItemSleepTimeBinding
import java.util.Calendar

/**
 * Sleep-cycle calculator tab. Two directions:
 *  - WAKE: "I go to bed at X" → best times to wake up.
 *  - BED:  "I want to wake at X" → best times to go to bed.
 * Tapping a suggested time opens the alarm editor pre-filled with it.
 */
class SleepFragment : Fragment() {

    private var _b: FragmentSleepBinding? = null
    private val b get() = _b!!

    private var mode = WAKE
    private var bedH = 23   // input time for WAKE mode
    private var bedM = 0
    private var wakeH = 7    // input time for BED mode
    private var wakeM = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentSleepBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.modeGroup.check(b.btnWake.id)
        b.modeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            mode = if (checkedId == b.btnBed.id) BED else WAKE
            compute()
        }
        b.inputTime.setOnClickListener { pickTime() }
        b.nowBtn.setOnClickListener {
            val c = Calendar.getInstance()
            setInput(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
            compute()
        }
        compute()
    }

    private fun curH() = if (mode == WAKE) bedH else wakeH
    private fun curM() = if (mode == WAKE) bedM else wakeM

    private fun setInput(h: Int, m: Int) {
        if (mode == WAKE) { bedH = h; bedM = m } else { wakeH = h; wakeM = m }
    }

    private fun pickTime() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(curH())
            .setMinute(curM())
            .setTitleText(if (mode == WAKE) R.string.sleep_input_bed else R.string.sleep_input_wake)
            .build()
        picker.addOnPositiveButtonClickListener {
            setInput(picker.hour, picker.minute)
            compute()
        }
        picker.show(childFragmentManager, "sleeptime")
    }

    private fun compute() {
        b.inputLabel.setText(if (mode == WAKE) R.string.sleep_input_bed else R.string.sleep_input_wake)
        b.inputTime.text = fmt(curH(), curM())
        b.resultsTitle.setText(if (mode == WAKE) R.string.sleep_results_wake else R.string.sleep_results_bed)
        val suggestions =
            if (mode == WAKE) SleepCalculator.wakeTimesFor(bedH, bedM)
            else SleepCalculator.bedTimesFor(wakeH, wakeM)
        render(suggestions)
    }

    private fun render(items: List<SleepCalculator.Suggestion>) {
        b.results.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        for (s in items) {
            val row = ItemSleepTimeBinding.inflate(inflater, b.results, false)
            row.time.text = fmt(s.hour, s.minute)
            row.subtitle.text = "${cyclesText(s.cycles)} · ${durationText(s.totalSleepMin)}"
            if (s.recommended) {
                row.time.setTextColor(
                    MaterialColors.getColor(row.time, com.google.android.material.R.attr.colorPrimary)
                )
                row.badge.visibility = View.VISIBLE
            } else {
                row.time.setTextColor(
                    MaterialColors.getColor(row.time, com.google.android.material.R.attr.colorOnSurface)
                )
                row.badge.visibility = View.GONE
            }
            row.root.setOnClickListener { createAlarmAt(s.hour, s.minute) }
            b.results.addView(row.root)
        }
    }

    private fun createAlarmAt(hour: Int, minute: Int) {
        startActivity(
            Intent(requireContext(), EditAlarmActivity::class.java)
                .putExtra(EditAlarmActivity.EXTRA_HOUR, hour)
                .putExtra(EditAlarmActivity.EXTRA_MINUTE, minute)
        )
    }

    private fun fmt(h: Int, m: Int) = String.format("%02d:%02d", h, m)

    private fun cyclesText(cycles: Int) =
        resources.getQuantityString(R.plurals.cycles, cycles, cycles)

    private fun durationText(totalMin: Int): String {
        val h = totalMin / 60
        val m = totalMin % 60
        val hu = getString(R.string.dur_h)
        return if (m == 0) "$h $hu" else "$h $hu $m ${getString(R.string.dur_m)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    companion object {
        private const val WAKE = 0
        private const val BED = 1
    }
}
