package com.meitu.cropimagelibrary.model;

/**
 * Created by zmc on 2017/7/24.
 */

public abstract class TransFormTask {

    protected long startTime;
    protected long endTime;
    protected long duration;
    protected boolean isFinished = false;
    protected boolean hasStart = false;

    public static final int TRANSFORM_ROTATE = 1;
    public static final int TRANSFORM_TRANSLATE = 2;
    public static final int TRANSFORM_SCALE = 3;

    public TransFormTask(long duration) {
        this.duration = duration;

    }

    public boolean isFinish() {
        return isFinished;
    }

    protected void init() {
        if (!hasStart) {
            startTime = System.currentTimeMillis();
            endTime = startTime + duration;
            hasStart = true;
        }
    }

    public abstract int getTaskId();


}
