package com.tim.loudalarm

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tim.loudalarm.databinding.ActivityAlarmBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt
import kotlin.random.Random

/** The full-screen "it's ringing" screen. Turns the alarm off only after the chosen challenge. */
class AlarmActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var b: ActivityAlarmBinding
    private lateinit var alarm: Alarm
    private var sensorManager: SensorManager? = null
    private var shakesLeft = 0
    private var lastShake = 0L
    private var expectedAnswer = 0
    private var soundMuted = false

    /** Safety: if the user went quiet after muting (e.g. dozed off mid-problem), bring
     *  the noise back so a silenced alarm can't turn into oversleeping. */
    private val reblare = Runnable {
        soundMuted = false
        sendServiceAction(AlarmService.ACTION_UNMUTE)
    }

    private val clockHandler = Handler(Looper.getMainLooper())
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val clockTick = object : Runnable {
        override fun run() {
            b.clock.text = timeFmt.format(Date())
            clockHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyColorScheme()
        showWhenLockedAndTurnScreenOn()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        b = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(b.root)

        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        alarm = AlarmStore(this).get(id) ?: Alarm(id = id)

        b.alarmLabel.text = if (alarm.label.isBlank()) getString(R.string.app_name) else alarm.label
        clockTick.run()
        setupChallenge()

        b.dismissBtn.setOnClickListener { onDismissAttempt() }
        b.snoozeBtn.setOnClickListener { snooze() }

        // Block Back so the alarm can't be swiped away without completing the challenge.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
    }

    /** The alarm can be launched twice at start (direct + full-screen intent). As a
     *  singleInstance, the second arrives here — keep the current challenge, don't rebuild. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun setupChallenge() {
        when (alarm.dismissMode) {
            DismissMode.BUTTON -> {
                b.mathPanel.visibility = View.GONE
                b.shakePanel.visibility = View.GONE
                b.dismissBtn.visibility = View.VISIBLE
            }
            DismissMode.MATH -> {
                b.mathPanel.visibility = View.VISIBLE
                b.shakePanel.visibility = View.GONE
                b.dismissBtn.visibility = View.VISIBLE
                newMathProblem()
                // Kill the noise the moment the user touches / starts typing the answer,
                // so mental math half-asleep happens in peace (not under the siren).
                b.mathAnswer.setOnTouchListener { v, e ->
                    if (e.actionMasked == MotionEvent.ACTION_DOWN) onAnswerInteraction()
                    v.performClick()
                    false
                }
                b.mathAnswer.addTextChangedListener(afterTextChanged = { s ->
                    if (!s.isNullOrEmpty()) onAnswerInteraction()
                })
            }
            DismissMode.SHAKE -> {
                b.mathPanel.visibility = View.GONE
                b.shakePanel.visibility = View.VISIBLE
                b.dismissBtn.visibility = View.GONE
                shakesLeft = alarm.shakeCount.coerceAtLeast(1)
                b.shakeProgress.max = shakesLeft
                b.shakeProgress.progress = 0
                b.shakeInfo.text = getString(R.string.shakes_left, shakesLeft)
                startShakeDetector()
            }
        }
    }

    private fun newMathProblem() {
        if (alarm.mathType == MathType.ADD_SUB) {
            // single-digit addition / subtraction (subtraction never goes negative)
            val a = Random.nextInt(1, 10)
            val c = Random.nextInt(1, 10)
            if (Random.nextBoolean()) {
                expectedAnswer = a + c
                b.mathProblem.text = "$a + $c = ?"
            } else {
                val hi = maxOf(a, c); val lo = minOf(a, c)
                expectedAnswer = hi - lo
                b.mathProblem.text = "$hi − $lo = ?"
            }
        } else {
            val digits = alarm.mathDigits.coerceIn(1, 3)
            val min = when (digits) { 1 -> 2; 2 -> 11; else -> 101 }
            val max = when (digits) { 1 -> 9; 2 -> 99; else -> 999 }
            val x = Random.nextInt(min, max + 1)
            val y = Random.nextInt(min, max + 1)
            expectedAnswer = x * y
            b.mathProblem.text = "$x × $y = ?"
        }
        b.mathAnswer.text?.clear()
    }

    private fun onDismissAttempt() {
        if (alarm.dismissMode == DismissMode.MATH) {
            val entered = b.mathAnswer.text?.toString()?.trim()?.toIntOrNull()
            if (entered == expectedAnswer) {
                dismiss()
            } else {
                b.mathInfo.text = getString(R.string.wrong_answer)
                newMathProblem()
            }
        } else {
            dismiss()
        }
    }

    /** Any interaction with the answer mutes the alarm and (re)arms the safety re-blare. */
    private fun onAnswerInteraction() {
        if (!soundMuted) {
            soundMuted = true
            sendServiceAction(AlarmService.ACTION_MUTE)
        }
        clockHandler.removeCallbacks(reblare)
        clockHandler.postDelayed(reblare, REBLARE_MS)
    }

    private fun sendServiceAction(action: String) {
        startService(Intent(this, AlarmService::class.java).setAction(action))
    }

    private fun startShakeDetector() {
        val sm = getSystemService(SENSOR_SERVICE) as SensorManager
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel == null) {
            // No accelerometer -> don't trap the user, show the button instead.
            b.dismissBtn.visibility = View.VISIBLE
            return
        }
        sensorManager = sm
        // UI rate is plenty for shake detection and draws far less power than GAME.
        sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val gForce = sqrt(
            event.values[0] * event.values[0] +
                event.values[1] * event.values[1] +
                event.values[2] * event.values[2]
        ) / SensorManager.GRAVITY_EARTH
        val now = System.currentTimeMillis()
        if (gForce > 2.4f && now - lastShake > 350) {
            lastShake = now
            shakesLeft--
            b.shakeProgress.progress = (alarm.shakeCount - shakesLeft).coerceAtLeast(0)
            if (shakesLeft <= 0) {
                dismiss()
            } else {
                b.shakeInfo.text = getString(R.string.shakes_left, shakesLeft)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun dismiss() {
        stopSensor()
        startService(Intent(this, AlarmService::class.java).setAction(AlarmService.ACTION_STOP))
        finish()
    }

    /** Ask "for how long?" before snoozing, defaulting nothing — a quick tap picks it. */
    private fun snooze() {
        val options = intArrayOf(5, 10, 15, 20, 30)
        val labels = options.map { getString(R.string.snooze_min_fmt, it) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.snooze_pick_title)
            .setItems(labels) { _, which -> doSnooze(options[which]) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun doSnooze(minutes: Int) {
        stopSensor()
        startService(Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmService.EXTRA_SNOOZE_MIN, minutes)
        })
        finish()
    }

    private fun stopSensor() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    override fun onDestroy() {
        clockHandler.removeCallbacks(clockTick)
        clockHandler.removeCallbacks(reblare)
        stopSensor()
        super.onDestroy()
    }

    companion object {
        /** After muting, if the user goes silent this long, the alarm blares again. */
        private const val REBLARE_MS = 90_000L
    }
}
