package com.meitu.cropimagelibrary.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zmc on 2017/7/19.
 */

public class ImageLoadUtil {
    private static final String TAG = "ImageLoadUtil";

    /**
     * @param contentResolver 内容提供器
     * @param uri             图片的uri
     * @param maxHeight       最大高度
     * @param maxWidth        最大宽度
     * @return 对应的Bitmap对象
     * @throws FileNotFoundException
     */
    public static Bitmap loadImage(ContentResolver contentResolver, Uri uri, int maxHeight, int maxWidth) throws FileNotFoundException {
        BitmapFactory.Options options = null;
        Bitmap bitmap = null;
        InputStream inputStream;
        try {
            options = calculateInSampleSize(contentResolver, uri, maxHeight, maxWidth);
            boolean isSuccess = false;
            while (!isSuccess) {
                try {
                    inputStream = contentResolver.openInputStream(uri);
                    bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                    isSuccess = true;
                } catch (OutOfMemoryError error) {
                    Log.d(TAG, "out of memory");
                    options.inSampleSize = options.inSampleSize * 2;
                    isSuccess = false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        bitmap = checkBitmapOrientation(contentResolver, uri, bitmap);//这里可能抛出oom
        return bitmap;

        // TODO: 2017/7/27 除了打开两次输入流还有其他办法嘛
    }

    /**
     * 检查图片orientation是否有旋转，没有的话返回原图
     *
     * @param contentResolver 内容提供器
     * @param uri             图片的uri
     * @param bitmap          原图
     * @return 旋转后的图片，如果不需要旋转则返回原图
     */
    private static Bitmap checkBitmapOrientation(ContentResolver contentResolver, Uri uri, Bitmap bitmap) {
        InputStream inputStream;
        try {
            inputStream = contentResolver.openInputStream(uri);
            if (inputStream != null) {
                android.support.media.ExifInterface exif = new android.support.media.ExifInterface(inputStream);
                int exifOrientation = exif.getAttributeInt(android.support.media.ExifInterface.TAG_DATETIME_ORIGINAL, ExifInterface.ORIENTATION_NORMAL);
                int degrees = exifToDegrees(exifOrientation);
                if (degrees != 0) {
                    bitmap = rotateBitmap(bitmap, -degrees);//返回旋转后的图片
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        boolean isSuccess = false;
        float compressScale = 1;
        Bitmap rotatedBitmap;
        matrix.setRotate(degrees, bitmap.getWidth() / 2, bitmap.getHeight() / 2);

        while (!isSuccess) {//知道压缩到合适的大小
            try {
                rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap.recycle();//创建成功，回收先
                bitmap = rotatedBitmap;
                isSuccess = true;
            } catch (OutOfMemoryError error) {
                Log.e(TAG, "rotateBitmap out of memory");
                error.printStackTrace();
                compressScale = compressScale * 0.7f;
                matrix.postScale(compressScale,compressScale,bitmap.getWidth()/2,bitmap.getHeight()/2);
            }
        }
        return bitmap;
    }


    private static int exifToDegrees(int exifOrientation) {
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 0;

            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;

            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }


    private static BitmapFactory.Options calculateInSampleSize(ContentResolver contentResolver, Uri uri, int maxHeight, int maxWidth) throws IOException {
        // TODO: 2017/7/28 应该怎么对参数进行限制？，所有参数都得判断 ？
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream inputStream = contentResolver.openInputStream(uri);
        if (inputStream != null) {
            BitmapFactory.decodeStream(inputStream, null, options);
            int width = options.outWidth;
            int height = options.outHeight;
            int scale = 1;
            while (height > maxHeight && width > maxWidth) {
                height /= 2;
                width /= 2;
                scale = scale * 2;
            }
            options.inSampleSize = scale;//缩小倍数
            options.inJustDecodeBounds = false;
            inputStream.close();
        }

        return options;
    }
}
