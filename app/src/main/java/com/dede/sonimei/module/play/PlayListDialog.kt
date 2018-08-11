package com.dede.sonimei.module.play

import android.content.Context
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.LinearLayout
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dede.sonimei.R
import com.dede.sonimei.data.BaseSong
import com.dede.sonimei.util.extends.color

/**
 * Created by hsh on 2018/8/8.
 */
class PlayListDialog(context: Context, val list: List<BaseSong>, val index: Int = 0) : BottomSheetDialog(context, R.style.BottomSheetDialog) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: Adapter

    init {
        setContentView(R.layout.dialog_play_list)
    }

    var callback: Callback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = findViewById<LinearLayout>(R.id.ll_content)
        var params = layout!!.layoutParams
        if (params == null) {
            params = CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        }
        params.height = (context.resources.displayMetrics.heightPixels * .6).toInt()

        recyclerView = findViewById(R.id.recycler_view)!!

        adapter = Adapter()
        recyclerView.adapter = adapter
        adapter.setOnItemClickListener { _, _, position ->
            callback?.onItemClick(position, list[position])
            dismiss()
        }

        adapter.setOnItemChildClickListener { _, view, position ->
            when (view.id) {
                R.id.iv_delete -> {
                    callback?.onItemRemove(position, list[position])
                    dismiss()
                }
            }
        }

    }

    inner class Adapter : BaseQuickAdapter<BaseSong, BaseViewHolder>(R.layout.item_search_his, list) {
        override fun convert(helper: BaseViewHolder?, item: BaseSong?) {
            helper!!.setText(R.id.tv_query, item?.getName())
                    .setTextColor(R.id.tv_query,
                            if (helper.layoutPosition == index)
                                context.color(R.color.text1)
                            else
                                context.color(R.color.text2))
                    .addOnClickListener(R.id.iv_delete)
        }
    }

    interface Callback {
        fun onItemClick(index: Int, baseSong: BaseSong)
        fun onItemRemove(index: Int, baseSong: BaseSong)
    }

}