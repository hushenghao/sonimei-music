package com.dede.sonimei.module.home

import android.content.Context
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dede.sonimei.MusicSource
import com.dede.sonimei.R
import com.dede.sonimei.data.Source
import com.dede.sonimei.sourceList
import com.dede.sonimei.util.extends.color
import org.jetbrains.anko.dip
import org.jetbrains.anko.find

/**
 * Created by hsh on 2018/5/23.
 */
class SourceSelectDialog(context: Context, @MusicSource val source: Int) : BottomSheetDialog(context) {

    private val recyclerView: RecyclerView
    private val adapter: Adapter
    private val list = sourceList

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_select_source, null, false)
        recyclerView = view.find(R.id.source_list)
        adapter = Adapter(list)
        recyclerView.adapter = adapter

        setContentView(view, FrameLayout.LayoutParams(-1, context.dip(300)))

        val behavior = BottomSheetBehavior
                .from(this.delegate.findViewById<View>(android.support.design.R.id.design_bottom_sheet))
        behavior.skipCollapsed = false
//        behavior.peekHeight = context.dip(300)

        var position = -1
        list.forEachIndexed { index, source ->
            if (source.source == this.source) {
                position = index
            }
        }
        if (position != -1) {
            recyclerView.scrollToPosition(position)
        }
    }

    fun onItemSelect(call: (Source) -> Unit): SourceSelectDialog {
        adapter.setOnItemClickListener { _, _, position ->
            call.invoke(list[position])
            dismiss()
        }
        return this
    }


    inner class Adapter(list: List<Source>) : BaseQuickAdapter<Source,
            BaseViewHolder>(R.layout.item_search_source, list) {
        override fun convert(helper: BaseViewHolder?, item: Source?) {
            val textView = helper?.getView<TextView>(R.id.tv_source)
            textView?.text = item?.name
            if (item?.source == source) {
                textView?.setTextColor(context.color(R.color.text1))
            } else {
                textView?.setTextColor(context.color(R.color.text2))
            }
        }
    }
}