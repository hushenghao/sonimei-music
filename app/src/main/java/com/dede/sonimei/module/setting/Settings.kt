package com.dede.sonimei.module.setting

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import com.dede.sonimei.R
import com.dede.sonimei.defaultDownloadPath
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.info
import org.jetbrains.anko.startActivityForResult

/**
 * Created by hsh on 2018/5/17.
 */
const val KEY_CUSTOM_PATH = "custom_path"
const val KEY_WIFI_DOWNLOAD = "wifi_download"
const val KEY_EDIT_SOURCE = "edit_source"

const val EDIT_SOURCE_CODE = 2

class Settings : PreferenceFragment(),
        AnkoLogger,
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    private val selectPathCode = 1

    override fun onPreferenceClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            KEY_CUSTOM_PATH -> {
                if (!isLollipop) {
                    false
                }
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                try {
                    startActivityForResult(Intent.createChooser(intent, "选择文件路径"),
                            selectPathCode)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
                true
            }
            KEY_EDIT_SOURCE -> {
                activity.startActivityForResult<EditSourceActivity>(EDIT_SOURCE_CODE)
                true
            }
            else -> false
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            KEY_WIFI_DOWNLOAD -> {

            }
            KEY_CUSTOM_PATH -> {
                findPreference(KEY_CUSTOM_PATH).summary = defaultSharedPreferences
                        .getString(KEY_CUSTOM_PATH, defaultDownloadPath.absolutePath)
            }
        }
    }

    private val isLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preference_settings)

        val customPath = findPreference(KEY_CUSTOM_PATH)
        if (!isLollipop) {
            preferenceScreen.removePreference(customPath)
        } else {
            customPath.onPreferenceClickListener = this
            customPath.summary = defaultSharedPreferences
                    .getString(KEY_CUSTOM_PATH, defaultDownloadPath.absolutePath)
        }
        findPreference(KEY_EDIT_SOURCE).onPreferenceClickListener = this
    }

    override fun onResume() {
        super.onResume()
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) return
        when (requestCode) {
            selectPathCode -> {
                val uri = data.data
                info(uri.toString())
                val path = SettingHelper.documentUri2Path(activity, uri)
                info(path)
                defaultSharedPreferences.edit()
                        .putString(KEY_CUSTOM_PATH, path)
                        .apply()
                // 这时候activity不可见，所以onSharedPreferenceChanged不会回调
                findPreference(KEY_CUSTOM_PATH).summary = defaultSharedPreferences
                        .getString(KEY_CUSTOM_PATH, defaultDownloadPath.absolutePath)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}