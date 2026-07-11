package com.tim.loudalarm

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/** Two top tabs: alarms (page 0) and the sleep calculator (page 1). */
class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2
    override fun createFragment(position: Int): Fragment =
        if (position == 0) AlarmsFragment() else SleepFragment()
}
