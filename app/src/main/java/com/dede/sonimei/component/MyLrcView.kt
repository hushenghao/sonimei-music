package com.dede.sonimei.component

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import me.wcy.lrcview.LrcView

class MyLrcView : LrcView {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val b = super.onTouchEvent(event)
        if (b) {
            // 在BottomSheetBehavior中会拦截垂直滚动的事件，请求父View不拦截
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        return b
    }
}