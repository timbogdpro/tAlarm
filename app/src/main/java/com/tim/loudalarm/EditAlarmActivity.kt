package com.tim.loudalarm

import android.app.AlarmManager
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.tim.loudalarm.databinding.ActivityEditBinding
import java.util.Calendar

class EditAlarmActivity : AppCompatActivity() {

    private lateinit var b: ActivityEditBinding
    private lateinit var store: AlarmStore
    private var preview: MediaPlayer? = null
    private var previewEnhancer: LoudnessEnhancer? = null
    private var savedAlarmVolume: Int? = null
    private val soundRadioIds = mutableListOf<Int>()
    private var hour = 7
    private var minute = 0

    private val dayButtons by lazy {
        listOf(
            b.dayMon to Calendar.MONDAY,
            b.dayTue to Calendar.TUESDAY,
            b.dayWed to Calendar.WEDNESDAY,
            b.dayThu to Calendar.THURSDAY,
            b.dayFri to Calendar.FRIDAY,
            b.daySat to Calendar.SATURDAY,
            b.daySun to Calendar.SUNDAY
        )
    }

    private val weekdayDays = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY
    )
    private val weekendDays = setOf(Calendar.SATURDAY, Calendar.SUNDAY)
    private val allDays = weekdayDays + weekendDays

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyColorScheme()
        b = ActivityEditBinding.inflate(layoutInflater)
        setContentView(b.root)
        store = AlarmStore(this)
        title = getString(R.string.edit_title)

        b.timeRow.setOnClickListener { showTimePicker() }
        buildSoundRadios()

        val id = intent.getIntExtra(EXTRA_ID, -1)
        // A new alarm can be pre-filled with a time (e.g. from the sleep calculator).
        val alarm = (if (id != -1) store.get(id) else null) ?: Alarm(
            id = store.nextId(),
            hour = intent.getIntExtra(EXTRA_HOUR, 7),
            minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        )
        bindToUi(alarm)

        b.volumeSlider.addOnChangeListener { _, v, _ -> b.volumeValue.text = "${v.toInt()}%" }
        b.boostSlider.addOnChangeListener { _, v, _ -> b.boostValue.text = "+${v.toInt()}" }
        b.snoozeSlider.addOnChangeListener { _, v, _ -> b.snoozeValue.text = "${v.toInt()}" }
        b.shakeSlider.addOnChangeListener { _, v, _ -> b.shakeValue.text = "${v.toInt()}" }
        // Reveal the math-type / shake-intensity options for the chosen dismiss mode.
        b.dismissGroup.setOnCheckedChangeListener { _, _ -> updateDismissOptions() }

        b.previewBtn.setOnClickListener { togglePreview() }
        b.saveBtn.setOnClickListener { save(alarm.id) }

        // Quick day presets. Tapping an active preset again clears it (one-shot).
        b.chipAll.setOnClickListener { applyPreset(allDays) }
        b.chipWeekdays.setOnClickListener { applyPreset(weekdayDays) }
        b.chipWeekends.setOnClickListener { applyPreset(weekendDays) }
        // Keep the chips in sync when days are toggled by hand.
        b.daysGroup.addOnButtonCheckedListener { _, _, _ -> syncPresetChips() }
    }

    private fun currentDays(): Set<Int> =
        dayButtons.filter { it.first.isChecked }.map { it.second }.toSet()

    private fun applyPreset(preset: Set<Int>) {
        val next = if (currentDays() == preset) emptySet() else preset
        dayButtons.forEach { (btn, day) -> btn.isChecked = next.contains(day) }
        syncPresetChips()
    }

    private fun syncPresetChips() {
        val d = currentDays()
        b.chipAll.isChecked = d == allDays
        b.chipWeekdays.isChecked = d == weekdayDays
        b.chipWeekends.isChecked = d == weekendDays
    }

    private fun buildSoundRadios() {
        val density = resources.displayMetrics.density
        val padV = (8 * density).toInt()
        Sounds.all.forEachIndexed { index, sound ->
            val rb = RadioButton(this).apply {
                text = getString(sound.titleRes)
                id = 3000 + index
                textSize = 16f
                minHeight = (48 * density).toInt()
                setPadding(paddingLeft, padV, paddingRight, padV)
            }
            soundRadioIds.add(rb.id)
            b.soundGroup.addView(rb)
        }
    }

    private fun bindToUi(a: Alarm) {
        hour = a.hour
        minute = a.minute
        updateTimeDisplay()
        b.labelInput.setText(a.label)
        dayButtons.forEach { (btn, day) -> btn.isChecked = a.days.contains(day) }
        syncPresetChips()
        b.soundGroup.check(soundRadioIds[Sounds.indexOf(a.soundId)])
        b.dismissGroup.check(
            when (a.dismissMode) {
                DismissMode.BUTTON -> b.dmButton.id
                DismissMode.MATH -> b.dmMath.id
                DismissMode.SHAKE -> b.dmShake.id
            }
        )
        b.mathTypeGroup.check(if (a.mathType == MathType.MULT) b.mathMult.id else b.mathAddsub.id)
        val shakes = a.shakeCount.coerceIn(5, 40)
        b.shakeSlider.value = shakes.toFloat()
        b.shakeValue.text = "$shakes"
        b.volumeSlider.value = a.volumePercent.toFloat()
        b.boostSlider.value = a.boostDb.toFloat()
        b.snoozeSlider.value = a.snoozeMinutes.toFloat()
        b.vibrateSwitch.isChecked = a.vibrate
        b.volumeValue.text = "${a.volumePercent}%"
        b.boostValue.text = "+${a.boostDb}"
        b.snoozeValue.text = "${a.snoozeMinutes}"
        updateDismissOptions()
    }

    /** Show the math-type toggle or the shake-intensity slider for the picked mode. */
    private fun updateDismissOptions() {
        val checked = b.dismissGroup.checkedRadioButtonId
        b.mathOptions.visibility = if (checked == b.dmMath.id) View.VISIBLE else View.GONE
        b.shakeOptions.visibility = if (checked == b.dmShake.id) View.VISIBLE else View.GONE
    }

    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hour)
            .setMinute(minute)
            .setTitleText(R.string.edit_title)
            .build()
        picker.addOnPositiveButtonClickListener {
            hour = picker.hour
            minute = picker.minute
            updateTimeDisplay()
        }
        picker.show(supportFragmentManager, "time")
    }

    private fun updateTimeDisplay() {
        b.timeDisplay.text = String.format("%02d:%02d", hour, minute)
    }

    private fun selectedSoundId(): String {
        val idx = soundRadioIds.indexOf(b.soundGroup.checkedRadioButtonId).coerceAtLeast(0)
        return Sounds.all[idx].id
    }

    private fun save(id: Int) {
        val days = dayButtons.filter { it.first.isChecked }.map { it.second }.toSet()
        val dismiss = when (b.dismissGroup.checkedRadioButtonId) {
            b.dmMath.id -> DismissMode.MATH
            b.dmShake.id -> DismissMode.SHAKE
            else -> DismissMode.BUTTON
        }
        val alarm = Alarm(
            id = id,
            hour = hour,
            minute = minute,
            days = days,
            enabled = true,
            label = b.labelInput.text?.toString()?.trim().orEmpty(),
            soundId = selectedSoundId(),
            dismissMode = dismiss,
            volumePercent = b.volumeSlider.value.toInt(),
            boostDb = b.boostSlider.value.toInt(),
            vibrate = b.vibrateSwitch.isChecked,
            snoozeMinutes = b.snoozeSlider.value.toInt().coerceAtLeast(1),
            mathType = if (b.mathTypeGroup.checkedButtonId == b.mathMult.id) MathType.MULT else MathType.ADD_SUB,
            shakeCount = b.shakeSlider.value.toInt()
        )
        store.put(alarm)
        AlarmScheduler.schedule(this, alarm)
        if (needsExactAlarmPermission()) promptExactAlarm() else finish()
    }

    private fun needsExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val am = getSystemService(AlarmManager::class.java) ?: return false
        return !am.canScheduleExactAlarms()
    }

    /** Without this permission the OS delays alarms in Doze — the usual cause of
     *  "it didn't ring (on time)". Ask right when an alarm is created. */
    private fun promptExactAlarm() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.perm_exact_title)
            .setMessage(R.string.perm_exact_body)
            .setPositiveButton(R.string.grant) { _, _ ->
                try {
                    val i = Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:$packageName")
                    )
                    val args = Bundle().apply { putString(":settings:fragment_args_key", packageName) }
                    i.putExtra(":settings:show_fragment_args", args)
                    i.putExtra(":settings:fragment_args_key", packageName)
                    startActivity(i)
                } catch (_: Exception) {
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener { finish() }
            .show()
    }

    private fun togglePreview() {
        if (preview != null) {
            stopPreview()
            return
        }
        // Preview at the REAL alarm loudness: drive the ALARM stream to the chosen
        // level (independent of the phone's media/ringer volume) and apply the boost.
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val pct = b.volumeSlider.value.toInt().coerceIn(1, 100)
        if (savedAlarmVolume == null) {
            savedAlarmVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        }
        audio.setStreamVolume(AudioManager.STREAM_ALARM, (maxVol * pct / 100).coerceIn(1, maxVol), 0)

        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            resources.openRawResourceFd(Sounds.byId(selectedSoundId()).resId).use { afd ->
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            isLooping = false
            setVolume(1f, 1f)
            setOnCompletionListener { stopPreview() }
            prepare()
        }
        preview = mp
        try {
            previewEnhancer = LoudnessEnhancer(mp.audioSessionId).apply {
                setTargetGain(b.boostSlider.value.toInt().coerceIn(0, AlarmService.MAX_BOOST_DB) * 100)
                enabled = true
            }
        } catch (_: Exception) {
        }
        mp.start()
        b.previewBtn.text = getString(R.string.stop)
    }

    private fun stopPreview() {
        preview?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: Exception) {
            }
            it.release()
        }
        preview = null
        previewEnhancer?.release()
        previewEnhancer = null
        savedAlarmVolume?.let { v ->
            (getSystemService(AUDIO_SERVICE) as AudioManager)
                .setStreamVolume(AudioManager.STREAM_ALARM, v, 0)
        }
        savedAlarmVolume = null
        b.previewBtn.text = getString(R.string.preview)
    }

    override fun onPause() {
        super.onPause()
        stopPreview()
    }

    companion object {
        const val EXTRA_ID = "alarmId"
        const val EXTRA_HOUR = "alarmHour"
        const val EXTRA_MINUTE = "alarmMinute"
    }
}
