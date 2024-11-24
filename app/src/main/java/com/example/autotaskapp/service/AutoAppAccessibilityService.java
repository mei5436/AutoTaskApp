package com.example.autotaskapp.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.autotaskapp.R;
import com.example.autotaskapp.ui.MainActivity;
import com.example.autotaskapp.ui.RecordActionsFragment;
import com.example.autotaskapp.model.RecordedAction;
import com.example.autotaskapp.utils.FileUtils;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AutoAppAccessibilityService extends AccessibilityService {
    private static final String TAG = "AutoAppService";
    private boolean isRecording = false;
    private boolean isExecuting = false;
    private long recordingStartTime;
    private List<RecordedAction> recordedActions = new ArrayList<>();
    private HandlerThread handlerThread;
    private Handler backgroundHandler;
    private Handler mainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == EVENT_UI_OPERATIONS && msg.obj instanceof AccessibilityEvent) {
                AccessibilityEvent event = (AccessibilityEvent) msg.obj;
                switch (event.getEventType()) {
                    case AccessibilityEvent.TYPE_VIEW_CLICKED:
                        performClick(event.getSource().getViewIdResourceName(), event.getSource().getText().toString());
                        break;
                    case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                        performScroll(event.getScrollX(), event.getScrollY());
                        break;
                    default:
                        Log.i(TAG, "handleMessage: ignore:" + event);
                        break;
                }
            }
        }
    };
    // 通知相关常量
    private static final int NOTIFICATION_ID = 1001;
    // 通知渠道相关常量
    private static final String NOTIFICATION_CHANNEL_ID = "AutoApp_Notification_Channel";
    private static final String NOTIFICATION_CHANNEL_NAME = "AutoApp Notification Channel";

    // 消息类型相关常量
    private static final int EVENT_EXECUTE_ACTIONS = 101;
    private static final int EVENT_UI_OPERATIONS = 102;
    private String mCurrentWindow = "";

    @Override
    public void onCreate() {
        super.onCreate();
        initThreadHandler();

        // 创建通知渠道（适用于Android 8.0及以上版本）
        createNotificationChannel();
    }

    private void initThreadHandler() {
        handlerThread = new HandlerThread("BackgroundThread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == EVENT_EXECUTE_ACTIONS && msg.obj instanceof String) {
                    String actionToExecute = (String) msg.obj;
                    executeRecordedActions(actionToExecute);
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handlerThread.quitSafely();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (isRecording) {
            recordEvent(event);
        } else if (isExecuting && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            mCurrentWindow = event.getClassName().toString();
        }
    }

    @Override
    public void onInterrupt() {
        // 暂时不需要处理此方法
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent!= null) {
            String action = intent.getAction();
            if ("START_RECORDING".equals(action)) {
                startRecording();
            } else if ("STOP_RECORDING".equals(action)) {
                stopRecordingAndShowDialog();
            } else if ("EXECUTE_ACTIONS".equals(action)) {
                String actionToExecute = intent.getStringExtra("actionToExecute");
                if (backgroundHandler!= null) {
                    backgroundHandler.obtainMessage(EVENT_EXECUTE_ACTIONS, actionToExecute).sendToTarget();
                }
            }
        }
        return START_STICKY;
    }

    private void startRecording() {
        if (isRecording) {
            Log.w(TAG, "startRecording but isRecording.");
            return;
        }
        isRecording = true;
        recordingStartTime = System.currentTimeMillis();
        recordedActions.clear();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            showNotification("正在记录操作，请在完成后通过通知栏停止按钮结束记录。", "STOP_RECORDING");
        }
    }

    private void stopRecordingAndShowDialog() {
        if (!isRecording) {
            Log.w(TAG, "stopRecordingAndShowDialog but isRecording is false.");
            return;
        }
        isRecording = false;
        // 先获取默认文件名，格式为录制开始时间
        final String defaultFileName = getDefaultFileName();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_recording, null);
        final EditText editTextFileName = dialogView.findViewById(R.id.editTextFileName);
        editTextFileName.setText(defaultFileName);

        builder.setView(dialogView).setTitle("保存录制内容").setMessage("请选择是否保存此次录制内容，并可编辑保存文件名称。").setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 取消保存，直接转跳到主界面的RecordActionsFragment界面
                Intent intent = new Intent(AutoAppAccessibilityService.this, MainActivity.class);
                intent.putExtra("FRAGMENT_TO_LOAD", "RecordActionsFragment");
                startActivity(intent);
            }
        }).setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 获取用户输入的文件名
                String fileName = editTextFileName.getText().toString();
                if (fileName.isEmpty()) {
                    fileName = defaultFileName;
                }
                // 保存录制内容到指定文件
                String finalFileName = fileName;
                backgroundHandler.post(() -> saveRecordedActionsToJSONFile(finalFileName));
                // 转跳到主界面的RecordActionsFragment界面
                Intent intent = new Intent(AutoAppAccessibilityService.this, MainActivity.class);
                intent.putExtra("FRAGMENT_TO_LOAD", "RecordActionsFragment");
                startActivity(intent);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String getDefaultFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return "recording_" + sdf.format(new Date(recordingStartTime));
    }

    private void recordEvent(AccessibilityEvent event) {
        long eventTime = System.currentTimeMillis() - recordingStartTime;
        RecordedAction action = new RecordedAction(eventTime, event);
        recordedActions.add(action);
        Log.d(TAG, "Recorded Event: " + new Gson().toJson(action));
    }

    private void saveRecordedActionsToJSONFile(String fileName) {
        try {
            FileUtils.saveRecordedActionsToJSONFile(this, recordedActions, fileName);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "保存记录动作到JSON文件失败: " + e.getMessage());
        }
    }

    private void executeRecordedActions(String actionToExecute) {
        isExecuting = true;
        String recordedActionFileName = actionToExecute;
        File file = new File(getFilesDir(), recordedActionFileName);
        if (file.exists()) {
            List<RecordedAction> actions = FileUtils.readRecordedActionsFromJSONFile(this, recordedActionFileName);
            String previousActivity = "";
            long delay = 0L;
            for (RecordedAction action : actions) {
                delay = action.getEventTime() - delay;
                // 等待原始时间间隔
                SystemClock.sleep(delay);

                AccessibilityEvent event = action.getAccessibilityEvent();
                int type = event.getEventType();
                if (AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED == type) {
                    previousActivity = event.getClassName().toString();
                    continue;
                } else if (!isCurrentWindowMatching(previousActivity)) {
                    Log.d(TAG, "Activity mismatch, stopping execution, previousActivity:" + previousActivity);
                    break;
                }
                mainHandler.obtainMessage(EVENT_UI_OPERATIONS, event).sendToTarget();
            }
        } else {
            Log.e(TAG, "指定的记录动作文件不存在: " + actionToExecute);
        }
        isExecuting = false;
    }

    private boolean isCurrentWindowMatching(String expectedActivity) {
        if (!TextUtils.isEmpty(expectedActivity) &&!TextUtils.isEmpty(mCurrentWindow) &&!TextUtils.equals(expectedActivity, mCurrentWindow)) {
            return false;
        }
        return true;
    }

    private void performClick(String viewId, String text) {
        AccessibilityNodeInfo rootNode = findNodeByViewIdAndText(getRootInActiveWindow(), viewId, text);
        if (rootNode!= null) {
            clickNode(rootNode);
        } else {
            Log.e(TAG, "未找到可点击的节点，点击操作失败: viewId=" + viewId + "text=" + text);
        }
    }

    private AccessibilityNodeInfo findNodeByViewIdAndText(AccessibilityNodeInfo node, String viewId, String text) {
        if (node == null) return null;

        if (viewId.equals(node.getViewIdResourceName()) && text.equals(node.getText())) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = node.getChild(i);
            AccessibilityNodeInfo resultNode = findNodeByViewIdAndText(childNode, viewId, text);
            if (resultNode!= null) {
                return resultNode;
            }
        }
        return null;
    }

    private void clickNode(AccessibilityNodeInfo node) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);

        int x = (bounds.left + bounds.right) / 2;
        int y = (bounds.top + bounds.bottom) / 2;

        Path clickPath = new Path();
        clickPath.moveTo(x, y);

        GestureDescription.StrokeDescription clickStroke = null;
        GestureDescription clickGesture = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            clickStroke = new GestureDescription.StrokeDescription(clickPath, 0, 100);
            clickGesture = new GestureDescription.Builder().addStroke(clickStroke).build();
            dispatchGesture(clickGesture, null, null);
        }
    }

    private void performScroll(int scrollX, int scrollY) {
        Path scrollPath = new Path();
        scrollPath.moveTo(scrollX, scrollY);

        GestureDescription.StrokeDescription scrollStroke = null;
        GestureDescription scrollGesture = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            scrollStroke = new GestureDescription.StrokeDescription(scrollPath, 0, 500);
            scrollGesture = new GestureDescription.Builder().addStroke(scrollStroke).build();
            dispatchGesture(scrollGesture, null, null);
        }
    }

    // 获取当前活动窗口的根节点
    public AccessibilityNodeInfo getRootInActiveWindow() {
        List<AccessibilityWindowInfo> windows = getWindows();
        for (AccessibilityWindowInfo window : windows) {
            if (window.getRoot()!= null && window.isActive()) {
                return window.getRoot();
            }
        }
        return null;
    }

    // 显示通知
    @SuppressLint("ForegroundServiceType")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showNotification(String content, String action) {
        Intent intent = new Intent(this, AutoAppAccessibilityService.class);
        intent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);

        Notification.Builder builder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID).setSmallIcon(android.R.drawable.ic_menu_save).setContentTitle("AutoApp操作记录").setContentText(content).addAction(android.R.drawable.ic_delete, "停止记录", pendingIntent);

        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
    }

    // 清除通知
    private void clearNotification() {
        stopForeground(true);
    }

    // 创建通知渠道（适用于Android 8.0及以上版本）
    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }
}