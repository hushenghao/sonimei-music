package com.dede.sonimei.module.play

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseFragment
import com.dede.sonimei.component.SeekBarChangeListener
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.util.extends.load
import com.dede.sonimei.util.extends.toTime
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_play.*
import kotlinx.android.synthetic.main.layout_bottom_sheet_play_control.*
import org.jetbrains.anko.support.v4.toast
import java.util.concurrent.TimeUnit


/**
 * Created by hsh on 2018/5/23.
 */
class PlayFragment : BaseFragment(), MediaPlayer.OnPreparedListener {

    override fun getLayoutId() = R.layout.fragment_play

    private val mediaPlayer by lazy { MediaPlayer() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context!!.registerReceiver(volumeChangeReceiver, filter)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        iv_play.setOnClickListener(playClick)
        iv_play_bottom.setOnClickListener(playClick)

        iv_play.isClickable = false

        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        sb_volume.max = maxVolume
        sb_volume.progress = volume
        sb_volume.setOnSeekBarChangeListener(object : SeekBarChangeListener() {
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        seekBar!!.progress,
                        AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
            }
        })
    }

    private val audioManager by lazy { context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    /**
     * 监听音量变化
     */
    private val volumeChangeReceiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    sb_volume.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                }
            }
        }
    }

    // 播放按钮点击事件
    private val playClick = View.OnClickListener {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            iv_play.setImageResource(R.drawable.ic_play_status)
            iv_play_bottom.setImageResource(R.drawable.ic_play_status)
        } else {
            mediaPlayer.pause()
            iv_play.setImageResource(R.drawable.ic_pause_status)
            iv_play_bottom.setImageResource(R.drawable.ic_pause_status)
        }
    }

    // 进度条定时器
    private var disposable: Disposable? = null

    fun playSong(song: SearchSong) {
        // bottomSheet mini control
        iv_play.isClickable = false
        iv_album_img.load(song.pic)
        tv_name.text = song.getName()

        // control
        tv_title.text = song.title
        tv_singer.text = song.author


        mediaPlayer.reset()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build())
        } else {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        }
        mediaPlayer.setDataSource(song.url)
        mediaPlayer.prepareAsync()
        // 准备完成后播放
        mediaPlayer.setOnPreparedListener(this)
        // 播放完回调
        mediaPlayer.setOnCompletionListener {
            iv_play.setImageResource(R.drawable.ic_pause_status)
            iv_play_bottom.setImageResource(R.drawable.ic_pause_status)
        }
        mediaPlayer.setOnBufferingUpdateListener { mp, percent ->

        }
        mediaPlayer.setOnErrorListener { _, what, extra ->
            toast("在线播放错误")
            false
        }
    }

    /**
     * 播放准备完成回调 implements [MediaPlayer.OnPreparedListener]
     */
    override fun onPrepared(mp: MediaPlayer) {
        val duration = mp.duration
        tv_all_time.text = duration.toTime()
        sb_progress.max = duration

        iv_play.isClickable = true
        iv_play.setImageResource(R.drawable.ic_play_status)
        iv_play_bottom.setImageResource(R.drawable.ic_play_status)

        disposable?.dispose()
        disposable = Observable.interval(0L, 1L, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe {
                    if (it > duration) {
                        disposable?.dispose()
                        disposable = null
                        return@subscribe
                    }
                    sb_progress.progress = mediaPlayer.currentPosition
                    tv_now_time.text = mediaPlayer.currentPosition.toTime()
                }

        mp.start()
        sb_progress.setOnSeekBarChangeListener(object : SeekBarChangeListener() {

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar!!.progress
                if (progress <= mediaPlayer.duration)
                    mediaPlayer.seekTo(progress)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer.isPlaying)
            mediaPlayer.release()
        disposable?.dispose()
        context!!.unregisterReceiver(volumeChangeReceiver)
    }
}