package com.meitu.cropimagelibrary.model;

/**
 * Created by zmc on 2017/7/24.
 */

public abstract class TransFormTask {

    protected long startTime;
    protected long endTime;
    protected long duration;
    protected boolean isFinished = false;

    public static final int TRANSFORM_ROTATE = 1;
    public static final int TRANSFORM_TRANSLATE = 2;
    public static final int TRANSFORM_SCALE = 3;

    public TransFormTask(long duration) {
        startTime = System.currentTimeMillis();
        this.duration = duration;
        endTime = startTime + duration;
    }

    public boolean isFinish(){
        return isFinished;
    }

    public abstract int getTaskId();


}
