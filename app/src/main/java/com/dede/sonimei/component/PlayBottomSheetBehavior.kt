package com.dede.sonimei.component

import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.coordinatorlayout.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration


/**
 * Created by hsh on 2018/6/13.
 */
class PlayBottomSheetBehavior<T : View> : BottomSheetBehavior<T> {

    private var mMaximumVelocity: Float = 0f

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mMaximumVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity.toFloat()
    }

    /**
     * 垂直方向的手指移动速度变化监听
     */
    interface OnYVelocityChangeListener {
        fun onChange(vy: Float)
    }

    private var velocityTracker: VelocityTracker? = null


    override fun onInterceptTouchEvent(parent: androidx.coordinatorlayout.widget.CoordinatorLayout, child: T, event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            reset()
        }

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(event)

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                velocityTracker!!.computeCurrentVelocity(1000, mMaximumVelocity)
                val yVelocity = velocityTracker!!.getYVelocity(event.getPointerId(event.actionIndex))
                onYVelocityChangeListener?.onChange(yVelocity)
            }
        }

        return super.onInterceptTouchEvent(parent, child, event)
    }

    var onYVelocityChangeListener: OnYVelocityChangeListener? = null

    override fun onTouchEvent(parent: androidx.coordinatorlayout.widget.CoordinatorLayout, child: T, event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            reset()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(event)

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                velocityTracker!!.computeCurrentVelocity(1000, mMaximumVelocity)
                val yVelocity = velocityTracker!!.getYVelocity(event.getPointerId(event.actionIndex))
                onYVelocityChangeListener?.onChange(yVelocity)
            }
        }

        return super.onTouchEvent(parent, child, event)
    }

    private fun reset() {
        velocityTracker?.recycle()
        velocityTracker = null
    }
}