package com.meitu.cropimagelibrary.info;

/**
 * Created by zmc on 2017/7/19.
 */

public class ImageInfo {
    private final float mInitWidth;
    private final float mInitHeight;
    private final float mInitScale;

    private float mGestureScale =1;

    public ImageInfo(float mWidth, float mHeight, float mScale) {
        this.mInitWidth = mWidth;
        this.mInitHeight = mHeight;
        this.mInitScale = mScale;
    }

    public float getInitWidth() {
        return mInitWidth;
    }

    public float getInitHeight() {
        return mInitHeight;
    }

    public float getScale() {
        return mInitScale;
    }

    public float getGestureScale() {
        return mGestureScale;
    }

    public void setGestureScale(float mGestureScale) {
        this.mGestureScale = mGestureScale;
    }
}
