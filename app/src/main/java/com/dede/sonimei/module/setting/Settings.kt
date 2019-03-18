package com.dede.sonimei.module.setting

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.support.v7.app.AlertDialog
import com.dede.sonimei.*
import com.dede.sonimei.data.Source
import com.dede.sonimei.module.home.AboutDialog
import com.dede.sonimei.module.local.LocalMusicFragment
import com.dede.sonimei.module.selector.*
import com.dede.sonimei.util.extends.Logger
import com.dede.sonimei.util.extends.info
import com.dede.sonimei.util.extends.isNull
import com.tbruyelle.rxpermissions2.RxPermissions
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import java.io.File
import java.io.FileFilter


/**
 * Created by hsh on 2018/5/17.
 */
class Settings : PreferenceFragment(),
        Logger,
        Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    companion object {
        const val KEY_CUSTOM_PATH = "custom_path"
        const val KEY_WIFI_DOWNLOAD = "wifi_download"
        const val KEY_DEFAULT_SEARCH_SOURCE = "default_search_source"
        const val KEY_BUG_REPORT = "bug_report"
        const val KEY_QQ_GROUP = "qq_group"
        const val KEY_IGNORE_60S = "ignore_60s"
        const val KEY_IGNORE_PATHS = "ignore_paths"
    }

    private val selectPathCode = 1
    private val selectSourceCode = 2

    override fun onPreferenceClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            KEY_DEFAULT_SEARCH_SOURCE -> {
                startActivityForResult<SelectSourceActivity>(selectSourceCode)
                true
            }
            KEY_CUSTOM_PATH -> {
                val path = defaultSharedPreferences
                        .getString(KEY_CUSTOM_PATH, defaultDownloadPath.absolutePath)
                startActivityForResult<FileSelectorActivity>(selectPathCode,
                        EXTRA_SELECT_TYPE to FILE_SELECT_PATH,
                        EXTRA_INIT_PATH to path)
                true
            }
            KEY_BUG_REPORT -> {
                AlertDialog.Builder(activity)
                        .setTitle(R.string.emile_theme)
                        .setMessage(R.string.dialog_bug_report)
                        .setNegativeButton(R.string.dont_send) { _, _ -> sendEmail(false) }
                        .setPositiveButton(R.string.do_send) { _, _ -> sendEmail(true) }
                        .create()
                        .show()
                true
            }
            KEY_QQ_GROUP -> {
                AboutDialog(activity).show()
                true
            }
            else -> false
        }
    }

    @SuppressLint("CheckResult")
    private fun sendEmail(b: Boolean) {
        if (!b) {
            callEmail()
            return
        }
        RxPermissions(activity)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe({
                    if (!it) {
                        callEmail()
                        return@subscribe
                    }
                    var file: File? = null
                    val dir = CrashHandler.instance().crashLogDir()
                    if (dir.exists() && dir.isDirectory) {
                        val files = dir.listFiles(FileFilter { f ->
                            return@FileFilter CrashHandler.instance().isLog(f)
                        })
                        if (files?.isNotEmpty() == true) {
                            files.sortByDescending { f -> f.lastModified() }// 按时间倒序
                            file = files[0]// 取最新的
                        }
                    }
                    callEmail(file)
                }, { it.printStackTrace() })
    }

    private fun callEmail(file: File? = null) {
        val email = getString(R.string.email)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "message/rfc882"
        val of = arrayOf(email)
        intent.putExtra(Intent.EXTRA_EMAIL, of)
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.emile_theme)) // 主题
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_text)) // 正文
        if (file != null) {
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.email_chooser)))
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        when (preference?.key) {
            KEY_IGNORE_60S -> {
                LocalMusicFragment.sendBroadcast(activity)
                return true
            }
        }
        return false
    }

    private lateinit var defaultSearchSource: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preference_settings)

        defaultSearchSource = findPreference(KEY_DEFAULT_SEARCH_SOURCE)// 默认搜索来源
        val source = defaultSharedPreferences.getInt(KEY_DEFAULT_SEARCH_SOURCE, normalSource)
        defaultSearchSource.summary = sourceName(source)
        defaultSearchSource.onPreferenceClickListener = this

        val customPath = findPreference(KEY_CUSTOM_PATH)// 自定义下载路径，5.0以下不可用，隐藏设置项
        customPath.onPreferenceClickListener = this
        customPath.summary = defaultSharedPreferences
                .getString(KEY_CUSTOM_PATH, defaultDownloadPath.absolutePath)
        customPath.onPreferenceChangeListener = this
        findPreference(KEY_BUG_REPORT).onPreferenceClickListener = this
        findPreference(KEY_QQ_GROUP).onPreferenceClickListener = this
        findPreference(KEY_IGNORE_60S).onPreferenceChangeListener = this
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) return
        when (requestCode) {
            selectSourceCode -> {
                val source = data.getSerializableExtra("result") as Source?
                if (source != null) {
                    defaultSearchSource.summary = source.name
                    defaultSharedPreferences.edit()
                            .putInt(KEY_DEFAULT_SEARCH_SOURCE, source.source)
                            .apply()
                }
            }
            selectPathCode -> {
                val path = data.getStringExtra(RESULT_FILE_PATH)
                info(path)
                if (path.isNull() || !File(path).canRead()) {
                    toast("路径不可用")
                    return
                }
                defaultSharedPreferences.edit()
                        .putString(KEY_CUSTOM_PATH, path)
                        .apply()
                findPreference(KEY_CUSTOM_PATH).summary = defaultSharedPreferences
                        .getString(KEY_CUSTOM_PATH, defaultDownloadPath.absolutePath)
            }
        }
    }

}