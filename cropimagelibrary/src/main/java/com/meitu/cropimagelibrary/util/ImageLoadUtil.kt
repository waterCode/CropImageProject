package com.meitu.cropimagelibrary.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import android.util.Log

import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * Created by zmc on 2017/7/19.
 */

object ImageLoadUtil {
    private val TAG = "ImageLoadUtil"

    /**
     * @param contentResolver 内容提供器
     * @param uri             图片的uri
     * @param maxHeight       最大高度
     * @param maxWidth        最大宽度
     * @return 对应的Bitmap对象
     * @throws FileNotFoundException
     */

    fun loadImage(contentResolver: ContentResolver, uri: Uri, maxHeight: Int, maxWidth: Int): Bitmap? {
        var options: BitmapFactory.Options? = null
        var bitmap: Bitmap? = null
        var inputStream: InputStream?
        try {
            options = calculateInSampleSize(contentResolver, uri, maxHeight, maxWidth)
            var isSuccess = false
            while (!isSuccess) {
                try {
                    inputStream = contentResolver.openInputStream(uri)
                    bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    isSuccess = true
                } catch (error: OutOfMemoryError) {
                    Log.d(TAG, "out of memory")
                    options.inSampleSize = options.inSampleSize * 2
                    isSuccess = false
                }

            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return bitmap

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
    fun checkBitmapOrientation(contentResolver: ContentResolver, uri: Uri, bitmap: Bitmap): Bitmap {
        var bitmap = bitmap
        val degree = getBitmapOrientation(contentResolver, uri)
        if (degree != 0) {
            bitmap = rotateBitmap(bitmap, (-degree).toFloat())
        }
        return bitmap
    }

    fun getBitmapOrientation(contentResolver: ContentResolver, uri: Uri): Int {
        val inputStream: InputStream?
        try {
            inputStream = contentResolver.openInputStream(uri)
            /*if (inputStream != null) {
                android.support.media.ExifInterface exif = new android.support.media.ExifInterface(inputStream);
                int exifOrientation = exif.getAttributeInt(android.support.media.ExifInterface.TAG_DATETIME_ORIGINAL, ExifInterface.ORIENTATION_NORMAL);
                return exifToDegrees(exifOrientation);//返回旋转度数
            }*/
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        /*catch (IOException e) {
            e.printStackTrace();
        }*/
        return 0//
    }


    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        var bitmap = bitmap
        val matrix = Matrix()
        var isSuccess = false
        var compressScale = 1f
        var rotatedBitmap: Bitmap
        matrix.setRotate(degrees, (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat())

        while (!isSuccess) {//知道压缩到合适的大小
            try {
                //如果没有旋转，他是不会创建新图片的
                rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                bitmap.recycle()//创建成功，回收先
                isSuccess = true
                bitmap = rotatedBitmap
            } catch (error: OutOfMemoryError) {
                Log.e(TAG, "rotateBitmap out of memory")
                error.printStackTrace()
                compressScale = compressScale * 0.7f
                matrix.postScale(compressScale, compressScale, (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat())
            }

        }
        return bitmap
    }


    private fun exifToDegrees(exifOrientation: Int): Int {
        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> return 0

            ExifInterface.ORIENTATION_ROTATE_180 -> return 180

            ExifInterface.ORIENTATION_ROTATE_270 -> return 270
            else -> return 0
        }
    }


    private fun CalculateBitmapSize(contentResolver: ContentResolver, uri: Uri): Rect {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val `in` = contentResolver.openInputStream(uri)
        if (`in` != null) {
            BitmapFactory.decodeStream(`in`, null, options)
        }
        val rect = Rect()
        rect.set(0, 0, options.outWidth, options.outHeight)
        return rect
    }


    private fun calculateInSampleSize(contentResolver: ContentResolver, uri: Uri, maxHeight: Int, maxWidth: Int): BitmapFactory.Options {
        // TODO: 2017/7/28 应该怎么对参数进行限制？，所有参数都得判断 ？
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val inputStream = contentResolver.openInputStream(uri)
        if (inputStream != null) {
            BitmapFactory.decodeStream(inputStream, null, options)
            var width = options.outWidth
            var height = options.outHeight
            var scale = 1
            while (height > maxHeight && width > maxWidth) {
                height /= 2
                width /= 2
                scale = scale * 2
            }
            options.inSampleSize = scale//缩小倍数
            options.inJustDecodeBounds = false
            inputStream.close()
        }

        return options
    }
}
