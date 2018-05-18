package com.dede.sonimei.module.setting

import android.app.Activity
import android.os.Bundle
import android.widget.CheckBox
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseActivity
import com.dede.sonimei.sourceList
import kotlinx.android.synthetic.main.activity_edit_source.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.find

/**
 * Created by hsh on 2018/5/18.
 */
const val KEY_DISABLE_SOURCE_MAP = "disable_sources"

class EditSourceActivity : BaseActivity() {
    override fun getLayoutId() = R.layout.activity_edit_source

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)

        val disableSet = defaultSharedPreferences
                .getStringSet(KEY_DISABLE_SOURCE_MAP, emptySet()).toMutableSet()

        val adapter = object : BaseQuickAdapter<Triple<Int, String, String>, BaseViewHolder>(
                R.layout.item_edit_source, sourceList) {
            override fun convert(helper: BaseViewHolder?, item: Triple<Int, String, String>?) {
                helper?.getView<CheckBox>(R.id.cb_enable)?.isChecked = !disableSet.contains(item?.third)
                helper?.setText(R.id.tv_source_name, item?.second)
            }
        }
        recycler_view.adapter = adapter

        adapter.setOnItemClickListener { _, view, position ->
            val pair = sourceList[position]
            val checkBox = view.find<CheckBox>(R.id.cb_enable)
            if (!checkBox.isChecked) {
                disableSet.remove(pair.third)
            } else {
                disableSet.add(pair.third)
            }
            checkBox.isChecked = !checkBox.isChecked
            defaultSharedPreferences.edit()
                    .putStringSet(KEY_DISABLE_SOURCE_MAP, disableSet).apply()
        }
    }

    override fun finish() {
        setResult(Activity.RESULT_OK)
        super.finish()
    }
}