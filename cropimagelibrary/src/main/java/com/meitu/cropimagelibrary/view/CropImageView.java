package com.meitu.cropimagelibrary.view;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.meitu.cropimagelibrary.info.ImageInfo;
import com.meitu.cropimagelibrary.util.ImageLoadUtil;
import com.meitu.cropimagelibrary.util.RectUtils;
import com.meitu.cropimagelibrary.util.RotationGestureDetector;

import java.io.FileNotFoundException;
import java.util.Arrays;

/**
 * Created by zmc on 2017/7/18.
 */

public class CropImageView extends android.support.v7.widget.AppCompatImageView {


    private static final String DEFAULT_BACKGROUND_COLOR_ID = "#99000000";//超过裁剪部分的矩形框
    private static final String TAG = "CropImageView";
    private static final long DEFAULT_ANIMATION_TIME = 500;
    private boolean isHorizontalEnable = false;
    private boolean isVerticalEnable = false;
    private float mMaxScale = 3f;


    private float mMinScale = 0.8f;
    private boolean mScaleEnable = true;
    private boolean mRotateEnable = true;


    private final Matrix mBaseMatrix = new Matrix();
    private final Matrix mDisplayMatrix = new Matrix();
    private final Matrix mTempMatrix = new Matrix();
    private final Matrix mMirrorMatrix = new Matrix();
    private final Matrix mConcatMatrix = new Matrix();

    private final RectF mCropRectF = new RectF();//裁剪框矩形区域
    private final RectF mBitmapRectF = new RectF();//当前的矩形区域

    private Paint mTransParentLayerPaint;//暗色区域背景
    private Paint mWhiteCropPaint;


    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;
    private RotationGestureDetector mRotationGestureDetector;

    private float mMidPntX, mMidPntY;//手指中心点


    private final float[] mMatrixValue = new float[9];

    private ImageInfo mImageInfo;//最开始图片信息,好像可以删掉


    private final float[] mCurrentImageCorners = new float[8];//用来存放当前顶点坐标啊
    private float[] mInitImageCorners;
    private Uri mUri;//图片的uri

    private TransformAnimator mRotateAnimator;
    private TransformAnimator mTranslateScaleAnimator;
    private Animator mCurrentActiveAnimator;


    public CropImageView(Context context) {
        this(context, null, 0);
    }

    public CropImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CropImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化裁剪框和阴影区域的画笔
     */
    private void initCropMaterials() {

        mTransParentLayerPaint = new Paint();
        mTransParentLayerPaint.setColor(Color.parseColor(DEFAULT_BACKGROUND_COLOR_ID));//设置颜色

        mWhiteCropPaint = new Paint();
        mWhiteCropPaint.setColor(Color.WHITE);//设置颜色
        mWhiteCropPaint.setStrokeWidth(1);//设置填充宽度
        mWhiteCropPaint.setStyle(Paint.Style.STROKE);//what？？
    }


    private void init() {
        initCropMaterials();

        mInitImageCorners = new float[8];

        mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        mGestureDetector = new GestureDetector(getContext(), new GestureListener());
        mRotationGestureDetector = new RotationGestureDetector(new RotationListener());

        initAnimator();

    }

