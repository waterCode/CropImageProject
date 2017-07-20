package com.meitu.cropimageproject.info;

/**
 * Created by zmc on 2017/7/19.
 */

public class ImageInfo {
    private final float mWidth;
    private final float mHeight;
    private final float mScale;


    public ImageInfo(float mWidth, float mHeight, float mScale) {
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.mScale = mScale;
    }

    public float getmWidth() {
        return mWidth;
    }

    public float getmHeight() {
        return mHeight;
    }

    public float getmScale() {
        return mScale;
    }
}
