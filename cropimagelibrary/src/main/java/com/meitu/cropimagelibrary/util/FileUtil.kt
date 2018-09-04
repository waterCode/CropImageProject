package com.meitu.cropimagelibrary.util

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.os.Environment
import android.util.Log

import com.meitu.cropimagelibrary.info.ImageInfo

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar

/**
 * Created by zmc on 2017/7/24.
 */

object FileUtil {


    val isExternalStoageWriable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return if (Environment.MEDIA_MOUNTED == state) {
                true
            } else {
                false
            }
        }


    fun bitmapConvertToFile(context: Context, bitmap: Bitmap, parent: File?, callback: SaveBitmapCallback): File? {
        var parent = parent

        if (parent == null) {
            parent = createDefaultFolder(context, callback)
        }

        if (parent == null) return null
        val bitmapFile = File(parent, "IMG_" + SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().time) + ".jpg")
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(bitmapFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            //通知本地扫描，这样可以让图库可以检测到

        } catch (e: IOException) {
            e.printStackTrace()
            callback.onFailed()
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback.onFailed()
                }

            }
        }

        return bitmapFile
    }

    private fun createDefaultFolder(context: Context, callback: SaveBitmapCallback): File? {
        val file = File(context.filesDir, "image_crop_sample")
        if (!file.exists()) {
            if (!file.mkdir()) {
                Log.e("FileUtil", "directory create failed")
                callback.onFailed()
            }
        }
        return file
    }
}
