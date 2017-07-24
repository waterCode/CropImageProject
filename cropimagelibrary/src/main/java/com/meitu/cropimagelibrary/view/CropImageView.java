package com.meitu.cropimagelibrary.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
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
import com.meitu.cropimagelibrary.model.RotateTask;
import com.meitu.cropimagelibrary.model.TransFormTask;
import com.meitu.cropimagelibrary.model.TranslateTask;
import com.meitu.cropimagelibrary.util.ImageLoadUtil;
import com.meitu.cropimagelibrary.util.RectUtils;
import com.meitu.cropimagelibrary.util.RotationGestureDetector;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by zmc on 2017/7/18.
 */

public class CropImageView extends android.support.v7.widget.AppCompatImageView {


    private static final String DEFAULT_BACKGROUND_COLOR_ID = "#99000000";//超过裁剪部分的矩形框
    private static final String TAG = "CropImageView";
    private static final long DEFAULT_ANIMATION_TIME = 500;
    private float MAX_SCALE = 3f;
    private boolean mScaleEnable = true;
    private boolean mRotateEnable = true;


    private Matrix mBaseMatrix = new Matrix();
    private Matrix mDisplayMatrix = new Matrix();
    private Matrix mTempMatrix = new Matrix();

    private RectF mCropRectF = new RectF();//裁剪框矩形区域
    private RectF mBitmapRectF = new RectF();//当前的矩形区域

    private Paint mTransParentLayerPaint;//暗色区域背景
    private Paint mWhiteCropPaint;


    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;
    private RotationGestureDetector mRotationGestureDetector;

    private float mMidPntX, mMidPntY;//手指中心点


    private float[] mMatrixValue = new float[9];

    private ImageInfo mImageInfo;//最开始图片信息,好像可以删掉

    private PointF mImageCenterPoint = new PointF();


    private float[] mCurrentImageCorners = new float[8];//用来存放当前顶点坐标啊
    private float[] mInitImageCorners = new float[8];
    private Uri mUri;//图片的uri

    private TransFormTaskPool mTransFormTaskPool;

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

    private void init() {
        mTransParentLayerPaint = new Paint();
        mTransParentLayerPaint.setColor(Color.parseColor(DEFAULT_BACKGROUND_COLOR_ID));//设置颜色

        mWhiteCropPaint = new Paint();
        mWhiteCropPaint.setColor(Color.WHITE);//设置颜色
        mWhiteCropPaint.setStrokeWidth(1);//设置填充宽度
        mWhiteCropPaint.setStyle(Paint.Style.STROKE);//what？？

        mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        mGestureDetector = new GestureDetector(getContext(), new GestureListener());
        mRotationGestureDetector = new RotationGestureDetector(new RotationListener());

        mTransFormTaskPool = new TransFormTaskPool(this, mDisplayMatrix);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

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


    public void setScaleEnable(boolean mScaleEnable) {
        this.mScaleEnable = mScaleEnable;
    }

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
        Log.d(TAG, "checkImagePosition");
        if (mImageInfo.getGestureScale() > MAX_SCALE) {
            backToMaxScale();
        }
        mDisplayMatrix.mapPoints(mCurrentImageCorners, mInitImageCorners);//求出当前的坐标
        float dx, dy;//中心便宜距离
        float deltaScale = 1;
        getBitmapRectf(mDisplayMatrix);
        dx = mCropRectF.centerX() - mBitmapRectF.centerX();//拿到需要移动距离
        dy = mCropRectF.centerY() - mBitmapRectF.centerY();
        //判断回到中心后能不能回到覆盖
        mTempMatrix.reset();
        mTempMatrix.set(mDisplayMatrix);//设置当前的位置
        mTempMatrix.postTranslate(dx, dy);//回到中心点
        float[] tempImageCorners = Arrays.copyOf(mInitImageCorners, mInitImageCorners.length);//拷贝一份初始点
        mTempMatrix.mapPoints(tempImageCorners);//获取移动到中心点的各个坐标

        boolean isWillImageWrapCropBounds = isImageWrapCropBounds(tempImageCorners);

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
        }


