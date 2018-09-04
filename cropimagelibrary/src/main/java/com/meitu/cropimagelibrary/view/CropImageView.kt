package com.meitu.cropimagelibrary.view

import android.animation.Animator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView

import com.meitu.cropimagelibrary.info.ImageInfo
import com.meitu.cropimagelibrary.util.ImageLoadUtil
import com.meitu.cropimagelibrary.util.RectUtils
import com.meitu.cropimagelibrary.util.RotationGestureDetector

import java.io.FileNotFoundException
import java.util.Arrays

/**
 * Created by zmc on 2017/7/18.
 */

class CropImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : android.support.v7.widget.AppCompatImageView(context, attrs, defStyleAttr) {
    private var isHorizontalEnable = false
    private var isVerticalEnable = false
    private var mMaxScale = 3f


    private var mMinScale = 0.8f
    private var mScaleEnable = true
    private var mRotateEnable = true


    private val mBaseMatrix = Matrix()
    private val mDisplayMatrix = Matrix()
    private val mTempMatrix = Matrix()
    private val mMirrorMatrix = Matrix()
    private val mConcatMatrix = Matrix()

    private val mCropRectF = RectF()//裁剪框矩形区域
    private val mBitmapRectF = RectF()//当前的矩形区域

    private var mTransParentLayerPaint: Paint? = null//暗色区域背景
    private var mWhiteCropPaint: Paint? = null


    private var mScaleGestureDetector: ScaleGestureDetector? = null
    private var mGestureDetector: GestureDetector? = null
    private var mRotationGestureDetector: RotationGestureDetector? = null

    private var mMidPntX: Float = 0.toFloat()
    private var mMidPntY: Float = 0.toFloat()//手指中心点


    private val mMatrixValue = FloatArray(9)

    private var mImageInfo: ImageInfo? = null//最开始图片信息,好像可以删掉


    private val mCurrentImageCorners = FloatArray(8)//用来存放当前顶点坐标啊
    private var mInitImageCorners: FloatArray? = null
    private var mUri: Uri? = null//图片的uri

    private var mRotateAnimator: TransformAnimator? = null
    private var mTranslateScaleAnimator: TransformAnimator? = null
    private var mCurrentActiveAnimator: Animator? = null


    /**
     * 获得当前的旋转角度
     */
    val currentAngle: Float
        get() = getMatrixAngle(mDisplayMatrix)

    /**
     * 获得当前的放大倍数
     */
    val currentScale: Float
        get() {

            val displayScale = getMatrixScale(mDisplayMatrix)
            return displayScale / mImageInfo!!.initScale
        }

    /**
     * 连接矩阵，主要将display矩阵和镜像矩阵进行连接
     *
     * @return 返回合成后的矩阵
     */
    val concatMatrix: Matrix
        get() {
            mConcatMatrix.reset()
            mConcatMatrix.set(mDisplayMatrix)
            mConcatMatrix.postConcat(mMirrorMatrix)
            return mConcatMatrix
        }

    /**
     * @return 返回当前的Bitmap对象
     */
    val imageBitmap: Bitmap?
        get() {
            val bitmapDrawable = drawable as BitmapDrawable
            return bitmapDrawable?.bitmap

        }

    init {
        init()
    }

    /**
     * 初始化裁剪框和阴影区域的画笔
     */
    private fun initCropMaterials() {

        mTransParentLayerPaint = Paint()
        mTransParentLayerPaint!!.color = Color.parseColor(DEFAULT_BACKGROUND_COLOR_ID)//设置颜色

        mWhiteCropPaint = Paint()
        mWhiteCropPaint!!.color = Color.WHITE//设置颜色
        mWhiteCropPaint!!.strokeWidth = 1f//设置填充宽度
        mWhiteCropPaint!!.style = Paint.Style.STROKE//what？？
    }


    private fun init() {
        initCropMaterials()

        mInitImageCorners = FloatArray(8)

        mScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        mGestureDetector = GestureDetector(context, GestureListener())
        mRotationGestureDetector = RotationGestureDetector(RotationListener())

        initAnimator()

    }

