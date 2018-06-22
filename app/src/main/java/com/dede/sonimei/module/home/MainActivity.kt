package com.dede.sonimei.module.home

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.widget.SimpleCursorAdapter
import android.support.v7.app.AlertDialog
import android.support.v7.widget.SearchView
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.AutoCompleteTextView
import android.widget.CursorAdapter
import android.widget.FrameLayout
import android.widget.TextView
import com.dede.sonimei.*
import com.dede.sonimei.base.BaseActivity
import com.dede.sonimei.component.CaretDrawable
import com.dede.sonimei.component.CircularRevealDrawable
import com.dede.sonimei.component.LinkTagClickableSpan
import com.dede.sonimei.component.PlayBottomSheetBehavior
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.module.db.DatabaseOpenHelper.Companion.COLUMNS_TEXT
import com.dede.sonimei.module.db.DatabaseOpenHelper.Companion.COLUMNS_TIMESTAMP
import com.dede.sonimei.module.db.DatabaseOpenHelper.Companion.TABLE_SEARCH_HIS
import com.dede.sonimei.module.db.db
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
import org.jetbrains.anko.*
import org.jetbrains.anko.db.replace
import org.jetbrains.anko.sdk25.coroutines.onClick

class MainActivity : BaseActivity(), SearchView.OnQueryTextListener {

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchView?.clearFocus()
//        searchResultFragment.search(query)
        if (query.notNull() && searchHisCursor != null) {
            doAsync {
                db.replace(TABLE_SEARCH_HIS,
                        COLUMNS_TEXT to query)
                searchHisCursor = db.query(TABLE_SEARCH_HIS, null, null,
                        null, null, null, "$COLUMNS_TIMESTAMP DESC")
                uiThread {
                    searchView?.suggestionsAdapter?.swapCursor(searchHisCursor)?.close()
                }
            }
        }
        return false// 关闭键盘
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return true
    }

    override fun getLayoutId() = R.layout.activity_main

    private lateinit var playBehavior: PlayBottomSheetBehavior<FrameLayout>
    private lateinit var drawable: CircularRevealDrawable
    private lateinit var searchResultFragment: SearchResultFragment
    private lateinit var playFragment: PlayFragment

    private val arrowAnim by lazy {
        val anim = ValueAnimator
                .ofFloat()
                .setDuration(250L)
        anim.interpolator = LinearInterpolator()
        return@lazy anim
    }

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
        }, 500L)
        tv_source_name.text = sourceName(source)

        val caretDrawable = CaretDrawable(this)
        caretDrawable.caretProgress = CaretDrawable.PROGRESS_CARET_POINTING_UP
        iv_arrow_indicators.setImageDrawable(caretDrawable)

        playBehavior = BottomSheetBehavior.from(bottom_sheet) as PlayBottomSheetBehavior<FrameLayout>
        playBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            var lastOffset = 0f
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (playBehavior.state == BottomSheetBehavior.STATE_SETTLING) {
                    if (lastOffset > slideOffset) {
                        caretDrawable.caretProgress = CaretDrawable.PROGRESS_CARET_POINTING_DOWN
                    } else {
                        caretDrawable.caretProgress = CaretDrawable.PROGRESS_CARET_POINTING_UP
                    }
                }
                lastOffset = slideOffset

                bottom_sheet.open = if (slideOffset > 0.85f) {
                    fl_bottom_play.hide()
                    true
                } else {
                    fl_bottom_play.show()
                    false
                }

                var b = 1 - slideOffset * 2f
                if (b < 0f) b = 0f
                fl_bottom_play.alpha = b
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        tool_bar.collapseActionView()// 关闭搜索框
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        if (arrowAnim.isRunning) {
                            arrowAnim.cancel()
                        }
                        arrowAnim.setFloatValues(caretDrawable.caretProgress, CaretDrawable.PROGRESS_CARET_POINTING_DOWN)
                        arrowAnim.addUpdateListener { animation ->
                            caretDrawable.caretProgress = animation.animatedValue as Float
                        }
                        arrowAnim.start()
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        if (arrowAnim.isRunning) {
                            arrowAnim.cancel()
                        }
                        arrowAnim.setFloatValues(caretDrawable.caretProgress, CaretDrawable.PROGRESS_CARET_POINTING_UP)
                        arrowAnim.addUpdateListener { animation ->
                            caretDrawable.caretProgress = animation.animatedValue as Float
                        }
                        arrowAnim.start()
                    }
                }
            }
        })
        playBehavior.onYVelocityChangeListener = object : PlayBottomSheetBehavior.OnYVelocityChangeListener {
            override fun onChange(vy: Float) {
                val state = playBehavior.state
                if (state == BottomSheetBehavior.STATE_EXPANDED ||
                        state == BottomSheetBehavior.STATE_COLLAPSED) {
                    return
                }
                val v = Math.max(-1f, Math.min(vy * .0025f, 1f))
                caretDrawable.caretProgress = v
            }
        }

        fl_bottom_play.onClick {
            toggleBottomSheet()
        }
        // 隐藏mini play control
        playBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        playBehavior.peekHeight = 0
    }

    /**
     * 播放音乐
     */
    fun playSong(song: SearchSong) {
        // 显示 mini play control
        playBehavior.isHideable = false
        playBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        iv_arrow_indicators.show()
        val bottomDimens = resources.getDimensionPixelSize(R.dimen.search_list_bottom_margin)
        playBehavior.peekHeight = bottomDimens
        fl_search_result.setPadding(0, 0, 0, bottomDimens)

        playFragment.playSong(song)
    }

    fun toggleBottomSheet() {
        if (playBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            playBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            playBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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

    private var searchHisCursor: Cursor? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        searchView = menu?.findItem(R.id.menu_search)?.actionView as SearchView?
        val searchType = searchType(SEARCH_NAME)
        tv_search_type.text = searchType
        searchView?.queryHint = searchType
        searchView?.setOnQueryTextListener(this)

        // 内部是继承于AutoCompleteTextView的SearchAutoComplete
        val autoCompleteTextView = searchView
                ?.findViewById<AutoCompleteTextView>(R.id.search_src_text)
        if (autoCompleteTextView != null) {
            // 设置一个小于0的值，默认显示筛选列表
            // 因为SearchView&SearchAutoComplete重写了enoughToFilter方法，小于0时返回true显示popList
            autoCompleteTextView.threshold = -1
            autoCompleteTextView.setDropDownBackgroundResource(R.drawable.abc_popup_background)

            doAsync {
                searchHisCursor = db.query(TABLE_SEARCH_HIS, null, null,
                        null, null, null, "$COLUMNS_TIMESTAMP DESC")
                uiThread {
                    searchView?.suggestionsAdapter = SimpleCursorAdapter(
                            this@MainActivity,
                            R.layout.item_search_his,
                            searchHisCursor,
                            arrayOf(COLUMNS_TEXT),
                            intArrayOf(R.id.tv_query),
                            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
                    )
                }
            }
        }
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
                val view = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)
                val tvVersion = view.findViewById<TextView>(R.id.tv_version)
                try {
                    val packageInfo = packageManager.getPackageInfo(packageName, 0)
                    tvVersion.text = String.format(tvVersion.text.toString(),
                            packageInfo.versionName,
                            packageInfo.versionCode)
                } catch (e: PackageManager.NameNotFoundException) {
                }

                val tvGithub = view.findViewById<TextView>(R.id.tv_github)
                tvGithub.tag = GITHUB_LINK
                val github = SpannableString(tvGithub.text)
                github.setSpan(LinkTagClickableSpan(), 7, github.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                tvGithub.movementMethod = LinkMovementMethod.getInstance()
                tvGithub.text = github

                val tvQQGroup = view.findViewById<TextView>(R.id.tv_group)
                tvQQGroup.tag = GROUP_LINK
                val qqGroup = SpannableString(tvQQGroup.text)
                qqGroup.setSpan(LinkTagClickableSpan(false), 6, qqGroup.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                tvQQGroup.movementMethod = LinkMovementMethod.getInstance()
                tvQQGroup.text = qqGroup

                AlertDialog.Builder(this)
                        .setView(view)
                        .setNegativeButton("确定", null)
                        .setNeutralButton(R.string.about_market) { _, _ ->
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=$packageName")))
                            } catch (e: ClassNotFoundException) {
                            }
                        }
                        .create()
                        .show()
                true
            }
            R.id.menu_webview -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WEB_LINK)))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        searchHisCursor?.close()
    }

    private var lastTime = 0L

    override fun onBackPressed() {
        if (playBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
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
