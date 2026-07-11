package com.tim.loudalarm

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.tim.loudalarm.databinding.FragmentAlarmsBinding

/** The alarms tab: list of alarms, the empty state, and the permission banner.
 *  The alarm engine (scheduler/service/store) is untouched — this only hosts the UI. */
class AlarmsFragment : Fragment() {

    private var _b: FragmentAlarmsBinding? = null
    private val b get() = _b!!
    private lateinit var store: AlarmStore
    private lateinit var adapter: AlarmAdapter

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentAlarmsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = AlarmStore(requireContext())
        adapter = AlarmAdapter(
            emptyList(),
            onToggle = { alarm, checked -> toggle(alarm, checked) },
            onClick = { alarm -> openEditor(alarm.id) },
            onDelete = { alarm -> deleteAlarm(alarm) }
        )
        b.list.layoutManager = LinearLayoutManager(requireContext())
        b.list.adapter = adapter
        b.permButton.setOnClickListener { handlePermissionClick() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    /** Called by the host after a permission result too. */
    fun refresh() {
        if (_b == null) return
        val alarms = store.getAll()
        adapter.submit(alarms)
        b.empty.visibility = if (alarms.isEmpty()) View.VISIBLE else View.GONE
        updatePermBanner()
    }

    private fun toggle(alarm: Alarm, checked: Boolean) {
        val updated = alarm.copy(enabled = checked)
        store.put(updated)
        if (checked) AlarmScheduler.schedule(requireContext(), updated)
        else AlarmScheduler.cancel(requireContext(), updated.id)
        refresh()
    }

    private fun deleteAlarm(alarm: Alarm) {
        AlarmScheduler.cancel(requireContext(), alarm.id)
        store.delete(alarm.id)
        refresh()
    }

    private fun openEditor(id: Int) {
        startActivity(
            Intent(requireContext(), EditAlarmActivity::class.java)
                .putExtra(EditAlarmActivity.EXTRA_ID, id)
        )
    }

    private fun updatePermBanner() {
        val ctx = requireContext()
        val msg = when {
            ctx.needsExactAlarm() -> getString(R.string.perm_exact)
            ctx.needsNotif() -> getString(R.string.perm_notif)
            ctx.needsFullScreen() -> getString(R.string.perm_fullscreen)
            else -> null
        }
        if (msg == null) {
            b.permCard.visibility = View.GONE
        } else {
            b.permCard.visibility = View.VISIBLE
            b.permText.text = msg
        }
    }

    private fun handlePermissionClick() {
        val ctx = requireContext()
        when {
            ctx.needsExactAlarm() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                startActivity(ctx.appSettingsIntent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))

            ctx.needsNotif() ->
                if (ctx.canAskNotifPermission()) notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else startActivity(ctx.notifSettingsIntent())

            ctx.needsFullScreen() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                startActivity(ctx.appSettingsIntent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
