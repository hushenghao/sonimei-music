package com.dede.sonimei.module.home

import android.animation.ValueAnimator
import android.content.Context
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
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import com.dede.sonimei.*
import com.dede.sonimei.base.BaseActivity
import com.dede.sonimei.component.CaretDrawable
import com.dede.sonimei.component.CircularRevealDrawable
import com.dede.sonimei.component.PlayBottomSheetBehavior
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.module.db.DatabaseOpenHelper.Companion.COLUMNS_TEXT
import com.dede.sonimei.module.play.PlayFragment
import com.dede.sonimei.module.searchresult.SearchResultFragment
import com.dede.sonimei.module.setting.SettingActivity
import com.dede.sonimei.module.setting.Settings
import com.dede.sonimei.util.extends.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_bottom_sheet_play_control.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class MainActivity : BaseActivity(), SearchView.OnQueryTextListener {

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchView.clearFocus()
        searchResultFragment.search(query)
        // 保存搜索历史
        searchView.suggestionsAdapter?.to<SearchHisAdapter>()?.newSearchHis(query)
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
        searchResultFragment.setTypeSource(source to searchResultFragment.getTypeSource().second)
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

        playBehavior = BottomSheetBehavior.from(bottom_sheet).to()
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
                val v = Math.max(-1f, Math.min(vy * .0023f, 1f))
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
    fun playSong(song: SearchSong?) {
        if (song == null || song.url.isNull()) {
            toast("播放链接为空")
            return
        }
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

    private lateinit var searchView: SearchView

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        searchView = menu!!.findItem(R.id.menu_search).actionView as SearchView
        val searchType = searchType(SEARCH_NAME)
        tv_search_type.text = searchType
        searchView.queryHint = searchType
        searchView.setOnQueryTextListener(this)

        // 内部是继承于AutoCompleteTextView的SearchAutoComplete
        val autoCompleteTextView = searchView
                .findViewById<AutoCompleteTextView>(R.id.search_src_text)
        if (autoCompleteTextView != null) {
            autoCompleteTextView.setTextColor(Color.WHITE)
            autoCompleteTextView.setHintTextColor(Color.argb(180, 255, 255, 255))
            // 因为SearchView&SearchAutoComplete重写了enoughToFilter方法，小于0时返回true直接显示popList
            // 但是会和键盘同时弹起，使pop显示位置不准确，这里不使用返回小于0的数是搜索历史自动显示

            // 监听焦点变化，延迟弹出搜索历史pop
            autoCompleteTextView.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    autoCompleteTextView.threshold = 1
                } else {
                    // 延时显示，等待键盘弹起
                    v.postDelayed({
                        autoCompleteTextView.showDropDown()
                        autoCompleteTextView.threshold = -1
                    }, 250)
                }
            }
            autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                val p = position - SearchHisAdapter.HEADER_COUNT// 去掉header的个数
                if (p < 0) return@setOnItemClickListener
                val cursor = searchView.suggestionsAdapter.cursor
                if (p >= cursor.count) return@setOnItemClickListener

                cursor.moveToPosition(p)
                val text = cursor.getString(cursor.getColumnIndex(COLUMNS_TEXT))
                // SearchView&SearchAutoComplete重写replaceText(text)方法为空方法，需要手动设置文本
                searchView.setQuery(text, true)
            }
            searchView.suggestionsAdapter = SearchHisAdapter(this@MainActivity)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_source_type -> {
                val dialog = SourceTypeDialog(this, searchResultFragment.getTypeSource())
                        .callback {
                            val source = it.first
                            tv_source_name.text = sourceName(source)
                            val searchType = searchType(it.second)
                            searchView.queryHint = searchType
                            tv_search_type.text = searchType
                            val query = searchView.query?.toString()
                            if (query.notNull()) {
                                searchResultFragment.search(query, it)
                            } else {
                                searchResultFragment.setTypeSource(it)
                            }
                            drawable.play(sourceColor(source))
                        }

                if (searchView.hasFocus()) {
                    val manager = getSystemService(Context.INPUT_METHOD_SERVICE).to<InputMethodManager>()
                    manager.hideSoftInputFromWindow(window.decorView.windowToken, 0)
                    searchView.postDelayed({ dialog.show() }, 200)
                } else {
                    dialog.show()
                }
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
                AboutDialog(this).show()
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
        searchView.suggestionsAdapter.cursor?.close()
        super.onDestroy()
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
