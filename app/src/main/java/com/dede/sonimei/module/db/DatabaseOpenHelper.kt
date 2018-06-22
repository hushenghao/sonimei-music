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
        const val COLUMNS_TIMESTAMP = "time_stamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(TABLE_SEARCH_HIS, true,
                COLUMNS_ID to INTEGER + PRIMARY_KEY + UNIQUE,
                COLUMNS_TEXT to TEXT + NOT_NULL,
                COLUMNS_TIMESTAMP to SqlType.create("TimeStamp NOT NULL DEFAULT (datetime('now','localtime'))"),
                "PRIMARY" to SqlType.create("KEY($COLUMNS_ID,$COLUMNS_TEXT)")// id和text作为复合主键
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Here you can upgrade tables, as usual
//        db.dropTable("User", true)
    }
}

// Access property for Context
val Context.database: DatabaseOpenHelper
    get() = DatabaseOpenHelper.getInstance(applicationContext)
val Context.db: SQLiteDatabase
    get() = database.writableDatabase