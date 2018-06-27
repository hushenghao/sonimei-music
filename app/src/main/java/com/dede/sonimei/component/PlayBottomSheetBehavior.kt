package com.dede.sonimei.component

import android.content.Context
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
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

    private var id: Int = -1

    override fun onInterceptTouchEvent(parent: CoordinatorLayout?, child: T, event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            reset()
        }

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN ->
                id = event.getPointerId(0)
            MotionEvent.ACTION_MOVE -> {
                if (id != -1) {
                    velocityTracker!!.computeCurrentVelocity(1000, mMaximumVelocity)
                    val yVelocity = velocityTracker!!.getYVelocity(event.getPointerId(id))
                    onYVelocityChangeListener?.onChange(yVelocity)
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                id = -1
            }
        }

        return super.onInterceptTouchEvent(parent, child, event)
    }

    var onYVelocityChangeListener: OnYVelocityChangeListener? = null

    override fun onTouchEvent(parent: CoordinatorLayout?, child: T, event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            reset()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN ->
                id = event.getPointerId(0)
            MotionEvent.ACTION_MOVE -> {
                if (id != -1) {
                    velocityTracker!!.computeCurrentVelocity(1000, mMaximumVelocity)
                    val yVelocity = velocityTracker!!.getYVelocity(event.getPointerId(id))
                    onYVelocityChangeListener?.onChange(yVelocity)
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                id = -1
            }
        }

        return super.onTouchEvent(parent, child, event)
    }

    private fun reset() {
        id = -1
        velocityTracker?.recycle()
        velocityTracker = null
    }
}