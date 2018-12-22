package com.dede.sonimei.player

import android.media.MediaPlayer
import com.dede.sonimei.module.play.*
import java.lang.IllegalStateException

/**
 * Created by hsh on 2018/8/1.
 */
class MusicPlayer : MediaPlayer(), MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener,
        IReviseOnPlayStateChangeListener {

    /**
     * 播放遇到严重错误时，使用内部类来进行状态恢复
     */
    inner class RecreateHelper {
        fun recreate(): MusicPlayer {
            val mediaPlayer = MusicPlayer()

            mediaPlayer.onPlayStateChangeListeners = this@MusicPlayer.onPlayStateChangeListeners
            mediaPlayer.isLooping = this@MusicPlayer.isLooping

            this@MusicPlayer.release()
            return mediaPlayer
        }
    }

    fun getRecreateHelper() = RecreateHelper()

    var onPlayStateChangeListeners = ArrayList<MusicPlayer.OnPlayStateChangeListener>()

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
    }

    override fun start() {
        if (state != STATE_PAUSED && state != STATE_PREPARED) {
            return
        }
        state = STATE_STARTED
        super.start()
        onPlayStateChangeListeners.forEach {
            it.onPlayStart(this@MusicPlayer)
        }
    }

    override fun stop() {
        if (state != STATE_PREPARED && state != STATE_STARTED &&
                state != STATE_PAUSED && state != STATE_PLAYBACK_COMPLETED) {
            return
        }
        state = STATE_STOPED
        super.stop()
        onPlayStateChangeListeners.forEach {
            it.onPlayStop()
        }
    }

    override fun pause() {
        if (state != STATE_PREPARED && state != STATE_STARTED) {
            return
        }
        state = STATE_PAUSED
        super.pause()
        onPlayStateChangeListeners.forEach {
            it.onPlayPause()
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
        super.prepareAsync()
    }

    override fun prepare() {
        if (state != STATE_INITIALIZED && state != STATE_STOPED) {
            return
        }
        state = STATE_PREPARED
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

    fun canPlay():Boolean {
        return state == STATE_PAUSED || state == STATE_PREPARING ||
                state == STATE_STARTED || state == STATE_PREPARED
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
        onPlayStateChangeListeners.forEach {
            it.onDataSourceChange()
        }
    }

    /**
     * 异步准备完毕回调 implement [MediaPlayer.OnPreparedListener]
     */
    override fun onPrepared(mp: MediaPlayer?) {
        state = STATE_PREPARED
        onPlayStateChangeListeners.forEach {
            it.onPrepared(this@MusicPlayer)
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
        state = STATE_PLAYBACK_COMPLETED
        onPlayStateChangeListeners.forEach {
            it.onCompletion()
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
         */
        fun onBufferUpdate(percent: Int)

        /**
         * 数据源改变
         */
        fun onDataSourceChange()

    }

    open class SimplePlayStateChangeListener : OnPlayStateChangeListener {

        override fun onDataSourceChange() {
        }

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