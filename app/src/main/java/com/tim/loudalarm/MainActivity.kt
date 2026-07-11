package com.tim.loudalarm

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.tim.loudalarm.databinding.ActivityMainBinding

/**
 * Host for the two tabs (alarms + sleep calculator). Owns the toolbar menu and the
 * first-run permission wizard; the alarm list itself lives in [AlarmsFragment].
 */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var store: AlarmStore

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            alarmsFragment()?.refresh()
            if (onboardingActive) onboardingNext()
        }
    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (onboardingActive) onboardingNext()
        }
    private var onboardingActive = false
    private var onboardingStep = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyColorScheme()
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        store = AlarmStore(this)

        b.pager.adapter = MainPagerAdapter(this)
        b.pager.offscreenPageLimit = 1
        TabLayoutMediator(b.tabs, b.pager) { tab, pos ->
            tab.setText(if (pos == 0) R.string.tab_alarms else R.string.tab_sleep)
        }.attach()
        b.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == 0) b.fab.show() else b.fab.hide()
            }
        })

        b.toolbar.inflateMenu(R.menu.main_menu)
        b.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_test -> { testRing(); true }
                R.id.action_scheme -> { showSchemeDialog(); true }
                R.id.action_donate -> { showDonateDialog(); true }
                else -> false
            }
        }

        b.fab.setOnClickListener { startActivity(Intent(this, EditAlarmActivity::class.java)) }

        if (!isOnboarded()) showOnboardingIntro()
    }

    override fun onResume() {
        super.onResume()
        // Re-arm everything with the best available method each time we're opened.
        // Self-heals if a schedule was lost and upgrades inexact -> exact once the
        // "Alarms & reminders" permission is granted.
        AlarmScheduler.rescheduleAll(this)
        if (isOnboarded()) requestNotifPermIfNeeded()
    }

    private fun alarmsFragment(): AlarmsFragment? =
        supportFragmentManager.findFragmentByTag("f0") as? AlarmsFragment

    /** Ring right now (using the first alarm) so the user can verify loudness and
     *  grant any permission prompts, without waiting for a scheduled time. */
    private fun testRing() {
        val alarm = store.getAll().firstOrNull()
        if (alarm == null) {
            Toast.makeText(this, R.string.test_need_alarm, Toast.LENGTH_SHORT).show()
            return
        }
        val i = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
        }
        ContextCompat.startForegroundService(this, i)
    }

    private fun showSchemeDialog() {
        val schemes = ColorScheme.entries.toTypedArray()
        val titles = schemes.map { getString(it.titleRes) }.toTypedArray()
        val current = ColorScheme.current(this)
        var chosen = schemes.indexOf(current)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.color_scheme)
            .setSingleChoiceItems(titles, chosen) { _, which -> chosen = which }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (schemes[chosen] != current) {
                    ColorScheme.save(this, schemes[chosen])
                    recreate()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** The app is free; offer (never nag) to support the Podari Zhizn charity. */
    private fun showDonateDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.donate_title)
            .setMessage(R.string.donate_body)
            .setNegativeButton(R.string.donate_close, null)
            .setPositiveButton(R.string.donate_open) { _, _ ->
                try {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://podari-zhizn.ru/ru/give-help"))
                    )
                } catch (_: Exception) {
                }
            }
            .show()
    }

    // --- permission / reliability nudges ---

    private fun requestNotifPermIfNeeded() {
        if (canAskNotifPermission()) notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // --- first-run onboarding: walk the user through every permission ---

    private fun prefs() = getSharedPreferences("loud_alarm_prefs", MODE_PRIVATE)
    private fun isOnboarded() = prefs().getBoolean("onboarded", false)

    private fun showOnboardingIntro() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.onboarding_title)
            .setMessage(R.string.onboarding_body)
            .setCancelable(false)
            .setPositiveButton(R.string.onboarding_go) { _, _ -> startOnboarding() }
            .setNegativeButton(R.string.onboarding_later) { _, _ ->
                prefs().edit().putBoolean("onboarded", true).apply()
            }
            .show()
    }

    private fun startOnboarding() {
        onboardingActive = true
        onboardingStep = 0
        onboardingNext()
    }

    /** Fire each needed system prompt in turn; every result advances to the next. */
    private fun onboardingNext() {
        if (!onboardingActive) return
        when (onboardingStep++) {
            0 -> if (needsNotif()) {
                if (canAskNotifPermission()) notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else launchSettingsIntent(notifSettingsIntent())
            } else onboardingNext()

            1 -> if (needsExactAlarm() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                launchSetting(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            } else onboardingNext()

            2 -> if (needsFullScreen() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                launchSetting(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            } else onboardingNext()

            3 -> if (needsBatteryOpt()) {
                launchSetting(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            } else onboardingNext()

            else -> {
                onboardingActive = false
                prefs().edit().putBoolean("onboarded", true).apply()
                alarmsFragment()?.refresh()
            }
        }
    }

    private fun launchSetting(action: String) = launchSettingsIntent(appSettingsIntent(action))

    private fun launchSettingsIntent(intent: Intent) {
        try {
            settingsLauncher.launch(intent)
        } catch (_: Exception) {
            onboardingNext()
        }
    }
}
