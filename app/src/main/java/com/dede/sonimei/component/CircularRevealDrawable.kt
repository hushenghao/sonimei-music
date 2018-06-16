package com.dede.sonimei.component

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import android.support.annotation.IntRange
import android.util.Log
import android.view.animation.LinearInterpolator

/**
 * @author hsh
 * @date 2017/12/25 13:54.
 * @doc 圆形揭露动画Drawable
 */
class CircularRevealDrawable constructor(@ColorInt private var mBgColor: Int,
                                         @IntRange(from = 0) private val mDuration: Long = 500)
    : Drawable(), ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

    private val mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mAnimColor: Int = 0

    private var mCenterX: Float = 0.toFloat()
    private var mCenterY: Float = 0.toFloat()
    private var mRadius = 0f

    private var mAnimIsEnd: Boolean = false
    private val radiusValueAnimator by lazy {
        val animator = ValueAnimator.ofFloat()
        animator.addUpdateListener(this)
        animator.addListener(this)
        animator.interpolator = LinearInterpolator()
        animator
    }

    private val height: Int
        get() = bounds.height()

    private val width: Int
        get() = bounds.width()

    fun play(@ColorInt animColor: Int, centerX: Float, centerY: Float, startRadius: Float, endRadius: Float) {
        if (animColor != mBgColor) {
            this.mAnimColor = animColor
            this.mCenterX = centerX
            this.mCenterY = centerY
            if (radiusValueAnimator.isRunning) {
                radiusValueAnimator.cancel()
            }
            radiusValueAnimator.setFloatValues(startRadius, endRadius)
            radiusValueAnimator.duration = mDuration

            radiusValueAnimator.start()
        }
    }

    fun play(@ColorInt animColor: Int, centerX: Float, centerY: Float) {
        if (width <= 0 || height <= 0) {
            Log.i(TAG, "Drawable width or height is zero ,play anim invalid.")
        }
        val radiusWidth = getDistance(width.toFloat(), centerX)
        val radiusHeight = getDistance(height.toFloat(), centerY)
        val endRadius = Math.sqrt(Math.pow(radiusHeight.toDouble(), 2.0) + Math.pow(radiusWidth.toDouble(), 2.0))// 勾股定理拿到斜边长
        this.play(animColor, centerX, centerY, 0f, endRadius.toFloat())
    }

    fun play(@ColorInt animColor: Int) {
        this.play(animColor, 0f, 0f)
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        mPaint.color = mBgColor
        canvas.drawRect(bounds, mPaint)
        if (!mAnimIsEnd && mAnimColor != mBgColor) {
            mPaint.color = mAnimColor
            canvas.drawCircle(mCenterX, mCenterY, mRadius, mPaint)
        }
    }

    override fun onBoundsChange(bounds: Rect) {
    }

    private fun getDistance(max: Float, i: Float): Float {
        return if (max / 2 > i) {
            max - i
        } else {
            i
        }
    }

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }


    /* implements ValueAnimator.AnimatorUpdateListener */
    override fun onAnimationUpdate(animation: ValueAnimator) {
        mRadius = animation.animatedValue as Float
        invalidateSelf()
    }

    /* implements Animator.AnimatorListener */
    override fun onAnimationStart(animation: Animator) {
        mAnimIsEnd = false
    }

    override fun onAnimationEnd(animation: Animator) {
        mBgColor = mAnimColor
        mAnimIsEnd = true
    }

    override fun onAnimationCancel(animation: Animator) {}

    override fun onAnimationRepeat(animation: Animator) {}

    companion object {

        private const val TAG = "CircularRevealDrawable"
    }
}
