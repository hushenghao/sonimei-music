package com.dede.sonimei.module.play

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v7.graphics.Palette
import android.view.KeyEvent
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.dede.sonimei.R
import com.dede.sonimei.data.BaseSong
import com.dede.sonimei.data.local.LocalSong
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.defaultSheep
import com.dede.sonimei.module.home.MainActivity
import com.dede.sonimei.net.GlideApp
import com.dede.sonimei.player.MusicPlayer
import com.dede.sonimei.player.SimplePlayStateChangeListener
import com.dede.sonimei.util.ClickEventHelper
import com.dede.sonimei.util.extends.*
import io.reactivex.disposables.Disposable
import org.jetbrains.anko.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


private const val ACTION_NOTIFICATION_PLAY = "play"
private const val ACTION_NOTIFICATION_PAUSE = "pause"
private const val ACTION_NOTIFICATION_NEXT = "next"
private const val ACTION_NOTIFICATION_LAST = "last"
private const val ACTION_NOTIFICATION_DELETE = "delete"

private const val PLAY_NOTIFICATION_ID = 1001
private const val PLAY_NOTIFICATION_CHANNEL = "sonimei_music_channel"

private const val SP_KEY_PLAY_INDEX = "play_index"
private const val SP_KEY_PLAY_LIST = "play_list"
private const val SP_KEY_PLAY_MODE = "play_mode"

/**
 * Created by hsh on 2018/8/2.
 */
