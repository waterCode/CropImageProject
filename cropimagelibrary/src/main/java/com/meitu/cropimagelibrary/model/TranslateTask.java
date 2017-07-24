package com.meitu.cropimagelibrary.model;

import android.util.Log;

/**
 * Created by zmc on 2017/7/24.
 */

public class TranslateTask extends TransFormTask {
    private static final String TAG = "TranslateTask";

    private float mALlDx;
    private float mAllDy;

    TranslateParams mParams = new TranslateParams();
    TranslateParams mHadTraslate = new TranslateParams();

    public TranslateTask(long duration, float dx, float dy) {
        super(duration);
        mALlDx = dx;
        mAllDy = dy;
    }


    public TranslateParams getTranslateParams() {

        float dTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "间隔间隔" + dTime + "动画时间duration" + duration);
        float ratio = (dTime / duration);//根据当前时间来判定需要多少angel
        Log.d(TAG, "时间ratio为" + ratio);
        mParams.x = (mALlDx * ratio);
        mParams.y = (mAllDy * ratio);
        if (Math.abs(mParams.x) >= Math.abs(mALlDx) && Math.abs(mParams.y) >= Math.abs(mAllDy)) {//两者都大才拿余下
            getLastTranslate(mParams);//拿到余下的角度
            isFinished = true;//则完成任务
        } else {
            Log.d(TAG, "获取位移1次");
            mParams.x = mParams.x - mHadTraslate.x;//减去已经转过的角度，就是需要转过的就角度
            mParams.y = mParams.y - mHadTraslate.y;
        }
        mHadTraslate.x += mParams.x;
        mHadTraslate.y += mParams.y;
        Log.d(TAG, "总共需要位移x:" + mALlDx + "y:" + mAllDy);
        Log.d(TAG, "已经位移x:" + mHadTraslate.x + "Y:" + mHadTraslate.y);
        return mParams;
    }

    public void getLastTranslate(TranslateParams mParams) {
        mParams.x = mALlDx - mHadTraslate.x;
        mParams.y = mAllDy - mHadTraslate.y;
    }


    @Override
    public int getTaskId() {
        return TRANSFORM_TRANSLATE;
    }

    public class TranslateParams {
        public float x;
        public float y;
    }
}