        //移动相应的距离
        postTranslate(dx, dy,true);
        //再放大
        if (!isWillImageWrapCropBounds) {
            mDisplayMatrix.postScale(deltaScale, deltaScale, mCropRectF.centerX(), mCropRectF.centerY());
            //设置imageInfo的放大倍数
            mImageInfo.setGestureScale(mImageInfo.getGestureScale() * deltaScale);
            setImageMatrix(mDisplayMatrix);
            invalidate();
        }
        //设置矩阵并重绘

    }



    private void backToMaxScale() {
        float scale = MAX_SCALE / mImageInfo.getGestureScale();
        // TODO: 2017/7/21 放大，缩小，中心点问题
        Log.d(TAG, "要回弹的倍数" + scale + "到最大倍数" + MAX_SCALE);
        mImageInfo.setGestureScale(MAX_SCALE);
        mDisplayMatrix.postScale(scale, scale, mCropRectF.centerX(), mCropRectF.centerY());
        setImageMatrix(mDisplayMatrix);
        invalidate();
    }


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

    protected boolean isImageWrapCropBounds(float[] imageCorners) {
        mTempMatrix.reset();
        mTempMatrix.setRotate(-getCurrentAngle());

        float[] unrotatedImageCorners = Arrays.copyOf(imageCorners, imageCorners.length);
        mTempMatrix.mapPoints(unrotatedImageCorners);//把矩阵摆正嘛？

        float[] unrotatedCropBoundsCorners = RectUtils.getCornersFromRect(mCropRectF);
        mTempMatrix.mapPoints(unrotatedCropBoundsCorners);

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
        return getMatrixScale(mDisplayMatrix);
    }


    /**
     * 返回对应举证的放大倍数，x的平方+y的平方，再求根号
     *
     * @param matrix 所求放大倍数的矩阵
     * @return 放大倍数
     */
    public float getMatrixScale(@NonNull Matrix matrix) {
        return (float) Math.sqrt(Math.pow(getMatrixValue(matrix, Matrix.MSCALE_X), 2) + Math.pow(getMatrixValue(matrix, Matrix.MSCALE_Y), 2));
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
        Log.d(TAG, "current angele beforeHorizontalMirror" + getCurrentAngle());
        Log.d(TAG, "current scale beforeHorizontalMirror" + getCurrentScale());
        postScale(-1f, 1f);
        Log.d(TAG, "current angele afterHorizontalMirror" + getCurrentAngle());
        Log.d(TAG, "current scale  afterHorizontalMirror" + getCurrentScale());
    }

    /**
     * 设置成水平镜像
     */
    public void setVerticalMirror() {
        postScale(1f, -1f);
    }

    public void rightRotate90() {
        postRotate(90, mCropRectF.centerX(), mCropRectF.centerY(),true);
    }

    public void leftRotate90() {
        postRotate(-90, mCropRectF.centerX(), mCropRectF.centerY(),true);
    }

    public void postAnyRotate(float anyAngel) {
        postRotate(anyAngel, mCropRectF.centerX(), mCropRectF.centerY(),true);
        checkImagePosition();
    }

    /**
     * 放大，以裁剪框为中心
     *
     * @param sx x轴放大的倍数
     * @param sy y轴放大的倍数
     */
    private void postScale(float sx, float sy) {
        mDisplayMatrix.postScale(sx, sy, mCropRectF.centerX(), mCropRectF.centerY());
        setImageMatrix(mDisplayMatrix);//为什么每次都要设置
        invalidate();
    }

    /**
     * 更新图片的中心点，可用来镜像的时候使用
     */
    private void updateImageCenter() {
        getBitmapRectf(mDisplayMatrix);//重新获取以下区域
        float x = (mBitmapRectF.right + mBitmapRectF.left) / 2;
        float y = (mBitmapRectF.bottom + mBitmapRectF.top) / 2;
        mImageCenterPoint.set(x, y);
    }


    /**
     * 移动Bitmap的位置
     *
     * @param dx x轴移动的距离
     * @param dy y轴移动的距离
     */
    private void moveImage(float dx, float dy) {
        mDisplayMatrix.postTranslate(dx, dy);
        setImageMatrix(mDisplayMatrix);
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        float mThisWidth = getMeasuredWidth();
        float mThisHeight = getMeasuredHeight();
        // TODO: 2017/7/19 这里表示高一定大于宽
        mCropRectF.set(0, (mThisHeight - mThisWidth) / 2, mThisWidth, (mThisHeight + mThisWidth) / 2);//这里初始化好矩形框框的范围

        if (getDrawable() != null) {
            getProperMatrix(mBaseMatrix);//获取矩阵，用来设置矩阵
            //拷贝矩阵
            mDisplayMatrix.set(mBaseMatrix);
            setImageMatrix(mDisplayMatrix);

            //设置化映射矩阵
            mBitmapRectF.set(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
            // TODO: 2017/7/21 理论上来说不可以在这new对象
            mInitImageCorners = RectUtils.getCornersFromRect(mBitmapRectF);//获取初始化的点
        }
    }

    private void getBitmapRectf(Matrix displayMatrix) {
        if (getDrawable() != null) {
            mBitmapRectF.set(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
            displayMatrix.mapRect(mBitmapRectF);
        }
    }

    /**
     * 获取让图片移动并居中到裁剪框的举证
     *
     * @param displayMatrix 展示用的矩阵
     */
    private void getProperMatrix(Matrix displayMatrix) {
        displayMatrix.reset();
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
        displayMatrix.postScale(scale, scale);//设置恰当的放大倍数
        displayMatrix.postTranslate(moveX, moveY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTransParentLayer(canvas);
        drawCropRect(canvas);

        if (getDrawable() != null) {
            logMatrixInfo(getImageMatrix());
            if (mImageInfo == null) {//第一次才需要记录，最开始高宽和长度，和放大倍数
                SetImageInfo();
            }
        }

        mTransFormTaskPool.onDrawFinish();//回调
    }

    private void SetImageInfo() {
        Matrix matrix = getImageMatrix();
        matrix.getValues(mMatrixValue);
        mImageInfo = new ImageInfo(getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight(), mMatrixValue[Matrix.MSCALE_X]);
    }

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


    public void setBitmap(Bitmap bitmap) {
        setImageBitmap(bitmap);
        requestLayout();//重新layout刷新布局
        invalidate();
    }

    @Override
    public void setImageURI(@Nullable Uri uri) {
        super.setImageURI(uri);
        mUri = uri;
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
    private void zoomTo(float mScaleFactor, float focusX, float focusY) {

        mDisplayMatrix.postScale(mScaleFactor, mScaleFactor, focusX, focusY);
        setImageMatrix(mDisplayMatrix);
    }

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

        Log.d(TAG, "rotaeRectWidth" + r.width());
    }

    /**
     * @return 返回当前的Bitmap对象
     */
    public Bitmap getImageBitmap() {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) getDrawable();
        return bitmapDrawable.getBitmap();

    }

    public Bitmap cropAndSaveImage() {
        Bitmap bitmap = getImageBitmap();
        Bitmap originBitmapFromUri = null;
        float initScale;
        if (bitmap != null) {
            //当前的大图
            //currentBigBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mDisplayMatrix, true);
            try {
                originBitmapFromUri = ImageLoadUtil.loadImage(getContext().getContentResolver(), mUri, 4000, 4000);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        //此时已经拿到最初的放大倍数

        //求出裁剪框和大图的相对位置dx,dy;
        if (bitmap != null && originBitmapFromUri != null) {

            getBitmapRectf(mDisplayMatrix);//mBitmapRectf就代表当前矩阵
            //获得旋转后的图片
            Matrix rotateMatrix = new Matrix();
            rotateMatrix.setRotate(getCurrentAngle());
            originBitmapFromUri = Bitmap.createBitmap(originBitmapFromUri, 0, 0, originBitmapFromUri.getWidth(), originBitmapFromUri.getHeight(), rotateMatrix, true);

            float scale_x = mBitmapRectF.width() / originBitmapFromUri.getWidth();
            float scale_y = mBitmapRectF.height() / originBitmapFromUri.getHeight();
            initScale = Math.min(scale_x, scale_y);
            int dx = (int) ((mCropRectF.left - mBitmapRectF.left) / initScale);
            int dy = (int) ((mCropRectF.top - mBitmapRectF.top) / initScale);
            int width = (int) ((int) mCropRectF.width() / initScale);
            int height = (int) ((int) mCropRectF.height() / initScale);
            return Bitmap.createBitmap(originBitmapFromUri, dx, dy, width, height);//这个为输出文件
        } else {
            return null;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1 == null || e2 == null) return false;
            if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
            if (mScaleGestureDetector.isInProgress()) return false;
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

            Log.d(TAG, "mImageInfo放大倍数" + mImageInfo.getGestureScale());

            mImageInfo.setGestureScale(mImageInfo.getGestureScale() * mScaleFactor);//设置当前放大倍数

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, MAX_SCALE));
            zoomTo(mScaleFactor, detector.getFocusX(), detector.getFocusY());
            invalidate();
            return true;
        }
    }

    private class RotationListener extends RotationGestureDetector.SimpleOnRotationGestureListener {

        @Override
        public boolean onRotation(RotationGestureDetector rotationDetector) {
            float angle = rotationDetector.getAngle();
            postRotate(angle, mMidPntX, mMidPntY,false);
            return super.onRotation(rotationDetector);
        }
    }


    private void postRotate(float angle, float centerX, float centerY,boolean hasAnimation) {
        if(hasAnimation){
        //代替的是。
        RotateTask rotateTask = new RotateTask(DEFAULT_ANIMATION_TIME, angle, centerX, centerY);
        mTransFormTaskPool.postTask(rotateTask);

        }else {
            mDisplayMatrix.postRotate(angle, centerX, centerY);
            setImageMatrix(mDisplayMatrix);
            invalidate();
        }

    }

    private void postTranslate(float dx, float dy,boolean hasAnimation) {
        if(hasAnimation){
            TranslateTask task = new TranslateTask(DEFAULT_ANIMATION_TIME,dx,dy);
            mTransFormTaskPool.postTask(task);
        }else {
            mDisplayMatrix.postTranslate(dx, dy);
            setImageMatrix(mDisplayMatrix);
            invalidate();
        }
    }

    private static class TransFormTaskPool {
        static Queue<TransFormTask> mTaskQueue = new LinkedList<>();
        static TransFormTask mActive;
        private Matrix mMatrix;
        private CropImageView mView;

        public TransFormTaskPool(CropImageView view, Matrix displayMatrix) {
            mMatrix = displayMatrix;
            mView = view;
        }


        public void postTask(TransFormTask task) {
            if (mActive == null) {
                mActive = task;
                continueTask(task);
            } else {
                mTaskQueue.add(task);
            }
        }

        public void onDrawFinish() {
            if (mActive == null) {
                if(mTaskQueue.size()>0){//任务数量大于0
                    mActive = mTaskQueue.poll();
                }else {
                    return;//直接就往下执行
                }
            }
            if (mActive.isFinish()) {
                //拿下一个任务
            } else {
                continueTask(mActive);
            }
        }

        private void continueTask(TransFormTask task) {
            switch (task.getTaskId()) {
                case TransFormTask.TRANSFORM_ROTATE: {
                    //旋转任务
                    if (task instanceof RotateTask) {
                        float centerX = ((RotateTask) task).getCenterX();
                        float centerY = ((RotateTask) task).getCenterY();
                        float angel = ((RotateTask) task).getAngel();
                        if (task.isFinish()) {
                            mActive = null;
                        }
                        mMatrix.postRotate(angel, centerX, centerY);
                    }
                    break;
                }
                case TransFormTask.TRANSFORM_TRANSLATE:{
                    if (task instanceof TranslateTask){
                        TranslateTask.TranslateParams params = ((TranslateTask) task).getTranslateParams();
                        if (task.isFinish()) {
                            mActive = null;
                        }
                        mMatrix.postTranslate(params.x,params.y);
                        Log.d(TAG,"此次位移"+"dx:"+params.x+"dy:"+params.y);
                    }
                    break;
                }



            }

            mView.setImageMatrix(mMatrix);
            mView.invalidate();
        }
    }

}
