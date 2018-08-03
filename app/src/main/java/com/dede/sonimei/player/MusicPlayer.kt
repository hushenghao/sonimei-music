package com.dede.sonimei.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build

/**
 * Created by hsh on 2018/8/1.
 */
class MusicPlayer : MediaPlayer(), AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener {

    /**
     * 使用内部类来进行状态恢复
     */
    inner class ReCreateHelper {
        fun reCreate(): MusicPlayer {
            val mediaPlayer = MusicPlayer()

            mediaPlayer.onPlayStateChangeListeners = this@MusicPlayer.onPlayStateChangeListeners
            mediaPlayer.audioManager = this@MusicPlayer.audioManager
            mediaPlayer.onResumeFocusAutoStart = this@MusicPlayer.onResumeFocusAutoStart
            mediaPlayer.autoManagerAudioFocus = this@MusicPlayer.autoManagerAudioFocus
            mediaPlayer.isLooping = this@MusicPlayer.isLooping

            this@MusicPlayer.release()
            return mediaPlayer
        }
    }

    fun getReCreateHelper() = ReCreateHelper()

    /**  ==================    */

    private var onPlayStateChangeListeners = ArrayList<OnPlayStateChangeListener>()

    fun addOnPlayStateChangeListener(listener: OnPlayStateChangeListener?) {
        if (listener != null && !onPlayStateChangeListeners.contains(listener)) {
            onPlayStateChangeListeners.add(listener)
        }
    }

    fun removeOnPlayStateChangeListener(listener: OnPlayStateChangeListener?) {
        if (listener == null) {
            onPlayStateChangeListeners.clear()
            return
        }
        onPlayStateChangeListeners.remove(listener)
    }

    init {
        setOnCompletionListener(this)
        setOnBufferingUpdateListener(this)
        setOnPreparedListener(this)
    }

    private var audioManager: AudioManager? = null
    private var autoManagerAudioFocus = true// 自动管理音频焦点

    /**
     * 自动管理音频焦点
     */
    fun autoManagerAudioFocus(context: Context, auto: Boolean) {
        autoManagerAudioFocus = auto
        if (audioManager == null) {
            this.audioManager = context.applicationContext
                    .getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
    }

    override fun start() {
        if (isPlaying) return
        if (autoManagerAudioFocus) {
            requestAudioFocus()
        }
        super.start()
        onPlayStateChangeListeners.forEach {
            it.onPlayStart(this@MusicPlayer)
        }
    }

    override fun stop() {
        super.stop()
        onPlayStateChangeListeners.forEach {
            it.onPlayStop()
        }
    }

    override fun pause() {
        if (!isPlaying) return
        super.pause()
        onPlayStateChangeListeners.forEach {
            it.onPlayPause()
        }
    }

    override fun reset() {
        setOnBufferingUpdateListener(this)
        super.reset()
    }

    private var onPreparedAutoStart = true

    /**
     * 是否自动播放在准备完成后
     */
    fun autoStartOnPrepared(auto: Boolean) {
        onPreparedAutoStart = auto
    }

    /**
     * 异步准备完毕回调 implement [MediaPlayer.OnPreparedListener]
     */
    override fun onPrepared(mp: MediaPlayer?) {
        onPlayStateChangeListeners.forEach {
            it.onPrepared(this@MusicPlayer)
        }
        if (onPreparedAutoStart) {
            start()
        }
    }

    /**
     * 异步缓冲直节流更新回调 implement [MediaPlayer.OnBufferingUpdateListener]
     */
    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {
        if (percent >= 100)
            this.setOnBufferingUpdateListener(null)// 清除回调，到100还会不停回调
        onPlayStateChangeListeners.forEach {
            it.onBufferUpdate(percent)
        }
    }

    /**
     * 播放完成后回调 implement [MediaPlayer.OnCompletionListener]
     */
    override fun onCompletion(mp: MediaPlayer?) {
        onPlayStateChangeListeners.forEach {
            it.onCompletion()
        }
    }

    override fun release() {
        removeOnPlayStateChangeListener(null)
        super.release()
        if (autoManagerAudioFocus) {
            releaseAudioFocus()
        }
    }

    /**
     * 获取焦点
     */
    private fun requestAudioFocus(): Boolean {
        if (audioManager == null) return false

        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager!!.abandonAudioFocusRequest(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setOnAudioFocusChangeListener(this)
                            .setWillPauseWhenDucked(true)
                            .setAudioAttributes(
                                    AudioAttributes.Builder()
                                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                                            .build()
                            )
                            .build()
            )
        } else {
            audioManager!!.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        return r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    /**
     * 释放焦点
     */
    private fun releaseAudioFocus(): Boolean {
        if (audioManager == null) return false

        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager!!.abandonAudioFocusRequest(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_LOSS)
                            .build()
            )
        } else {
            audioManager!!.abandonAudioFocus(this)
        }
        return r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    // 短暂失去焦点时，恢复焦点后自动播放
    private var onResumeFocusAutoStart = false

    /**
     * 焦点变化回调 implement [AudioManager.OnAudioFocusChangeListener]
     */
    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (!isPlaying && onResumeFocusAutoStart) {
                    start()
                    onResumeFocusAutoStart = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (isPlaying) {
                    onResumeFocusAutoStart = true
                    pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (isPlaying) {
                    pause()
                }
            }
        }
    }

    /**
     * 播放状态改变监听
     */
    interface OnPlayStateChangeListener {

        fun onPlayStart(mp: MusicPlayer)

        fun onPlayPause()

        fun onPlayStop()

        fun onPrepared(mp: MusicPlayer)

        fun onCompletion()

        fun onBufferUpdate(percent: Int)

    }

    class SimplePlayStateChangeListener() : OnPlayStateChangeListener {

        override fun onPlayStart(mp: MusicPlayer) {
        }

        override fun onPlayPause() {
        }

        override fun onPlayStop() {
        }

        override fun onPrepared(mp: MusicPlayer) {
        }

        override fun onCompletion() {
        }

        override fun onBufferUpdate(percent: Int) {
        }
    }
}