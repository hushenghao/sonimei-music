package com.dede.sonimei.module.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.dede.sonimei.*
import com.dede.sonimei.base.BaseActivity
import com.dede.sonimei.component.CircularRevealDrawable
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.module.play.PlayFragment
import com.dede.sonimei.module.setting.SettingActivity
import com.dede.sonimei.module.setting.Settings
import com.dede.sonimei.util.extends.color
import com.dede.sonimei.util.extends.hide
import com.dede.sonimei.util.extends.notNull
import com.dede.sonimei.util.extends.show
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_bottom_sheet_play_control.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class MainActivity : BaseActivity(), SearchView.OnQueryTextListener {

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchView?.clearFocus()
        searchResultFragment.search(query)
        return false// 关闭键盘
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return true
    }

    override fun getLayoutId() = R.layout.activity_main

    private lateinit var behavior: BottomSheetBehavior<FrameLayout>
    private lateinit var drawable: CircularRevealDrawable
    private lateinit var searchResultFragment: SearchResultFragment
    private lateinit var playFragment: PlayFragment

    override fun initView(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 全透明状态栏
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        setSupportActionBar(tool_bar)

        val source = defaultSharedPreferences.getInt(Settings.KEY_DEFAULT_SEARCH_SOURCE, NETEASE)

        searchResultFragment = supportFragmentManager.findFragmentById(R.id.search_result_fragment) as SearchResultFragment
        playFragment = supportFragmentManager.findFragmentById(R.id.play_fragment) as PlayFragment

        drawable = CircularRevealDrawable(color(R.color.colorPrimary))
        app_bar.background = drawable
        app_bar.postDelayed({
            val color = sourceColor(source)
            drawable.play(color)
        }, 500)
        tv_source_name.text = sourceName(source)

        behavior = BottomSheetBehavior.from(bottom_sheet)
        behavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            //            private var isBlackBar = false
//            private val isM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            @SuppressLint("InlinedApi")
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset > 0.85f) {
//                    if (isM) {
//                        if (!isBlackBar) {
//                            // 状态栏黑色字体
//                            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
//                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
//                            isBlackBar = true
//                        }
//                    }
                    rl_bottom_play.hide()
                    bottom_sheet.open = true
                } else {
//                    if (isM) {
//                        if (isBlackBar) {
//                            // 状态栏白色字体
//                            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility xor
//                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
//                            isBlackBar = false
//                        }
//                    }
                    rl_bottom_play.show()
                    bottom_sheet.open = false
                }

                var b = 1 - slideOffset * 2f
                if (b < 0f) b = 0f
                rl_bottom_play.alpha = b
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
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

    fun toggleBottomSheet() {
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

    private var searchView: SearchView? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        searchView = menu?.findItem(R.id.menu_search)?.actionView as SearchView?
        val searchType = searchType(SEARCH_NAME)
        tv_search_type.text = searchType
        searchView?.queryHint = searchType
        searchView?.setOnQueryTextListener(this)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_source_type -> {
                SourceTypeDialog(this, searchResultFragment.getTypeSource())
                        .callback {
                            val source = it.first
                            tv_source_name.text = sourceName(source)
                            val searchType = searchType(it.second)
                            searchView?.queryHint = searchType
                            tv_search_type.text = searchType
                            val query = searchView?.query?.toString()
                            if (query.notNull()) {
                                searchResultFragment.search(query, it)
                            } else {
                                searchResultFragment.setTypeSource(it)
                            }
                            drawable.play(sourceColor(source))
                        }
                        .show()
                true
            }
            R.id.menu_setting -> {
                startActivity<SettingActivity>()
                true
            }
            R.id.menu_ape -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(APE_LINK)))
                true
            }
            R.id.menu_about -> {
                false
            }
            R.id.menu_github -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_LINK)))
                true
            }
            R.id.menu_webview -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WEB_LINK)))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private var lastTime = 0L

    override fun onBackPressed() {
        if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            toggleBottomSheet()
            return
        }
        val millis = System.currentTimeMillis()
        if (lastTime + 2000L < millis) {
            toast("再按一次退出")
            lastTime = millis
        } else {
            super.onBackPressed()
        }
    }
}
