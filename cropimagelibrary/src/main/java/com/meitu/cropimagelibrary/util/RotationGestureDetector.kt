package com.meitu.cropimagelibrary.util

import android.view.MotionEvent

class RotationGestureDetector(private val mListener: OnRotationGestureListener?) {

    private var fX: Float = 0.toFloat()
    private var fY: Float = 0.toFloat()
    private var sX: Float = 0.toFloat()
    private var sY: Float = 0.toFloat()

    private var mPointerIndex1: Int = 0
    private var mPointerIndex2: Int = 0
    var angle: Float = 0.toFloat()
        private set
    private var mIsFirstTouch: Boolean = false

    init {
        mPointerIndex1 = INVALID_POINTER_INDEX
        mPointerIndex2 = INVALID_POINTER_INDEX
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                sX = event.x
                sY = event.y
                mPointerIndex1 = event.findPointerIndex(event.getPointerId(0))
                angle = 0f
                mIsFirstTouch = true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                fX = event.x
                fY = event.y
                mPointerIndex2 = event.findPointerIndex(event.getPointerId(event.actionIndex))
                angle = 0f
                mIsFirstTouch = true
            }
            MotionEvent.ACTION_MOVE -> if (mPointerIndex1 != INVALID_POINTER_INDEX && mPointerIndex2 != INVALID_POINTER_INDEX && event.pointerCount > mPointerIndex2) {
                val nfX: Float
                val nfY: Float
                val nsX: Float
                val nsY: Float

                nsX = event.getX(mPointerIndex1)
                nsY = event.getY(mPointerIndex1)
                nfX = event.getX(mPointerIndex2)
                nfY = event.getY(mPointerIndex2)

                if (mIsFirstTouch) {
                    angle = 0f
                    mIsFirstTouch = false
                } else {
                    calculateAngleBetweenLines(fX, fY, sX, sY, nfX, nfY, nsX, nsY)
                }

                mListener?.onRotation(this)
                fX = nfX
                fY = nfY
                sX = nsX
                sY = nsY
            }
            MotionEvent.ACTION_UP -> mPointerIndex1 = INVALID_POINTER_INDEX
            MotionEvent.ACTION_POINTER_UP -> mPointerIndex2 = INVALID_POINTER_INDEX
            else -> {
            }
        }
        return true
    }

    private fun calculateAngleBetweenLines(fx1: Float, fy1: Float, fx2: Float, fy2: Float,
                                           sx1: Float, sy1: Float, sx2: Float, sy2: Float): Float {
        return calculateAngleDelta(
                Math.toDegrees(Math.atan2((fy1 - fy2).toDouble(), (fx1 - fx2).toDouble()).toFloat().toDouble()).toFloat(),
                Math.toDegrees(Math.atan2((sy1 - sy2).toDouble(), (sx1 - sx2).toDouble()).toFloat().toDouble()).toFloat())
    }

    private fun calculateAngleDelta(angleFrom: Float, angleTo: Float): Float {
        angle = angleTo % 360.0f - angleFrom % 360.0f

        if (angle < -180.0f) {
            angle += 360.0f
        } else if (angle > 180.0f) {
            angle -= 360.0f
        }

        return angle
    }

    open class SimpleOnRotationGestureListener : OnRotationGestureListener {

        override fun onRotation(rotationDetector: RotationGestureDetector): Boolean {
            return false
        }
    }

    interface OnRotationGestureListener {

        fun onRotation(rotationDetector: RotationGestureDetector): Boolean
    }

    companion object {

        private val INVALID_POINTER_INDEX = -1
    }

}