package com.dede.sonimei.module.home

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseFragment
import com.dede.sonimei.module.local.LocalMusicFragment
import com.dede.sonimei.module.search.SearchFragment

/**
 * 首页Fragment，只是一个空壳
 */
class HomeFragment : BaseFragment() {

//    override fun getLayoutId() = R.layout.fragment_home_layout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        val fragment = childFragmentManager.findFragmentByTag("local_music")
        if (savedInstanceState == null) {
            val localMusicFragment = fragment as? LocalMusicFragment ?: LocalMusicFragment()
            childFragmentManager.beginTransaction()
                    .replace(R.id.home_fragment, localMusicFragment, "local_music")
                    .disallowAddToBackStack()// 不添加到返回栈
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (super.onOptionsItemSelected(item)) return true
        return when (item?.itemId) {
            R.id.menu_to_search -> {
                val searchFragment = SearchFragment()
                childFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.anim_fade_in, R.anim.anim_fade_out,
                                R.anim.anim_fade_in, R.anim.anim_fade_out)
                        .replace(R.id.home_fragment, searchFragment, "search_fragment")
                        .addToBackStack(null)
                        .commit()
                true
            }
            else -> false
        }
    }

}