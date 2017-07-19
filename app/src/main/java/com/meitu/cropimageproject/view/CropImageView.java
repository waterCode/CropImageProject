package com.meitu.cropimageproject.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.meitu.cropimageproject.R;

/**
 * Created by zmc on 2017/7/18.
 */

public class CropImageView extends ImageView {


    private static final String DEFAULT_BACKGROUND_COLOR_ID = "#99000000";

    private int mThisWidth = -1;
    private int mThisHeight = -1;


    private Matrix mBaseMatrix = new Matrix();
    private Matrix mDisplayMatrix = new Matrix();

    private RectF mCropRectf = new RectF();//裁剪框矩形区域

    private Paint mTransParentLayerPaint;//暗色区域背景
    private Paint mWhiteCropPaint;


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
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mThisWidth = getMeasuredWidth();
        mThisHeight = getMeasuredHeight();
        // TODO: 2017/7/19 这里表示高一定大于宽
        mCropRectf.set(0, (mThisHeight - mThisWidth) / 2, mThisWidth, (mThisHeight + mThisWidth) / 2);//这里初始化好矩形框框的范围

        getProperMatrix(mDisplayMatrix);//获取矩阵，用来设置矩阵
        setImageMatrix(mDisplayMatrix);
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
        float moveX=0,moveY=0;
        if (drawableRatio > cropRectRatio) {//表示是长图，就是高大于宽
            //按照宽的比例来扩大
            scale = cropRectWidth/intrinsicWidth;
            moveY = mCropRectf.top;
        } else {
            //按照高的比例来扩大
            scale = cropRectHeight/intrinsicHeight;
            moveX = (cropRectWidth - scale*intrinsicWidth)/2;
            moveY = mCropRectf.top;
        }
        displayMatrix.postScale(scale, scale);//设置恰当的放大倍数
        displayMatrix.postTranslate(moveX,moveY);




    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTransParentLayer(canvas);
        drawCropRect(canvas);
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
}
