package com.meitu.cropimagelibrary.model;

/**
 * Created by zmc on 2017/7/24.
 */

public class TranslateTask extends TransFormTask {

    private float mDx;
    private float mDy;

    TranslateParams mParams =new TranslateParams();
    TranslateParams mHadTraslate =new TranslateParams();

    public TranslateTask(long duration, float dx, float dy) {
        super(duration);
        mDx = dx;
        mDy = dy;
    }


    public TranslateParams getAngel() {

        long dTime = System.currentTimeMillis() - startTime;
        float ratio = dTime / duration;//根据当前时间来判定需要多少angel
        mParams.x = mDx * ratio;
        mParams.y = mDy * ratio;
        if (Math.abs(mParams.x) > Math.abs(mDx) &&Math.abs(mParams.y) > Math.abs(mDy)) {//两者都大才拿余下
            getLastAngel(mParams);//拿到余下的角度
            isFinished = true;//则完成任务
        } else {
            mParams.x = mDx - mHadTraslate.x;//减去已经转过的角度，就是需要转过的就角度
            mParams.y = mDy - mHadTraslate.y;
        }
        mHadTraslate.x += mParams.x;
        mHadTraslate.y += mParams.y;
        return mParams;
    }

    public void getLastAngel(TranslateParams mParams) {
        mParams.x = mDx - mHadTraslate.x;
        mParams.y = mDy - mHadTraslate.y;
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
