package com.dede.sonimei.module.home

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.dede.sonimei.module.setting.KEY_DISABLE_SOURCE_MAP
import com.dede.sonimei.sourceList
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by hsh on 2018/5/15.
 */
class MusicSourceAdapter(val context: Context, fm: FragmentManager) : FragmentPagerAdapter(fm) {

    private var enableEntry: List<Triple<Int, String, String>>

    init {
        val disableSet = context.defaultSharedPreferences
                .getStringSet(KEY_DISABLE_SOURCE_MAP, emptySet()).toMutableSet()
        enableEntry = sourceList.filter { !disableSet.contains(it.third) }
    }

    override fun getItem(position: Int): Fragment {
        val triple = enableEntry[position]
        return SearchResultFragment.newInstance(triple.first)
    }

    override fun getCount() = enableEntry.size

    override fun getPageTitle(position: Int): CharSequence? {
        return enableEntry[position].second
    }

    override fun notifyDataSetChanged() {
        val disableSet = context.defaultSharedPreferences
                .getStringSet(KEY_DISABLE_SOURCE_MAP, emptySet()).toMutableSet()
        enableEntry = sourceList.filter { !disableSet.contains(it.third) }
        super.notifyDataSetChanged()
    }
}