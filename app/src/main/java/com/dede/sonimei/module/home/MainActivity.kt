package com.dede.sonimei.module.home

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
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
import com.dede.sonimei.util.extends.*
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_bottom_sheet_play_control.*
import org.jetbrains.anko.dip
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
                            val query = search_bar.query
                            if (query.notNull()) {
                                fragment.search(query, source)
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
    private lateinit var fragment: SearchResultFragment
    private lateinit var playFragment: PlayFragment
    @MusicSource
    private var source: Int = NETEASE

    fun getBg(): Bitmap {
        val inB = Bitmap.createBitmap(dip(50), dip(50), Bitmap.Config.ARGB_4444)
        val canvas = Canvas(inB)
        canvas.drawColor(0x99ffffff.toInt())

        val bitmap = Bitmap.createBitmap(dip(50), dip(50), Bitmap.Config.ARGB_4444)

        val script = RenderScript.create(this)
        val input = Allocation.createFromBitmap(script, inB, Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT)
        val output = Allocation.createTyped(script, input.type)
        val blur = ScriptIntrinsicBlur.create(script, Element.U8_4(script))
        blur.setInput(input)
        blur.setRadius(3f)
        blur.forEach(output)
        output.copyTo(bitmap)
        script.destroy()
        blur.destroy()
        return bitmap
    }

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

        fragment = supportFragmentManager.findFragmentById(R.id.search_list) as SearchResultFragment
        playFragment = supportFragmentManager.findFragmentById(R.id.play_fragment) as PlayFragment

        drawable = CircularRevealDrawable(color(R.color.colorPrimary))
        app_bar.background = drawable
        app_bar.postDelayed({
            val color = sourceColor(source)
            drawable.play(color)
        }, 200)
        tv_source_name.text = sourceName(source)
//        bottom_sheet.background = BitmapDrawable(getBg())

        app_bar.addOnOffsetChangedListener { _, verticalOffset ->
            val topMargin = (search_bar.layoutParams as ViewGroup.MarginLayoutParams).topMargin
            val h = search_bar.height + topMargin
            val a = 1f - Math.abs(verticalOffset).toFloat() / h
            ll_bottom_bar.alpha = a
        }

        search_bar.setOnMenuItemClickListener(this)
        search_bar.setOnSearchListener(object : FloatingSearchView.OnSearchListener {
            override fun onSearchAction(currentQuery: String?) {
                fragment.search(currentQuery, source)
            }

            override fun onSuggestionClicked(searchSuggestion: SearchSuggestion?) {
            }
        })


        behavior = BottomSheetBehavior.from(bottom_sheet)
        behavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            var isBlackBar = false
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

                if (slideOffset > 0.85f) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!isBlackBar) {
                            // 状态栏黑色字体
                            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            isBlackBar = true
                        }
                    }
                    search_bar.hide()
                    rl_bottom_play.hide()
                    bottom_sheet.open = true
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (isBlackBar) {
                            // 状态栏白色字体
                            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility xor
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            isBlackBar = false
                        }
                    }
                    search_bar.show()
                    rl_bottom_play.show()
                    bottom_sheet.open = false
                }

                var a = 1 - slideOffset * 1.4f
                if (a < 0f) a = 0f
                search_bar.alpha = a
                rl_bottom_play.alpha = a
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        if (!arrowIsTop) {
                            arrow2up()
                        }
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        if (arrowIsTop) {
                            arrow2Bottom()
                        }
                    }
                }
            }
        })
        float_action_bt.onClick {
            toggleBottomSheet()
        }
        // 隐藏mini play control
//        float_action_bt.gone()
//        behavior.state = BottomSheetBehavior.STATE_HIDDEN
//        behavior.peekHeight = 0
    }

    fun playSong(song: SearchSong) {
//        behavior.isHideable = false
//        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        playFragment.playSong(song)
    }

    private var arrowIsTop = true

    private fun toggleBottomSheet() {
        if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            arrow2Bottom()
        } else {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            arrow2up()
        }
    }

    private fun arrow2up() {
        arrowIsTop = true
        ObjectAnimator.ofFloat(float_action_bt, "rotation", -180f, 0f)
                .setDuration(200)
                .start()
    }

    private fun arrow2Bottom() {
        arrowIsTop = false
        ObjectAnimator.ofFloat(float_action_bt, "rotation", 0f, 180f)
                .setDuration(200)
                .start()
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

    override fun onBackPressed() {
        if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            toggleBottomSheet()
            return
        }
        super.onBackPressed()
    }
}
