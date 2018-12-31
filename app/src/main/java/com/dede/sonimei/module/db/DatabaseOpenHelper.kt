package com.dede.sonimei.module.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*

/**
 * Created by hsh on 2018/6/6.
 */
class DatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "sonimei_db", null, 1) {
    companion object {
        private var instance: DatabaseOpenHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): DatabaseOpenHelper {
            if (instance == null) {
                instance = DatabaseOpenHelper(ctx.applicationContext)
            }
            return instance!!
        }

        const val TABLE_SEARCH_HIS = "search_his"
        const val COLUMNS_ID = "_id"
        const val COLUMNS_TEXT = "text"
        const val COLUMNS_TIMESTAMP = "times_tamp"

//        const val TABLE_LOCAL_MUSIC = "local_music"
//        const val COLUMNS_MUSIC_ID = "_id"
//        const val COLUMNS_MUSIC_TITLE = "title"
//        const val COLUMNS_MUSIC_AUTHOR = "author"
//        const val COLUMNS_MUSIC_ALBUM = "album"
//        const val COLUMNS_MUSIC_DURATION = "duration"
//        const val COLUMNS_MUSIC_PATH = "path"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(TABLE_SEARCH_HIS, true,
                COLUMNS_ID to INTEGER + PRIMARY_KEY + UNIQUE,
                COLUMNS_TEXT to TEXT + NOT_NULL,
                // 默认值时间戳，单位为秒
                COLUMNS_TIMESTAMP to SqlType.create("TIMESTAMP NOT NULL DEFAULT (strftime('%s',datetime('now')))")
        )
//        db.createTable(TABLE_LOCAL_MUSIC, true,
//                COLUMNS_MUSIC_ID to INTEGER + PRIMARY_KEY + UNIQUE,
//                COLUMNS_MUSIC_TITLE to TEXT + NOT_NULL,
//                COLUMNS_MUSIC_AUTHOR to TEXT,
//                COLUMNS_MUSIC_ALBUM to TEXT,
//                COLUMNS_MUSIC_DURATION to INTEGER,
//                COLUMNS_MUSIC_PATH to TEXT + NOT_NULL
//        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
//        if (newVersion >= 2) {// 2 版本添加了本地音乐数据表
//            db.createTable(TABLE_LOCAL_MUSIC, true,
//                    COLUMNS_MUSIC_ID to INTEGER + PRIMARY_KEY + UNIQUE,
//                    COLUMNS_MUSIC_TITLE to TEXT + NOT_NULL,
//                    COLUMNS_MUSIC_AUTHOR to TEXT,
//                    COLUMNS_MUSIC_ALBUM to TEXT,
//                    COLUMNS_MUSIC_DURATION to INTEGER,
//                    COLUMNS_MUSIC_PATH to TEXT + NOT_NULL
//            )
//        }
    }
}

// Access property for Context
val Context.database: DatabaseOpenHelper
    get() = DatabaseOpenHelper.getInstance(applicationContext)
val Context.db: SQLiteDatabase
    get() = database.writableDatabase