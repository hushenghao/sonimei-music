package com.dede.sonimei.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.support.annotation.IntRange
import android.util.Log
import com.dede.sonimei.BuildConfig


/**
 * Created by hsh on 2018/5/25.
 */
object ImageUtil {

    private val TAG = if (BuildConfig.DEBUG) "ImageUtil" else ""

    /**
     * fast 高斯模糊。将Bitmap等比缩放后进行高斯模糊处理，再放大到原来大小
     * @param context 用于初始化RenderScript
     * @param source 源Bitmap
     * @param radius 高斯模糊的范围
     * @param scale 缩放比例
     */
    fun rsBlur(context: Context, source: Bitmap,
               @IntRange(from = 0, to = 25) radius: Int,
               @IntRange(from = 0, to = 1) scale: Float): Bitmap {

        Log.i(TAG, "origin size:" + source.width + "*" + source.height)
        val width = Math.round(source.width * scale)
        val height = Math.round(source.height * scale)

        val inputBmp = Bitmap.createScaledBitmap(source, width, height, false)

        val renderScript = RenderScript.create(context)

        Log.i(TAG, "scale size:" + inputBmp.width + "*" + inputBmp.height)

        // Allocate memory for Renderscript to work with

        val input = Allocation.createFromBitmap(renderScript, inputBmp)
        val output = Allocation.createTyped(renderScript, input.type)

        // Load up an instance of the specific script that we want to use.
        val scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        scriptIntrinsicBlur.setInput(input)

        // Set the blur radius
        scriptIntrinsicBlur.setRadius(radius.toFloat())

        // Start the ScriptIntrinisicBlur
        scriptIntrinsicBlur.forEach(output)

        // Copy the output to the blurred bitmap
        output.copyTo(inputBmp)

        scriptIntrinsicBlur.destroy()
        renderScript.destroy()
        output.destroy()
        input.destroy()

        return inputBmp
    }

    fun getPlayBitmap(context: Context, source: Bitmap): Bitmap {
        val rsBlur = ImageUtil.rsBlur(context, ImageUtil.getScreenScaleBitmap(source, context), 15, .8f)
        val canvas = Canvas(rsBlur)
        canvas.drawColor(0x55000000)// 画个颜色，防止部分图片颜色较浅使文字不明显
        return rsBlur
    }

    /**
     * 将Bitmap的比例裁切到屏幕比例大小，只是比例为屏幕大小
     */
    private fun getScreenScaleBitmap(source: Bitmap, context: Context): Bitmap {
        val bwidth = source.width
        val bheight = source.height

        val twidth: Int
        val theight: Int
        val x: Int
        val y: Int

        val metrics = context.resources.displayMetrics
        val screenScale = metrics.widthPixels.toFloat() / metrics.heightPixels// 屏幕宽高比
        val sourceScale = bwidth.toFloat() / bheight// 图片宽高比

        if (screenScale == sourceScale) return source

        if (sourceScale > screenScale) {
            // 图片的宽高比大于屏幕宽高比，说明图片需要切割左右两边
            twidth = (bheight * screenScale + .5f).toInt()// 图片目标宽度
            x = (bwidth - twidth) / 2
            theight = bheight
            y = 0
        } else {
            // 图片的宽高比小于屏幕宽高比，说明图片需要切割上下两边
            theight = (bwidth / screenScale + .5f).toInt()
            y = (bheight - theight) / 2
            twidth = bwidth
            x = 0
        }

        return Bitmap.createBitmap(source, x, y, twidth, theight)
    }
}