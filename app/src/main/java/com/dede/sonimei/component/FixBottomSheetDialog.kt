package com.dede.sonimei.component

import android.content.Context
import androidx.annotation.NonNull
import androidx.annotation.StyleRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import java.lang.ref.WeakReference


/**
 * Created by hsh on 2019/1/18 9:49 AM
 *
 * fix android 8.0+ 嵌套RecyclerView，RecyclerView无法向上滚动的问题
 */
open class FixBottomSheetDialog : BottomSheetDialog, ViewTreeObserver.OnGlobalLayoutListener {

    override fun onGlobalLayout() {
        updateBehavior()
    }

    /**
     * [BottomSheetBehavior.nestedScrollingChildRef]字段保存了嵌套滑动的子滑动View。
     * 所以这里手动设置一下[BottomSheetBehavior.nestedScrollingChildRef]
     */
    private fun updateBehavior() {
        val list: View = findScrollingChild(window!!.decorView) ?: return
        val bottomSheet = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return
        val params = (bottomSheet.layoutParams as? androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams?) ?: return
        try {
            val field = BottomSheetBehavior::class.java.getDeclaredField("nestedScrollingChildRef")
            field.isAccessible = true
            field.set(params.behavior, WeakReference(list))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun findScrollingChild(view: View): View? {
        if (ViewCompat.isNestedScrollingEnabled(view)) {
            return view
        } else {
            if (view is ViewGroup) {
                var i = 0
                val count = view.childCount
                while (i < count) {
                    val scrollingChild = this.findScrollingChild(view.getChildAt(i))
                    if (scrollingChild != null) {
                        return scrollingChild
                    }
                    ++i
                }
            }
            return null
        }
    }

    constructor(@NonNull context: Context) : this(context, 0)
    constructor(@NonNull context: Context, @StyleRes theme: Int) : super(context, theme)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        window!!.decorView.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onDetachedFromWindow() {
        window!!.decorView.viewTreeObserver.removeOnGlobalLayoutListener(this)
        super.onDetachedFromWindow()
    }
}