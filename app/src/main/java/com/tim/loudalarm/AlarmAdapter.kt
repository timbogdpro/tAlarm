package com.tim.loudalarm

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tim.loudalarm.databinding.ItemAlarmBinding
import java.util.Calendar

class AlarmAdapter(
    private var items: List<Alarm>,
    private val onToggle: (Alarm, Boolean) -> Unit,
    private val onClick: (Alarm) -> Unit,
    private val onDelete: (Alarm) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.VH>() {

    inner class VH(val b: ItemAlarmBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(newItems: List<Alarm>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val a = items[position]
        val b = holder.b
        b.time.text = String.format("%02d:%02d", a.hour, a.minute)
        b.subtitle.text = subtitle(b.root.context, a)
        b.enabledSwitch.setOnCheckedChangeListener(null)
        b.enabledSwitch.isChecked = a.enabled
        b.enabledSwitch.setOnCheckedChangeListener { _, checked -> onToggle(a, checked) }
        b.root.setOnClickListener { onClick(a) }
        b.deleteBtn.setOnClickListener { onDelete(a) }
    }

    private fun subtitle(ctx: Context, a: Alarm): String {
        val label = if (a.label.isBlank()) "" else "${a.label} · "
        val sound = ctx.getString(Sounds.byId(a.soundId).titleRes)
        val dismiss = ctx.getString(a.dismissMode.titleRes)
        return "$label${daysText(ctx, a)} · $sound · $dismiss"
    }

    private fun daysText(ctx: Context, a: Alarm): String {
        if (a.days.isEmpty()) return ctx.getString(R.string.once)
        if (a.days.size == 7) return ctx.getString(R.string.everyday)
        val order = listOf(
            Calendar.MONDAY to R.string.day_mon, Calendar.TUESDAY to R.string.day_tue,
            Calendar.WEDNESDAY to R.string.day_wed, Calendar.THURSDAY to R.string.day_thu,
            Calendar.FRIDAY to R.string.day_fri, Calendar.SATURDAY to R.string.day_sat,
            Calendar.SUNDAY to R.string.day_sun
        )
        return order.filter { a.days.contains(it.first) }.joinToString(" ") { ctx.getString(it.second) }
    }
}
