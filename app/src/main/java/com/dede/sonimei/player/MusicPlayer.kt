package com.dede.sonimei.player

import android.media.MediaPlayer
import com.dede.sonimei.module.play.IReviseOnPlayStateChangeListener

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

    init {
        setOnCompletionListener(this)
        setOnBufferingUpdateListener(this)
        setOnPreparedListener(this)
    }

    override fun start() {
        if (isPlaying) return
        super.start()
        onPlayStateChangeListeners.forEach {
            it.onPlayStart(this@MusicPlayer)
        }
    }

    override fun stop() {
        if (!isPlaying) return
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
        super.reset()
        isAsyncPrepared = false
        hasDataSource = false
        setOnBufferingUpdateListener(this)
    }

    /**
     * 是否异步准备完成
     */
    var isAsyncPrepared = false
        private set

    var hasDataSource = false
        private set

    override fun prepareAsync() {
        super.prepareAsync()
        isAsyncPrepared = false
    }

    override fun setDataSource(path: String?) {
        super.setDataSource(path)
        hasDataSource = true
        onPlayStateChangeListeners.forEach {
            it.onDataSourceChange()
        }
    }

    /**
     * 异步准备完毕回调 implement [MediaPlayer.OnPreparedListener]
     */
    override fun onPrepared(mp: MediaPlayer?) {
        isAsyncPrepared = true
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
        onPlayStateChangeListeners.forEach {
            it.onCompletion()
        }
    }

    override fun release() {
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