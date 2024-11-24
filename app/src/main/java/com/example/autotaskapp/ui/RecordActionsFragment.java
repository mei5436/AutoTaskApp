package com.example.autotaskapp.ui;

import android.content.Context;
import android.content.Intent;
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
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autotaskapp.R;
import com.example.autotaskapp.model.RecordedAction;
import com.example.autotaskapp.service.AutoAppAccessibilityService;
import com.example.autotaskapp.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecordActionsFragment extends Fragment {

    private RecyclerView recyclerViewRecordActions;
    private RecordActionsAdapter adapter;
    private List<File> recordedActionFiles = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record_actions, container, false);

        recyclerViewRecordActions = view.findViewById(R.id.recyclerViewRecordActions);

        // 设置RecyclerView的布局管理器
        recyclerViewRecordActions.setLayoutManager(new LinearLayoutManager(getContext()));

        // 创建并设置Adapter
        adapter = new RecordActionsAdapter(recordedActionFiles, getContext());
        recyclerViewRecordActions.setAdapter(adapter);

        // 初始化数据，遍历ActionRecords文件夹获取所有文件
        initData();

        // 设置FloatingActionButton的点击事件，开始录制操作
        ImageButton floatingActionButton = view.findViewById(R.id.floatingActionButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), AutoAppAccessibilityService.class);
                intent.setAction("START_RECORDING");
                getContext().startService(intent);
            }
        });

        setHasOptionsMenu(true);

        return view;
    }

    private void initData() {
        File actionRecordsFolder = new File(getContext().getFilesDir(), "ActionRecords");
        if (actionRecordsFolder.exists() && actionRecordsFolder.isDirectory()) {
            File[] files = actionRecordsFolder.listFiles();
            if (files!= null) {
                for (File file : files) {
                    if (file.isFile()) {
                        recordedActionFiles.add(file);
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    @SuppressWarnings("unused")
    public void onResume() {
        super.onResume();
        // 重新遍历文件夹刷新数据，例如在成功保存新的用户操作记录后调用此方法刷新列表
        initData();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_record_actions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    @SuppressWarnings("unused")
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_clear_all) {
            clearAllRecordedActions();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearAllRecordedActions() {
        File actionRecordsFolder = new File(getContext().getFilesDir(), "ActionRecords");
        if (actionRecordsFolder.exists() && actionRecordsFolder.isDirectory()) {
            File[] files = actionRecordsFolder.listFiles();
            if (files!= null) {
                for (File file : files) {
                    if (file.delete()) {
                        Log.d("RecordActionsFragment", "已删除文件：" + file.getName());
                    } else {
                        Log.e("RecordActionsFragment", "删除文件失败：" + file.getName());
                    }
                }
            }
            recordedActionFiles.clear();
            adapter.notifyDataSetChanged();
            Toast.makeText(getContext(), "已清除所有记录动作文件", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "动作记录文件夹不存在", Toast.LENGTH_SHORT).show();
        }
    }

    private static class ActionViewHolder extends RecyclerView.ViewHolder {

        TextView textViewFileName;
        ImageButton imageButtonOptions;
        int position;

        public interface ClickEvent {
            public void onClick(ActionViewHolder v);

            public boolean onLongClick(ActionViewHolder v);
        }

        ClickEvent event;

        public ActionViewHolder(View itemView, ClickEvent event) {
            super(itemView);
            textViewFileName = itemView.findViewById(R.id.textViewFileName);
            imageButtonOptions = itemView.findViewById(R.id.imageButtonOptions);

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    event.onLongClick(ActionViewHolder.this);
                    return true;
                }
            });
            itemView.setOnClickListener(view -> event.onClick(ActionViewHolder.this));
        }
    }

    private static class RecordActionsAdapter extends RecyclerView.Adapter<ActionViewHolder> {

        private List<File> actionFiles;
        private Context context;

        public RecordActionsAdapter(List<File> actionFiles, Context context) {
            this.actionFiles = actionFiles;
            this.context = context;
        }

        @NonNull
        @Override
        public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_record_action, parent, false);
            return new ActionViewHolder(view, new ActionViewHolder.ClickEvent() {
                @Override
                public void onClick(ActionViewHolder v) {
                    File file = actionFiles.get(v.position);
                    Intent intent = new Intent(context, AutoAppAccessibilityService.class);
                    intent.putExtra("actionToExecute", file.getName());
                    intent.setAction("EXECUTE_ACTIONS");
                    context.startService(intent);
                }

                @Override
                public boolean onLongClick(ActionViewHolder v) {
                    showPopupMenu(v.itemView, v.position);
                    return true;
                }
            });
        }

        @Override
        public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
            File file = actionFiles.get(position);
            holder.textViewFileName.setText(file.getName());
        }

        @Override
        public int getItemCount() {
            return actionFiles.size();
        }

        private void showPopupMenu(View view, int position) {
            PopupMenu popupMenu = new PopupMenu(context, view);
            popupMenu.inflate(R.menu.menu_item_record_action_options);
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.menu_item_delete) {
                        deleteRecordedAction(position);
                        return true;
                    } else if (item.getItemId() == R.id.menu_item_trigger_action) {
                        triggerRecordedAction(position);
                        return true;
                    }
                    return false;                }
            });
            popupMenu.show();
        }

        private void deleteRecordedAction(int position) {
            File file = actionFiles.get(position);
            if (file.delete()) {
                Log.d("RecordActionsFragment", "已删除记录动作文件：" + file.getName());
                actionFiles.remove(position);
                notifyDataSetChanged();
                Toast.makeText(context, "已删除指定记录动作文件", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("RecordActionsFragment", "删除记录动作文件失败：" + file.getName());
                Toast.makeText(context, "删除记录动作文件失败，请重试", Toast.LENGTH_SHORT).show();
            }
        }

        private void triggerRecordedAction(int position) {
            File file = actionFiles.get(position);
            Intent intent = new Intent(context, AutoAppAccessibilityService.class);
            intent.putExtra("actionToExecute", file.getName());
            intent.setAction("EXECUTE_ACTIONS");
            context.startService(intent);
        }
    }
}