    /**
     * 初始化移动和放大得用的ValueAnimator
     */
    private void initAnimator() {
        mRotateAnimator = new TransformAnimator();
        mRotateAnimator.setDuration(DEFAULT_ANIMATION_TIME);
        mRotateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float goalRotate = (float) animation.getAnimatedValue();
                float postRotate = goalRotate - mRotateAnimator.getLastRote();
                mRotateAnimator.setLastRote(goalRotate);
                postRotate(postRotate, mCropRectF.centerX(), mCropRectF.centerY());
            }
        });
        mRotateAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mRotateAnimator.setLastRote(0);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                checkImagePosition();
                mRotateAnimator.setLastRote(0);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        mTranslateScaleAnimator = new TransformAnimator();
        mTranslateScaleAnimator.setDuration(DEFAULT_ANIMATION_TIME);
        mTranslateScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float goalTranslateX = (float) animation.getAnimatedValue(TransformAnimator.PROPERTY_NAME_TRANSLATE_X);
                float goalTranslateY = (float) animation.getAnimatedValue(TransformAnimator.PROPERTY_NAME_TRANSLATE_Y);
                float goalScale_XAndY = (float) animation.getAnimatedValue(TransformAnimator.PROPERTY_NAME_SCALE_XANDY);
                if (Float.isNaN(goalTranslateX) || Float.isNaN(goalTranslateY) || Float.isNaN(goalScale_XAndY)) {
                    return;
                }
                Log.d(TAG, "goalTranslateX：" + goalTranslateX + "goalTranslateY：" + goalTranslateY + "goalScale_XAndY：" + goalScale_XAndY);
                float postTranslateX = goalTranslateX - mTranslateScaleAnimator.getLastTraslateX();
                float postTranslateY = goalTranslateY - mTranslateScaleAnimator.getLastTraslateY();
                float postScaleXAndY = goalScale_XAndY / mTranslateScaleAnimator.getLastScale();

                mTranslateScaleAnimator.setLastTraslateX(goalTranslateX);
                mTranslateScaleAnimator.setLastTraslateY(goalTranslateY);
                mTranslateScaleAnimator.setLastScale(goalScale_XAndY);
                Log.d(TAG, "postTranslateX：" + postTranslateX + "postTranslateY：" + postTranslateY + "postScaleXAndY：" + postScaleXAndY);
                postTranslateAndScale(postTranslateX, postTranslateY, postScaleXAndY);
            }
        });
        mTranslateScaleAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mTranslateScaleAnimator.setLastScale(1);
                mTranslateScaleAnimator.setLastTraslateX(0);
                mTranslateScaleAnimator.setLastTraslateY(0);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mTranslateScaleAnimator.setLastScale(1);
                mTranslateScaleAnimator.setLastTraslateX(0);
                mTranslateScaleAnimator.setLastTraslateY(0);
                // checkImagePosition();

            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    /**
     * 同时位移和放大
     *
     * @param translateX  位移x量
     * @param translateY  位移y量
     * @param scale_xAndY 放大x和y的放大倍数
     */
    private void postTranslateAndScale(float translateX, float translateY, float scale_xAndY) {
        mDisplayMatrix.postTranslate(translateX, translateY);
        updateBitmapRectf(mDisplayMatrix);
        mDisplayMatrix.postScale(scale_xAndY, scale_xAndY, mBitmapRectF.centerX(), mBitmapRectF.centerY());
        setImageMatrix(getConcatMatrix());
    }

    /**
     * 设置最小放大倍数
     *
     * @param MIN_SCALE 最小放大倍数
     */
    public void setMinScale(float MIN_SCALE) {
        this.mMinScale = MIN_SCALE;
    }

    /**
     * 设置最小放大倍数
     *
     * @param MAX_SCALE 最大放大倍数
     */
    public void setMaxScale(float MAX_SCALE) {
        this.mMaxScale = MAX_SCALE;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (mCurrentActiveAnimator != null) {
            mCurrentActiveAnimator.cancel();
        }

        if (event.getPointerCount() > 1) {
            mMidPntX = (event.getX(0) + event.getX(1)) / 2;//算出中心点
            mMidPntY = (event.getY(0) + event.getY(1)) / 2;
        }


        if (mScaleEnable) {
            mScaleGestureDetector.onTouchEvent(event);
        }
        if (mRotateEnable) {
            mRotationGestureDetector.onTouchEvent(event);
        }

        if (!mScaleGestureDetector.isInProgress()) {
            //检测拖动
            mGestureDetector.onTouchEvent(event);
        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {//松手动手
            checkImagePosition();
        }
        return true;
    }


    /**
     * 放大设置开关
     *
     * @param mScaleEnable 是否开启
     */
    public void setScaleEnable(boolean mScaleEnable) {
        this.mScaleEnable = mScaleEnable;
    }

    /**
     * 旋转开关
     *
     * @param mRotateEnable 是否开启
     */
    public void setRotateEnable(boolean mRotateEnable) {
        this.mRotateEnable = mRotateEnable;
    }


    @Override
    public void setScaleType(ScaleType scaleType) {
        if (scaleType != ScaleType.MATRIX) {
            throw new IllegalArgumentException("scaleType must be matrix");
        } else {
            super.setScaleType(scaleType);
        }
    }

    /**
     * 检车是否越界
     */
    private void checkImagePosition() {
        Log.d(TAG, "现在放大倍数为：" + getCurrentScale() + "初始的放大倍数为：" + mImageInfo.getInitScale());
        if (getCurrentScale() > mMaxScale) {
            backToMaxScale();
        }
        mDisplayMatrix.mapPoints(mCurrentImageCorners, mInitImageCorners);//求出当前的坐标
        float dx, dy;//中心便宜距离
        float deltaScale = 1;
        updateBitmapRectf(mDisplayMatrix);
        dx = mCropRectF.centerX() - mBitmapRectF.centerX();//拿到需要移动距离
        dy = mCropRectF.centerY() - mBitmapRectF.centerY();
        //判断回到中心后能不能回到覆盖
        mTempMatrix.reset();
        mTempMatrix.set(mDisplayMatrix);//设置当前的位置
        mTempMatrix.postTranslate(dx, dy);//回到中心点
        float[] tempImageCorners = Arrays.copyOf(mInitImageCorners, mInitImageCorners.length);//拷贝一份初始点
        mTempMatrix.mapPoints(tempImageCorners);//获取移动到中心点的各个坐标

        boolean isWillImageWrapCropBounds = isImageWrapCropBounds(tempImageCorners);
        Log.d(TAG, "checkImagePosition" + "假如图片移到中心,是否覆盖裁剪框" + isWillImageWrapCropBounds);
        //可以话只求出移动的距离，否则求出放大倍数
        if (isWillImageWrapCropBounds) {
            final float[] imageIndents = calculateImageIndents();
            dx = -(imageIndents[0] + imageIndents[2]);//估计现在和那个之前的距离
            dy = -(imageIndents[1] + imageIndents[3]);
        } else {//表示没有包裹
            RectF tempCropRect = new RectF(mCropRectF);
            mTempMatrix.reset();
            mTempMatrix.setRotate(getCurrentAngle());//摸你旋转角度
            mTempMatrix.mapRect(tempCropRect);//吧矩形框转到和图像一样的角度

            final float[] currentImageSides = RectUtils.getRectSidesFromCorners(mCurrentImageCorners);//得到分别是宽和高

            deltaScale = Math.max(tempCropRect.width() / currentImageSides[0],
                    tempCropRect.height() / currentImageSides[1]);
            Log.d(TAG, "checkImagePosition" + "不可以覆盖裁剪框，需要放大倍数为" + "dx" + deltaScale);
        }

        //再放大
        if (isWillImageWrapCropBounds) {//只移动就可以
            mTranslateScaleAnimator.setValues(PropertyValuesHolder.ofFloat(TransformAnimator.PROPERTY_NAME_TRANSLATE_X, 0, dx),
                    PropertyValuesHolder.ofFloat(TransformAnimator.PROPERTY_NAME_TRANSLATE_Y, 0, dy),
                    PropertyValuesHolder.ofFloat(TransformAnimator.PROPERTY_NAME_SCALE_XANDY, 1f, 1f));//相当于不放大
            Log.d(TAG, "可以覆盖裁剪框，需要移动距离为" + "dx" + dx + " dy " + dy);
        } else {
            mTranslateScaleAnimator.setValues(PropertyValuesHolder.ofFloat(TransformAnimator.PROPERTY_NAME_TRANSLATE_X, 0, dx),
                    PropertyValuesHolder.ofFloat(TransformAnimator.PROPERTY_NAME_TRANSLATE_Y, 0, dy),
                    PropertyValuesHolder.ofFloat(TransformAnimator.PROPERTY_NAME_SCALE_XANDY, 1f, deltaScale));//相当于不放大
            Log.d(TAG, "可以覆盖裁剪框，需要移动距离为" + "dx" + dx + " dy " + dy);
        }
        //设置矩阵并重绘
        mCurrentActiveAnimator = mTranslateScaleAnimator;
        mTranslateScaleAnimator.start();

    }


    private void backToMaxScale() {
        float scale = mMaxScale / getCurrentScale();
        Log.d(TAG, "要回弹的倍数" + scale + "到最大倍数" + mMaxScale);
        mDisplayMatrix.postScale(scale, scale, mCropRectF.centerX(), mCropRectF.centerY());
        setImageMatrix(getConcatMatrix());
    }


    /**
     * 计算出当图片小于裁剪框时候的间距，即离那个角的平行距离
     *
     * @return 4个角垂直距离
     */
    private float[] calculateImageIndents() {
        mTempMatrix.reset();
        mTempMatrix.setRotate(-getCurrentAngle());

        float[] unrotatedImageCorners = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.length);
        float[] unrotatedCropBoundsCorners = RectUtils.getCornersFromRect(mCropRectF);

        mTempMatrix.mapPoints(unrotatedImageCorners);
        mTempMatrix.mapPoints(unrotatedCropBoundsCorners);

        RectF unrotatedImageRect = RectUtils.trapToRect(unrotatedImageCorners);
        RectF unrotatedCropRect = RectUtils.trapToRect(unrotatedCropBoundsCorners);

        float deltaLeft = unrotatedImageRect.left - unrotatedCropRect.left;
        float deltaTop = unrotatedImageRect.top - unrotatedCropRect.top;
        float deltaRight = unrotatedImageRect.right - unrotatedCropRect.right;
        float deltaBottom = unrotatedImageRect.bottom - unrotatedCropRect.bottom;

        float indents[] = new float[4];
        indents[0] = (deltaLeft > 0) ? deltaLeft : 0;
        indents[1] = (deltaTop > 0) ? deltaTop : 0;
        indents[2] = (deltaRight < 0) ? deltaRight : 0;
        indents[3] = (deltaBottom < 0) ? deltaBottom : 0;

        mTempMatrix.reset();
        mTempMatrix.setRotate(getCurrentAngle());
        mTempMatrix.mapPoints(indents);

        return indents;
    }

    /**
     * 计算是图片是否包裹了裁剪框
     *
     * @param imageCorners 图片各个角的坐标
     * @return true表示图片够大如果移动到中心已经包裹裁剪框
     */
    protected boolean isImageWrapCropBounds(float[] imageCorners) {
        mTempMatrix.reset();
        mTempMatrix.setRotate(-getCurrentAngle());

        float[] unrotatedImageCorners = Arrays.copyOf(imageCorners, imageCorners.length);
        mTempMatrix.mapPoints(unrotatedImageCorners);//把矩阵摆正嘛？

        float[] unrotatedCropBoundsCorners = RectUtils.getCornersFromRect(mCropRectF);
        mTempMatrix.mapPoints(unrotatedCropBoundsCorners);
        //脑残吧？把图片旋转却旋转框框，
        return RectUtils.trapToRect(unrotatedImageCorners).contains(RectUtils.trapToRect(unrotatedCropBoundsCorners));

    }


    /**
     * 获得当前的旋转角度
     */
    public float getCurrentAngle() {
        return getMatrixAngle(mDisplayMatrix);
    }

    /**
     * 获得当前的放大倍数
     */
    public float getCurrentScale() {
        float displayScale = getMatrixScale(mDisplayMatrix);
        return displayScale / mImageInfo.getInitScale();
    }


    /**
     * @param matrix 应为这个库xy放大倍数相同，所以只去一个方向放大倍数代表现在放大倍数
     * @return 放大倍数
     */
    public float getMatrixScale(@NonNull Matrix matrix) {
        return (float) Math.sqrt(Math.pow(getMatrixValue(matrix, Matrix.MSCALE_X), 2) + Math.pow(getMatrixValue(matrix, Matrix.MSKEW_Y), 2));
    }

    /**
     * 获得对应矩阵的旋转角度
     */
    public float getMatrixAngle(@NonNull Matrix matrix) {
        return (float) -(Math.atan2(getMatrixValue(matrix, Matrix.MSKEW_X),
                getMatrixValue(matrix, Matrix.MSCALE_X)) * (180 / Math.PI));
    }

    protected float getMatrixValue(@NonNull Matrix matrix, int valueIndex) {
        matrix.getValues(mMatrixValue);
        return mMatrixValue[valueIndex];
    }

    /**
     * 设置成水平镜像
     */
    public void setHorizontalMirror() {


        mMirrorMatrix.postScale(-1f, 1f, mCropRectF.centerX(), mCropRectF.centerY());
        setImageMatrix(getConcatMatrix());
        isHorizontalEnable = !isHorizontalEnable;
        invalidate();

    }

    /**
     * 连接矩阵，主要将display矩阵和镜像矩阵进行连接
     *
     * @return 返回合成后的矩阵
     */
    public Matrix getConcatMatrix() {
        mConcatMatrix.reset();
        mConcatMatrix.set(mDisplayMatrix);
        mConcatMatrix.postConcat(mMirrorMatrix);
        return mConcatMatrix;
    }


    /**
     * 设置成水平镜像
     */
    public void setVerticalMirror() {
        mMirrorMatrix.postScale(1f, -1f, mCropRectF.centerX(), mCropRectF.centerY());
        setImageMatrix(getConcatMatrix());
        isVerticalEnable = true;
        invalidate();
    }

    public void rightRotate90() {
        postAnyRotate(90f);
    }

    private void postRotate(float angel, float centerX, float centerY) {
        mDisplayMatrix.postRotate(angel, centerX, centerY);
        setImageMatrix(getConcatMatrix());
    }

    public void leftRotate90() {
        postAnyRotate(-90f);
    }

    public void postAnyRotate(float anyAngel) {
        mRotateAnimator.cancel();
        mRotateAnimator.setFloatValues(0, anyAngel);
        mRotateAnimator.setDuration(DEFAULT_ANIMATION_TIME);
        mCurrentActiveAnimator = mRotateAnimator;
        mRotateAnimator.start();
    }


    /**
     * 移动Bitmap的位置
     *
     * @param dx x轴移动的距离
     * @param dy y轴移动的距离
     */
    private void moveImage(float dx, float dy) {
        mDisplayMatrix.postTranslate(dx, dy);
        setImageMatrix(getConcatMatrix());
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        float mThisWidth = getMeasuredWidth();
        float mThisHeight = getMeasuredHeight();

        mCropRectF.set(0, (mThisHeight - mThisWidth) / 2, mThisWidth, (mThisHeight + mThisWidth) / 2);//这里初始化好矩形框框的范围

        if (getDrawable() != null) {
            getProperMatrix(mBaseMatrix);//获取矩阵，用来设置矩阵
            //拷贝矩阵
            mDisplayMatrix.set(mBaseMatrix);
            setImageMatrix(getConcatMatrix());

            //设置化映射矩阵
            mBitmapRectF.set(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());

            mInitImageCorners = RectUtils.getCornersFromRect(mBitmapRectF);//获取初始化的点
        }
    }

    private void updateBitmapRectf(Matrix displayMatrix) {
        if (getDrawable() != null) {
            mBitmapRectF.set(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
            displayMatrix.mapRect(mBitmapRectF);
        }
    }

    /**
     * 获取让图片移动并居中到裁剪框的举证
     *
     * @param baseMatrix 展示用的矩阵
     */
    private void getProperMatrix(Matrix baseMatrix) {
        baseMatrix.reset();
        Drawable drawable = getDrawable();
        float intrinsicWidth = drawable.getIntrinsicWidth();
        float intrinsicHeight = drawable.getIntrinsicHeight();
        float drawableRatio = intrinsicHeight / intrinsicWidth;//图片的高/宽 比例

        float cropRectWidth = mCropRectF.width();
        float cropRectHeight = mCropRectF.height();
        float cropRectRatio = cropRectHeight / cropRectWidth;

        //
        float scale;
        float moveX = 0, moveY;
        if (drawableRatio > cropRectRatio) {//表示是长图，就是高大于宽
            //按照宽的比例来扩大
            scale = cropRectWidth / intrinsicWidth;
            moveY = (getMeasuredHeight() - intrinsicHeight * scale) / 2;//视图的高度减去图片的扩大后的高度/2
        } else {
            //按照高的比例来扩大
            scale = cropRectHeight / intrinsicHeight;
            moveX = (cropRectWidth - scale * intrinsicWidth) / 2;
            moveY = mCropRectF.top;
        }
        baseMatrix.postScale(scale, scale);//设置恰当的放大倍数
        baseMatrix.postTranslate(moveX, moveY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, mDisplayMatrix.toString());
        super.onDraw(canvas);
        drawTransParentLayer(canvas);
        drawCropRect(canvas);

        if (getDrawable() != null) {
            logMatrixInfo(getImageMatrix());
            if (mImageInfo == null) {//第一次才需要记录，最开始高宽和长度，和放大倍数
                setImageInfo();
            }
        }
    }

    private void setImageInfo() {

        mImageInfo = new ImageInfo(getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight(), getMatrixScale(mDisplayMatrix));
    }

    /**
     * 绘画阴影区域
     *
     * @param canvas 画布
     */
    private void drawTransParentLayer(Canvas canvas) {
        Rect r = new Rect();
        getLocalVisibleRect(r);
        canvas.drawRect(r.left, r.top, r.right, mCropRectF.top, mTransParentLayerPaint);
        canvas.drawRect(r.left, mCropRectF.bottom, r.right, r.bottom, mTransParentLayerPaint);
    }

    private void drawCropRect(Canvas canvas) {
        float halfLineWidth = mWhiteCropPaint.getStrokeWidth() * 0.5f;
        canvas.drawRect(mCropRectF.left + halfLineWidth, mCropRectF.top - halfLineWidth, mCropRectF.right - halfLineWidth, mCropRectF.bottom + halfLineWidth, mWhiteCropPaint);
    }


    @Override
    public void setImageURI(@Nullable Uri uri) {
        mUri = uri;
        try {
            Bitmap bmp = ImageLoadUtil.loadImage(getContext().getContentResolver(),uri,1500,1500);
            setImageBitmap(bmp);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setDrawable(Drawable drawable) {
        setImageDrawable(drawable);
        requestLayout();
        invalidate();
    }

    /**
     * 将图片扩大到指定倍数
     *
     * @param mScaleFactor 扩大倍数
     * @param focusX       放大中心点x坐标
     * @param focusY       放大中心点y坐标
     */
    private void postScale(float mScaleFactor, float focusX, float focusY) {

        mDisplayMatrix.postScale(mScaleFactor, mScaleFactor, focusX, focusY);
        setImageMatrix(getConcatMatrix());
    }

    /**
     * 打印检查log
     *
     * @param matrix 需要检查的矩阵
     */
    private void logMatrixInfo(Matrix matrix) {
        matrix.getValues(mMatrixValue);
        Log.d(TAG, "SCALEX：" + mMatrixValue[Matrix.MSCALE_X] + "ScaleY: " + mMatrixValue[Matrix.MSCALE_Y] + "transX "
                + mMatrixValue[Matrix.MTRANS_X] + " transY " + mMatrixValue[Matrix.MTRANS_Y]);
        Log.d(TAG, "Drawable width " + getDrawable().getIntrinsicWidth() + "Drawable height" + getDrawable().getIntrinsicHeight());
        Log.d(TAG, "BitmapRect " + mBitmapRectF.width() + " Bitmap left " + mBitmapRectF.left);

        RectF r = new RectF(0, 0, 100, 100);
        Matrix m = new Matrix();
        m.setRotate(100);
        m.mapRect(r);

        Log.d(TAG, "rotateRectWidth" + r.width());
    }

    /**
     * @return 返回当前的Bitmap对象
     */
    public Bitmap getImageBitmap() {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) getDrawable();
        return bitmapDrawable.getBitmap();

    }


    /**
     * 裁剪并保存图片
     *
     * @return 返回需要保存图片的BItmap对象
     */
    public Bitmap cropAndSaveImage() {
        Bitmap bitmap = getImageBitmap();
        Bitmap originBitmapFromUri = null;
        if (bitmap != null) {
            //当前的大图
            try {
                originBitmapFromUri = ImageLoadUtil.loadImage(getContext().getContentResolver(), mUri, Integer.MAX_VALUE, Integer.MAX_VALUE);//可能oom
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        //此时已经拿到最初的放大倍数
        //求出裁剪框和大图的相对位置dx,dy;
        if (bitmap != null && originBitmapFromUri != null) {
            Matrix matrix = new Matrix();
            float scale = 1;
            Bitmap scaledBitmap,resultBitmap=null;
            boolean isSuccess = false, needScale = false;
            Bitmap currentRotatedBitmap = getCurrentRotatedOriginalBitmap(originBitmapFromUri); //,拿到旋转图片
            while (!isSuccess) {
                try {
                    if (needScale) {
                        matrix.postScale(scale, scale, currentRotatedBitmap.getWidth() / 2, currentRotatedBitmap.getHeight() / 2);
                        scaledBitmap = Bitmap.createBitmap(currentRotatedBitmap, 0, 0, currentRotatedBitmap.getWidth(), currentRotatedBitmap.getHeight(), matrix, true);
                        currentRotatedBitmap.recycle();
                        resultBitmap = scaledBitmap;
                        isSuccess = true;
                    }
                    return getCropBitmapInOriginalBitmap(currentRotatedBitmap);//拿到裁剪框位置的图片,

                } catch (OutOfMemoryError e) {
                    scale = scale * 0.7f;
                    needScale = true;
                }
            }
            return resultBitmap;
        } else {
            return null;
        }
    }


    /**
     * 拿到裁剪框所在原图的位置
     *
     * @param currentRotatedBitmap 旋转后的原图
     * @return 裁剪框所在位置的图片
     * @throws OutOfMemoryError 内存溢出
     */
    public Bitmap getCropBitmapInOriginalBitmap(Bitmap currentRotatedBitmap) throws OutOfMemoryError {
        float scale_x = mBitmapRectF.width() / currentRotatedBitmap.getWidth();
        float scale_y = mBitmapRectF.height() / currentRotatedBitmap.getHeight();
        float initScale = Math.min(scale_x, scale_y);//这个裁剪框和要裁剪的原图的长宽比
        //算出裁剪框所在原图的位置，还有裁剪框映射到原图的长和宽
        int dx = (int) ((mCropRectF.left - mBitmapRectF.left) / initScale);
        int dy = (int) ((mCropRectF.top - mBitmapRectF.top) / initScale);
        int width = (int) ((int) mCropRectF.width() / initScale);
        int height = (int) ((int) mCropRectF.height() / initScale);
        return Bitmap.createBitmap(currentRotatedBitmap, dx, dy, width, height);//这个为输出文件

    }

    /**
     * 产生旋转后的源图片
     *
     * @param originBitmap 原图
     * @return 旋转后的图片
     * @throws OutOfMemoryError 内存溢出
     */
    private Bitmap getCurrentRotatedOriginalBitmap(Bitmap originBitmap) {
        updateBitmapRectf(mDisplayMatrix);//mBitmapRectf就代表当前矩阵
        //获得旋转后的图片
        return ImageLoadUtil.rotateBitmap(originBitmap, getCurrentAngle());
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1 == null || e2 == null) return false;
            if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
            if (mScaleGestureDetector.isInProgress()) return false;
            // TODO: 2017/7/25 如果改成先镜像后移动应该可以解决这个问题
            if (isHorizontalEnable) {//设置了水平镜像,往反方向移动，以为镜像是以裁剪框为中心的
                distanceX = -distanceX;
            }
            if (isVerticalEnable) {//设置了水平镜像,往反方向移动，以为镜像是以裁剪框为中心的
                distanceY = -distanceY;
            }
            CropImageView.this.onScroll(distanceX, distanceY);
            return true;
        }
    }

    private void onScroll(float distanceX, float distanceY) {
        Log.d(TAG, "onScroll dx " + distanceX + "dy" + distanceY);
        moveImage(-distanceX, -distanceY);
    }


    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private float mScaleFactor = 1;
        private static final String TAG = "ScaleListener";


        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor = detector.getScaleFactor();


            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, mMaxScale));
            mScaleFactor = checkScale(mScaleFactor);
            Log.d(TAG, "最终手势放大的放大倍数postScale为" + mScaleFactor);
            postScale(mScaleFactor, detector.getFocusX(), detector.getFocusY());
            mImageInfo.setGestureScale(mImageInfo.getGestureScale() * mScaleFactor);//设置当前放大倍数
            return true;
        }

        private float checkScale(float scaleFactor) {
            float finalScale;
            float currentScale = getCurrentScale();
            Log.d(TAG, "当前的放大倍数" + getCurrentScale());
            if (currentScale * mScaleFactor <= mMinScale) {//如果超过最小值，则就直接到最小值
                finalScale = mMinScale / currentScale;
            } else if (currentScale * mScaleFactor >= mMaxScale * 1.5f) {
                finalScale = mMaxScale * 1.5f / currentScale;
            } else {
                finalScale = scaleFactor;
            }
            return finalScale;
        }
    }

    private class RotationListener extends RotationGestureDetector.SimpleOnRotationGestureListener {

        @Override
        public boolean onRotation(RotationGestureDetector rotationDetector) {
            float angle = rotationDetector.getAngle();
            postRotate(angle, mMidPntX, mMidPntY);
            return true;
        }
    }


    private class TransformAnimator extends ValueAnimator {
        private static final String PROPERTY_NAME_TRANSLATE_X = "TRANSLATE_X";
        private static final String PROPERTY_NAME_TRANSLATE_Y = "TRANSLATE_Y";
        private static final String PROPERTY_NAME_SCALE_XANDY = "SCALE";


        private float mLastRote = 0;
        private float mLastTranslateX = 0;
        private float mLastTranslateY = 0;
        private float mLastScale = 1;

        private float getLastScale() {
            return mLastScale;
        }

        private void setLastScale(float mLastScale) {
            this.mLastScale = mLastScale;
        }

        private float getLastRote() {
            return mLastRote;
        }


        private void setLastRote(float mLastRote) {
            this.mLastRote = mLastRote;
        }

        private float getLastTraslateX() {
            return mLastTranslateX;
        }

        private float getLastTraslateY() {
            return mLastTranslateY;
        }

        private void setLastTraslateX(float mLastTraslateX) {
            this.mLastTranslateX = mLastTraslateX;
        }

        private void setLastTraslateY(float mLastTraslateY) {
            this.mLastTranslateY = mLastTraslateY;
        }
    }

}
