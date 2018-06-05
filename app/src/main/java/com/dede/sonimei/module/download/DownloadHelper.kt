package com.dede.sonimei.module.download

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.dede.sonimei.R
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.defaultDownloadPath
import com.dede.sonimei.module.setting.Settings
import org.jetbrains.anko.*
import java.io.File


/**
 * Created by hsh on 2018/5/18.
 */
class DownloadHelper private constructor(val context: Context) : AnkoLogger {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: DownloadHelper? = null

        fun getInstance(context: Context): DownloadHelper {
            if (instance == null)
                instance = DownloadHelper(context.applicationContext)
            return instance!!
        }
    }

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val downloadManager: DownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private fun filterDownload(downloadUrl: String?, download: () -> Unit) {
        doAsync {
            val query = DownloadManager.Query()
            // 查询已成功的任务
            query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL)
            val successCursor = downloadManager.query(query)
            var cancel = false// 是否取消任务
            while (successCursor.moveToNext()) {
                val url = successCursor.getString(successCursor.getColumnIndex(DownloadManager.COLUMN_URI))
                if (url == downloadUrl) {
                    val uriStr = successCursor.getString(successCursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                    val path = Uri.parse(uriStr).path// 文件路径
                    val file = File(path)
                    if (file.exists()) {
                        toast("文件已经下载过了...")
                        cancel = true
                        break
                    }
                }
            }
            successCursor.close()
            if (cancel) return@doAsync

            // 查询下载中的任务
            query.setFilterByStatus(DownloadManager.STATUS_RUNNING)
            val runningCursor = downloadManager.query(query)
            while (runningCursor.moveToNext()) {
                val url = runningCursor.getString(runningCursor.getColumnIndex(DownloadManager.COLUMN_URI))
                if (url == downloadUrl) {
                    toast("已经正在下载了...")
                    cancel = true
                }
            }
            if (cancel) return@doAsync

            // 查询在等待中的任务
            query.setFilterByStatus(DownloadManager.STATUS_PENDING)
            val pendingCursor = downloadManager.query(query)
            while (pendingCursor.moveToNext()) {
                val url = pendingCursor.getString(pendingCursor.getColumnIndex(DownloadManager.COLUMN_URI))
                if (url == downloadUrl) {
                    toast("已经在下载队列了...")
                    cancel = true
                }
            }
            pendingCursor.close()
            if (cancel) return@doAsync

            handler.post {
                download.invoke()
            }
        }
    }

    private fun toast(msg: String) {
        uiThread(Runnable {
            context.toast(msg)
        })
    }

    private fun uiThread(runnable: Runnable) {
        handler.post(runnable)
    }

    fun download(song: SearchSong) {
        filterDownload(song.url, {
            val request = DownloadManager.Request(Uri.parse(song.url))
            request.setTitle(song.getName())
            request.setMimeType("audio/mpeg")
            val wifiDownload = context.defaultSharedPreferences.getBoolean(Settings.KEY_WIFI_DOWNLOAD, false)
            if (wifiDownload) {
                if (!isWifiConnected(context)) {
                    toast("已开启仅Wi-Fi下载...")
                }
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            }
            request.setDescription(context.resources.getString(R.string.app_name))
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            val path = context.defaultSharedPreferences.getString(Settings.KEY_CUSTOM_PATH, defaultDownloadPath.absolutePath)
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            request.setDestinationUri(Uri.fromFile(File(file, song.getName() + ".mp3")))
            val id = downloadManager.enqueue(request)
            toast("开始下载 " + song.getName())
            info("id:" + id)
        })
    }

    fun isWifiConnected(context: Context?): Boolean {
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