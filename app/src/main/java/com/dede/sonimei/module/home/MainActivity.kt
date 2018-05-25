package com.dede.sonimei.module.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.arlib.floatingsearchview.FloatingSearchView
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion
import com.dede.sonimei.*
import com.dede.sonimei.base.BaseActivity
import com.dede.sonimei.component.CircularRevealDrawable
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.module.play.PlayFragment
import com.dede.sonimei.module.setting.SettingActivity
import com.dede.sonimei.util.extends.color
import com.dede.sonimei.util.extends.hide
import com.dede.sonimei.util.extends.notNull
import com.dede.sonimei.util.extends.show
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_bottom_sheet_play_control.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class MainActivity : BaseActivity(), FloatingSearchView.OnMenuItemClickListener {

    override fun onActionMenuItemSelected(item: MenuItem?) {
        when (item?.itemId) {
            R.id.menu_search -> {
            }
            R.id.menu_source -> {
                SourceSelectDialog(this, source)
                        .onItemSelect {
                            source = it.source
                            tv_source_name.text = sourceName(source)
                            val query = fsv_search_bar.query
                            if (query.notNull()) {
                                searchResultFragment.search(query, source)
                            }
                            drawable.play(it.color)
                        }
                        .show()
            }
            R.id.menu_setting -> {
                startActivity<SettingActivity>()
            }
            R.id.menu_about -> {
            }
            R.id.menu_github -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hushenghao/music")))
            }
            R.id.menu_webview -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://music.sonimei.cn/")))
            }
        }
    }

    override fun getLayoutId() = R.layout.activity_main

    private lateinit var behavior: BottomSheetBehavior<FrameLayout>
    private lateinit var drawable: CircularRevealDrawable
    private lateinit var searchResultFragment: SearchResultFragment
    private lateinit var playFragment: PlayFragment
    @MusicSource
    private var source: Int = NETEASE

    override fun initView(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 全透明状态栏
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        searchResultFragment = supportFragmentManager.findFragmentById(R.id.search_result_fragment) as SearchResultFragment
        playFragment = supportFragmentManager.findFragmentById(R.id.play_fragment) as PlayFragment

        drawable = CircularRevealDrawable(color(R.color.colorPrimary))
        app_bar.background = drawable
        app_bar.postDelayed({
            val color = sourceColor(source)
            drawable.play(color)
        }, 500)
        tv_source_name.text = sourceName(source)

        app_bar.addOnOffsetChangedListener { _, verticalOffset ->
            val topMargin = (fsv_search_bar.layoutParams as ViewGroup.MarginLayoutParams).topMargin
            val h = fsv_search_bar.height + topMargin
            val a = 1f - Math.abs(verticalOffset).toFloat() / h
            ll_source_bar.alpha = a
        }

        fsv_search_bar.setOnMenuItemClickListener(this)
        fsv_search_bar.setOnSearchListener(object : FloatingSearchView.OnSearchListener {
            override fun onSearchAction(currentQuery: String?) {
                searchResultFragment.search(currentQuery, source)
            }

            override fun onSuggestionClicked(searchSuggestion: SearchSuggestion?) {
            }
        })


        behavior = BottomSheetBehavior.from(bottom_sheet)
        behavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            private var isBlackBar = false
            // private val isM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            private val isM = false

            @SuppressLint("InlinedApi")
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset > 0.85f) {
                    if (isM) {
                        if (!isBlackBar) {
                            // 状态栏黑色字体
                            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            isBlackBar = true
                        }
                    }
                    fsv_search_bar.hide()
                    rl_bottom_play.hide()
                    bottom_sheet.open = true
                } else {
                    if (isM) {
                        if (isBlackBar) {
                            // 状态栏白色字体
                            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility xor
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            isBlackBar = false
                        }
                    }
                    fsv_search_bar.show()
                    rl_bottom_play.show()
                    bottom_sheet.open = false
                }

                var a = 1 - slideOffset * 1.4f
                if (a < 0f) a = 0f
                fsv_search_bar.alpha = a
                var b = 1 - slideOffset * 2f
                if (b < 0f) b = 0f
                rl_bottom_play.alpha = b
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
//                when (newState) {
//                    BottomSheetBehavior.STATE_COLLAPSED -> {
//                    }
//                    BottomSheetBehavior.STATE_EXPANDED -> {
//                    }
//                }
            }
        })
        rl_bottom_play.onClick {
            toggleBottomSheet()
        }
        // 隐藏mini play control
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
        behavior.peekHeight = 0
    }

    /**
     * 播放音乐
     */
    fun playSong(song: SearchSong) {
        // 显示 mini play control
        behavior.isHideable = false
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        val bottomDimens = resources.getDimensionPixelSize(R.dimen.search_list_bottom_margin)
        behavior.peekHeight = bottomDimens
        fl_search_result.setPadding(0, 0, 0, bottomDimens)

        playFragment.playSong(song)
    }

    private fun toggleBottomSheet() {
        if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private val rxPermissions by lazy { RxPermissions(this) }

    override fun onResume() {
        super.onResume()
        rxPermissions
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .filter { !it }
                .subscribe { toast("读取SD卡权限被拒绝") }
    }

    override fun onPause() {
        fsv_search_bar.clearSearchFocus()
        super.onPause()
    }

    override fun onBackPressed() {
        if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            toggleBottomSheet()
            return
        }
        super.onBackPressed()
    }
}
