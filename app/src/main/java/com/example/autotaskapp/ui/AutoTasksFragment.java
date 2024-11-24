package com.example.autotaskapp.ui;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autotaskapp.R;
import com.example.autotaskapp.database.AutoTaskDatabaseHelper;
import com.example.autotaskapp.model.AutomatedTask;
import com.example.autotaskapp.service.AutoAppAccessibilityService;

import java.util.ArrayList;
import java.util.List;

public class AutoTasksFragment extends Fragment {

    private RecyclerView recyclerViewAutoTasks;
    private AutoTasksAdapter adapter;
    private List<AutomatedTask> automatedTasks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_auto_tasks, container, false);
        recyclerViewAutoTasks = view.findViewById(R.id.recyclerViewAutoTasks);

        // 设置RecyclerView的布局管理器
        recyclerViewAutoTasks.setLayoutManager(new LinearLayoutManager(getContext()));

        // 创建并设置Adapter
        adapter = new AutoTasksAdapter(automatedTasks, getContext());
        recyclerViewAutoTasks.setAdapter(adapter);

        // 初始化数据，查询数据库获取所有自动任务
        initData();

        // 设置FloatingActionButton的拍击事件，跳转到新建自动任务界面
        ImageButton floatingActionButton = view.findViewById(R.id.floatingActionButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), NewAutoTaskActivity.class);
                startActivity(intent);
            }
        });

        setHasOptionsMenu(true);

        return view;
    }

    private void initData() {
        automatedTasks = AutomatedTask.queryAllTasks(getContext());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 重新查询数据库刷新数据
        initData();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_record_actions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==R.id.menu_item_clear_all){
            clearAllAutomatedTasks();
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    private void clearAllAutomatedTasks() {
        // 获取数据库帮助类实例
        AutoTaskDatabaseHelper databaseHelper = new AutoTaskDatabaseHelper(getContext());
        // 获取可写数据库对象
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        // 删除automated_task表中的所有记录
        db.delete(AutoTaskDatabaseHelper.TABLE_AUTOMATED_TASK, null, null);

        db.close();

        automatedTasks.clear();
        adapter.notifyDataSetChanged();

        Toast.makeText(getContext(), "已清除所有自动任务", Toast.LENGTH_SHORT).show();
    }

    private class AutoTasksAdapter extends RecyclerView.Adapter<AutoTasksAdapter.ActionViewHolder> {

        private List<AutomatedTask> taskList;
        private Context context;

        public AutoTasksAdapter(List<AutomatedTask> taskList, Context context) {
            this.taskList = taskList;
            this.context = context;
        }

        @NonNull
        @Override
        public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_auto_task, parent, false);
            return new ActionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
            AutomatedTask task = taskList.get(position);
            holder.textViewTitle.setText(getTitleForTask(task));
            holder.textViewContent.setText(getContentForTask(task));
        }

        @Override
        public int getItemCount() {
            return taskList.size();
        }

        private String getTitleForTask(AutomatedTask task) {
            if (task.getTriggerCondition() == 1) {
                return "单次任务";
            } else if (task.getTriggerCondition() == 2) {
                return "多次任务";
            }
            return "";
        }

        private String getContentForTask(AutomatedTask task) {
            return "提示在 " + task.getExecutionTimeFormatted() + " 执行 " + task.getActionToExecute();
        }

        public class ActionViewHolder extends RecyclerView.ViewHolder {

            TextView textViewTitle;
            TextView textViewContent;

            public ActionViewHolder(View itemView) {
                super(itemView);
                textViewTitle = itemView.findViewById(R.id.textViewTitle);
                textViewContent = itemView.findViewById(R.id.textViewContent);

                // 设置点击事件，跳转到编辑任务界面
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AutomatedTask task = taskList.get(getAdapterPosition());
                        Intent intent = new Intent(getContext(), NewAutoTaskActivity.class);
                        intent.putExtra("taskId", task.getTaskId());
                        startActivity(intent);
                    }
                });
            }
        }
    }
}