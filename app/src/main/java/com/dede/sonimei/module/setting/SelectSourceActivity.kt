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
import com.dede.sonimei.normalSource
import com.dede.sonimei.sourceList
import com.dede.sonimei.util.extends.color
import kotlinx.android.synthetic.main.activity_select_source.*
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by hsh on 2018/6/5.
 */
class SelectSourceActivity : BaseActivity() {

    override fun getLayoutId() = R.layout.activity_select_source

    override fun initView(savedInstanceState: Bundle?) {
        val selected = defaultSharedPreferences.getInt(Settings.KEY_DEFAULT_SEARCH_SOURCE, normalSource)

        val adapter = object : BaseQuickAdapter<Source, BaseViewHolder>(R.layout.item_search_source, sourceList) {
            override fun convert(helper: BaseViewHolder?, item: Source?) {
                val s = item?.source == selected
                helper!!.setText(R.id.tv_source, item?.name)
                        .setTextColor(R.id.tv_source, if (s) color(R.color.text1) else color(R.color.text2))
                        .setVisible(R.id.iv_done, s)
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