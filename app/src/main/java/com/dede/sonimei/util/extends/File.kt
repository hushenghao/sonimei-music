package com.dede.sonimei.util.extends

import android.util.Log
import java.io.*

/**
 * Created by hsh on 2018/8/8.
 */

fun <T : Any> T.save(file: File) {
    var out: ObjectOutputStream? = null
    try {
        if (!file.exists()) {
            file.createNewFile()
        }
        if (file.isDirectory) {
            Log.i("FileUtil", "save: path" + file.absolutePath + "isDirectory")
            return
        }
        val outputStream = FileOutputStream(file)
        out = ObjectOutputStream(outputStream)
        out.writeObject(this)
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        try {
            out?.close()
        } catch (e: IOException) {
        }
    }
}

fun <T> File.load(): T? {
    var input: ObjectInputStream? = null
    try {
        if (!this.exists()) {
            Log.i("FileUtil", "save: path" + this + "un exists")
            return null
        }
        if (this.isDirectory) {
            Log.i("FileUtil", "save: path" + this + "is directory")
            return null
        }
        val inputStream = FileInputStream(this)
        input = ObjectInputStream(inputStream)
        return input.readObject().to<T>()
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        try {
            input?.close()
        } catch (e: IOException) {
        }
    }
    return null
}
