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
import kotlinx.android.synthetic.main.dialog_play_list.*

/**
 * Created by hsh on 2018/8/8.
 */
class PlayListDialog(context: Context, val list: List<BaseSong>, val index: Int = 0) : BottomSheetDialog(context, R.style.BottomSheetDialog) {

    private lateinit var adapter: Adapter

    init {
        setContentView(R.layout.dialog_play_list)
    }

    var callback: Callback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var params = ll_content.layoutParams
        if (params == null) {
            params = CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        }
        params.height = (context.resources.displayMetrics.heightPixels * .6).toInt()


        adapter = Adapter()
        recycler_view.adapter = adapter
        adapter.setOnItemClickListener { _, _, position ->
            callback?.onItemClick(position, list[position])
            dismiss()
        }

        adapter.setOnItemChildClickListener { _, view, position ->
            when (view.id) {
                R.id.iv_delete -> {
                    callback?.onItemRemove(position, list[position])
                    adapter.remove(position)
                    tv_list_count.text = context.getString(R.string.play_list_count, adapter.itemCount)
//                    dismiss()
                }
            }
        }

        tv_list_count.text = context.getString(R.string.play_list_count, adapter.itemCount)
    }

    inner class Adapter : BaseQuickAdapter<BaseSong, BaseViewHolder>(R.layout.item_play_list, list) {
        override fun convert(helper: BaseViewHolder?, item: BaseSong?) {
            val playing = helper!!.layoutPosition == index
            helper.setGone(R.id.iv_playing, playing)
                    .setText(R.id.tv_content, item?.getName())
                    .setTextColor(R.id.tv_content, context.color(R.color.text1))
                    .addOnClickListener(R.id.iv_delete)
        }
    }

    interface Callback {
        fun onItemClick(index: Int, baseSong: BaseSong)
        fun onItemRemove(index: Int, baseSong: BaseSong)
    }

}