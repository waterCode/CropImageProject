package com.meitu.cropimagelibrary.util

import android.graphics.RectF

object RectUtils {

    fun getCornersFromRect(r: RectF): FloatArray {
        return floatArrayOf(r.left, r.top, r.right, r.top, r.right, r.bottom, r.left, r.bottom)
    }


    fun getRectSidesFromCorners(corners: FloatArray): FloatArray {
        return floatArrayOf(Math.sqrt(Math.pow((corners[0] - corners[2]).toDouble(), 2.0) + Math.pow((corners[1] - corners[3]).toDouble(), 2.0)).toFloat(), Math.sqrt(Math.pow((corners[2] - corners[4]).toDouble(), 2.0) + Math.pow((corners[3] - corners[5]).toDouble(), 2.0)).toFloat())
    }

    fun getCenterFromRect(r: RectF): FloatArray {
        return floatArrayOf(r.centerX(), r.centerY())
    }


    fun trapToRect(array: FloatArray): RectF {
        val r = RectF(java.lang.Float.POSITIVE_INFINITY, java.lang.Float.POSITIVE_INFINITY,
                java.lang.Float.NEGATIVE_INFINITY, java.lang.Float.NEGATIVE_INFINITY)
        var i = 1
        while (i < array.size) {
            val x = Math.round(array[i - 1] * 10) / 10f
            val y = Math.round(array[i] * 10) / 10f
            r.left = if (x < r.left) x else r.left
            r.top = if (y < r.top) y else r.top
            r.right = if (x > r.right) x else r.right
            r.bottom = if (y > r.bottom) y else r.bottom
            i += 2
        }
        r.sort()
        return r
    }

}