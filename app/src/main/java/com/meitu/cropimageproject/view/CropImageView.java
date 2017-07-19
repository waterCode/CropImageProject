package com.meitu.cropimageproject.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

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
        mCropRectf.set(0,(mThisHeight-mThisWidth)/2,mThisWidth,(mThisHeight+mThisWidth)/2);//这里初始化好矩形框框的范围
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTransParentLayer(canvas);
        drawCropRect(canvas);
    }

    private void drawTransParentLayer(Canvas canvas){
        Rect r =new Rect();
        getLocalVisibleRect(r);
        canvas.drawRect(r.left,r.top,r.right,mCropRectf.top,mTransParentLayerPaint);
        canvas.drawRect(r.left,mCropRectf.bottom,r.right,r.bottom,mTransParentLayerPaint);
    }

    private void drawCropRect(Canvas canvas){
        float halfLineWidth = mWhiteCropPaint.getStrokeWidth()*0.5f;
        canvas.drawRect(mCropRectf.left+halfLineWidth,mCropRectf.top-halfLineWidth,mCropRectf.right-halfLineWidth,mCropRectf.bottom+halfLineWidth, mWhiteCropPaint);
    }
}
