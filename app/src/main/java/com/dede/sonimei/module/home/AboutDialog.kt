package com.dede.sonimei.module.home

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AlertDialog
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.dede.sonimei.GITHUB_LINK
import com.dede.sonimei.GROUP_LINK
import com.dede.sonimei.R
import com.dede.sonimei.component.LinkTagClickableSpan

/**
 * Created by hsh on 2018/6/28.
 */
class AboutDialog(context: Context) {

    private val dialog: AlertDialog

    init {
        dialog = AlertDialog.Builder(context)
                .setView(createView(context))
                .setNegativeButton(R.string.sure, null)
                .setNeutralButton(R.string.about_market) { _, _ ->
                    context.startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")),
                            context.getString(R.string.chooser_market)))
                }
                .create()
    }

    private fun createView(context: Context): View {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_about, null)
        val tvVersion = view.findViewById<TextView>(R.id.tv_version)
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            tvVersion.text = String.format(tvVersion.text.toString(),
                    packageInfo.versionName,
                    packageInfo.versionCode)
        } catch (e: PackageManager.NameNotFoundException) {
        }

        val tvGithub = view.findViewById<TextView>(R.id.tv_github)
        tvGithub.tag = GITHUB_LINK
        val github = SpannableString(tvGithub.text)
        github.setSpan(LinkTagClickableSpan(), 7, github.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvGithub.movementMethod = LinkMovementMethod.getInstance()
        tvGithub.text = github

        val tvQQGroup = view.findViewById<TextView>(R.id.tv_group)
        tvQQGroup.tag = GROUP_LINK
        val qqGroup = SpannableString(tvQQGroup.text)
        qqGroup.setSpan(LinkTagClickableSpan(false), 6, qqGroup.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvQQGroup.movementMethod = LinkMovementMethod.getInstance()
        tvQQGroup.text = qqGroup

        return view
    }

    fun show() {
        if (dialog.isShowing) return

        dialog.show()
    }
}