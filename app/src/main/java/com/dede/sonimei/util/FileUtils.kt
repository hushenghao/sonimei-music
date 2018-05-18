package com.dede.sonimei.util

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.support.annotation.Keep
import android.text.TextUtils
import java.lang.reflect.InvocationTargetException
import java.util.*

/**
 * Created by hsh on 2017/3/10 010 上午 10:50.
 */
object FileUtils {

    /**
     * 获取系统下所有的挂载点
     *
     * @param context 上下文
     * @return 包含所有挂载点的集合
     */
    @Keep
    @Synchronized
    fun getStorageList(context: Context): List<StorageInfo> {
        val storageInfoList = ArrayList<StorageInfo>()
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        try {
            val getVolumeList = storageManager.javaClass.getMethod("getVolumeList")
            getVolumeList.isAccessible = true
            val volumes = getVolumeList.invoke(storageManager) as Array<StorageVolume>

            for (volume in volumes) {
                val getPath = volume.javaClass.getMethod("getPath")
                getPath.isAccessible = true
                val path = getPath.invoke(volume) as String
                if (TextUtils.isEmpty(path))
                    break

                val getState = volume.javaClass.getMethod("getState")
                getState.isAccessible = true
                val state = getState.invoke(volume) as String

                val isRemovable = volume.javaClass.getMethod("isRemovable")
                isRemovable.isAccessible = true
                val removable = isRemovable.invoke(volume) as Boolean

                val info = StorageInfo(path)
                info.isRemovable = removable
                info.state = state
                storageInfoList.add(info)
            }
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

        storageInfoList.trimToSize()
        return storageInfoList
    }

    @Keep
    class StorageInfo(path: String?) {

        var path: String? = null
            private set
        var state: String? = null// mounted已安装可用，unmounted不可用
            set(value) {
                field = if (TextUtils.isEmpty(path)) {
                    this.isMounted = false
                    Environment.MEDIA_UNMOUNTED
                } else {
                    this.isMounted = true
                    state
                }
            }
        var isRemovable: Boolean = false// true可以被移除，false不可移除
        var isMounted: Boolean = false// 是否被挂载
            get() = Environment.MEDIA_MOUNTED == state

        init {
            if (TextUtils.isEmpty(path)) {
                this.state = Environment.MEDIA_UNMOUNTED//如果路径为空，则挂载点不可用
            } else
                this.path = path
        }
    }
}
