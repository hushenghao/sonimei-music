package com.dede.sonimei.player

import android.media.MediaPlayer
import android.support.annotation.IntRange
import android.util.Log
import com.dede.sonimei.module.play.*

private const val TAG = "MusicPlayer"

/**
 * Created by hsh on 2018/8/1.
 */
class MusicPlayer : MediaPlayer(), MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener,
        IReviseOnPlayStateChangeListener {

    /**
     * 播放遇到严重错误时，使用内部类来进行状态恢复
     */
    inner class RecreateHelper {
        fun recreate(): MusicPlayer {
            val mediaPlayer = MusicPlayer()

            mediaPlayer.onPlayStateChangeListeners = this@MusicPlayer.onPlayStateChangeListeners
            mediaPlayer.onErrorListener = this@MusicPlayer.onErrorListener
            mediaPlayer.isLooping = this@MusicPlayer.isLooping

            this@MusicPlayer.release()
            return mediaPlayer
        }
    }

    fun getRecreateHelper() = RecreateHelper()

    private var onPlayStateChangeListeners = ArrayList<MusicPlayer.OnPlayStateChangeListener>()
    private var onErrorListener: MediaPlayer.OnErrorListener? = null

    /**  ========= implement [IReviseOnPlayStateChangeListener] =========  */

    override fun addOnPlayStateChangeListener(listener: OnPlayStateChangeListener?) {
        if (listener != null && !onPlayStateChangeListeners.contains(listener)) {
            onPlayStateChangeListeners.add(listener)
        }
    }

    override fun removeOnPlayStateChangeListener(listener: OnPlayStateChangeListener?) {
        if (listener == null) {
            onPlayStateChangeListeners.clear()
            return
        }
        onPlayStateChangeListeners.remove(listener)
    }


    @PlayState
    var state: Int = STATE_IDLE
        private set

    init {
        setOnCompletionListener(this)
        setOnBufferingUpdateListener(this)
        setOnPreparedListener(this)
        super.setOnErrorListener(this)
        setOnInfoListener(this)
        setOnSeekCompleteListener {
            Log.i(TAG, "OnSeekCompleteListener: ")
        }
    }

    /**
     * implement [MediaPlayer.OnInfoListener] 主要处理缓冲状态的loading状态
     */
    override fun onInfo(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        return when (what) {
            MEDIA_INFO_BUFFERING_START -> {
                for (listener in onPlayStateChangeListeners) {
                    listener.onBuffer(true)
                }
                true
            }
            MEDIA_INFO_BUFFERING_END -> {
                for (listener in onPlayStateChangeListeners) {
                    listener.onBuffer(false)
                }
                true
            }
            else -> {
                false// 返回值没什么用 android-28
            }
        }
    }

    /**
     * 发生错误时回调 implement [MediaPlayer.OnErrorListener]
     */
    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        val error = this.onErrorListener?.onError(mp, what, extra) ?: false
        if (!error) {
            state = STATE_ERROR
        }
        return error
    }

    override fun setOnErrorListener(listener: OnErrorListener?) {
        this.onErrorListener = listener
    }

    override fun start() {
        if (state != STATE_PAUSED && state != STATE_PREPARED) {
            return
        }
        state = STATE_STARTED
        super.start()
        for (listener in onPlayStateChangeListeners) {
            listener.onPlayStart(this@MusicPlayer)
        }
    }

    override fun stop() {
        if (state != STATE_PREPARED && state != STATE_STARTED &&
                state != STATE_PAUSED && state != STATE_PLAYBACK_COMPLETED) {
            return
        }
        state = STATE_STOPED
        super.stop()
        for (listener in onPlayStateChangeListeners) {
            listener.onPlayStop()
        }
    }

    override fun pause() {
        if (state != STATE_PREPARED && state != STATE_STARTED) {
            return
        }
        state = STATE_PAUSED
        super.pause()
        for (listener in onPlayStateChangeListeners) {
            listener.onPlayPause()
        }
    }

    override fun reset() {
        state = STATE_IDLE
        super.reset()
        setOnBufferingUpdateListener(this)
    }

    override fun prepareAsync() {
        if (state != STATE_INITIALIZED && state != STATE_STOPED) {
            return
        }
        state = STATE_PREPARING
        for (listener in onPlayStateChangeListeners) {
            listener.onBuffer(true)
        }
        super.prepareAsync()
    }

    override fun prepare() {
        if (state != STATE_INITIALIZED && state != STATE_STOPED) {
            return
        }
        state = STATE_PREPARED
        for (listener in onPlayStateChangeListeners) {
            listener.onBuffer(false)
        }
        super.prepare()
    }

    override fun isPlaying(): Boolean {
        return if (state == STATE_ERROR || state == STATE_IDLE ||
                state == STATE_INITIALIZED || state == STATE_PREPARING) {
            false
        } else {
            super.isPlaying()
        }
    }

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        try {
            super.setVolume(leftVolume, rightVolume)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    override fun getDuration(): Int {
        if (state != STATE_PREPARED && state != STATE_PAUSED &&
                state != STATE_STARTED && state != STATE_PLAYBACK_COMPLETED &&
                state != STATE_STOPED) {
            return 0
        }
        return super.getDuration()
    }

    override fun getCurrentPosition(): Int {
        if (state != STATE_PREPARED && state != STATE_PAUSED &&
                state != STATE_STARTED && state != STATE_PLAYBACK_COMPLETED) {
            return 0
        }
        return super.getCurrentPosition()
    }

    override fun seekTo(msec: Long, mode: Int) {
        if (state != STATE_PREPARED && state != STATE_PAUSED &&
                state != STATE_STARTED && state != STATE_PLAYBACK_COMPLETED) {
            return
        }
        super.seekTo(msec, mode)
    }

    override fun setDataSource(path: String?) {
        if (state != STATE_IDLE) {
            return
        }
        state = STATE_INITIALIZED
        super.setDataSource(path)
        for (listener in onPlayStateChangeListeners) {
            listener.onDataSourceChange()
        }
    }

    /**
     * 异步准备完毕回调 implement [MediaPlayer.OnPreparedListener]
     */
    override fun onPrepared(mp: MediaPlayer?) {
        state = STATE_PREPARED
        for (listener in onPlayStateChangeListeners) {
            listener.onPrepared(this@MusicPlayer)
            listener.onBuffer(false)
        }
    }

    /**
     * 异步缓冲直节流更新回调 implement [MediaPlayer.OnBufferingUpdateListener]
     */
    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {
        if (percent >= 100)
            this.setOnBufferingUpdateListener(null)// 清除回调，到100还会不停回调
        for (listener in onPlayStateChangeListeners) {
            listener.onBufferUpdate(percent)
        }
    }

    /**
     * 播放完成后回调 implement [MediaPlayer.OnCompletionListener]
     */
    override fun onCompletion(mp: MediaPlayer?) {
        state = STATE_PLAYBACK_COMPLETED
        for (listener in onPlayStateChangeListeners) {
            listener.onCompletion()
        }
    }

    override fun release() {
        state = STATE_END
        removeOnPlayStateChangeListener(null)
        super.release()
    }

    /**
     * 播放状态改变监听
     */
    interface OnPlayStateChangeListener {

        /**
         * 开始播放
         */
        fun onPlayStart(mp: MusicPlayer)

        /**
         * 播放暂停
         */
        fun onPlayPause()

        /**
         * 播放停止
         */
        fun onPlayStop()

        /**
         * 准备完成
         */
        fun onPrepared(mp: MusicPlayer)

        /**
         * 播放完成
         */
        fun onCompletion()

        /**
         * 缓冲流更新
         * @param percent 缓冲进度
         */
        fun onBufferUpdate(@IntRange(from = 0, to = 100) percent: Int)

        /**
         * 缓冲状态
         */
        fun onBuffer(inBuffer: Boolean)

        /**
         * 数据源改变
         */
        fun onDataSourceChange()

    }
}