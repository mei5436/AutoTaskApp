package com.sen.mei.autotaskapp.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.sen.mei.autotaskapp.database.AutoTaskDatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AutomatedTask {
    private int taskId;
    // 存储要执行的用户动作记录文件名称
    private String actionToExecute;
    // 任务的触发条件类型，1表示一次性定时任务，2表示重复性定时任务
    private int triggerCondition;
    // 存储任务的执行时间，以时间戳形式表示（单位：毫秒）
    private long executionTime;

    // 构造函数
    public AutomatedTask(int taskId, String actionToExecute, int triggerCondition, long executionTime) {
        this.taskId = taskId;
        this.actionToExecute = actionToExecute;
        this.triggerCondition = triggerCondition;
        this.executionTime = executionTime;
    }

    // Getter和Setter方法
    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getActionToExecute() {
        return actionToExecute;
    }

    public void setActionToExecute(String actionToExecute) {
        this.actionToExecute = actionToExecute;
    }

    public int getTriggerCondition() {
        return triggerCondition;
    }

    public void setTriggerCondition(int triggerCondition) {
        this.triggerCondition = triggerCondition;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    // 将AutomatedTask保存到数据库的方法
    public void saveToDatabase(Context context) {
        AutoTaskDatabaseHelper databaseHelper = new AutoTaskDatabaseHelper(context);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(AutoTaskDatabaseHelper.COLUMN_ACTION_TO_EXECUTE, actionToExecute);
        values.put(AutoTaskDatabaseHelper.COLUMN_TRIGGER_CONDITION, triggerCondition);
        values.put(AutoTaskDatabaseHelper.COLUMN_EXECUTION_TIME, executionTime);

        long rowId = db.insert(AutoTaskDatabaseHelper.TABLE_AUTOMATED_TASK, null, values);
        if (rowId!= -1) {
            Log.d("AutomatedTask", "保存成功，行ID: " + rowId);
        } else {
            Log.e("AutomatedTask", "保存失败");
        }

        db.close();
    }

    // 从数据库根据taskId查询AutomatedTask的方法
    public static AutomatedTask queryByTaskId(Context context, int taskId) {
        AutoTaskDatabaseHelper databaseHelper = new AutoTaskDatabaseHelper(context);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String[] columns = {
                AutoTaskDatabaseHelper.COLUMN_TASK_ID,
                AutoTaskDatabaseHelper.COLUMN_ACTION_TO_EXECUTE,
                AutoTaskDatabaseHelper.COLUMN_TRIGGER_CONDITION,
                AutoTaskDatabaseHelper.COLUMN_EXECUTION_TIME
        };
        String selection = AutoTaskDatabaseHelper.COLUMN_TASK_ID + " =?";
        String[] selectionArgs = {String.valueOf(taskId)};

        Cursor cursor = db.query(AutoTaskDatabaseHelper.TABLE_AUTOMATED_TASK, columns, selection, selectionArgs, null, null, null);

        AutomatedTask task = null;
        if (cursor.moveToFirst()) {
            int retrievedTaskId = cursor.getInt(cursor.getColumnIndex(AutoTaskDatabaseHelper.COLUMN_TASK_ID));
            String retrievedActionToExecute = cursor.getString(cursor.getColumnIndex(AutoTaskDatabaseHelper.COLUMN_ACTION_TO_EXECUTE));
            int retrievedTriggerCondition = cursor.getInt(cursor.getColumnIndex(AutoTaskDatabaseHelper.COLUMN_TRIGGER_CONDITION));
            long retrievedExecutionTime = cursor.getLong(cursor.getColumnIndex(AutoTaskDatabaseHelper.COLUMN_EXECUTION_TIME));

            task = new AutomatedTask(retrievedTaskId, retrievedActionToExecute, retrievedTriggerCondition, retrievedExecutionTime);
        }

        cursor.close();
        db.close();

        return task;
    }

    // 从数据库根据actionToExecute查询AutomatedTask的方法
    public static AutomatedTask queryByActionToExecute(Context context, String actionToExecute) {
        AutoTaskDatabaseHelper databaseHelper = new AutoTaskDatabaseHelper(context);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String[] columns = {
                AutoTaskDatabaseHelper.COLUMN_TASK_ID,
                AutoTaskDatabaseHelper.COLUMN_ACTION_TO_EXECUTE,
                AutoTaskDatabaseHelper.COLUMN_TRIGGER_CONDITION,
                AutoTaskDatabaseHelper.COLUMN_EXECUTION_TIME
        };
        String selection = AutoTaskDatabaseHelper.COLUMN_ACTION_TO_EXECUTE + " =?";
        String[] selectionArgs = {actionToExecute};

        Cursor cursor = db.query(AutoTaskDatabaseHelper.TABLE_AUTOMATED_TASK, columns, selection, selectionArgs, null, null, null);

        AutomatedTask task = null;
        if (cursor.moveToFirst()) {
            int retrievedTaskId = cursor.getInt(cursor.getColumnIndex(AutoTaskDatabaseHelper.COLUMN_TASK_ID));
            String retrievedActionToExecute = cursor.getString(cursor.getColumnIndex(AutoTaskDatabaseHelper.COLUMN_ACTION_TO_EXECUTE));
        }

        cursor.close();
        db.close();

        return task;
    }

    // 查询数据库获取所有AutomatedTask数据的方法
    public static List<AutomatedTask> queryAllTasks(Context context) {
        List<AutomatedTask> taskList = new ArrayList<>();
        AutoTaskDatabaseHelper databaseHelper = new AutoTaskDatabaseHelper(context);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor cursor = db.query(
                AutoTaskDatabaseHelper.TABLE_AUTOMATED_TASK,
                null,
                null,
                null,
                null,
                null,
                null
        );

        if (cursor!= null && cursor.moveToFirst()) {
            do {
                int taskId = cursor.getInt(cursor.getColumnIndex(AutoTaskDatabaseHelper.COLUMN_TASK_ID));
                String actionToExecute = cursor.getString(cursor.getColumnIndex(AutoTaskDatabaseHelper.COLUMN_ACTION_TO_EXECUTE));
                int triggerCondition = cursor.getInt(cursor.getColumnIndex(AutoTaskDatabaseHelper.COLUMN_TRIGGER_CONDITION));
                long executionTime = cursor.getLong(cursor.getColumnIndex(AutoTaskDatabaseHelper.COLUMN_EXECUTION_TIME));

                AutomatedTask task = new AutomatedTask(taskId, actionToExecute, triggerCondition, executionTime);
                taskList.add(task);
            } while (cursor.moveToNext());

            cursor.close();
        }

        db.close();

        return taskList;
    }

    // 将执行时间戳转换为易读的日期时间格式
    public String getExecutionTimeFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(executionTime);
        return sdf.format(date);
    }
}