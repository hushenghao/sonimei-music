package com.dede.sonimei.module.setting

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import com.dede.sonimei.util.FileUtils
import java.io.File


/**
 * Created by hsh on 2018/5/17.
 */
object SettingHelper {

    /**
     * Gets the corresponding path to a file from the given content:// URI
     * @param context The content resolver to use to perform the query.
     * @param uri The content:// URI to find the file path from
     * @return the file path as a string
     */
    fun contentUri2Path(context: Context, uri: Uri): String {
        val filePathColumn = arrayOf(MediaColumns.DATA)

        val cursor = context.contentResolver.query(uri, filePathColumn, null, null, null)

        cursor.moveToFirst()

        val columnIndex = cursor.getColumnIndex(filePathColumn[0])
        val filePath = cursor.getString(columnIndex)
        cursor.close()
        return filePath
    }


    fun documentUri2Path(context: Context, uri: Uri): String? {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            contentUri2Path(context, uri)
        } else {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(uri,
                    DocumentsContract.getTreeDocumentId(uri))
            getPath(context, docUri)
        }
    }

    private fun getPath(context: Context, uri: Uri): String? {

        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    if (split.size < 2) {
                        return Environment.getExternalStorageDirectory().absolutePath
                    }
                    return Environment.getExternalStorageDirectory().absolutePath + File.separator + split[1]
                } else {
                    // 其他挂载点
                    val storageList = FileUtils.getStorageList(context)
                    storageList
                            .filter { it.path?.contains(type) ?: false }
                            .forEach {
                                if (split.size < 2) {
                                    return it.path
                                }
                                return it.path + File.separator + split[1]
                            }
                }

            } else if (isDownloadsDocument(uri)) {
                val idStr = DocumentsContract.getDocumentId(uri)
                return try {
                    val id = idStr.toLong()
                    val contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), id)
                    getDataColumn(context, contentUri, null, null)
                } catch (e: NumberFormatException) {
                    null
                }
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(context, contentUri, selection, selectionArgs)
            }// MediaProvider
            // DownloadsProvider
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {

            // Return the remote address
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, null, null)

        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }// File
        // MediaStore (and general)

        return null
    }

    private fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                              selectionArgs: Array<String>?): String? {

        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            if (cursor != null)
                cursor.close()
        }
        return null
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }
}