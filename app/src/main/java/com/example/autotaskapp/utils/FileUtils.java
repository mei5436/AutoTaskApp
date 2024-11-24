package com.example.autotaskapp.utils;

import android.content.Context;
import android.util.Log;

import com.example.autotaskapp.model.RecordedAction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static void saveRecordedActionsToJSONFile(Context context, List<RecordedAction> recordedActions, String fileName) throws IOException {
        Gson gson = new Gson();
        String json = gson.toJson(recordedActions);

        FileOutputStream fos = null;
        try {
            File file = new File(context.getFilesDir(), fileName);
            fos = new FileOutputStream(file);
            fos.write(json.getBytes());
        } catch (IOException e) {
            throw e;
        } finally {
            if (fos!= null) {
                fos.close();
            }
        }
    }

    public static List<RecordedAction> readRecordedActionsFromJSONFile(Context context, String fileName) {
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<RecordedAction>>() {}.getType();
        List<RecordedAction> recordedActions = null;

        File file = new File(context.getFilesDir(), fileName);
        if (file.exists()) {
            try {
                InputStream is = context.getAssets().open(fileName);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                String json = new String(buffer, "UTF-8");
                recordedActions = gson.fromJson(json, type);
                is.close();
            } catch (IOException e) {
                Log.e("FileUtils", "读取记录动作文件失败：" + e.getMessage());
            }
        }

        return recordedActions;
    }
}