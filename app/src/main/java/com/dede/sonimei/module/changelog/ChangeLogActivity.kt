package com.dede.sonimei.module.changelog

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseActivity
import com.dede.sonimei.data.changelog.ChangeLog
import com.dede.sonimei.util.extends.kson.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import kotlinx.android.synthetic.main.activity_change_log.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.startActivity
import java.io.InputStreamReader

/**
 * 更新日志
 */
class ChangeLogActivity : BaseActivity() {

    companion object {
        fun show(context: Context) {
            val sp = context.defaultSharedPreferences
            try {
                val code = sp.getInt("version_code", -1)
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val newCode = packageInfo.versionCode
                if (newCode != code) {
                    context.startActivity<ChangeLogActivity>()
                    sp.edit().putInt("version_code", newCode)
                            .apply()
                }
            } catch (e: PackageManager.NameNotFoundException) {
            }
        }
    }

    override fun getLayoutId() = R.layout.activity_change_log

    override fun initView(savedInstanceState: Bundle?) {
        change_log_list.setAdapter(ChangeLogAdapter())
        change_log_list.expandGroup(0)
    }

    private inner class ChangeLogAdapter : BaseExpandableListAdapter() {

        val list: List<ChangeLog>

        init {
            val inputStream = resources.openRawResource(R.raw.change_log)
            val jsonReader = JsonReader(InputStreamReader(inputStream, "utf-8"))
            list = GsonBuilder()
                    .create()
                    .fromJson(jsonReader)
            jsonReader.close()
        }

        override fun getGroup(groupPosition: Int): ChangeLog {
            return list[groupPosition]
        }

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
            return false
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
            val view = convertView
                    ?: LayoutInflater.from(this@ChangeLogActivity)
                            .inflate(R.layout.item_change_log, parent, false)
            view.findViewById<TextView>(R.id.tv_version).text = getGroup(groupPosition).getTitle()
            return view
        }

        override fun getChildrenCount(groupPosition: Int): Int {
            return getGroup(groupPosition).changeLog.size
        }

        override fun getChild(groupPosition: Int, childPosition: Int): String {
            return getGroup(groupPosition).changeLog[childPosition]
        }

        override fun getGroupId(groupPosition: Int): Long {
            return groupPosition.toLong()
        }

        override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean,
                                  convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@ChangeLogActivity)
                    .inflate(R.layout.item_change_log_text, parent, false)
            view.findViewById<TextView>(R.id.tv_log).text = getChild(groupPosition, childPosition)
            return view
        }

        override fun getChildId(groupPosition: Int, childPosition: Int): Long {
            return (groupPosition * childPosition).toLong()
        }

        override fun getGroupCount(): Int {
            return list.size
        }
    }
}