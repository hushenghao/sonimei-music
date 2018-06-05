package com.dede.sonimei.module.setting

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dede.sonimei.NETEASE
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseActivity
import com.dede.sonimei.data.Source
import com.dede.sonimei.sourceList
import kotlinx.android.synthetic.main.activity_select_source.*
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by hsh on 2018/6/5.
 */
class SelectSourceActivity : BaseActivity() {

    override fun getLayoutId() = R.layout.activity_select_source

    override fun initView(savedInstanceState: Bundle?) {
        val selected = defaultSharedPreferences.getInt(Settings.KEY_DEFAULT_SEARCH_SOURCE, NETEASE)

        val adapter = object : BaseQuickAdapter<Source, BaseViewHolder>(R.layout.item_select_source, sourceList) {
            override fun convert(helper: BaseViewHolder?, item: Source?) {
                val radioButton = helper?.getView<RadioButton>(R.id.rb_source)
                radioButton?.text = item?.name
                radioButton?.isChecked = item?.source == selected
            }
        }
        adapter.setOnItemClickListener { _, _, position ->
            val source = sourceList[position]
            if (selected == source.source) return@setOnItemClickListener

            setResult(Activity.RESULT_OK, Intent().putExtra("result", source))
            finish()
        }
        recycler_view.adapter = adapter
    }
}