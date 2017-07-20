package com.meitu.cropimagelibrary.util;

import android.app.Application;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

/**
 * Created by zmc on 2017/7/19.
 */

public class CropImageApplication extends Application {
    public static int mScreenWidth = 720;
    public static int mScreenHeight = 720;


    @Override
    public void onCreate() {
        super.onCreate();
        //获取到屏幕宽度
        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        Point p = new Point();
        Display defaultDisplay = wm.getDefaultDisplay();
        defaultDisplay.getSize(p);
        mScreenWidth = p.x;
        mScreenHeight = p.y;
    }
}
