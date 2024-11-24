package com.example.autotaskapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class AutoTaskDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "auto_task.db";
    private static final int DATABASE_VERSION = 1;

    // 表名及列名常量定义
    public static final String TABLE_AUTOMATED_TASK = "automated_task";
    public static final String COLUMN_TASK_ID = "task_id";
    public static final String COLUMN_ACTION_TO_EXECUTE = "action_to_execute";
    public static final String COLUMN_TRIGGER_CONDITION = "trigger_condition";
    public static final String COLUMN_EXECUTION_TIME = "execution_time";

    public AutoTaskDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建automated_task表的SQL语句
        String createTableQuery = "CREATE TABLE " + TABLE_AUTOMATED_TASK + "("
                + COLUMN_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_ACTION_TO_EXECUTE + "TEXT,"
                + COLUMN_TRIGGER_CONDITION + "INTEGER,"
                + COLUMN_EXECUTION_TIME + "LONG"
                + ")";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 这里可根据数据库版本升级情况添加相应的表结构修改逻辑
        // 例如，如果后续版本需要添加新列等操作，可以在这里实现
    }
}