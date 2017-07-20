package com.meitu.cropimageproject.view;

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
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import com.meitu.cropimageproject.R;
import com.meitu.cropimageproject.info.ImageInfo;

/**
 * Created by zmc on 2017/7/18.
 */

public class CropImageView extends ImageView {


    private static final String DEFAULT_BACKGROUND_COLOR_ID = "#99000000";
    private static final String TAG = "CropImageView";

    private int mThisWidth = -1;
    private int mThisHeight = -1;


    private Matrix mBaseMatrix = new Matrix();
    private Matrix mDisplayMatrix = new Matrix();

    private RectF mCropRectf = new RectF();//裁剪框矩形区域
    private RectF mBitmapRectf;

    private Paint mTransParentLayerPaint;//暗色区域背景
    private Paint mWhiteCropPaint;


    private ScaleGestureDetector mScaleGestureDector;
    private GestureDetector mGestureDetector;

    private float mLastPointX;
    private float mLastPointY;
    private float[] mMatrixValue = new float[9];

    private ImageInfo mImageInfo;//最开始招聘信息

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

        mScaleGestureDector = new ScaleGestureDetector(getContext(), new ScaleListener());
        mGestureDetector = new GestureDetector(getContext(),new GestureListener());
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDector.onTouchEvent(event);
        if (!mScaleGestureDector.isInProgress()) {
            //检测拖动
            mGestureDetector.onTouchEvent(event);
            /*if (event.getPointerCount() == 1) {
                detectMove(event);
            }*/
        }

        int action = MotionEventCompat.getActionMasked(event);
        if (action == MotionEvent.ACTION_CANCEL) {//松手动手
            checkImagePosition();
        }
        return true;
    }


    private void checkImagePosition() {
        Log.d(TAG, "checkImagePosition");
        float[] values = new float[9];
        mDisplayMatrix.getValues(values);
        float scaleX = values[Matrix.MSCALE_X];
        float scaleY = values[Matrix.MSCALE_Y];

        Drawable image = getDrawable();
        /*if (scaleX < 1 || scaleY < 1) {//缩小到太小
            resetImage();
        }*/

    }

    private void resetImage() {
        mDisplayMatrix.set(mBaseMatrix);
        setImageMatrix(mDisplayMatrix);
        invalidate();
    }


    /**
     * 检测滑动，用来实现拖动视图
     *
     * @param event
     */
    private void detectMove(MotionEvent event) {
        int action = event.getAction();
        switch (action) {

            case MotionEvent.ACTION_DOWN: {//记录开始的点
                event.getActionIndex();
                mLastPointX = event.getX();//记录最后一个点
                mLastPointY = event.getY();
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                // TODO: 2017/7/19 pointerxIndex  out of range
                float x = event.getX();
                float y = event.getY();
                float dx = x - mLastPointX;//拿到拖动距离
                float dy = y - mLastPointY;
                mDisplayMatrix.postTranslate(dx, dy);
                setImageMatrix(mDisplayMatrix);
                invalidate();

                mLastPointX = x;
                mLastPointY = y;
            }
        }


    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mThisWidth = getMeasuredWidth();
        mThisHeight = getMeasuredHeight();
        // TODO: 2017/7/19 这里表示高一定大于宽
        mCropRectf.set(0, (mThisHeight - mThisWidth) / 2, mThisWidth, (mThisHeight + mThisWidth) / 2);//这里初始化好矩形框框的范围

        getProperMatrix(mBaseMatrix);//获取矩阵，用来设置矩阵
        //拷贝矩阵
        mDisplayMatrix.set(mBaseMatrix);
        setImageMatrix(mDisplayMatrix);
        MapBitmapRectf(mDisplayMatrix);
    }

    private void MapBitmapRectf(Matrix displayMatrix) {
        if (getDrawable() != null) {
            mBitmapRectf = new RectF(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
            displayMatrix.mapRect(mBitmapRectf);
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

        float cropRectWidth = mCropRectf.width();
        float cropRectHeight = mCropRectf.height();
        float cropRectRatio = cropRectHeight / cropRectWidth;

        //
        float scale = 1;
        float moveX = 0, moveY = 0;
        if (drawableRatio > cropRectRatio) {//表示是长图，就是高大于宽
            //按照宽的比例来扩大
            scale = cropRectWidth / intrinsicWidth;
            moveY = mCropRectf.top;
        } else {
            //按照高的比例来扩大
            scale = cropRectHeight / intrinsicHeight;
            moveX = (cropRectWidth - scale * intrinsicWidth) / 2;
            moveY = mCropRectf.top;
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
        Matrix matirx = getImageMatrix();
        matirx.getValues(mMatrixValue);
        mImageInfo = new ImageInfo(getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight(), mMatrixValue[Matrix.MSCALE_X]);
    }

    private void drawTransParentLayer(Canvas canvas) {
        Rect r = new Rect();
        getLocalVisibleRect(r);
        canvas.drawRect(r.left, r.top, r.right, mCropRectf.top, mTransParentLayerPaint);
        canvas.drawRect(r.left, mCropRectf.bottom, r.right, r.bottom, mTransParentLayerPaint);
    }

    private void drawCropRect(Canvas canvas) {
        float halfLineWidth = mWhiteCropPaint.getStrokeWidth() * 0.5f;
        canvas.drawRect(mCropRectf.left + halfLineWidth, mCropRectf.top - halfLineWidth, mCropRectf.right - halfLineWidth, mCropRectf.bottom + halfLineWidth, mWhiteCropPaint);
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
        Log.d(getContext().getString(R.string.TAG), "setDrawable");
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
        Log.d(TAG, "BitmapRect " + mBitmapRectf.width() + " Bitmap left " + mBitmapRectf.left);

    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1 == null || e2 == null) return false;
            if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
            if (mScaleGestureDector.isInProgress()) return false;
            CropImageView.this.onScroll(distanceX, distanceY);
            return true;
        }
    }

    private void onScroll(float distanceX, float distanceY) {
        Log.d(TAG, "onScroll dx " + distanceX + "dy" + distanceY);
        mDisplayMatrix.postTranslate(-distanceX, -distanceY);
        setImageMatrix(mDisplayMatrix);
        invalidate();
    }


    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private float mScaleFactor = 1;
        private static final String TAG = "ScaleListener";
        private int i = 0;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            Log.d(TAG, "" + mScaleFactor);
            i++;
            Log.d(TAG, "" + i);
            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));
            zoomTo(mScaleFactor, detector.getFocusX(), detector.getFocusY());
            invalidate();
            return true;
        }
    }

}
