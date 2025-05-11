package com.sen.mei.autotaskapp.ui;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import com.sen.mei.autotaskapp.R;
import com.sen.mei.autotaskapp.model.AutomatedTask;

import java.text.SimpleDateFormat;
import java.util.Date;

public class NewAutoTaskActivity extends AppCompatActivity {

    private EditText editTextActionToExecute;
    private RadioGroup radioGroupTriggerCondition;
    private TimePicker timePickerExecutionTime;
    private Button buttonSaveTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_auto_task);

        editTextActionToExecute = findViewById(R.id.editTextActionToExecute);
        radioGroupTriggerCondition = findViewById(R.id.radioGroupTriggerCondition);
        timePickerExecutionTime = findViewById(R.id.timePickerExecutionTime);
        buttonSaveTask = findViewById(R.id.buttonSaveTask);

        int taskId = getIntent().getIntExtra("taskId", -1);
        if (taskId != -1) {
            // 如果传入了taskId，说明是编辑任务，加载已有任务信息
            AutomatedTask task = AutomatedTask.queryByTaskId(this, taskId);
            if (task != null) {
                editTextActionToExecute.setText(task.getActionToExecute());
                if (task.getTriggerCondition() == 1) {
                    radioGroupTriggerCondition.check(R.id.type_one);
                } else if (task.getTriggerCondition() == 2) {
                    radioGroupTriggerCondition.check(R.id.type_two);
                }
                Date executionTimeDate = new Date(task.getExecutionTime());
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                timePickerExecutionTime.setCurrentHour(executionTimeDate.getHours());
                timePickerExecutionTime.setCurrentMinute(executionTimeDate.getMinutes());
            }
        }

        buttonSaveTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTask();
            }
        });
    }

    private void saveTask() {
        String actionToExecute = editTextActionToExecute.getText().toString();
        if (TextUtils.isEmpty(actionToExecute)) {
            Log.e("NewAutoTaskActivity", "要执行的动作不能为空");
            return;
        }

        int triggerCondition = radioGroupTriggerCondition.getCheckedRadioButtonId();
        if (triggerCondition == R.id.type_one) {
            triggerCondition = 1;
        } else if (triggerCondition == R.id.type_two) {
            triggerCondition = 2;
        }

        int hour = 0;
        int minute = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hour = timePickerExecutionTime.getHour();
            minute = timePickerExecutionTime.getMinute();
        }else {
            hour = timePickerExecutionTime.getCurrentHour();
            minute = timePickerExecutionTime.getCurrentMinute();
        }

        Date executionTimeDate = new Date();
        executionTimeDate.setHours(hour);
        executionTimeDate.setMinutes(minute);
        long executionTime = executionTimeDate.getTime();

        AutomatedTask task = new AutomatedTask(-1, actionToExecute, triggerCondition, executionTime);

        if (getIntent().getIntExtra("taskId", -1) != -1) {
            // 如果传入了taskId，说明是编辑任务，更新已有任务信息
            task.setTaskId(getIntent().getIntExtra("taskId", -1));
            task.saveToDatabase(this);
            Log.d("NewAutoTaskActivity", "任务已更新");
        } else {
            // 否则是新建任务，保存新任务信息
            task.saveToDatabase(this);
            Log.d("NewAutoTaskActivity", "任务已保存");
        }

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}