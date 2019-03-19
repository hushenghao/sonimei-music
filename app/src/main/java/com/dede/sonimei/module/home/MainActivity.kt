package com.dede.sonimei.module.home

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.*
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import com.dede.sonimei.APE_LINK
import com.dede.sonimei.R
import com.dede.sonimei.WEB_LINK
import com.dede.sonimei.base.BaseActivity
import com.dede.sonimei.component.CaretDrawable
import com.dede.sonimei.component.PlayBottomSheetBehavior
import com.dede.sonimei.data.BaseSong
import com.dede.sonimei.data.local.LocalSong
import com.dede.sonimei.module.changelog.ChangeLogActivity
import com.dede.sonimei.module.play.MusicBinder
import com.dede.sonimei.module.play.MusicService
import com.dede.sonimei.module.play.PlayFragment
import com.dede.sonimei.module.setting.SettingActivity
import com.dede.sonimei.util.extends.gone
import com.dede.sonimei.util.extends.hide
import com.dede.sonimei.util.extends.show
import com.dede.sonimei.util.extends.to
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_play.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast


/**
 * 应用主页面
 */
class MainActivity : BaseActivity() {

    override fun getLayoutId() = R.layout.activity_main

    private lateinit var playBehavior: PlayBottomSheetBehavior<FrameLayout>
    private lateinit var playFragment: PlayFragment

    private val arrowAnim by lazy {
        val anim = ValueAnimator
                .ofFloat()
                .setDuration(250L)
        anim.interpolator = LinearInterpolator()
        return@lazy anim
    }

    override fun initView(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 允许刘海区域绘制
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 全透明状态栏
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        parseIntent(intent)

        val homeFragmentContent = findViewById<View>(R.id.home_fragment)
        val homeFragment = supportFragmentManager.findFragmentById(R.id.home_fragment) as HomeFragment
        supportFragmentManager.beginTransaction()
                .setPrimaryNavigationFragment(homeFragment)// 使子Fragment接收到返回事件
//                .disallowAddToBackStack()
                .commit()
        playFragment = supportFragmentManager.findFragmentById(R.id.play_fragment) as PlayFragment

        val caretDrawable = CaretDrawable(this)
        caretDrawable.caretProgress = CaretDrawable.PROGRESS_CARET_POINTING_UP
        iv_arrow_indicators.setImageDrawable(caretDrawable)

        playBehavior = BottomSheetBehavior.from(bottom_sheet).to()
        playBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            var lastOffset = 0f
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                playFragment.onBottomSheetSlideOffsetChange(slideOffset)
                if (playBehavior.state == BottomSheetBehavior.STATE_SETTLING) {
                    if (lastOffset > slideOffset) {
                        caretDrawable.caretProgress = CaretDrawable.PROGRESS_CARET_POINTING_DOWN
                    } else {
                        caretDrawable.caretProgress = CaretDrawable.PROGRESS_CARET_POINTING_UP
                    }
                }
                lastOffset = slideOffset

                bottom_sheet.open = slideOffset > 0.85f

                // 隐藏fragment 优化过度绘制
                if (slideOffset >= 1f) {
                    homeFragmentContent.hide()
                } else {
                    homeFragmentContent.show()
                }
            }