class MusicService : Service(), IPlayControllerListenerI, ILoadPlayList,
        AudioManager.OnAudioFocusChangeListener, AnkoLogger {

    /** implement [IPlayControllerListenerI] */

    override fun getPlayList(): List<BaseSong> = ArrayList(playList)

    override fun removeAt(index: Int) {
        if (index >= playList.size || index < 0) return

        if (index < playIndex) {
            playList.removeAt(index)
            playIndex--// 调整索引
        } else {
            playList.removeAt(index)
            // 移除当前播放的位置
            if (index == playIndex) {
                if (playList.isEmpty()) {
                    playIndex = 0
                    musicPlayer.stop()
                    notificationManager.cancel(PLAY_NOTIFICATION_ID)
                } else {
                    playIndex = index
                    val end = playList.size - 1
                    if (playIndex > end) {
                        playIndex = end
                    }
                    play(playList[playIndex])
                }
            }
        }
        randomLastIndexs.clear()
        randomNextIndexs.clear()
        savePlayList()
    }

    override fun remove(song: BaseSong?) {
        val indexOf = playList.indexOf(song)
        removeAt(indexOf)
    }

    override fun add(song: BaseSong?, index: Int) {
        if (song == null) return
        if (index >= 0) {
            if (index <= playIndex) {
                playIndex++
            }
            playList.add(index, song)
        } else {
            playList.add(song)// 加到最后面不用处理历史光标
        }

        if (playList.size == 1) {
            playIndex = 0
            play(song)
        }
        randomNextIndexs.clear()
        randomLastIndexs.clear()
        savePlayList()
    }

    override fun clear() {
        playList.clear()
        playIndex = 0
        randomNextIndexs.clear()
        randomLastIndexs.clear()
        musicPlayer.stop()
        notificationManager.cancel(PLAY_NOTIFICATION_ID)
    }

    private var pathSubscribe: Disposable? = null

    @SuppressLint("CheckResult")
    override fun play(song: BaseSong?) {
        if (song == null) {
            toast(R.string.play_path_empty)
            return
        }
        pathSubscribe?.dispose()

        val indexOf = this.playList.indexOf(song)
        if (indexOf == -1) {
            this.playList.add(song)//添加到最后，修改索引直接播放
            this.playIndex = this.playList.size - 1

            savePlayList()
        } else {
            this.playIndex = indexOf
        }
        startForeground(PLAY_NOTIFICATION_ID, createNotification())
        pathSubscribe = song.loadPlayLink()
                .applySchedulers()
                .doOnNext { song.path = it }
                .doOnSubscribe {
                    for (listener in musicPlayer.getPlayStateChangeListeners()) {
                        listener.onBuffer(true)
                    }
                }
                .doOnError {
                    for (listener in musicPlayer.getPlayStateChangeListeners()) {
                        listener.onBuffer(false)
                    }
                }
                .doOnComplete {
                    for (listener in musicPlayer.getPlayStateChangeListeners()) {
                        listener.onBuffer(false)
                    }
                }
                .subscribe({
                    if (it.isNull()) {
                        toast(R.string.play_path_empty)
                    } else {
                        play(it)
                    }
                }) {
                    toast(R.string.load_play_path_error)
                    it.printStackTrace()
                }
    }

    override fun plays(playList: List<BaseSong>?, index: Int) {
        if (playList == null || playList.isEmpty()) return

        if (this.playList != playList) {
            this.playList.clear()
            this.playList.addAll(playList)

            savePlayList()

            randomNextIndexs.clear()
            randomLastIndexs.clear()
        } else {
            if (index != playIndex) {
                randomLastIndexs.push(playIndex)
            }
        }

        playIndex = if (index in 0..this.playList.size) {
            index
        } else {
            0
        }

        play(this.playList[playIndex])
    }

    /**
     * 播放
     */
    private fun play(path: String) {
        info("index:$playIndex  path:$path")
        sp.edit().putInt(SP_KEY_PLAY_INDEX, playIndex).apply()
        try {
            this.musicPlayer.reset()
            this.musicPlayer.isLooping = playMode == MODE_SINGLE // fix single loop mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.musicPlayer.setAudioAttributes(AudioAttributes.Builder()
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build())
            } else {
                this.musicPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            }
            this.musicPlayer.setDataSource(path)
            this.musicPlayer.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun start() {
        requestAudioFocus()

        if (musicPlayer.state == STATE_PREPARED || musicPlayer.state == STATE_PAUSED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                musicPlayer.playbackParams = musicPlayer.playbackParams.setSpeed(playSpeed)
            }
            musicPlayer.start()
            startForeground(PLAY_NOTIFICATION_ID, createNotification())
        } else {
            plays(playList, playIndex)
        }
    }

    override fun pause() {
        musicPlayer.pause()
        stopForeground(true)
        notificationManager.notify(PLAY_NOTIFICATION_ID, createNotification())
    }

    override fun next() {
        val size = playList.size
        if (size == 0) return
        playIndex = nextIndex()
        play(playList[playIndex])
    }

    override fun last() {
        val size = playList.size
        if (size == 0) return
        playIndex = lastIndex()
        play(playList[playIndex])
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun sheep(sheep: Float): Float {
        if (sheep <= 0) {// 小于0说明是get方法
            return playSpeed
        }
        this.playSpeed = sheep
        if (musicPlayer.isPlaying) {
            musicPlayer.playbackParams = musicPlayer.playbackParams.setSpeed(sheep)
        }
        return this.playSpeed
    }

    override fun getPlayInfo(): BaseSong? {
        return if (playIndex >= 0 && playIndex < playList.size) {
            playList[playIndex]
        } else {
            null
        }
    }

    override fun updatePlayMode(@PlayMode mode: Int) {
        if (mode < MODE_ORDER || mode > MODE_RANDOM) return

        this.playMode = mode
        toast(getPlayModeStrRes(this.playMode))
        musicPlayer.isLooping = this.playMode == MODE_SINGLE// 是否是单曲循环

        sp.edit().putInt(SP_KEY_PLAY_MODE, playMode).apply()
    }

    @PlayMode
    override fun getPlayMode(): Int {
        return this.playMode
    }

    override fun getPlayIndex(): Int {
        return playIndex
    }

    override fun addOnPlayStateChangeListener(listener: MusicPlayer.OnPlayStateChangeListener?) {
        musicPlayer.addOnPlayStateChangeListener(listener)
    }

    override fun removeOnPlayStateChangeListener(listener: MusicPlayer.OnPlayStateChangeListener?) {
        musicPlayer.removeOnPlayStateChangeListener(listener)
    }

    fun nextIndex(): Int {
        var index = playIndex
        val size = playList.size
        if (size == 1) {
            index = 0
        } else {
            when (playMode) {
                MODE_ORDER, MODE_SINGLE -> {
                    if (++index >= size) {
                        index = 0
                    }
                }
                MODE_RANDOM -> {
                    randomLastIndexs.push(playIndex)// 保存当前索引为上一曲索引
                    while (index == playIndex && randomNextIndexs.size > 0) {
                        index = randomNextIndexs.pop()
                    }
                    while (index == playIndex) {
                        index = random.nextInt(size)
                    }
                    randomNextIndexs.push(index)// 保存下一首索引
                }
            }
        }
        return index
    }

    fun lastIndex(): Int {
        var index = playIndex
        val size = playList.size
        if (size == 1) {
            index = 0
        } else {
            when (playMode) {
                MODE_ORDER, MODE_SINGLE -> {
                    if (--index < 0) {
                        index = size - 1
                    }
                }
                MODE_RANDOM -> {
                    randomNextIndexs.push(playIndex)// 保存当前索引为下一曲索引
                    while (index == playIndex && randomLastIndexs.size > 0) {
                        index = randomLastIndexs.pop()
                    }
                    while (index == playIndex) {
                        index = random.nextInt(size)
                    }
                    randomLastIndexs.push(index)// 保存上一首索引
                }
            }
        }
        return index
    }

    /** ================== */

    private lateinit var musicPlayer: MusicPlayer

    private val playList = ArrayList<BaseSong>()// 播放列表
    private var playIndex = 0// 播放索引

    @PlayMode
    private var playMode: Int = MODE_ORDER// 播放模式，默认顺序播放
    /** 随机播放取随机数 */
    private val random = Random()
    /** 随机播放历史顺序 */
    private val randomNextIndexs by lazy { LinkedList<Int>() }
    private val randomLastIndexs by lazy { LinkedList<Int>() }

    private var autoStart = true

    /** 处理音频焦点 */
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE).to<AudioManager>() }

    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE).to<NotificationManager>() }

    private val sp by lazy { applicationContext.defaultSharedPreferences }

    /**
     * 获取焦点
     */
    private fun requestAudioFocus(): Boolean {
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(
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
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        return r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    /**
     * 释放焦点
     */
    private fun releaseAudioFocus(): Boolean {
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setOnAudioFocusChangeListener(this)
                            .build()
            )
        } else {
            audioManager.abandonAudioFocus(this)
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
                if (!musicPlayer.isPlaying && onResumeFocusAutoStart) {
                    start()
                    onResumeFocusAutoStart = false
                }
                musicPlayer.setVolume(1f, 1f)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (musicPlayer.isPlaying) {
                    onResumeFocusAutoStart = true
                    pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (musicPlayer.isPlaying) {
                    musicPlayer.stop()
                    stopForeground(false)
                    notificationManager.notify(PLAY_NOTIFICATION_ID, createNotification())
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (musicPlayer.isPlaying) {
                    musicPlayer.setVolume(0.5f, 0.5f)
                }
            }
        }
    }

    fun getMusicPlayer(): MusicPlayer {
        return musicPlayer
    }

    private lateinit var sessionCompat: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        playIndex = sp.getInt(SP_KEY_PLAY_INDEX, 0)
        if (playIndex < 0) playIndex = 0
        playMode = sp.getInt(SP_KEY_PLAY_MODE, MODE_ORDER)
        if (!isPlayMode(playMode)) playMode = MODE_ORDER

        initMusicPlayer()

        // 注册耳机断开广播
        val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(headsetPlugReceiver, intentFilter)

        // 注册通知点击广播
        val notifyFilter = IntentFilter(ACTION_NOTIFICATION_LAST)
        notifyFilter.addAction(ACTION_NOTIFICATION_NEXT)
        notifyFilter.addAction(ACTION_NOTIFICATION_PAUSE)
        notifyFilter.addAction(ACTION_NOTIFICATION_PLAY)
        notifyFilter.addAction(ACTION_NOTIFICATION_DELETE)
        registerReceiver(notificationActionReceiver, notifyFilter)

        setLoadPlayListListener(null)// 预加载播放列表

        val clickEventHelper = ClickEventHelper(object : ClickEventHelper.Callback {
            override fun onClick() {
                if (musicPlayer.isPlaying) {
                    pause()
                } else {
                    start()
                }
            }

            override fun onDoubleClick() {
                next()
            }

            override fun onTripleClick() {
                last()
            }

        })

        sessionCompat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaSessionCompat(this, "com.dede.sonimei_mediasession")
        } else {// api 19以下必须调用4个参数的方法
            val intent = Intent(this, MusicService::class.java)
            val pendingIntent = PendingIntent.getService(this, 0, intent, 0)
            MediaSessionCompat(this, "com.dede.sonimei_mediasession", intent.component, pendingIntent)
        }
        sessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
        sessionCompat.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                val keyEvent = mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                        ?: return false
                if (keyEvent.action == KeyEvent.ACTION_UP) {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            clickEventHelper.sendClickEvent()
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            next()
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            last()
                        }
                    }
                    info("MediaButtonEvent: " + keyEvent.keyCode)
                }
                return false
            }
        })
        sessionCompat.isActive = true
    }

    /**
     * 加载播放列表完成
     */
    private fun loadPlayListFinish() {
        val size = playList.size
        if (playIndex >= size - 1) playIndex = size - 1

        loadPlayListListener?.onLoadFinish()
    }

    /**
     * 接收耳机断开广播，包括蓝牙
     */
    private val headsetPlugReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == action) {
                info("Headset Disconnected 暂停播放")
                pause()
            }
        }
    }

    // 播放速度
    private var playSpeed = defaultSheep

    /**
     * 初始化MediaPlayer
     */
    private fun initMusicPlayer() {
        musicPlayer = MusicPlayer()
        musicPlayer.setOnErrorListener { mp, what, extra ->
            when (what) {
                MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
                    musicPlayer = (mp as MusicPlayer).getRecreateHelper().recreate()
                    initMusicPlayer()
                    return@setOnErrorListener true
                }
                MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
                    toast(getString(R.string.play_error, extra))
                    next()
                    return@setOnErrorListener true
                }
                -38 -> {
                    // getDuration error ,ignore
                    return@setOnErrorListener true
                }
            }
            false
        }

        musicPlayer.addOnPlayStateChangeListener(object : SimplePlayStateChangeListener() {
            override fun onPrepared(mp: MusicPlayer) {
                if (autoStart) {
                    start()// 准备完成自动播放
                } else {
                    autoStart = true
                }
            }

            override fun onCompletion() {
                next()// 播放下一曲，单曲循环时不会回调这里
            }
        })
    }

    private var binder: MusicBinder? = null

    override fun onBind(intent: Intent?): IBinder {
        stopForeground(false)
        binder = MusicBinder(this)
        return binder!!
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        stopForeground(false)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        loadPlayListListener = null
        savePlayList()
        sp.edit().putInt(SP_KEY_PLAY_INDEX, playIndex).apply()
        if (musicPlayer.isPlaying) {
            // 如果不是可播放状态就不显示通知
            startForeground(PLAY_NOTIFICATION_ID, createNotification())
        }
        return false
    }

    private val notificationActionReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_NOTIFICATION_PLAY -> {
                    start()
                }
                ACTION_NOTIFICATION_PAUSE -> {
                    pause()
                }
                ACTION_NOTIFICATION_LAST -> {
                    last()
                }
                ACTION_NOTIFICATION_NEXT -> {
                    next()
                }
                ACTION_NOTIFICATION_DELETE -> {
                    stopSelf()
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = notificationManager.getNotificationChannel(PLAY_NOTIFICATION_CHANNEL)
            if (channel == null) {
                channel = NotificationChannel(PLAY_NOTIFICATION_CHANNEL,
                        getString(R.string.play_notification_name),
                        NotificationManager.IMPORTANCE_LOW)
                channel.setShowBadge(false)
                notificationManager.createNotificationChannel(channel)
            }
            NotificationCompat.Builder(this, PLAY_NOTIFICATION_CHANNEL)
        } else {
            NotificationCompat.Builder(this)
        }
        val song = playList[playIndex]

        val mediaStyle = android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1, 2)
                .setMediaSession(sessionCompat.sessionToken)
        builder.setContentTitle(song.getName())
                .setContentText(song.title)
                .setChannelId(PLAY_NOTIFICATION_CHANNEL)// channel Id 兼容8.0+
                .setSmallIcon(R.drawable.ic_notify_music_1)// 通知栏图标
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)// 完全可见通知
                .setAutoCancel(false)// 点击不清楚
                .setOngoing(musicPlayer.isPlaying)// 播放状态不可清除
                .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)// 关闭桌面图标角标
                .setPriority(NotificationCompat.PRIORITY_LOW)// 优先级
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)// 播放控制类型，好像没有什么用
        builder.setUsesChronometer(true)
                .setStyle(mediaStyle)

        addNotificationAction(builder)

        val click = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 1,
                click, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)// 内容点击intent

        if (song is SearchSong) {
            GlideApp.with(this)
                    .asBitmap()
                    .load(song.pic)
                    .error(R.mipmap.ic_launcher)
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            // 获取图片主色调
                            Palette.from(resource).generate {
                                notificationManager.notify(PLAY_NOTIFICATION_ID,
                                        builder.setLargeIcon(resource)
                                                .setColor(it?.mutedSwatch?.rgb ?: Color.WHITE)
                                                .setColorized(true)
                                                .build())
                            }
                        }
                    })
        } else if (song is LocalSong && song.picByteArray() != null) {
            builder.setContentText(song.album)
            GlideApp.with(this)
                    .asBitmap()
                    .load(song.picByteArray())
                    .error(R.mipmap.ic_launcher)
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            // 获取图片主色调
                            Palette.from(resource).generate {
                                notificationManager.notify(PLAY_NOTIFICATION_ID,
                                        builder.setLargeIcon(resource)
                                                .setColor(it?.mutedSwatch?.rgb ?: Color.WHITE)
                                                .setColorized(true)
                                                .build())
                            }
                        }
                    })
        } else {
            builder.setColor(-5205824)
                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher))
        }
        builder.setColorized(true)
        return builder.build()
    }

    private fun addNotificationAction(builder: NotificationCompat.Builder) {
        val intent = Intent(ACTION_NOTIFICATION_LAST)
        intent.setPackage(this.packageName)
        var pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.addAction(R.drawable.ic_play_last, getString(R.string.notify_last), pendingIntent)

        if (musicPlayer.isPlaying) {
            intent.action = ACTION_NOTIFICATION_PAUSE
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            builder.addAction(R.drawable.ic_play_status, getString(R.string.notify_pause), pendingIntent)
        } else {
            intent.action = ACTION_NOTIFICATION_PLAY
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            builder.addAction(R.drawable.ic_pause_status, getString(R.string.notify_play), pendingIntent)
        }
        intent.action = ACTION_NOTIFICATION_NEXT
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.addAction(R.drawable.ic_play_next, getString(R.string.notify_next), pendingIntent)

        intent.action = ACTION_NOTIFICATION_DELETE
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setDeleteIntent(pendingIntent)
    }


    override fun onDestroy() {
        pathSubscribe?.dispose()
        stopForeground(true)
        notificationManager.cancel(PLAY_NOTIFICATION_ID)
        unregisterReceiver(headsetPlugReceiver)
        unregisterReceiver(notificationActionReceiver)
        musicPlayer.release()
        releaseAudioFocus()
        sp.edit().putInt(SP_KEY_PLAY_INDEX, playIndex).apply()
        sessionCompat.release()
        super.onDestroy()
    }

    /** ======== 播放状态保存 ======= */

    private fun savePlayList() {
        doAsync({ it.printStackTrace() }) {
            playList.save(File(filesDir, SP_KEY_PLAY_LIST))
        }
    }

    private var loadPlayListListener: ILoadPlayList.OnLoadPlayListListener? = null
    private var isLoading = false

    override fun setLoadPlayListListener(loadPlayListListener: ILoadPlayList.OnLoadPlayListListener?) {
        this.loadPlayListListener = loadPlayListListener
        if (isLoading) return
        if (playList.isNotEmpty()) {
            loadPlayListFinish()
            return
        }
        isLoading = true
        doAsync({
            isLoading = false
            it.printStackTrace()
        }) {
            val list = File(filesDir, SP_KEY_PLAY_LIST).load<List<BaseSong>>()
            if (list != null && list.isNotEmpty()) {
                uiThread {
                    playList.clear()
                    playList.addAll(list)

                    loadPlayListFinish()
                    isLoading = false
                }
            }
        }
    }

}