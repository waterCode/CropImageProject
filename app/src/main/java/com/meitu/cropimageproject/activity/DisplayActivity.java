package com.meitu.cropimageproject.activity;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.meitu.cropimagelibrary.util.FileUtil;
import com.meitu.cropimagelibrary.util.SaveBitmapCallback;
import com.meitu.cropimagelibrary.view.CropImageView;
import com.meitu.cropimageproject.R;

import java.io.File;


/**
 * Created by zmc on 2017/7/18.
 */

public class DisplayActivity extends AppCompatActivity {
    private Uri uri;
    private CropImageView mNeedCropView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_iamge_activity);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            //
            //finish();
        }

        mNeedCropView = (CropImageView) findViewById(R.id.crop_photo_civ);
        uri = getIntent().getParcelableExtra("uri");
        mNeedCropView.setImageURI(uri);

    }

    // TODO: 2017/7/27 线程 
    // TODO: 2017/7/27 删掉通知媒体库 final
    // TODO: 2017/7/27 Exif 方向
    // TODO: 2017/7/27 大尺寸图片oom ，不同阶段
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.set_mirror_image_bt:
                mNeedCropView.setHorizontalMirror();
                break;
            case R.id.crop_Image_bt:
                
                Bitmap bitmap = mNeedCropView.cropAndSaveImage();
                File parent = getDefaultDir();
                FileUtil.bitmapConvertToFile(this, bitmap, parent, new SaveBitmapCallback() {

                    @Override
                    public void onFailed() {

                    }
                });
                break;
            case R.id.rightRotate_bt:
                mNeedCropView.postAnyRotate(45);
                break;
            case R.id.leftRotate_bt:
                mNeedCropView.leftRotate90();
                break;
            case R.id.cancel_crop_activity_bt:
                finish();
            default:
                break;
        }
    }


    public File getDefaultDir() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "crop_iamge");
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }
}