            @SuppressLint("RestrictedApi")
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        supportActionBar?.collapseActionView()// 关闭ActionBar搜索框

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
                caretDrawable.caretProgress = vy * .0023f
            }
        }

        val open = savedInstanceState?.getBoolean("is_open", false) ?: false
        if (open) {
            showBottomController()
            playBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            ll_bottom_play.hide()
        } else {
            hideBottomController()
        }
    }

    private var connection: ServiceConnection? = null

    private fun parseIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val scheme = uri.scheme ?: return
        var asyncQueryHandler: AsyncQueryHandler? = null
        @SuppressLint("HandlerLeak")
        asyncQueryHandler = object : AsyncQueryHandler(contentResolver) {
            override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor?) {
                asyncQueryHandler?.cancelOperation(0)
                if (cursor == null) return
                if (!cursor.moveToNext()) {
                    cursor.close()
                    return
                }
                val titleIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val idIdx = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val detaIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val displaynameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                val localSong = LocalSong()
                if (idIdx >= 0) {
                    localSong.songId = cursor.getLong(idIdx)
                }

                if (titleIdx >= 0) {
                    val title = cursor.getString(titleIdx)
                    if (artistIdx >= 0) {
                        val artist = cursor.getString(artistIdx)
                        localSong.author = artist
                    }
                    localSong.title = title
                } else if (displaynameIdx >= 0) {
                    val name = cursor.getString(displaynameIdx)
                    localSong.title = name
                }
                if (albumIdx >= 0) {
                    localSong.album = cursor.getString(albumIdx)
                }
                if (detaIdx >= 0) {
                    localSong.path = cursor.getString(detaIdx)
                } else {
                    localSong.path = uri.path
                }
                cursor.close()

                val service = Intent(this@MainActivity, MusicService::class.java)
                connection = object : ServiceConnection {
                    override fun onServiceDisconnected(name: ComponentName?) {
                    }

                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        val musicBinder = service as? MusicBinder ?: return
                        musicBinder.play(localSong)
                        connection = null
                        this@MainActivity.unbindService(this)
                    }
                }
                this@MainActivity.bindService(service, connection!!, Context.BIND_AUTO_CREATE)
            }
        }

        val strings = arrayOf(MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA)

        if (scheme == ContentResolver.SCHEME_CONTENT) {
            if (uri.authority == MediaStore.AUTHORITY) {
                // try to get title and artist from the media content provider
                asyncQueryHandler.startQuery(0, null, uri,
                        strings, null, null, null)
            } else {
                // Try to get the display name from another content provider.
                // Don't specifically ask for the display name though, since the
                // provider might not actually support that column.
                asyncQueryHandler.startQuery(0, null, uri,
                        null, null, null, null)
            }
        } else if (scheme == "file") {
            // check if this file is in the media database (clicking on a download
            // in the download manager might follow this path
            val path = uri.path ?: return
            asyncQueryHandler.startQuery(0, null, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    strings, MediaStore.Audio.Media.DATA + "=?", arrayOf(path), null)
        }

    }

    fun showBottomController() {
        val height = resources.getDimensionPixelOffset(R.dimen.dimen_bottom_play_controller_height)
        iv_arrow_indicators.show()
        playBehavior.peekHeight = height
        val homeFragmentContent = findViewById<View>(R.id.home_fragment)
        val params = homeFragmentContent.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = height
        homeFragmentContent.layoutParams = params
    }

    fun hideBottomController() {
        iv_arrow_indicators.gone()
        playBehavior.peekHeight = 0
        val homeFragmentContent = findViewById<View>(R.id.home_fragment)
        val params = homeFragmentContent.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = 0
        homeFragmentContent.layoutParams = params

        playBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun playSongs(songs: List<BaseSong>, song: BaseSong?) {
        if (song == null) {
            toast(R.string.play_path_empty)
            return
        }
        playBehavior.isHideable = false
        playBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        playFragment.playSongs(songs, song)
    }

    fun add2PlayList(song: BaseSong?) {
        if (song == null) {
            toast(R.string.play_path_empty)
            return
        }
        playBehavior.isHideable = false
        playBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        playFragment.add2PlayList(song)
    }

    fun toggleBottomSheet() {
        if (playBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            playBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            playBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onStart() {
        super.onStart()
        ChangeLogActivity.show(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        parseIntent(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (super.onOptionsItemSelected(item)) {// 先走fragment的菜单键事件
            return true
        }
        return when (item?.itemId) {
            R.id.menu_setting -> {
                startActivity<SettingActivity>()
                true
            }
            R.id.menu_ape -> {
                startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_VIEW, Uri.parse(APE_LINK)),
                        getString(R.string.chooser_browser)))
                true
            }
            R.id.menu_about -> {
                AboutDialog(this).show()
                true
            }
            R.id.menu_webview -> {
                startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_VIEW, Uri.parse(WEB_LINK)),
                        getString(R.string.chooser_browser)))
                true
            }
            else -> {
                false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putBoolean("is_open", playBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
    }

    override fun onBackPressed() {
        if (playBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            toggleBottomSheet()
            return
        }

        super.onBackPressed()
    }

    override fun onDestroy() {
        if (connection != null) unbindService(connection)
        super.onDestroy()
    }
}
