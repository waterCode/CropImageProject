package com.meitu.cropimagelibrary.util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by zmc on 2017/7/24.
 */

public class FileUtil {

    public static File bitmapConvertToFile(@NonNull Bitmap bitmap, final Activity activity) {
        final WeakReference<Activity> mWeakActivity = new WeakReference<>(activity);
        File file = new File(Environment.getExternalStoragePublicDirectory("image_crop_sample"), "");
        if (!file.exists()) {//如果不存在
            if (!file.mkdir()) {//创建不成功返回null
                return null;
            }
        }
        File bitmapFile = new File(file, "IMG_" + (new SimpleDateFormat("yyyyMMddHHmmss")).format(Calendar.getInstance().getTime()) + ".jpg");
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(bitmapFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            //通知本地扫描，这样可以让图库可以检测到
            if (mWeakActivity.get() != null){
                MediaScannerConnection.scanFile(mWeakActivity.get().getApplicationContext(), new String[]{bitmapFile.getAbsolutePath()}, null, new MediaScannerConnection.MediaScannerConnectionClient() {
                    @Override
                    public void onMediaScannerConnected() {

                    }

                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        if (mWeakActivity.get()!=null) {
                            mWeakActivity.get().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mWeakActivity.get(), "图片已经保存", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
        }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return bitmapFile;
    }
}
