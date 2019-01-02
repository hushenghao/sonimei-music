package com.dede.sonimei.module.download

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.support.annotation.StringRes
import android.webkit.MimeTypeMap
import com.dede.sonimei.R
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.defaultDownloadPath
import com.dede.sonimei.module.setting.Settings
import com.dede.sonimei.util.extends.isNull
import com.dede.sonimei.util.extends.notNull
import com.tbruyelle.rxpermissions2.RxPermissions
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

        /**
         * 判断运行时权限后下载，没有权限时申请后再下载
         */
        @SuppressLint("CheckResult")
        fun download(activity: Activity?, song: SearchSong) {
            if (activity == null) {
                instance?.download(song)
                return
            }
            RxPermissions(activity)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribe {
                        if (it)
                            getInstance(activity).download(song)
                        else
                            activity.toast(R.string.permission_sd_error)
                    }
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
                        toast(R.string.download_file_downed)
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
                    toast(R.string.download_isdownloading)
                    cancel = true
                    break
                }
            }
            runningCursor.close()
            if (cancel) return@doAsync

            // 查询在等待中的任务
            query.setFilterByStatus(DownloadManager.STATUS_PENDING)
            val pendingCursor = downloadManager.query(query)
            while (pendingCursor.moveToNext()) {
                val url = pendingCursor.getString(pendingCursor.getColumnIndex(DownloadManager.COLUMN_URI))
                if (url == downloadUrl) {
                    toast(R.string.download_haslinked)
                    cancel = true
                    break
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

    private fun toast(@StringRes res: Int) {
        uiThread(Runnable {
            context.toast(context.getString(res))
        })
    }

    private fun uiThread(runnable: Runnable) {
        handler.post(runnable)
    }

    /**
     * 获取链接下载，忽略运行时权限
     */
    @SuppressLint("CheckResult")
    fun download(song: SearchSong) {
        song.loadPlayLink()
                .subscribe({
                    _download(song)
                }) {
                    toast(R.string.load_play_path_error)
                    it.printStackTrace()
                }
    }

    /**
     * 直接下载，忽略运行时权限
     */
    private fun _download(song: SearchSong) {
        if (song.path.isNull()) {
            toast(R.string.download_link_empty)
            return
        }
        filterDownload(song.path) {
            val request = DownloadManager.Request(Uri.parse(song.path))
            request.setTitle(song.getName())
            request.setDescription(context.resources.getString(R.string.app_name))
            val mimeTypeMap = MimeTypeMap.getSingleton()
            val mimeString = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(song.path))
            if (mimeString.notNull()) {
                request.setMimeType(mimeString)
            } else {
                request.setMimeType("audio/mpeg")
            }
            val wifiDownload = context.defaultSharedPreferences.getBoolean(Settings.KEY_WIFI_DOWNLOAD, false)
            if (wifiDownload) {
                if (!isWifiConnected(context)) {
                    toast(R.string.download_onlywifi)
                }
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            } else {
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
            }
            request.allowScanningByMediaScanner()
            request.setVisibleInDownloadsUi(true)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            val path = context.defaultSharedPreferences.getString(Settings.KEY_CUSTOM_PATH, defaultDownloadPath.absolutePath)
            val file = File(path)
            val r = if (!file.exists()) {
                file.mkdirs()
            } else {
                file.isDirectory && file.canWrite()
            }
            if (!r) {
                toast(R.string.download_path_error)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, song.getName() + ".mp3")
            } else {
                request.setDestinationUri(Uri.fromFile(File(file, song.getName() + ".mp3")))
            }
            val id = downloadManager.enqueue(request)
            toast(String.format(context.getString(R.string.download_start), song.getName()))
            info("id:$id")
        }
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