GitHub同步
https://github.com/waterCode


动态图：
![image](https://github.com/waterCode/CropImageProject/blob/master/app/src/main/assets/ezgif.com-video-to-gif.gif)

用法：
```xml
<com.meitu.cropimagelibrary.view.CropImageView
        android:scaleType="matrix"
        android:id="@+id/crop_photo_civ"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"/>
```
```java
        CropImageView mNeedCropView = (CropImageView) findViewById(R.id.crop_photo_civ);
        uri = getIntent().getParcelableExtra("uri");
        mNeedCropView.setImageURI(uri);
```
用法大概和ImageView相同，不过要保存图片要申请权限，，6.0以上要动态申请,直接设置uri，我这里的uri是从图库里面选着返回的

其他public方法,
```java
        /**
         * 设置最小放大倍数
         *
         * @param MIN_SCALE 最小放大倍数
         */
        setMinScale(float MIN_SCALE)
        /**
         * 设置最小放大倍数
         *
         * @param MAX_SCALE 最大放大倍数
         */
        setMaxScale(float MAX_SCALE);
    
    
        /**
         * 放大设置开关
         *
         * @param mScaleEnable 是否开启
         */
        setScaleEnable(boolean mScaleEnable);
    
        /**
         * 旋转开关
         *
         * @param mRotateEnable 是否开启
         */
        setRotateEnable(boolean mRotateEnable);//设置是否开启旋转
           
        setHorizontalMirror();//设置是否开启水平镜像
        setVerticalMirror();//设置垂直镜像
        postAnyRotate(float anyAngel);//旋转图片任意角度， 
```