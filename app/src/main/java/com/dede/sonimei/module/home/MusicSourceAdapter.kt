package com.dede.sonimei.module.home

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.dede.sonimei.sourceArray
import com.dede.sonimei.sourceName

/**
 * Created by hsh on 2018/5/15.
 */
class MusicSourceAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return SearchResultFragment.newInstance(sourceArray[position])
    }

    override fun getCount() = sourceArray.size

    override fun getPageTitle(position: Int): CharSequence? {
        return sourceName(sourceArray[position])
    }
}