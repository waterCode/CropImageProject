package com.meitu.cropimagelibrary.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by zmc on 2017/7/19.
 */

public class ImageLoadUtil {


    public static Bitmap loadImage(ContentResolver contentResolver, Uri uri) throws FileNotFoundException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream inputStream = contentResolver.openInputStream(uri);
        BitmapFactory.decodeStream(inputStream, null, options);
        int width = options.outWidth;
        int height = options.outHeight;
        int scale = 1;
        while (height > 1000 && width > 1000) {
            height /= 2;
            width /= 2;
            scale = scale * 2;
        }
        options.inSampleSize = scale;//缩小倍数
        options.inJustDecodeBounds = false;
        //然后就可以直接加载了

        //再开一个输入流
        inputStream = contentResolver.openInputStream(uri);
        return BitmapFactory.decodeStream(inputStream, null, options);
    }
}
