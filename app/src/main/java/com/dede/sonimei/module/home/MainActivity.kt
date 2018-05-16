package com.dede.sonimei.module.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseActivity
import com.dede.sonimei.sourceKey
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity(), SearchView.OnQueryTextListener {

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchText = query
        val fragment = SearchResultFragment.newInstance(sourceKey[view_pager.currentItem])
        fragment.search(query)
        return false
    }

    override fun onQueryTextChange(newText: String?) :Boolean {
        searchText = newText
        return false
    }

    override fun getLayoutId() = R.layout.activity_main

    override fun initView(savedInstanceState: Bundle?) {
        supportActionBar?.elevation = 0f
        view_pager.adapter = MusicSourceAdapter(supportFragmentManager)
        tab_layout.setupWithViewPager(view_pager)
    }

    private var searchView: SearchView? = null

    var searchText: String? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        searchView = menu?.findItem(R.id.menu_search)?.actionView as SearchView? ?: return true
        searchView?.isIconified = false
        searchView?.setOnQueryTextListener(this)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_about -> {
                true
            }
            R.id.menu_webview -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://music.sonimei.cn/")))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
