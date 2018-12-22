package com.dede.sonimei.module.home

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dede.sonimei.*
import com.dede.sonimei.data.Source
import com.dede.sonimei.util.extends.color
import com.dede.sonimei.util.extends.hide
import com.dede.sonimei.util.extends.show
import kotlinx.android.synthetic.main.dialog_source_type.*

/**
 * Created by hsh on 2018/5/23.
 */
class SourceTypeDialog(context: Context, val data: Pair<Int, String>) :
        BottomSheetDialog(context, R.style.BottomSheetDialog),
        CompoundButton.OnCheckedChangeListener,
        DialogInterface.OnDismissListener {

    override fun onDismiss(dialog: DialogInterface?) {
        if (data.toString() == selectData.toString()) return
        callback?.invoke(selectData)
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (!isChecked) return
        selectData = this.selectData.first to
                when (buttonView?.id) {
                    R.id.rb_search_name -> SEARCH_NAME
                    R.id.rb_search_id -> SEARCH_ID
                    R.id.rb_search_url -> SEARCH_URL
                    else -> SEARCH_NAME
                }
        dismiss()
    }

    private val recyclerView: RecyclerView
    private val adapter: Adapter
    private val list = sourceList
    private var selectData = data.first to data.second
    private var selectPosition: Int

    init {
        setContentView(R.layout.dialog_source_type)
        recyclerView = findViewById(R.id.source_list)!!
        adapter = Adapter(list)

        val header = LayoutInflater.from(context).inflate(R.layout.layout_select_dialog_header, null)
        adapter.addHeaderView(header)

        val rbSearchName = header.findViewById<RadioButton>(R.id.rb_search_name)
        val rbSearchId = header.findViewById<RadioButton>(R.id.rb_search_id)
        val rbSearchUrl = header.findViewById<RadioButton>(R.id.rb_search_url)
        when (data.second) {
            SEARCH_NAME -> {
                rbSearchName.isChecked = true
            }
            SEARCH_ID -> {
                rbSearchId.isChecked = true
            }
            SEARCH_URL -> {
                rbSearchUrl.isChecked = true
            }
        }
        rbSearchName.setOnCheckedChangeListener(this)
        rbSearchId.setOnCheckedChangeListener(this)
        rbSearchUrl.setOnCheckedChangeListener(this)

//        val behavior = BottomSheetBehavior
//                .from(findViewById<View>(android.support.design.R.id.design_bottom_sheet))

        selectPosition = 0
        list.forEachIndexed { index, source ->
            if (source.source == this.selectData.first) {
                selectPosition = index
            }
        }
        recyclerView.adapter = adapter

        adapter.setOnItemClickListener { _, _, p ->
            if (p == selectPosition) return@setOnItemClickListener
            selectPosition = p
            selectData = list[p].source to this.selectData.second
            adapter.notifyDataSetChanged()
            dismiss()
        }

        setOnDismissListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var params = ll_content.layoutParams
        if (params == null) {
            params = CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        }
        params.height = (context.resources.displayMetrics.heightPixels * .6).toInt()
    }

    private var callback: ((Pair<Int, String>) -> Unit)? = null

    fun callback(callback: (Pair<Int, String>) -> Unit): SourceTypeDialog {
        this.callback = callback
        return this
    }

    inner class Adapter(list: List<Source>) : BaseQuickAdapter<Source,
            BaseViewHolder>(R.layout.item_search_source, list) {
        override fun convert(helper: BaseViewHolder?, item: Source?) {
            val textView = helper!!.getView<TextView>(R.id.tv_source)
            textView.text = item?.name
            if (helper.layoutPosition - this.headerLayoutCount == selectPosition) {
                helper.getView<ImageView>(R.id.iv_done).show()
            } else {
                helper.getView<ImageView>(R.id.iv_done).hide()
            }
            textView.setTextColor(context.color(R.color.text1))
        }
    }
}