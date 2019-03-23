package com.dede.sonimei.module.download

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.fragment.app.FragmentActivity
import com.dede.sonimei.R
import com.dede.sonimei.data.BaseSong
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.log.Logger
import com.tbruyelle.rxpermissions2.RxPermissions
import org.jetbrains.anko.toast
import java.util.*
import kotlin.collections.ArrayList


/**
 * Created by hsh on 2018/5/18.
 */
class DownloadHelper private constructor(val context: Context) : ServiceConnection, Logger {

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
        fun download(activity: FragmentActivity?, song: SearchSong) {
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

    private val tempQueue = ArrayList<BaseSong>()// 绑定成功前的队列
    private val allQueue = Collections.synchronizedList<BaseSong>(ArrayList())
    private var binder: DownloadService.DownloadBinder? = null

    override fun onServiceDisconnected(name: ComponentName?) {
        binder = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        binder = service as? DownloadService.DownloadBinder
        if (binder == null) {
            return
        }
        var song: BaseSong
        while (tempQueue.isNotEmpty()) {
            song = tempQueue.removeAt(0)
            binder!!.download(song)
        }
    }

    /**
     * 下载完成
     */
    fun remove(song: BaseSong?) {
        allQueue.remove(song)
        if (allQueue.isEmpty() && binder != null) {
            binder = null
            context.unbindService(this)// 全部下载完成，解绑服务
        }
    }

    fun hasTask(): Boolean {
        return allQueue.isNotEmpty()
    }

    /**
     * 获取链接下载
     */
    @SuppressLint("CheckResult")
    fun download(song: BaseSong) {
        song.loadPlayLink()
                .subscribe({
                    allQueue.add(song)
                    if (binder == null) {
                        tempQueue.add(song)
                        context.bindService(Intent(context, DownloadService::class.java), this, Context.BIND_AUTO_CREATE)
                    } else {
                        binder!!.download(song)
                    }
                }) {
                    context.toast(context.getString(R.string.load_play_path_error))
                    it.printStackTrace()
                }
    }

}