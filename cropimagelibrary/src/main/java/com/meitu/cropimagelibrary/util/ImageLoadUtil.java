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


    /**
     * @param contentResolver 内容提供器
     * @param uri             图片的uri
     * @param maxHeight       最大高度
     * @param maxWidth        最大宽度
     * @return 对应的Bitmap对象
     * @throws FileNotFoundException
     */
    public static Bitmap loadImage(ContentResolver contentResolver, Uri uri, int maxHeight, int maxWidth) throws FileNotFoundException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream inputStream = contentResolver.openInputStream(uri);
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
        //然后就可以直接加载了

        //再开一个输入流
        inputStream = contentResolver.openInputStream(uri);
        return BitmapFactory.decodeStream(inputStream, null, options);
    }
}
