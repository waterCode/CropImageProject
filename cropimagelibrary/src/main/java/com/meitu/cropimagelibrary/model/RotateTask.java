package com.meitu.cropimagelibrary.model;

/**
 * Created by zmc on 2017/7/24.
 */

public class RotateTask extends TransFormTask{

    private float mAngel;
    private float hadRotatedAngel;
    private float mCenterX;
    private float mCenterY;

    public RotateTask(long duration, float angel, float centerX, float centerY) {
        super(duration);
        mAngel = angel;
        mCenterX =centerX;
        mCenterY = centerY;
    }



    public float getAngel() {
        init();

        long dTime = System.currentTimeMillis()-startTime;
        float angel = mAngel*dTime/duration;//根据当前时间来判定需要多少angel
        if(Math.abs(angel)>Math.abs(mAngel)){
            angel=getLastAngel();//拿到余下的角度
            isFinished=true;//则完成任务
        }else {
            angel = angel - hadRotatedAngel;//减去已经转过的角度，就是需要转过的就角度
        }
        hadRotatedAngel+=angel;
        return angel;
    }

    public float getLastAngel(){
        return mAngel - hadRotatedAngel;
    }

    public float getCenterX() {
        return mCenterX;
    }

    public float getCenterY() {
        return mCenterY;
    }

    @Override
    public int getTaskId() {
        return TRANSFORM_ROTATE;
    }
}