    /**
     * 初始化移动和放大得用的ValueAnimator
     */
    private fun initAnimator() {
        mRotateAnimator = TransformAnimator()
        mRotateAnimator!!.duration = DEFAULT_ANIMATION_TIME
        mRotateAnimator!!.addUpdateListener { animation ->
            val goalRotate = animation.animatedValue as Float
            val postRotate = goalRotate - mRotateAnimator!!.lastRote
            mRotateAnimator!!.lastRote = goalRotate
            postRotate(postRotate, mCropRectF.centerX(), mCropRectF.centerY())
        }
        mRotateAnimator!!.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                mRotateAnimator!!.lastRote = 0f
            }

            override fun onAnimationEnd(animation: Animator) {
                checkImagePosition()
                mRotateAnimator!!.lastRote = 0f
            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        })

        mTranslateScaleAnimator = TransformAnimator()
        mTranslateScaleAnimator!!.duration = DEFAULT_ANIMATION_TIME
        mTranslateScaleAnimator!!.addUpdateListener(ValueAnimator.AnimatorUpdateListener { animation ->
            val goalTranslateX = animation.getAnimatedValue(PROPERTY_NAME_TRANSLATE_X) as Float
            val goalTranslateY = animation.getAnimatedValue(PROPERTY_NAME_TRANSLATE_Y) as Float
            val goalScale_XAndY = animation.getAnimatedValue(PROPERTY_NAME_SCALE_XANDY) as Float
            if (java.lang.Float.isNaN(goalTranslateX) || java.lang.Float.isNaN(goalTranslateY) || java.lang.Float.isNaN(goalScale_XAndY)) {
                return@AnimatorUpdateListener
            }
            Log.d(TAG, "goalTranslateX：" + goalTranslateX + "goalTranslateY：" + goalTranslateY + "goalScale_XAndY：" + goalScale_XAndY)
            val postTranslateX = goalTranslateX - mTranslateScaleAnimator!!.lastTraslateX
            val postTranslateY = goalTranslateY - mTranslateScaleAnimator!!.lastTraslateY
            val postScaleXAndY = goalScale_XAndY / mTranslateScaleAnimator!!.lastScale

            mTranslateScaleAnimator!!.lastTraslateX = goalTranslateX
            mTranslateScaleAnimator!!.lastTraslateY = goalTranslateY
            mTranslateScaleAnimator!!.lastScale = goalScale_XAndY
            Log.d(TAG, "postTranslateX：" + postTranslateX + "postTranslateY：" + postTranslateY + "postScaleXAndY：" + postScaleXAndY)
            postTranslateAndScale(postTranslateX, postTranslateY, postScaleXAndY)
        })
        mTranslateScaleAnimator!!.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                mTranslateScaleAnimator!!.lastScale = 1f
                mTranslateScaleAnimator!!.lastTraslateX = 0f
                mTranslateScaleAnimator!!.lastTraslateY = 0f
            }

            override fun onAnimationEnd(animation: Animator) {
                mTranslateScaleAnimator!!.lastScale = 1f
                mTranslateScaleAnimator!!.lastTraslateX = 0f
                mTranslateScaleAnimator!!.lastTraslateY = 0f
                // checkImagePosition();

            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {

            }
        })
    }

    /**
     * 同时位移和放大
     *
     * @param translateX  位移x量
     * @param translateY  位移y量
     * @param scale_xAndY 放大x和y的放大倍数
     */
    private fun postTranslateAndScale(translateX: Float, translateY: Float, scale_xAndY: Float) {
        mDisplayMatrix.postTranslate(translateX, translateY)
        updateBitmapRectf(mDisplayMatrix)
        mDisplayMatrix.postScale(scale_xAndY, scale_xAndY, mBitmapRectF.centerX(), mBitmapRectF.centerY())
        imageMatrix = concatMatrix
    }

    /**
     * 设置最小放大倍数
     *
     * @param MIN_SCALE 最小放大倍数
     */
    fun setMinScale(MIN_SCALE: Float) {
        this.mMinScale = MIN_SCALE
    }

    /**
     * 设置最小放大倍数
     *
     * @param MAX_SCALE 最大放大倍数
     */
    fun setMaxScale(MAX_SCALE: Float) {
        this.mMaxScale = MAX_SCALE
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (imageBitmap != null) {
            if (mCurrentActiveAnimator != null) {
                mCurrentActiveAnimator!!.cancel()
            }

            if (event.pointerCount > 1) {
                mMidPntX = (event.getX(0) + event.getX(1)) / 2//算出中心点
                mMidPntY = (event.getY(0) + event.getY(1)) / 2
            }


            if (mScaleEnable) {
                mScaleGestureDetector!!.onTouchEvent(event)
            }
            if (mRotateEnable) {
                mRotationGestureDetector!!.onTouchEvent(event)
            }

            if (!mScaleGestureDetector!!.isInProgress) {
                //检测拖动
                mGestureDetector!!.onTouchEvent(event)
            }

            if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_UP) {//松手动手
                checkImagePosition()
            }
            return true
        } else {
            return false
        }
    }


    /**
     * 放大设置开关
     *
     * @param mScaleEnable 是否开启
     */
    fun setScaleEnable(mScaleEnable: Boolean) {
        this.mScaleEnable = mScaleEnable
    }

    /**
     * 旋转开关
     *
     * @param mRotateEnable 是否开启
     */
    fun setRotateEnable(mRotateEnable: Boolean) {
        this.mRotateEnable = mRotateEnable
    }


    override fun setScaleType(scaleType: ImageView.ScaleType) {
        if (scaleType != ImageView.ScaleType.MATRIX) {
            throw IllegalArgumentException("scaleType must be matrix")
        } else {
            super.setScaleType(scaleType)
        }
    }

    /**
     * 检车是否越界
     */
    private fun checkImagePosition() {
        Log.d(TAG, "现在放大倍数为：" + currentScale + "初始的放大倍数为：" + mImageInfo!!.initScale)
        if (currentScale > mMaxScale) {
            backToMaxScale()
        }
        mDisplayMatrix.mapPoints(mCurrentImageCorners, mInitImageCorners)//求出当前的坐标
        var dx: Float
        var dy: Float//中心便宜距离
        var deltaScale = 1f
        updateBitmapRectf(mDisplayMatrix)
        dx = mCropRectF.centerX() - mBitmapRectF.centerX()//拿到需要移动距离
        dy = mCropRectF.centerY() - mBitmapRectF.centerY()
        //判断回到中心后能不能回到覆盖
        mTempMatrix.reset()
        mTempMatrix.set(mDisplayMatrix)//设置当前的位置
        mTempMatrix.postTranslate(dx, dy)//回到中心点
        val tempImageCorners = Arrays.copyOf(mInitImageCorners!!, mInitImageCorners!!.size)//拷贝一份初始点
        mTempMatrix.mapPoints(tempImageCorners)//获取移动到中心点的各个坐标

        val isWillImageWrapCropBounds = isImageWrapCropBounds(tempImageCorners)
        Log.d(TAG, "checkImagePosition假如图片移到中心,是否覆盖裁剪框$isWillImageWrapCropBounds")
        //可以话只求出移动的距离，否则求出放大倍数
        if (isWillImageWrapCropBounds) {
            val imageIndents = calculateImageIndents()
            dx = -(imageIndents[0] + imageIndents[2])//估计现在和那个之前的距离
            dy = -(imageIndents[1] + imageIndents[3])
        } else {//表示没有包裹
            val tempCropRect = RectF(mCropRectF)
            mTempMatrix.reset()
            mTempMatrix.setRotate(currentAngle)//摸你旋转角度
            mTempMatrix.mapRect(tempCropRect)//吧矩形框转到和图像一样的角度

            val currentImageSides = RectUtils.getRectSidesFromCorners(mCurrentImageCorners)//得到分别是宽和高

            deltaScale = Math.max(tempCropRect.width() / currentImageSides[0],
                    tempCropRect.height() / currentImageSides[1])
            Log.d(TAG, "checkImagePosition不可以覆盖裁剪框，需要放大倍数为dx$deltaScale")
        }

        //再放大
        if (isWillImageWrapCropBounds) {//只移动就可以
            mTranslateScaleAnimator!!.setValues(PropertyValuesHolder.ofFloat(PROPERTY_NAME_TRANSLATE_X, 0f, dx),
                    PropertyValuesHolder.ofFloat(PROPERTY_NAME_TRANSLATE_Y, 0f, dy),
                    PropertyValuesHolder.ofFloat(PROPERTY_NAME_SCALE_XANDY, 1f, 1f))//相当于不放大
            Log.d(TAG, "可以覆盖裁剪框，需要移动距离为dx$dx dy $dy")
        } else {
            mTranslateScaleAnimator!!.setValues(PropertyValuesHolder.ofFloat(PROPERTY_NAME_TRANSLATE_X, 0f, dx),
                    PropertyValuesHolder.ofFloat(PROPERTY_NAME_TRANSLATE_Y, 0f, dy),
                    PropertyValuesHolder.ofFloat(PROPERTY_NAME_SCALE_XANDY, 1f, deltaScale))//相当于不放大
            Log.d(TAG, "可以覆盖裁剪框，需要移动距离为dx$dx dy $dy")
        }
        //设置矩阵并重绘
        mCurrentActiveAnimator = mTranslateScaleAnimator
        mTranslateScaleAnimator!!.start()

    }


    private fun backToMaxScale() {
        val scale = mMaxScale / currentScale
        Log.d(TAG, "要回弹的倍数" + scale + "到最大倍数" + mMaxScale)
        mDisplayMatrix.postScale(scale, scale, mCropRectF.centerX(), mCropRectF.centerY())
        imageMatrix = concatMatrix
    }


    /**
     * 计算出当图片小于裁剪框时候的间距，即离那个角的平行距离
     *
     * @return 4个角垂直距离
     */
    private fun calculateImageIndents(): FloatArray {
        mTempMatrix.reset()
        mTempMatrix.setRotate(-currentAngle)

        val unrotatedImageCorners = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.size)
        val unrotatedCropBoundsCorners = RectUtils.getCornersFromRect(mCropRectF)

        mTempMatrix.mapPoints(unrotatedImageCorners)
        mTempMatrix.mapPoints(unrotatedCropBoundsCorners)

        val unrotatedImageRect = RectUtils.trapToRect(unrotatedImageCorners)
        val unrotatedCropRect = RectUtils.trapToRect(unrotatedCropBoundsCorners)

        val deltaLeft = unrotatedImageRect.left - unrotatedCropRect.left
        val deltaTop = unrotatedImageRect.top - unrotatedCropRect.top
        val deltaRight = unrotatedImageRect.right - unrotatedCropRect.right
        val deltaBottom = unrotatedImageRect.bottom - unrotatedCropRect.bottom

        val indents = FloatArray(4)
        indents[0] = if (deltaLeft > 0) deltaLeft else 0f
        indents[1] = if (deltaTop > 0) deltaTop else 0f
        indents[2] = if (deltaRight < 0) deltaRight else 0f
        indents[3] = if (deltaBottom < 0) deltaBottom else 0f

        mTempMatrix.reset()
        mTempMatrix.setRotate(currentAngle)
        mTempMatrix.mapPoints(indents)

        return indents
    }

    /**
     * 计算是图片是否包裹了裁剪框
     *
     * @param imageCorners 图片各个角的坐标
     * @return true表示图片够大如果移动到中心已经包裹裁剪框
     */
    protected fun isImageWrapCropBounds(imageCorners: FloatArray): Boolean {
        mTempMatrix.reset()
        mTempMatrix.setRotate(-currentAngle)

        val unrotatedImageCorners = Arrays.copyOf(imageCorners, imageCorners.size)
        mTempMatrix.mapPoints(unrotatedImageCorners)//把矩阵摆正嘛？

        val unrotatedCropBoundsCorners = RectUtils.getCornersFromRect(mCropRectF)
        mTempMatrix.mapPoints(unrotatedCropBoundsCorners)
        //脑残吧？把图片旋转却旋转框框，
        return RectUtils.trapToRect(unrotatedImageCorners).contains(RectUtils.trapToRect(unrotatedCropBoundsCorners))

    }


    /**
     * @param matrix 应为这个库xy放大倍数相同，所以只去一个方向放大倍数代表现在放大倍数
     * @return 放大倍数
     */
    fun getMatrixScale(matrix: Matrix): Float {
        return Math.sqrt(Math.pow(getMatrixValue(matrix, Matrix.MSCALE_X).toDouble(), 2.0) + Math.pow(getMatrixValue(matrix, Matrix.MSKEW_Y).toDouble(), 2.0)).toFloat()
    }

    /**
     * 获得对应矩阵的旋转角度
     */
    fun getMatrixAngle(matrix: Matrix): Float {
        return (-(Math.atan2(getMatrixValue(matrix, Matrix.MSKEW_X).toDouble(),
                getMatrixValue(matrix, Matrix.MSCALE_X).toDouble()) * (180 / Math.PI))).toFloat()
    }

    protected fun getMatrixValue(matrix: Matrix, valueIndex: Int): Float {
        matrix.getValues(mMatrixValue)
        return mMatrixValue[valueIndex]
    }

    /**
     * 设置成水平镜像
     */
    fun setHorizontalMirror() {


        mMirrorMatrix.postScale(-1f, 1f, mCropRectF.centerX(), mCropRectF.centerY())
        imageMatrix = concatMatrix
        isHorizontalEnable = !isHorizontalEnable
        invalidate()

    }


    /**
     * 设置成水平镜像
     */
    fun setVerticalMirror() {
        mMirrorMatrix.postScale(1f, -1f, mCropRectF.centerX(), mCropRectF.centerY())
        imageMatrix = concatMatrix
        isVerticalEnable = true
        invalidate()
    }

    fun rightRotate90() {
        postAnyRotate(90f)
    }

    private fun postRotate(angel: Float, centerX: Float, centerY: Float) {
        mDisplayMatrix.postRotate(angel, centerX, centerY)
        imageMatrix = concatMatrix
    }

    fun leftRotate90() {
        postAnyRotate(-90f)
    }

    fun postAnyRotate(anyAngel: Float) {
        if (imageBitmap != null) {
            mRotateAnimator!!.cancel()
            mRotateAnimator!!.setFloatValues(0f, anyAngel)
            mRotateAnimator!!.duration = DEFAULT_ANIMATION_TIME
            mCurrentActiveAnimator = mRotateAnimator
            mRotateAnimator!!.start()
        }
    }


    /**
     * 移动Bitmap的位置
     *
     * @param dx x轴移动的距离
     * @param dy y轴移动的距离
     */
    private fun moveImage(dx: Float, dy: Float) {
        mDisplayMatrix.postTranslate(dx, dy)
        imageMatrix = concatMatrix
        invalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val mThisWidth = measuredWidth.toFloat()
        val mThisHeight = measuredHeight.toFloat()

        mCropRectF.set(0f, (mThisHeight - mThisWidth) / 2, mThisWidth, (mThisHeight + mThisWidth) / 2)//这里初始化好矩形框框的范围

        if (drawable != null) {
            getProperMatrix(mBaseMatrix)//获取矩阵，用来设置矩阵
            //拷贝矩阵
            mDisplayMatrix.set(mBaseMatrix)
            imageMatrix = concatMatrix

            //设置化映射矩阵
            mBitmapRectF.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())

            mInitImageCorners = RectUtils.getCornersFromRect(mBitmapRectF)//获取初始化的点
        }
    }

    private fun updateBitmapRectf(displayMatrix: Matrix) {
        if (drawable != null) {
            mBitmapRectF.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
            displayMatrix.mapRect(mBitmapRectF)
        }
    }

    /**
     * 获取让图片移动并居中到裁剪框的举证
     *
     * @param baseMatrix 展示用的矩阵
     */
    private fun getProperMatrix(baseMatrix: Matrix) {
        baseMatrix.reset()
        val drawable = drawable
        val intrinsicWidth = drawable.intrinsicWidth.toFloat()
        val intrinsicHeight = drawable.intrinsicHeight.toFloat()
        val drawableRatio = intrinsicHeight / intrinsicWidth//图片的高/宽 比例

        val cropRectWidth = mCropRectF.width()
        val cropRectHeight = mCropRectF.height()
        val cropRectRatio = cropRectHeight / cropRectWidth

        //
        val scale: Float
        var moveX = 0f
        val moveY: Float
        if (drawableRatio > cropRectRatio) {//表示是长图，就是高大于宽
            //按照宽的比例来扩大
            scale = cropRectWidth / intrinsicWidth
            moveY = (measuredHeight - intrinsicHeight * scale) / 2//视图的高度减去图片的扩大后的高度/2
        } else {
            //按照高的比例来扩大
            scale = cropRectHeight / intrinsicHeight
            moveX = (cropRectWidth - scale * intrinsicWidth) / 2
            moveY = mCropRectF.top
        }
        baseMatrix.postScale(scale, scale)//设置恰当的放大倍数
        baseMatrix.postTranslate(moveX, moveY)
    }

    override fun onDraw(canvas: Canvas) {
        val start = System.currentTimeMillis()
        Log.d(TAG, mDisplayMatrix.toString())
        super.onDraw(canvas)
        drawTransParentLayer(canvas)
        drawCropRect(canvas)

        if (drawable != null) {
            logMatrixInfo(imageMatrix)
            if (mImageInfo == null) {//第一次才需要记录，最开始高宽和长度，和放大倍数
                setImageInfo()
            }
        }
        Log.d("onDrawTime", "" + (System.currentTimeMillis() - start))
    }

    private fun setImageInfo() {

        mImageInfo = ImageInfo(drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat(), getMatrixScale(mDisplayMatrix))
    }

    /**
     * 绘画阴影区域
     *
     * @param canvas 画布
     */
    private fun drawTransParentLayer(canvas: Canvas) {
        val r = Rect()
        getLocalVisibleRect(r)
        canvas.drawRect(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), mCropRectF.top, mTransParentLayerPaint!!)
        canvas.drawRect(r.left.toFloat(), mCropRectF.bottom, r.right.toFloat(), r.bottom.toFloat(), mTransParentLayerPaint!!)
    }

    private fun drawCropRect(canvas: Canvas) {
        val halfLineWidth = mWhiteCropPaint!!.strokeWidth * 0.5f
        canvas.drawRect(mCropRectF.left + halfLineWidth, mCropRectF.top - halfLineWidth, mCropRectF.right - halfLineWidth, mCropRectF.bottom + halfLineWidth, mWhiteCropPaint!!)
    }


    override fun setImageURI(uri: Uri?) {
        if (uri != null) {
            mUri = uri
            //super.setImageURI(uri);
            try {
                val bmp = ImageLoadUtil.loadImage(context.contentResolver, uri, 1500, 1500)
                if(bmp!=null) {
                    val rotatedBitmap = ImageLoadUtil.checkBitmapOrientation(context.contentResolver, uri, bmp)//检查图片方向
                    setImageBitmap(rotatedBitmap)
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

        }
    }

    fun setDrawable(drawable: Drawable) {
        setImageDrawable(drawable)
        requestLayout()
        invalidate()
    }

    /**
     * 将图片扩大到指定倍数
     *
     * @param mScaleFactor 扩大倍数
     * @param focusX       放大中心点x坐标
     * @param focusY       放大中心点y坐标
     */
    private fun postScale(mScaleFactor: Float, focusX: Float, focusY: Float) {

        mDisplayMatrix.postScale(mScaleFactor, mScaleFactor, focusX, focusY)
        imageMatrix = concatMatrix
    }

    /**
     * 打印检查log
     *
     * @param matrix 需要检查的矩阵
     */
    private fun logMatrixInfo(matrix: Matrix) {
        matrix.getValues(mMatrixValue)
        Log.d(TAG, "SCALEX：" + mMatrixValue[Matrix.MSCALE_X] + "ScaleY: " + mMatrixValue[Matrix.MSCALE_Y] + "transX "
                + mMatrixValue[Matrix.MTRANS_X] + " transY " + mMatrixValue[Matrix.MTRANS_Y])
        Log.d(TAG, "Drawable width " + drawable.intrinsicWidth + "Drawable height" + drawable.intrinsicHeight)
        Log.d(TAG, "BitmapRect " + mBitmapRectF.width() + " Bitmap left " + mBitmapRectF.left)

        val r = RectF(0f, 0f, 100f, 100f)
        val m = Matrix()
        m.setRotate(100f)
        m.mapRect(r)

        Log.d(TAG, "rotateRectWidth" + r.width())
    }


    /**
     * 裁剪并保存图片
     *
     * @return 返回需要保存图片的BItmap对象
     */
    fun cropAndSaveImage(): Bitmap? {
        val bitmap = imageBitmap
        var originBitmapFromUri: Bitmap? = null
        if (bitmap != null) {
            //当前的大图
            try {
                val uri = mUri
                if(uri!=null) {
                    originBitmapFromUri = ImageLoadUtil.loadImage(context.contentResolver, uri, Integer.MAX_VALUE, Integer.MAX_VALUE)//可能oom
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

        }

        //此时已经拿到最初的放大倍数
        //求出裁剪框和大图的相对位置dx,dy;
        if (bitmap != null && originBitmapFromUri != null) {
            val matrix = Matrix()
            var scale = 1f
            var scaledBitmap: Bitmap
            var resultBitmap: Bitmap? = null
            var isSuccess = false
            var needScale = false
            val currentRotatedBitmap = getCurrentRotatedOriginalBitmap(originBitmapFromUri) //,拿到旋转图片
            while (!isSuccess) {
                try {
                    if (needScale) {//第一次不需要放大，直接裁剪，
                        matrix.postScale(scale, scale, (currentRotatedBitmap.width / 2).toFloat(), (currentRotatedBitmap.height / 2).toFloat())
                        scaledBitmap = Bitmap.createBitmap(currentRotatedBitmap, 0, 0, currentRotatedBitmap.width, currentRotatedBitmap.height, matrix, true)
                        currentRotatedBitmap.recycle()
                        resultBitmap = scaledBitmap
                        isSuccess = true
                    }
                    return getCropBitmapInOriginalBitmap(currentRotatedBitmap)//拿到裁剪框位置的图片,

                } catch (e: OutOfMemoryError) {
                    scale = scale * 0.7f
                    needScale = true
                }

            }
            return resultBitmap
        } else {
            return null
        }
    }


    /**
     * 拿到裁剪框所在原图的位置
     *
     * @param currentRotatedBitmap 旋转后的原图
     * @return 裁剪框所在位置的图片
     * @throws OutOfMemoryError 内存溢出
     */
    @Throws(OutOfMemoryError::class)
    fun getCropBitmapInOriginalBitmap(currentRotatedBitmap: Bitmap): Bitmap {


        val scale_x = mBitmapRectF.width() / currentRotatedBitmap.width
        val scale_y = mBitmapRectF.height() / currentRotatedBitmap.height
        val initScale = Math.min(scale_x, scale_y)//这个裁剪框和要裁剪的原图的长宽比
        //算出裁剪框所在原图的位置，还有裁剪框映射到原图的长和宽
        val dx = ((mCropRectF.left - mBitmapRectF.left) / initScale).toInt()
        val dy = ((mCropRectF.top - mBitmapRectF.top) / initScale).toInt()
        val width = (mCropRectF.width().toInt() / initScale).toInt()
        val height = (mCropRectF.height().toInt() / initScale).toInt()
        return Bitmap.createBitmap(currentRotatedBitmap, dx, dy, width, height)//这个为输出文件

    }

    /**
     * 产生旋转后的源图片
     *
     * @param originBitmap 原图
     * @return 旋转后的图片
     * @throws OutOfMemoryError 内存溢出
     */
    private fun getCurrentRotatedOriginalBitmap(originBitmap: Bitmap): Bitmap {
        updateBitmapRectf(mDisplayMatrix)//mBitmapRectf就代表当前矩阵
        //获得旋转后的图片
        val uri = mUri
        if (uri!=null) {
            return ImageLoadUtil.rotateBitmap(originBitmap, currentAngle - ImageLoadUtil.getBitmapOrientation(context.contentResolver, uri))
        }else{
            // todo you bug
            return originBitmap
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            var distanceX = distanceX
            var distanceY = distanceY
            if (e1 == null || e2 == null) return false
            if (e1.pointerCount > 1 || e2.pointerCount > 1) return false
            if (mScaleGestureDetector!!.isInProgress) return false
            // TODO: 2017/7/25 如果改成先镜像后移动应该可以解决这个问题
            if (isHorizontalEnable) {//设置了水平镜像,往反方向移动，以为镜像是以裁剪框为中心的
                distanceX = -distanceX
            }
            if (isVerticalEnable) {//设置了水平镜像,往反方向移动，以为镜像是以裁剪框为中心的
                distanceY = -distanceY
            }
            this@CropImageView.onScroll(distanceX, distanceY)
            return true
        }
    }

    private fun onScroll(distanceX: Float, distanceY: Float) {
        Log.d(TAG, "onScroll dx " + distanceX + "dy" + distanceY)
        moveImage(-distanceX, -distanceY)
    }


    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        private var mScaleFactor = 1f


        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mScaleFactor = detector.scaleFactor


            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, mMaxScale))
            mScaleFactor = checkScale(mScaleFactor)
            Log.d(TAG, "最终手势放大的放大倍数postScale为$mScaleFactor")
            postScale(mScaleFactor, detector.focusX, detector.focusY)
            Log.d(TAG, "放大focusX" + detector.focusX + "放大focusY" + detector.focusY)
            mImageInfo!!.gestureScale = mImageInfo!!.gestureScale * mScaleFactor//设置当前放大倍数
            return true
        }

        private fun checkScale(scaleFactor: Float): Float {
            val finalScale: Float
            val currentScale = currentScale
            Log.d(TAG, "当前的放大倍数$currentScale")
            if (currentScale * mScaleFactor <= mMinScale) {//如果超过最小值，则就直接到最小值
                finalScale = mMinScale / currentScale
            } else if (currentScale * mScaleFactor >= mMaxScale * 1.5f) {
                finalScale = mMaxScale * 1.5f / currentScale
            } else {
                finalScale = scaleFactor
            }
            return finalScale
        }


    }

    private inner class RotationListener : RotationGestureDetector.SimpleOnRotationGestureListener() {

        override fun onRotation(rotationDetector: RotationGestureDetector): Boolean {
            var angle = rotationDetector.angle
            if (isHorizontalEnable) {
                angle = -angle
            }
            postRotate(angle, mMidPntX, mMidPntY)
            return true
        }
    }



    private inner class TransformAnimator : ValueAnimator() {


         var lastRote = 0f
         var lastTraslateX = 0f
         var lastTraslateY = 0f
         var lastScale = 1f


    }

    companion object {


        private val DEFAULT_BACKGROUND_COLOR_ID = "#99000000"//超过裁剪部分的矩形框
        private val TAG = "CropImageView"
        private val DEFAULT_ANIMATION_TIME: Long = 500

        val PROPERTY_NAME_TRANSLATE_X = "TRANSLATE_X"
        val PROPERTY_NAME_TRANSLATE_Y = "TRANSLATE_Y"
        val PROPERTY_NAME_SCALE_XANDY = "SCALE"
    }

}
