package com.dede.sonimei.component

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import me.wcy.lrcview.LrcView

class MyLrcView : LrcView {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var eventX: Float = 0f
    private var eventY: Float = 0f

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!hasLrc()) return false

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                eventX = event.x
                eventY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                val dx = Math.abs(x - eventX)
                val dy = Math.abs(y - eventY)
                eventX = x
                eventY = y
                if (dy < dx) {
                    // 处理和ViewPager的滑动冲突，水平滑动请求父容器进行拦截
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return false// 不消费事件，将事件回传给上层ViewPager
                } else {
                    // 在BottomSheetBehavior中会拦截垂直滚动的事件，请求父View不拦截
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
        }
        return super.onTouchEvent(event)
    }
}