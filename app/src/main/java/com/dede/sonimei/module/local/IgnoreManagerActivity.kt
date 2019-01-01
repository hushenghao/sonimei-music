package com.dede.sonimei.module.local

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dede.sonimei.R
import com.dede.sonimei.base.BaseActivity
import com.dede.sonimei.module.selector.EXTRA_SELECT_TYPE
import com.dede.sonimei.module.selector.FILE_SELECT_PATH
import com.dede.sonimei.module.selector.FileSelectorActivity
import com.dede.sonimei.module.selector.RESULT_FILE_PATH
import com.dede.sonimei.module.setting.Settings.Companion.KEY_IGNORE_PATHS
import kotlinx.android.synthetic.main.activity_ignore_manager.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.startActivityForResult
import java.io.File

/**
 * 本地音频忽略管理
 */
class IgnoreManagerActivity : BaseActivity() {

    private val sp by lazy { defaultSharedPreferences }

    override fun getLayoutId() = R.layout.activity_ignore_manager

    private val selectedPath = 100

    override fun initView(savedInstanceState: Bundle?) {

        val view = LayoutInflater.from(this)
                .inflate(R.layout.item_header_add_path, recycler_view, false)
        view.setOnClickListener {
            startActivityForResult<FileSelectorActivity>(selectedPath, EXTRA_SELECT_TYPE to FILE_SELECT_PATH)
        }

        val pathAdapter = PathAdapter()
        pathAdapter.addFooterView(view)
        val set = sp.getStringSet(KEY_IGNORE_PATHS, null) ?: emptySet()
        pathAdapter.setNewData(set.map { File(it) }.toMutableList())
        recycler_view.adapter = pathAdapter
        pathAdapter.setOnItemChildClickListener { adapter, _, position ->
            adapter.remove(position)
            sp.edit().putStringSet(KEY_IGNORE_PATHS, pathAdapter.data.map { it.absolutePath }.toSet())
                    .apply()
        }

        fab_done.setOnClickListener {
            LocalMusicFragment.sendBroadcast(this)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || requestCode != selectedPath || data == null) {
            return
        }
        val path = data.getStringExtra(RESULT_FILE_PATH)
        val pathAdapter = recycler_view.adapter as? PathAdapter ?: return
        pathAdapter.addData(File(path))
        sp.edit().putStringSet(KEY_IGNORE_PATHS, pathAdapter.data.map { it.absolutePath }.toSet())
                .apply()
    }

    private inner class PathAdapter : BaseQuickAdapter<File, BaseViewHolder>(R.layout.item_ignore_path) {
        override fun convert(helper: BaseViewHolder?, item: File?) {
            helper?.setText(R.id.tv_name, item?.name)
                    ?.setText(R.id.tv_abs_path, item?.absolutePath)
                    ?.addOnClickListener(R.id.iv_del)
        }
    }
}