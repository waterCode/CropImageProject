package com.meitu.cropimagelibrary.model;

import android.util.Log;

/**
 * Created by zmc on 2017/7/25.
 */

public class ScaleTask extends TransFormTask {


    private static final String TAG = "ScaleTask";

    private float mALlSx;
    private float mAllSy;

    private ScaleParams mParams = new ScaleParams();
    private ScaleParams mCurrentTimeScaleGoal = new ScaleParams();
    private ScaleParams mHadScale = new ScaleParams();

    public ScaleTask(long duration, float sx, float sy, float centerX, float centerY) {
        super(duration);
        mALlSx = sx;//总共需要放大倍数,比如需要放大到1.5，这里就是0.5
        mAllSy = sy;
        mParams.centerX = centerX;
        mParams.centerY = centerY;//初始化

        mHadScale.x = 1;//初始值设为1
        mHadScale.y = 1;
    }


    public ScaleParams getScaleParams() {
        init();

        float dTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "间隔间隔" + dTime + "动画时间duration" + duration);
        float ratio = (dTime / duration);//根据当前时间来判定需要多少angel
        Log.d(TAG, "时间ratio为" + ratio);
        mCurrentTimeScaleGoal.x = (mALlSx - 1) * ratio + 1;//需要放大到的倍数
        mCurrentTimeScaleGoal.y = (mAllSy - 1) * ratio + 1;
        Log.d(TAG, "总共需要放大到x:" + mALlSx + "y:" + mAllSy);
        Log.d(TAG, "此时需要放大倍数到 X:" + mParams.x + " Y:" + mParams.y);
        Log.d(TAG, "已经放大倍数到x:" + mHadScale.x + "Y:" + mHadScale.y);
        if (Math.abs(mCurrentTimeScaleGoal.x) >= Math.abs(mALlSx) && Math.abs(mCurrentTimeScaleGoal.y) >= Math.abs(mAllSy)) {//两者都大才拿余下
            getLastScale(mParams);//拿到余下的角度
            isFinished = true;//则完成任务
        } else {
            Log.d(TAG, "获取放大1次");
            mParams.x = mCurrentTimeScaleGoal.x / mHadScale.x;//减去已经转过的角度，就是需要转过的就角度
            mParams.y = mCurrentTimeScaleGoal.y / mHadScale.y;
        }
        mHadScale.x = mCurrentTimeScaleGoal.x;
        mHadScale.y = mCurrentTimeScaleGoal.y;
        Log.d(TAG, "计算出需要放大倍数为 X:" + mParams.x + " Y:" + mParams.y);


        return mParams;
    }


    public void getLastScale(ScaleParams mParams) {
        mParams.x = mALlSx / mHadScale.x;
        mParams.y = mAllSy / mHadScale.y;
    }


    @Override
    public int getTaskId() {
        return TRANSFORM_SCALE;
    }

    public class ScaleParams {
        public float x;
        public float y;
        public float centerX;
        public float centerY;

    }
}
