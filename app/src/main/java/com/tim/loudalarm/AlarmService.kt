package com.tim.loudalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

/**
 * Foreground service that makes the noise. This is where "super loud" happens:
 *  - plays on the ALARM stream (bypasses silent / Do-Not-Disturb),
 *  - forces that stream to its maximum volume,
 *  - adds a LoudnessEnhancer on top for extra gain,
 *  - vibrates and pops the full-screen ringing activity.
 */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var enhancer: LoudnessEnhancer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                val id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
                val minutes = intent.getIntExtra(EXTRA_SNOOZE_MIN, 5)
                if (id != -1) AlarmScheduler.scheduleSnooze(this, id, minutes)
                stopEverything()
                return START_NOT_STICKY
            }
            // Silence the noise while the user solves the math challenge, but keep the
            // alarm alive (service + wakelock) so it isn't actually dismissed yet.
            ACTION_MUTE -> {
                muteSound()
                return START_NOT_STICKY
            }
            ACTION_UNMUTE -> {
                unmuteSound()
                return START_NOT_STICKY
            }
        }

        val alarmId = intent?.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1
        // Only ring for a genuine START of a known alarm. A system restart delivers a
        // null intent; a stale id may point at a deleted alarm. Either way, do not spin
        // up playback / wakelock / foreground — just stop, so we never drain the battery
        // on a phantom alarm.
        val alarm = if (intent?.action == ACTION_START && alarmId != -1) {
            AlarmStore(this).get(alarmId)
        } else null
        if (alarm == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Must call startForeground quickly after startForegroundService.
        startForeground(NOTIF_ID, buildNotification(alarm))
        acquireWakeLock()
        startPlayback(alarm)
        if (alarm.vibrate) startVibration()
        launchAlarmScreen(alarmId)
        // Not sticky: if the system kills us mid-ring, AlarmManager (setAlarmClock)
        // remains the source of truth and will re-fire — no zombie restart.
        return START_NOT_STICKY
    }

    private fun startPlayback(alarm: Alarm) {
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        // Force the alarm stream to (a share of) its maximum.
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val target = (max * alarm.volumePercent / 100).coerceIn(1, max)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)

        val sound = Sounds.byId(alarm.soundId)
        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            resources.openRawResourceFd(sound.resId).use { afd ->
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            isLooping = true
            setVolume(1f, 1f)
            prepare()
        }
        player = mp

        // Boost beyond the normal maximum. Some devices/ROMs may not support the effect.
        try {
            enhancer = LoudnessEnhancer(mp.audioSessionId).apply {
                setTargetGain(alarm.boostDb.coerceIn(0, MAX_BOOST_DB) * 100) // dB -> millibels
                enabled = true
            }
        } catch (_: Exception) {
        }
        mp.start()
    }

    private fun startVibration() {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = vib
        val pattern = longArrayOf(0, 600, 400)
        val amplitudes = intArrayOf(0, 255, 0)
        vib.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
    }

    /** Silence just the sound (keep vibrating) so the user can solve the math in peace. */
    private fun muteSound() {
        try { player?.setVolume(0f, 0f) } catch (_: Exception) {}
        try { enhancer?.enabled = false } catch (_: Exception) {}
    }

    /** Bring the sound back (e.g. safety re-blare if the user went quiet too long). */
    private fun unmuteSound() {
        try { player?.setVolume(1f, 1f) } catch (_: Exception) {}
        try { enhancer?.enabled = true } catch (_: Exception) {}
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "loudalarm:ring").apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L) // safety timeout
        }
    }

    /** Best-effort direct launch; the notification's full-screen intent is the reliable path.
     *  SINGLE_TOP (not CLEAR_TASK) so that when both this and the full-screen intent fire, the
     *  second one reuses the existing screen (onNewIntent) instead of recreating it — otherwise
     *  the challenge would regenerate and any "muted" state would be lost. */
    private fun launchAlarmScreen(alarmId: Int) {
        val i = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        try {
            startActivity(i)
        } catch (_: Exception) {
        }
    }

    private fun buildNotification(alarm: Alarm): Notification {
        createChannel()
        val fsIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
        }
        val fsPi = PendingIntent.getActivity(
            this, alarm.id, fsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 1000 + alarm.id,
            Intent(this, AlarmService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (alarm.label.isBlank()) getString(R.string.app_name) else alarm.label
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(getString(R.string.alarm_ringing))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fsPi, true)
            .setContentIntent(fsPi)
            .addAction(0, getString(R.string.turn_off), stopPi)
            .build()
    }

    private fun createChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                CHANNEL_ID, getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_desc)
                setSound(null, null)     // audio is played by the service, not the channel
                enableVibration(false)   // service handles vibration
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(ch)
        }
    }

    private fun stopEverything() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        player?.release(); player = null
        enhancer?.release(); enhancer = null
        vibrator?.cancel(); vibrator = null
        wakeLock?.let { if (it.isHeld) it.release() }; wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopEverything()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.tim.loudalarm.action.START"
        const val ACTION_STOP = "com.tim.loudalarm.action.STOP"
        const val ACTION_SNOOZE = "com.tim.loudalarm.action.SNOOZE"
        const val ACTION_MUTE = "com.tim.loudalarm.action.MUTE"
        const val ACTION_UNMUTE = "com.tim.loudalarm.action.UNMUTE"
        const val EXTRA_SNOOZE_MIN = "snoozeMinutes"
        /** Ceiling for the extra LoudnessEnhancer gain. High enough to clip/distort
         *  on purpose — the user picks a level their speaker can take. */
        const val MAX_BOOST_DB = 60
        private const val CHANNEL_ID = "alarm_ring"
        private const val NOTIF_ID = 42
    }
}
