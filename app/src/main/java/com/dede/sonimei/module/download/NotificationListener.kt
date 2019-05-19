package com.dede.sonimei.module.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import android.util.Log
import com.dede.sonimei.R
import com.dede.sonimei.data.BaseSong
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.SpeedCalculator
import com.liulishuo.okdownload.core.breakpoint.BlockInfo
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed
import com.liulishuo.okdownload.core.listener.assist.Listener4SpeedAssistExtend
import kotlin.properties.Delegates


internal const val NOTIFY_DOWNLOAD_FINISH_CHANNEL_ID = "download_finish_channel_id"

/**
 * 下载文件通知栏监听
 */
internal class NotificationListener(context: Context) : DownloadListener4WithSpeed() {

    private var totalLength: Int = 0

    private val context: Context = context.applicationContext
    private var builder: NotificationCompat.Builder by Delegates.notNull()
    private val manager: NotificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    DownloadService.NOTIFY_DOWNLOAD_CHANNEL_ID,
                    context.getString(R.string.file_download_notify_name),
                    NotificationManager.IMPORTANCE_LOW)
            channel.setShowBadge(true)
            manager.createNotificationChannel(channel)
        }

        builder = NotificationCompat.Builder(context, DownloadService.NOTIFY_DOWNLOAD_CHANNEL_ID)
        builder.setDefaults(Notification.DEFAULT_LIGHTS)
                .setOngoing(false)
                .setAutoCancel(false)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setChannelId(DownloadService.NOTIFY_DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
    }

    override fun taskStart(task: DownloadTask) {
        Log.d("NotificationListener", "taskStart")
        val song = getSong(task)
        builder.setContentTitle("准备下载")
                .setContentText(song.getName())
                .setProgress(0, 0, true)
        manager.notify(task.id, builder.build())
        val bundle = Bundle()
        bundle.putParcelable("song", song)
        bundle.putInt("task_id", task.id)
        val intent = PendingIntent.getBroadcast(context, 0,
                Intent(DownloadService.ACTION_CANCEL_DOWNLOAD), PendingIntent.FLAG_CANCEL_CURRENT)
        val action = NotificationCompat.Action.Builder(R.drawable.ic_delete, "取消", intent)
                .addExtras(bundle)
                .build()
        builder.addAction(action)
        manager.notify(task.id, builder.build())
    }

    override fun connectStart(task: DownloadTask, blockIndex: Int,
                              requestHeaderFields: Map<String, List<String>>) {
    }

    override fun connectEnd(task: DownloadTask, blockIndex: Int, responseCode: Int,
                            responseHeaderFields: Map<String, List<String>>) {
    }

    override fun infoReady(task: DownloadTask, info: BreakpointInfo,
                           fromBreakpoint: Boolean,
                           model: Listener4SpeedAssistExtend.Listener4SpeedModel) {
        Log.d("NotificationListener", "infoReady $info $fromBreakpoint")

        builder.setContentTitle("开始下载")
                .setProgress(info.totalLength.toInt(), info.totalOffset.toInt(), true)
        manager.notify(task.id, builder.build())

        totalLength = info.totalLength.toInt()
    }

    override fun progressBlock(task: DownloadTask, blockIndex: Int,
                               currentBlockOffset: Long,
                               blockSpeed: SpeedCalculator) {
    }

    override fun progress(task: DownloadTask, currentOffset: Long,
                          taskSpeed: SpeedCalculator) {
        Log.d("NotificationListener", "progress $currentOffset")
        builder.setContentTitle("正在下载 " + taskSpeed.speed())
                .setProgress(totalLength, currentOffset.toInt(), false)
        manager.notify(task.id, builder.build())
    }

    override fun blockEnd(task: DownloadTask, blockIndex: Int, info: BlockInfo,
                          blockSpeed: SpeedCalculator) {
    }

    override fun taskEnd(task: DownloadTask, cause: EndCause,
                         realCause: Exception?,
                         taskSpeed: SpeedCalculator) {
        Log.d("NotificationListener", "taskEnd $cause $realCause")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    NOTIFY_DOWNLOAD_FINISH_CHANNEL_ID,
                    context.getString(R.string.file_download_finish_notify_name),
                    NotificationManager.IMPORTANCE_DEFAULT)
            channel.setShowBadge(true)
            manager.createNotificationChannel(channel)
        }
        builder = NotificationCompat.Builder(context, NOTIFY_DOWNLOAD_FINISH_CHANNEL_ID)
        builder.setTicker("下载完成")
        val song = getSong(task)
        builder.setDefaults(Notification.DEFAULT_SOUND)
                .setContentText(song.getName())
                .setOngoing(false)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
        val intent = Intent(DownloadService.ACTION_FINISH_DOWNLOAD)
        intent.putExtra("song", song as Parcelable)
        when (cause) {
            EndCause.COMPLETED -> {
                builder.setContentTitle("下载完成")
                intent.putExtra("file_path", task.file?.absolutePath)
            }
            EndCause.CANCELED -> builder.setContentTitle("取消下载")
            EndCause.ERROR -> builder.setContentTitle("下载失败")
            EndCause.FILE_BUSY -> builder.setContentTitle("文件错误")
            EndCause.SAME_TASK_BUSY -> builder.setContentTitle("下载任务正忙")
            EndCause.PRE_ALLOCATE_FAILED -> builder.setContentTitle("预分配空间失败")
        }
        context.sendBroadcast(intent)
        Log.i("NotificationListener", task.info?.toString() ?: "")
        DownloadHelper.getInstance(context)
                .remove(song)// 移除任务
        handler.postDelayed({
            // 在高频率notify下，有些设备会忽略部分通知刷新，这里延时一段时间
            manager.notify(task.id, builder.build())
        }, 300)
    }

    private fun getSong(task: DownloadTask): BaseSong {
        return task.tag as BaseSong
    }
}
