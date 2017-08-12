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
用法大概和ImageView相同，不过要保存图片要申请权限，，6.0以上要动态申请