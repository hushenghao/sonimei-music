package com.dede.sonimei.module.play

import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseActivity
import com.dede.sonimei.base.BaseFragment
import com.dede.sonimei.component.MyLrcView
import com.dede.sonimei.data.BaseSong
import com.dede.sonimei.data.local.LocalSong
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.defaultSheepIndex
import com.dede.sonimei.module.download.DownloadHelper
import com.dede.sonimei.module.home.MainActivity
import com.dede.sonimei.net.GlideApp
import com.dede.sonimei.sheepList
import com.dede.sonimei.util.extends.gone
import com.dede.sonimei.util.extends.notNull
import com.dede.sonimei.util.extends.show
import kotlinx.android.synthetic.main.fragment_play_content.*
import kotlinx.android.synthetic.main.layout_play_pic_view.*
import org.jetbrains.anko.support.v4.dip
import org.jetbrains.anko.support.v4.toast

/**
 * 播放页中间歌词内容
 */
class PlayContentFragment : BaseFragment(), ValueAnimator.AnimatorUpdateListener {

    override fun onAnimationUpdate(animation: ValueAnimator?) {
        imageView.rotation = animation?.animatedValue as? Float ?: imageView.rotation
    }

    override fun getLayoutId(): Int = R.layout.fragment_play_content

    private lateinit var lrcViewContent: View
    lateinit var lrcView: MyLrcView
        private set
    private lateinit var picView: View
    private lateinit var imageView: ImageView

    private lateinit var playAnimator: ValueAnimator

    private var pagerChangeListener = object : ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            playAnim(position == 0 && playFragment?.musicBinder?.isPlaying == true)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        lrcViewContent = LayoutInflater.from(context).inflate(R.layout.layout_lrc_view, view_pager, false)
        lrcView = lrcViewContent.findViewById(R.id.lrc_view)
        lrcView.setOnLineChangeListener { _, lineStr, _ ->
            if (stop) return@setOnLineChangeListener
            tv_music_content_lrc.text = lineStr
        }
        picView = LayoutInflater.from(context).inflate(R.layout.layout_play_pic_view, view_pager, false)
        imageView = picView.findViewById(R.id.iv_music)
        view_pager.adapter = Adapter()
        view_pager.addOnPageChangeListener(pagerChangeListener)

        playAnimator = ValueAnimator.ofFloat(0f, 360f)
                .apply {
                    interpolator = LinearInterpolator()
                    duration = 20000
                    repeatCount = -1
                    repeatMode = ValueAnimator.RESTART
                    addUpdateListener(this@PlayContentFragment)
                }

        picView.findViewById<View>(R.id.tv_play_lrc).setOnClickListener {
            view_pager.currentItem = 1
        }

        val closeSheet = View.OnClickListener {
            (activity as? MainActivity?)?.toggleBottomSheet()
        }
        lrcViewContent.findViewById<ImageView>(R.id.iv_close_sheet_1).setOnClickListener(closeSheet)
        picView.findViewById<ImageView>(R.id.iv_close_sheet_2).setOnClickListener(closeSheet)
    }

    private var stop = false
    private var animRunning = false

    override fun onStart() {
        super.onStart()
        stop = false
        if (animRunning) {
            playAnimator.resume()
        }
    }

    override fun onStop() {
        super.onStop()
        stop = true
        if (animRunning) {
            playAnimator.pause()
        }
    }

    private val playFragment by lazy {
        (activity as BaseActivity?)?.supportFragmentManager
                ?.findFragmentById(R.id.play_fragment) as? PlayFragment
    }

    private var sheepIndex: Int = defaultSheepIndex

    fun setSongInfo(song: BaseSong) {
        val tvSpeed = picView.findViewById<TextView>(R.id.tv_play_speed)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && playFragment != null) {
            val sheep = playFragment!!.getSheep()
            sheepIndex = sheepList.indexOf(sheep)
            tvSpeed.text = getString(R.string.play_steep, sheep)
            tvSpeed.show()
            tvSpeed.setOnClickListener {
                val s = sheepList.size
                sheepIndex++
                if (sheepIndex >= s) {
                    sheepIndex = 0
                }
                val newSheep = sheepList[sheepIndex]
                toast(getString(R.string.toast_play_steep, newSheep))
                tvSpeed.text = getString(R.string.play_steep, newSheep)
                playFragment!!.setSheep(newSheep)
            }
        }
        picView.findViewById<TextView>(R.id.tv_music_content_lrc).text = song.getName()
        val download = picView.findViewById<ImageView>(R.id.iv_download)
        download.show()
        if (song is SearchSong) {
            lrcView.loadLrc(song.lrc)
            GlideApp.with(picView)
                    .load(song.pic)
                    .error(R.drawable.ic_music)
                    .placeholder(R.drawable.ic_music)
                    .transform(RoundedCorners(dip(200)))
                    .into(imageView)
            download.setImageResource(R.drawable.ic_music_download)
            download.setOnClickListener { DownloadHelper.download(activity, song) }
        } else if (song is LocalSong) {
            lrcView.loadLrc("")
            GlideApp.with(picView)
                    .load(song.picByteArray())
                    .error(R.drawable.ic_music)
                    .placeholder(R.drawable.ic_music)
                    .transform(RoundedCorners(dip(200)))
                    .into(imageView)
            download.setImageResource(R.drawable.ic_music_local)
            download.setOnClickListener { toast(R.string.toast_local_music) }
            lrcView.loadLrc(null as String?)
        } else {
            download.gone()
        }
    }

    fun playAnim(start: Boolean) {
        if (animRunning == start) return
        animRunning = start
        if (view_pager.currentItem != 0) return
        if (start) {
            if (playAnimator.isStarted) {
                playAnimator.resume()
            } else {
                playAnimator.start()
            }
        } else {
            if (playAnimator.isRunning) {
                playAnimator.pause()
            }
        }
    }

    fun updateTime(time: Long) {
        if (stop) return
        if (view_pager.currentItem == 0) {
            findLine(time)
        } else {
            lrcView.updateTime(time)
        }
    }

    fun findLine(time: Long): String? {
        if (stop) return null
        val lrcLine = lrcView.findLine(time)
        if (lrcLine.notNull()) {
            tv_music_content_lrc.text = lrcLine
        }
        return lrcLine
    }

    override fun onDestroyView() {
        playAnimator.removeUpdateListener(this)
        playAnimator.cancel()
        view_pager.removeOnPageChangeListener(pagerChangeListener)
        super.onDestroyView()
    }

    private inner class Adapter : PagerAdapter() {

        override fun isViewFromObject(p0: View, p1: Any): Boolean = p0 == p1

        override fun getCount(): Int = 2

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val view = when (position) {
                1 -> lrcViewContent
                else -> picView
            }
            container.addView(view)
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View?)
        }
    }
}