package com.dede.sonimei.module.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.util.Log
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.dede.sonimei.R
import com.dede.sonimei.data.BaseSong
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.defaultDownloadPath
import com.dede.sonimei.module.setting.Settings
import com.dede.sonimei.net.GlideApp
import com.dede.sonimei.util.ImageUtil
import com.dede.sonimei.util.Logger
import com.dede.sonimei.util.info
import com.dede.sonimei.util.extends.isNull
import com.dede.sonimei.util.extends.notNull
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.OkDownload
import com.liulishuo.okdownload.UnifiedListenerManager
import com.mpatric.mp3agic.ID3v2
import com.mpatric.mp3agic.ID3v24Tag
import com.mpatric.mp3agic.Mp3File
import org.jetbrains.anko.*
import java.io.File
import kotlin.properties.Delegates


class DownloadService : Service(), Logger {

    companion object {
        const val ACTION_CANCEL_DOWNLOAD = "com.dede.sonimei.DOWNLOAD_CANCEL"
        const val ACTION_FINISH_DOWNLOAD = "com.dede.sonimei.DOWNLOAD_FINISH"

        const val DOWNLOAD_NOTIFICATION_ID = 1002
        const val NOTIFY_DOWNLOAD_CHANNEL_ID = "download_channel_id"
    }

    inner class DownloadBinder : Binder() {
        fun download(song: BaseSong?) {
            if (song == null) return
            this@DownloadService.download(song)
        }
    }

    private fun download(song: BaseSong) {
        if (song.path.isNull()) {
            toast(R.string.download_link_empty)
            return
        }
        val wifiDownload = defaultSharedPreferences.getBoolean(Settings.KEY_WIFI_DOWNLOAD, false)
        if (wifiDownload && !isWifiConnected(this)) {
            toast(R.string.download_onlywifi)
            return
        }
        val builder = DownloadTask.Builder(song.path!!, getDownloadPath())
                .setFilename(song.getName() + ".mp3")
                .setMinIntervalMillisCallbackProcess(500)
                .setPreAllocateLength(false)
                .setAutoCallbackToUIThread(true)
        builder.setWifiRequired(wifiDownload)
        builder.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
        val task = builder.build()
        task.tag = song// 设置tag
        val listener = NotificationListener(this)
        listenerManager.attachListener(task, listener)
        listenerManager.addAutoRemoveListenersWhenTaskEnd(task.id)
        listenerManager.enqueueTaskWithUnifiedListener(task, listener)

        toast(String.format(getString(R.string.download_start), song.getName()))
    }

    private var binder: DownloadBinder by Delegates.notNull()
    private var listenerManager = UnifiedListenerManager()
    private val manager: NotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private fun getDownloadPath(): File {
        val downloadPath = defaultSharedPreferences.getString(Settings.KEY_CUSTOM_PATH, defaultDownloadPath.absolutePath)
        var parentFile = File(downloadPath)
        val r = if (!parentFile.exists()) {
            parentFile.mkdirs()
        } else {
            parentFile.isDirectory && parentFile.canWrite()
        }
        if (!r) {
            toast(R.string.download_path_error)
            parentFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        }
        return parentFile
    }

    private val downloadReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_CANCEL_DOWNLOAD -> {
                    val taskId = intent.getIntExtra("task_id", -1)
                    Log.i("DownloadReceiver", "cancel: $taskId")
                    OkDownload.with().downloadDispatcher().cancel(taskId)
                }
                ACTION_FINISH_DOWNLOAD -> {
                    val song = intent.getParcelableExtra<BaseSong>("song")
                    val filePath = intent.getStringExtra("file_path")
                    Log.i("DownloadReceiver", "finish: $filePath")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        saveAlbumImageNotify(song, filePath)
                    } else {
                        notifyFinish(song, filePath)
                    }
                }
            }
        }
    }

    private fun notifyFinish(song: BaseSong, filePath: String?) {
        if (filePath.notNull()) {
            val file = File(filePath)
            if (file.exists()) {
                toast(String.format(getString(R.string.download_finish), song.getName()))
                val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                scanIntent.data = Uri.fromFile(file)
                sendBroadcast(scanIntent)// 通知媒体库更新
            }
        }
        if (DownloadHelper.getInstance(this@DownloadService).hasTask().not()) {
            stopForeground(true)
        }
    }

    private fun saveAlbumImageNotify(song: BaseSong, filePath: String?) {
        if (filePath.isNull()) return
        if (song !is SearchSong) return

        var canNotify = true
        doAsync({
            it.printStackTrace()
            runOnUiThread {
                if (!canNotify) return@runOnUiThread
                notifyFinish(song, filePath)
            }
        }) {
            val mp3file = Mp3File(filePath)
            val id3v2: ID3v2
            if (mp3file.hasId3v2Tag()) {// 有ID3v2 tag
                id3v2 = mp3file.id3v2Tag
                if (id3v2.lyrics.isNull() && song.lrc.notNull()) {
                    id3v2.lyrics = song.lrc
                }
            } else {
                id3v2 = ID3v24Tag()
                mp3file.id3v2Tag = id3v2
                if (song.author.notNull()) id3v2.artist = song.author
                if (song.title.notNull()) id3v2.title = song.title
                if (song.lrc.notNull()) id3v2.lyrics = song.lrc
            }
            mp3file.id3v1Tag = null// 清除v1tag

            val albumImage = id3v2.albumImage
            if (albumImage == null || albumImage.isEmpty()) {
                val file = GlideApp.with(this@DownloadService)
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .load(song.pic)
                        .submit()
                        .get()
                val bytes = ImageUtil.toByteArray(file)
                if (bytes.isNotEmpty()) {
                    id3v2.setAlbumImage(bytes, "image/jpeg")// mimeType JPEG格式
                }
            }

            val newFile = File("$filePath.sonimei")
            mp3file.save(newFile.absolutePath)// 保存的时候出错时，直接通知刷新
            canNotify = false
            val file = File(filePath)
            file.delete()
            newFile.renameTo(file)

            uiThread {
                notifyFinish(song, filePath)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        binder = DownloadBinder()
        val filter = IntentFilter(ACTION_CANCEL_DOWNLOAD)
        filter.addAction(ACTION_FINISH_DOWNLOAD)
        registerReceiver(downloadReceiver, filter)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onRebind(intent: Intent?) {
        stopForeground(true)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        info("onUnbind")
        if (DownloadHelper.getInstance(this).hasTask().not()) {
            stopForeground(true)
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    NOTIFY_DOWNLOAD_CHANNEL_ID,
                    this.getString(R.string.file_download_notify_name),
                    NotificationManager.IMPORTANCE_MIN)
            channel.setShowBadge(false)
            manager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(this, DownloadService.NOTIFY_DOWNLOAD_CHANNEL_ID)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setOngoing(true)
                .setAutoCancel(false)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentTitle(getString(R.string.foreground_download))
                .setChannelId(DownloadService.NOTIFY_DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notify_music_1)
        startForeground(DOWNLOAD_NOTIFICATION_ID, builder.build())
        return false
    }

    override fun onDestroy() {
        unregisterReceiver(downloadReceiver)
        super.onDestroy()
    }

    private fun isWifiConnected(context: Context?): Boolean {
        if (context != null) {
            // 获取手机所有连接管理对象(包括对wi-fi,net等连接的管理)
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            // 获取NetworkInfo对象
            val networkInfo = manager.activeNetworkInfo
            //判断NetworkInfo对象是否为空 并且类型是否为WIFI
            if (networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_WIFI)
                return networkInfo.isAvailable
        }
        return false
    }
}