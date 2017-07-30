package com.meitu.cropimagelibrary.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by zmc on 2017/7/24.
 */

public class FileUtil {


    public static File bitmapConvertToFile(Context context, @NonNull Bitmap bitmap, File parent, final SaveBitmapCallback callback) {


        if (parent == null) {
            parent = createDefaultFolder(context, callback);
        }

        if (parent == null) return null;
        File bitmapFile = new File(parent, "IMG_" + (new SimpleDateFormat("yyyyMMddHHmmss")).format(Calendar.getInstance().getTime()) + ".jpg");
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(bitmapFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            //通知本地扫描，这样可以让图库可以检测到

        } catch (IOException e) {
            e.printStackTrace();
            callback.onFailed();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onFailed();
                }
            }
        }

        return bitmapFile;
    }

    @Nullable
    private static File createDefaultFolder(Context context, SaveBitmapCallback callback) {
        File file = new File(context.getFilesDir(), "image_crop_sample");
        if (!file.exists()) {
            if (!file.mkdir()) {
                Log.e("FileUtil", "directory create failed");
                callback.onFailed();
            }
        }
        return file;
    }


    public static boolean isExternalStoageWriable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        } else {
            return false;
        }
    }
}
