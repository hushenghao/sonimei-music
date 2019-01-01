package com.dede.sonimei.module.opensource

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseActivity
import com.dede.sonimei.data.opensource.OpenSource
import com.dede.sonimei.util.extends.kson.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import kotlinx.android.synthetic.main.activity_recycler_view.*
import java.io.InputStreamReader

/**
 * open source list
 */
class OpenSourceActivity : BaseActivity() {

    override fun getLayoutId() = R.layout.activity_recycler_view

    override fun initView(savedInstanceState: Bundle?) {
        val inputStream = resources.openRawResource(R.raw.open_source)
        val jsonReader = JsonReader(InputStreamReader(inputStream, "utf-8"))
        val list = GsonBuilder()
                .create()
                .fromJson<List<OpenSource>>(jsonReader)
        jsonReader.close()
        val adapter = object : BaseQuickAdapter<OpenSource, BaseViewHolder>(R.layout.item_open_source, list) {
            override fun convert(helper: BaseViewHolder?, item: OpenSource?) {
                helper?.setText(R.id.tv_title, item?.title)
                        ?.setText(R.id.tv_author, item?.author)
                        ?.setText(R.id.tv_desc, item?.desc)
            }
        }
        adapter.setOnItemClickListener { _, _, position ->
            val source = list[position]
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(source.link)
            startActivity(Intent.createChooser(intent, getString(R.string.chooser_browser)))
        }
        recycler_view.adapter = adapter

    }
}