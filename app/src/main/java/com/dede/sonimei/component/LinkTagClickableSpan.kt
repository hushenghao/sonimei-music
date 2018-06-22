package com.dede.sonimei.component

import android.content.Intent
import android.net.Uri
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View

/**
 * Created by hsh on 2018/6/22.
 */
class LinkTagClickableSpan(private val underLine: Boolean = true) : ClickableSpan() {

    override fun onClick(widget: View?) {
        val link = widget?.tag
        val uri: Uri = when (link) {
            is String -> Uri.parse(link)
            is Uri -> link
            else -> null
        } ?: return
        try {
            widget!!.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.color = ds.linkColor
        ds.isUnderlineText = underLine
    }

}