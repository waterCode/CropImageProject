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
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.meitu.cropimagelibrary.info.ImageInfo;

/**
 * Created by zmc on 2017/7/18.
 */

public class CropImageView extends android.support.v7.widget.AppCompatImageView {


    private static final String DEFAULT_BACKGROUND_COLOR_ID = "#99000000";
    private static final String TAG = "CropImageView";


    private Matrix mBaseMatrix = new Matrix();
    private Matrix mDisplayMatrix = new Matrix();

    private RectF mCropRectF = new RectF();//裁剪框矩形区域
    private RectF mBitmapRectF = new RectF();

    private Paint mTransParentLayerPaint;//暗色区域背景
    private Paint mWhiteCropPaint;


    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;


    private float[] mMatrixValue = new float[9];

    private ImageInfo mImageInfo;//最开始图片信息,好像可以删掉

    private PointF mImageCenterPoint = new PointF();

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
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        if (!mScaleGestureDetector.isInProgress()) {
            //检测拖动
            mGestureDetector.onTouchEvent(event);
        }

        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {//松手动手
            checkImagePosition();
        }
        return true;
    }


    @Override
    public void setScaleType(ScaleType scaleType) {
        if(scaleType != ScaleType.MATRIX){
            throw new IllegalArgumentException("scaleType must be matrix");
        }else {
            super.setScaleType(scaleType);
        }
    }

    /**
     * 检车是否越界
     */
    private void checkImagePosition() {
        Log.d(TAG, "checkImagePosition");
        getBitmapRectf(mDisplayMatrix);//拿到此时图片位置
        if (mBitmapRectF.width() < mCropRectF.width() || mBitmapRectF.height() < mCropRectF.height()) {//缩小到太小回到初始化
            resetImage();
        } else {

            float dx = 0, dy = 0;
            //检查其他4个边界，最多有两个越界
            if (mBitmapRectF.left > mCropRectF.left) {//左边界越界
                dx = mCropRectF.left - mBitmapRectF.left;
            }
            if (mBitmapRectF.right < mCropRectF.right) {//右边界越界
                dx = mCropRectF.right - mBitmapRectF.right;
            }
            if (mBitmapRectF.top > mCropRectF.top) {//上边界越界
                dy = mCropRectF.top - mBitmapRectF.top;
            }
            if (mBitmapRectF.bottom < mCropRectF.bottom) {//下边界越界
                dy = mCropRectF.bottom - mBitmapRectF.bottom;
            }

            moveImage(dx, dy);
        }
    }


    /**
     * 设置成水平镜像
     */
    public void setHorizontalMirror() {
        Log.d(TAG, "setHorizontalMirror");
        updateImageCenter();
        mDisplayMatrix.postScale(-1, 1, mImageCenterPoint.x, mImageCenterPoint.y);
        mBaseMatrix.postScale(-1, 1, mImageCenterPoint.x, mImageCenterPoint.y);
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
     * 复原Image到最初的位置
     */
    private void resetImage() {
        Log.d(TAG, "resetImage");
        mDisplayMatrix.set(mBaseMatrix);
        setImageMatrix(mDisplayMatrix);
        invalidate();
    }


    /**
     * 移动Bitmap的位置
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
        int mThisWidth = getMeasuredWidth();
        int mThisHeight = getMeasuredHeight();
        // TODO: 2017/7/19 这里表示高一定大于宽
        mCropRectF.set(0, (mThisHeight - mThisWidth) / 2, mThisWidth, (mThisHeight + mThisWidth) / 2);//这里初始化好矩形框框的范围

        getProperMatrix(mBaseMatrix);//获取矩阵，用来设置矩阵
        //拷贝矩阵
        mDisplayMatrix.set(mBaseMatrix);
        setImageMatrix(mDisplayMatrix);

        //设置化映射矩阵
        mBitmapRectF.set(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());

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
            moveY = (getMeasuredHeight() - intrinsicHeight * scale)/2;//视图的高度减去图片的扩大后的高度/2
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
        logMatrixInfo(getImageMatrix());
        super.onDraw(canvas);
        drawTransParentLayer(canvas);
        drawCropRect(canvas);
        if (mImageInfo == null) {//第一次才需要记录，最开始高宽和长度，和放大倍数
            SetImageInfo();
        }
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

    public void setBitmapUri(String uri) {
        Bitmap bitmap = getBitmapFromUri(uri);
        setBitmap(bitmap);
    }

    private Bitmap getBitmapFromUri(String uri) {
        return null;
    }


    public void setBitmap(Bitmap bitmap) {
        setImageBitmap(bitmap);
        requestLayout();//重新layout刷新布局
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
        mDisplayMatrix.set(mBaseMatrix);
        mDisplayMatrix.postScale(mScaleFactor, mScaleFactor, focusX, focusY);
        setImageMatrix(mDisplayMatrix);
    }

    private void logMatrixInfo(Matrix matrix) {
        matrix.getValues(mMatrixValue);
        Log.d(TAG, "SCALEX：" + mMatrixValue[Matrix.MSCALE_X] + "ScaleY: " + mMatrixValue[Matrix.MSCALE_Y] + "transX "
                + mMatrixValue[Matrix.MTRANS_X] + " transY " + mMatrixValue[Matrix.MTRANS_Y]);
        Log.d(TAG, "Drawable width " + getDrawable().getIntrinsicWidth() + "Drawable height" + getDrawable().getIntrinsicHeight());
        Log.d(TAG, "BitmapRect " + mBitmapRectF.width() + " Bitmap left " + mBitmapRectF.left);

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
            mScaleFactor *= detector.getScaleFactor();
            Log.d(TAG, "" + mScaleFactor);
            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));
            zoomTo(mScaleFactor, detector.getFocusX(), detector.getFocusY());
            invalidate();
            return true;
        }
    }

}
